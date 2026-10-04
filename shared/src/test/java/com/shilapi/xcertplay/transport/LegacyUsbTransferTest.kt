package com.shilapi.xcertplay.transport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Chunking, short writes and deadline behaviour, driven through the injectable transfer callback so
 * no USB hardware is involved. Every write chunk is checked against the bytes actually present at
 * that offset, so a helper that re-sent offset 0 could not pass.
 */
class LegacyUsbTransferTest {

    private var now = 0L

    private fun withClock(body: () -> Unit) {
        val previous = LegacyUsbTransfer.clock
        LegacyUsbTransfer.clock = { now }
        try {
            body()
        } finally {
            LegacyUsbTransfer.clock = previous
        }
    }

    private fun deadline(millis: Long) = now + millis * 1_000_000L

    /** A buffer whose bytes encode their own index, so a wrong offset is detectable. */
    private fun pattern(size: Int, seed: Int = 1) = ByteArray(size) { ((it + seed) and 0xFF).toByte() }

    @Test fun readRequestsOneChunkAndNeverExceedsCapacity() {
        withClock {
            val calls = mutableListOf<Pair<Int, Int>>()
            val transfer = LegacyUsbTransfer.ChunkTransfer { offset, length, _ ->
                calls += offset to length
                length
            }
            val target = ByteArray(64 * 1024)
            val read = LegacyUsbTransfer.readOnce(transfer, target, 0, target.size, 1_000)
            // A frame larger than 16 KB must not make the caller wait for a second chunk.
            assertEquals(LegacyUsbTransfer.MAX_CHUNK_BYTES, read)
            assertEquals(listOf(0 to LegacyUsbTransfer.MAX_CHUNK_BYTES), calls)
        }
    }

    @Test fun readHonoursANonZeroTargetOffset() {
        withClock {
            var seen = -1
            val transfer = LegacyUsbTransfer.ChunkTransfer { offset, _, _ -> seen = offset; 8 }
            LegacyUsbTransfer.readOnce(transfer, ByteArray(64), 16, 32, 100)
            assertEquals(0, seen)
        }
    }

    @Test fun readReportsZeroAndNegativeWithoutInventingData() {
        withClock {
            val empty = LegacyUsbTransfer.ChunkTransfer { _, _, _ -> 0 }
            assertEquals(0, LegacyUsbTransfer.readOnce(empty, ByteArray(32), 0, 32, 50))

            val failed = LegacyUsbTransfer.ChunkTransfer { _, _, _ -> -1 }
            assertEquals(-1, LegacyUsbTransfer.readOnce(failed, ByteArray(32), 0, 32, 50))
        }
    }

    @Test fun readRejectsNonPositiveTimeoutAndOutOfRangeBuffers() {
        withClock {
            val transfer = LegacyUsbTransfer.ChunkTransfer { _, length, _ -> length }
            val target = ByteArray(32)
            for (block in listOf<() -> Unit>(
                { LegacyUsbTransfer.readOnce(transfer, target, 0, 32, 0) },
                { LegacyUsbTransfer.readOnce(transfer, target, -1, 32, 10) },
                { LegacyUsbTransfer.readOnce(transfer, target, 0, 0, 10) },
                { LegacyUsbTransfer.readOnce(transfer, target, 16, 32, 10) },
            )) {
                try {
                    block()
                    fail("expected an IllegalArgumentException")
                } catch (_: IllegalArgumentException) {
                    // Expected.
                }
            }
        }
    }

    @Test fun writeSplitsALongFrameAndWalksEveryByteInOrder() {
        withClock {
            val source = pattern(32 * 1024)
            val seen = mutableListOf<IntRange>()
            val transfer = LegacyUsbTransfer.ChunkTransfer { offset, length, _ ->
                seen.add(offset until (offset + length))
                // Each chunk must carry the source's own bytes at that offset.
                for (i in 0 until length) {
                    if (source[offset + i] != ((offset + i + 1) and 0xFF).toByte()) {
                        fail("chunk content mismatch at ${offset + i}")
                    }
                }
                length
            }
            val total = LegacyUsbTransfer.writeAll(transfer, 0, source.size, deadline(5_000))
            assertEquals(32 * 1024, total)
            assertEquals(2, seen.size)
            // Contiguous, ordered, nothing repeated or skipped.
            assertEquals(0, seen[0].first)
            assertEquals(seen[0].last + 1, seen[1].first)
            assertEquals((0 until source.size).toList(), seen.flatten())
        }
    }

    @Test fun shortWriteAdvancesSoTheWholeFrameIsSent() {
        withClock {
            val source = pattern(500, seed = 7)
            val responses = ArrayDeque(listOf(100, 60, Int.MAX_VALUE))
            val covered = ArrayList<IntRange>()
            val transfer = LegacyUsbTransfer.ChunkTransfer { offset, length, _ ->
                val next = responses.removeFirst()
                val accepted = if (next == Int.MAX_VALUE) length else minOf(next, length)
                covered.add(offset until (offset + accepted))
                for (i in 0 until accepted) {
                    if (source[offset + i] != ((offset + i + 7) and 0xFF).toByte()) {
                        fail("chunk content mismatch at ${offset + i}")
                    }
                }
                accepted
            }
            val total = LegacyUsbTransfer.writeAll(transfer, 0, 500, deadline(5_000))
            assertEquals(500, total)
            assertEquals(3, covered.size)
            assertEquals((0 until 500).toList(), covered.flatten())
        }
    }

    @Test fun writeReportsWhatWasTransferredWhenTheDeviceStops() {
        withClock {
            val responses = ArrayDeque(listOf(100, 0))
            val transfer = LegacyUsbTransfer.ChunkTransfer { _, length, _ ->
                val next = responses.removeFirst()
                if (next == 0) 0 else minOf(next, length)
            }
            assertEquals(100, LegacyUsbTransfer.writeAll(transfer, 0, 500, deadline(5_000)))
        }
    }

    @Test fun writeStopsWhenTheDeadlineHasPassed() {
        withClock {
            var calls = 0
            val transfer = LegacyUsbTransfer.ChunkTransfer { _, length, _ -> calls++; length }
            assertEquals(0, LegacyUsbTransfer.writeAll(transfer, 0, 64 * 1024, deadline(-1)))
            assertEquals(0, calls)
        }
    }

    @Test fun theTotalDeadlineIsSharedAndShrinksAcrossChunks() {
        withClock {
            val budgets = mutableListOf<Int>()
            val transfer = LegacyUsbTransfer.ChunkTransfer { _, length, timeoutMillis ->
                budgets += timeoutMillis
                // Each chunk consumes real time, so the next one must see less budget.
                now += 400_000_000L
                length
            }
            val total = LegacyUsbTransfer.writeAll(transfer, 0, 64 * 1024, deadline(10_000))
            assertEquals(64 * 1024, total)
            assertEquals(4, budgets.size)
            assertTrue("every chunk needs a positive budget", budgets.all { it > 0 })
            assertTrue("budget must shrink", budgets.zipWithNext().all { (a, b) -> b < a })
        }
    }

    @Test fun writeStopsOnceTheSharedDeadlineIsExhausted() {
        withClock {
            var calls = 0
            val transfer = LegacyUsbTransfer.ChunkTransfer { _, length, _ ->
                calls++
                now += 2_000_000_000L // 2s per chunk against a 3s budget
                length
            }
            val total = LegacyUsbTransfer.writeAll(transfer, 0, 256 * 1024, deadline(3_000))
            assertTrue("must not spin past the deadline", calls < 16)
            assertTrue(total < 256 * 1024)
        }
    }
}