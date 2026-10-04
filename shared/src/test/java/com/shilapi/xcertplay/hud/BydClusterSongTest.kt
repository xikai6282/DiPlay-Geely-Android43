package com.shilapi.xcertplay.hud

import com.shilapi.xcertplay.iap2.message.Iap2Messages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BydClusterSongTest {
    private fun update(block: com.shilapi.xcertplay.iap2.body.Iap2BodyBuilder.() -> Unit) =
        Iap2Messages.buildRaw(ClusterSongState.NOW_PLAYING_UPDATE, block)

    @Test
    fun followsTitleArtistAndPlaybackStatus() {
        val state = ClusterSongState()

        assertEquals(ClusterSong("Numb — Linkin Park", false),
            state.accept(update { group(0) { string(1, "Numb"); string(12, "Linkin Park") } }))
        assertEquals(ClusterSong("Numb — Linkin Park", true), state.accept(update { group(1) { u8(0, 1) } }))
        // Elapsed time alone changes nothing on the card.
        assertNull(state.accept(update { group(1) { u32(1, 120_706L) } }))
        assertEquals(ClusterSong("Numb — Linkin Park", false), state.accept(update { group(1) { u8(0, 2) } }))
        // A new title without an artist is a new item that has none.
        assertEquals(ClusterSong("Podcast", false), state.accept(update { group(0) { string(1, "Podcast") } }))
        assertEquals(ClusterSong("Podcast — Host", false), state.accept(update { group(0) { string(12, "Host") } }))
    }

    @Test
    fun nothingWithoutATitleOrForOtherMessages() {
        val state = ClusterSongState()
        assertNull(state.accept(update { group(1) { u8(0, 1) } }))
        assertNull(state.accept(Iap2Messages.buildRaw(0x5201) { group(0) { string(1, "Numb") } }))
        assertNull(state.current())

        state.accept(update { group(0) { string(1, "Numb") } })
        state.clear()
        assertNull(state.current())
    }

    @Test
    fun clearedTitlesForgetThePreviousSongUntilANewTitleArrives() {
        val state = ClusterSongState()
        state.accept(update { group(0) { string(1, "Previous song"); string(12, "Artist") } })
        state.accept(update { group(0) { string(1, "") } })
        assertNull(state.current())
        state.accept(update { group(1) { u8(0, 1) } })
        assertNull(state.current())
        assertEquals(ClusterSong("Next song", true),
            state.accept(update { group(0) { string(1, "Next song") } }))
        state.accept(update { group(0) { string(1, "  ") } })
        assertNull(state.current())
    }

    @Test
    fun textFitsTheDashboard() {
        assertNull(ClusterSongState.text("  ", "Artist"))
        assertEquals("Title", ClusterSongState.text(" Title ", ""))

        val long = ClusterSongState.text("Пісня".repeat(40), "Виконавець")!!
        assertTrue(long.toByteArray(Charsets.UTF_16LE).size <= ClusterSongState.MAX_TEXT_BYTES)
        assertEquals(127, long.length)

        // An emoji is never cut in half.
        val emoji = ClusterSongState.text("a" + "🎵".repeat(100), null)!!
        assertTrue(emoji.toByteArray(Charsets.UTF_16LE).size <= ClusterSongState.MAX_TEXT_BYTES)
        assertTrue(!Character.isHighSurrogate(emoji.last()))
    }
}
