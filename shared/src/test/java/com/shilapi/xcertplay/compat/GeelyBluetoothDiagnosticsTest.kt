package com.shilapi.xcertplay.compat

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class GeelyBluetoothDiagnosticsTest {
    @Test fun anwGettersUse10500DescriptorCodesAndExactOutputCapacities() {
        val binder = FakeAnwBinder(power = 1, pairedCount = 2, spp = 1)
        val result = GeelyBluetoothReadOnlyProtocol.read(binder)

        assertEquals(BindingState.CONNECTED, result.bindingState)
        assertEquals(AnwPowerState.ON, result.power.value)
        assertEquals(2, result.pairedCount.value)
        assertEquals(true, result.sppInitialized.value)
        assertEquals(listOf(0x03, 0x10, 0x43), binder.codes)
        assertEquals(listOf(1, 16, 16, 16), binder.pairedCapacities)
        assertFalse(binder.protocolError)
    }

    @Test fun unsupportedTransactionIsNotReportedAsOffOrFalse() {
        val binder = FakeAnwBinder(power = 0, pairedCount = 0, spp = 0, unsupportedCode = 0x03)
        val result = GeelyBluetoothReadOnlyProtocol.read(binder)
        assertEquals(ReadState.UNSUPPORTED, result.power.state)
        assertNull(result.power.value)
        assertEquals(ReadState.OK, result.sppInitialized.state)
        assertEquals(false, result.sppInitialized.value)
    }

    @Test fun unknownPowerCodeAndTruncatedRepliesRemainUnknown() {
        val unknown = GeelyBluetoothReadOnlyProtocol.read(FakeAnwBinder(power = 9, pairedCount = 0, spp = 0))
        assertEquals(ReadState.FAILED, unknown.power.state)
        assertEquals(ReadFailure.INVALID_RESPONSE, unknown.power.failure)

        val truncated = GeelyBluetoothReadOnlyProtocol.read(
            FakeAnwBinder(power = 0, pairedCount = 0, spp = 0, emptyReplyCodes = setOf(0x03, 0x43)),
        )
        assertEquals(ReadState.FAILED, truncated.power.state)
        assertEquals(ReadState.FAILED, truncated.sppInitialized.state)
        assertNull(truncated.power.value)
        assertNull(truncated.sppInitialized.value)
    }

    @Test fun malformedPairedArrayCapacitiesAndReturnCodeDoNotBecomeEmptyList() {
        val wrongCapacities = GeelyBluetoothReadOnlyProtocol.read(
            FakeAnwBinder(power = 1, pairedCount = 0, spp = 0, wrongPairedReplyLength = true),
        )
        assertEquals(ReadState.FAILED, wrongCapacities.pairedCount.state)
        assertNull(wrongCapacities.pairedCount.value)

        val failedReturn = GeelyBluetoothReadOnlyProtocol.read(
            FakeAnwBinder(power = 1, pairedCount = 0, spp = 0, pairedReturnCode = 0),
        )
        assertEquals(ReadState.FAILED, failedReturn.pairedCount.state)
        assertNull(failedReturn.pairedCount.value)
    }

    @Test fun wrongAnwDescriptorStopsBeforeAnyTransaction() {
        val binder = FakeAnwBinder(power = 1, pairedCount = 0, spp = 0, descriptor = "other.IPhoneLink")
        val result = GeelyBluetoothReadOnlyProtocol.read(binder)
        assertEquals(BindingState.INVALID_BINDER, result.bindingState)
        assertTrue(binder.codes.isEmpty())
        assertEquals(ReadFailure.DESCRIPTOR_MISMATCH, result.power.failure)
    }

    @Test fun ecarxGetterUsesItsOwnDescriptorAndTransactionAndRejectsNonBooleanValues() {
        val binder = FakeEcarxBinder(value = 0)
        assertEquals(ReadValue(ReadState.OK, false), EcarxBluetoothReadOnlyProtocol.readBinder(binder))
        assertEquals(listOf(0x05), binder.codes)
        assertFalse(binder.protocolError)

        val malformed = FakeEcarxBinder(value = 7)
        assertEquals(ReadFailure.INVALID_RESPONSE, EcarxBluetoothReadOnlyProtocol.readBinder(malformed).failure)

        val wrongDescriptor = FakeEcarxBinder(value = 1, descriptor = "com.anwsdk.service.IAnwPhoneLink")
        assertEquals(ReadFailure.DESCRIPTOR_MISMATCH, EcarxBluetoothReadOnlyProtocol.readBinder(wrongDescriptor).failure)
        assertTrue(wrongDescriptor.codes.isEmpty())

        val truncated = FakeEcarxBinder(value = 0, emptyReply = true)
        assertEquals(ReadFailure.INVALID_RESPONSE, EcarxBluetoothReadOnlyProtocol.readBinder(truncated).failure)
    }

    @Test fun bindTimeoutUnbindsAndLateServiceCallbackCannotStartBinderReads() {
        val binding = FakeBinding()
        val client = client(binding)
        var result: GeelyBluetoothSnapshot? = null
        client.query(timeoutMillis = 20) { result = it }
        mainLooper().idle()
        assertTrue(binding.bindCalled)
        mainLooper().idleFor(25, TimeUnit.MILLISECONDS)

        assertEquals(BindingState.BIND_TIMEOUT, result?.bindingState)
        assertEquals(1, binding.unbindCount)
        val late = FakeAnwBinder(power = 1, pairedCount = 0, spp = 0)
        binding.connection!!.onServiceConnected(GeelyBluetoothDiagnostics.SERVICE_COMPONENT, late)
        mainLooper().idle()
        assertTrue(late.codes.isEmpty())
    }

    @Test fun serviceDisconnectProducesUnknownAnwStateAndIndependentEcarxRead() {
        val binding = FakeBinding()
        val client = client(binding, ecarxReader = { ReadValue(ReadState.OK, false) })
        var result: GeelyBluetoothSnapshot? = null
        client.query { result = it }
        mainLooper().idle()
        binding.connection!!.onServiceDisconnected(GeelyBluetoothDiagnostics.SERVICE_COMPONENT)
        mainLooper().idle()

        assertEquals(BindingState.DISCONNECTED, result?.bindingState)
        assertEquals(ReadState.FAILED, result?.power?.state)
        assertEquals(ReadState.OK, result?.ecarxEnabled?.state)
        assertEquals(false, result?.ecarxEnabled?.value)
        assertEquals(1, binding.unbindCount)
    }

    @Test fun disconnectDuringBinderReadMarksAnwUnknownButKeepsEcarxResultSeparate() {
        val binding = FakeBinding()
        val executor = ManualExecutor()
        val client = client(binding, executor) { ReadValue(ReadState.OK, true) }
        var result: GeelyBluetoothSnapshot? = null
        client.query { result = it }
        mainLooper().idle()
        val binder = FakeAnwBinder(power = 1, pairedCount = 3, spp = 1)
        binding.connection!!.onServiceConnected(GeelyBluetoothDiagnostics.SERVICE_COMPONENT, binder)
        mainLooper().idle()
        assertTrue(executor.task != null)
        binding.connection!!.onServiceDisconnected(GeelyBluetoothDiagnostics.SERVICE_COMPONENT)
        mainLooper().idle()
        executor.runPending()
        mainLooper().idle()

        assertEquals(BindingState.DISCONNECTED, result?.bindingState)
        assertEquals(ReadState.FAILED, result?.power?.state)
        assertEquals(ReadState.FAILED, result?.pairedCount?.state)
        assertEquals(ReadState.FAILED, result?.sppInitialized?.state)
        assertEquals(ReadState.OK, result?.ecarxEnabled?.state)
        assertEquals(true, result?.ecarxEnabled?.value)
    }

    @Test fun busyCallbackCanBeCancelledBeforeMainQueueDelivery() {
        val binding = FakeBinding()
        val client = client(binding)
        val first = client.query { }
        mainLooper().idle()
        var called = false
        val second = client.query { called = true }
        second.cancel()
        mainLooper().idle()
        assertFalse(called)
        first.cancel()
        mainLooper().idle()
    }

    private fun client(
        binding: FakeBinding,
        executor: Executor = Executor { it.run() },
        ecarxReader: () -> ReadValue<Boolean> = { ReadValue(ReadState.FAILED, failure = ReadFailure.SERVICE_UNAVAILABLE) },
    ) = GeelyBluetoothDiagnostics(
        RuntimeEnvironment.getApplication(),
        binding,
        executor,
        android.os.Handler(Looper.getMainLooper()),
        ecarxReader,
    )

    private fun mainLooper() = shadowOf(Looper.getMainLooper())

    private class ManualExecutor : Executor {
        var task: Runnable? = null
        override fun execute(command: Runnable) { task = command }
        fun runPending() { task?.run(); task = null }
    }

    private class FakeBinding : GeelyBluetoothDiagnostics.ServiceBinding {
        var bindCalled = false
        var unbindCount = 0
        var connection: ServiceConnection? = null

        override fun bind(context: Context, intent: Intent, connection: ServiceConnection): Boolean {
            bindCalled = true
            assertEquals(GeelyBluetoothDiagnostics.SERVICE_COMPONENT, intent.component)
            this.connection = connection
            return true
        }

        override fun unbind(context: Context, connection: ServiceConnection) {
            unbindCount++
        }
    }

    private class FakeAnwBinder(
        private val power: Int,
        private val pairedCount: Int,
        private val spp: Int,
        private val descriptor: String = GeelyBluetoothReadOnlyProtocol.DESCRIPTOR,
        private val unsupportedCode: Int? = null,
        private val emptyReplyCodes: Set<Int> = emptySet(),
        private val wrongPairedReplyLength: Boolean = false,
        private val pairedReturnCode: Int = 1,
    ) : Binder() {
        val codes = mutableListOf<Int>()
        var pairedCapacities = emptyList<Int>()
        var protocolError = false

        init { attachInterface(null, descriptor) }

        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            val output = requireNotNull(reply)
            if (code == INTERFACE_TRANSACTION) {
                output.writeString(descriptor)
                return true
            }
            if (code == unsupportedCode) return false
            codes += code
            data.enforceInterface(descriptor)
            if (emptyReplyCodes.contains(code)) return true
            output.writeNoException()
            when (code) {
                0x03 -> output.writeInt(power)
                0x10 -> {
                    val capacities = List(4) { data.readInt() }
                    pairedCapacities = capacities
                    protocolError = data.dataAvail() != 0 || capacities != listOf(1, 16, 16, 16)
                    if (protocolError) return false
                    output.writeInt(pairedReturnCode)
                    if (pairedReturnCode == 1) {
                        output.writeIntArray(if (wrongPairedReplyLength) intArrayOf(pairedCount, 1) else intArrayOf(pairedCount))
                        output.writeStringArray(Array(16) { if (it == 0) "sample phone" else "" })
                        output.writeStringArray(Array(16) { if (it == 0) "02:00:00:00:00:01" else "" })
                        output.writeIntArray(IntArray(16))
                    }
                }
                0x43 -> output.writeInt(spp)
                else -> return false
            }
            return true
        }
    }

    private class FakeEcarxBinder(
        private val value: Int,
        private val descriptor: String = EcarxBluetoothReadOnlyProtocol.DESCRIPTOR,
        private val emptyReply: Boolean = false,
    ) : Binder() {
        val codes = mutableListOf<Int>()
        var protocolError = false

        init { attachInterface(null, descriptor) }

        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            val output = requireNotNull(reply)
            if (code == INTERFACE_TRANSACTION) {
                output.writeString(descriptor)
                return true
            }
            codes += code
            data.enforceInterface(descriptor)
            protocolError = code != 0x05 || data.dataAvail() != 0
            if (protocolError) return false
            if (emptyReply) return true
            output.writeNoException()
            output.writeInt(value)
            return true
        }
    }
}
