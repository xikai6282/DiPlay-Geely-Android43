package com.shilapi.xcertplay.mfi

import com.shilapi.xcertplay.transport.I2cTransport
import com.shilapi.xcertplay.transport.I2cTransportException

/** Transport-independent MFi presence check for the documented address candidates. */
class MfiSelfCheck(
    private val transport: I2cTransport,
) {
    fun run(): MfiSelfCheckResult {
        val discovery = MfiDeviceScanner(transport).scan()
        val chip = discovery.chip ?: return MfiSelfCheckResult(discovery, null)
        return MfiSelfCheckResult(
            discovery = discovery,
            chip = MfiSelfCheckChip(
                address7Bit = chip.address7Bit,
                deviceVersion = chip.deviceVersion,
                protocolMajor = try {
                    MfiProtocolMajorResult.Value(
                        chip.address7Bit,
                        MfiAuthenticationClient(transport, chip.address7Bit).protocolMajor(),
                    )
                } catch (error: MfiException) {
                    MfiProtocolMajorResult.MfiFailure(chip.address7Bit, error)
                } catch (error: I2cTransportException) {
                    MfiProtocolMajorResult.TransportFailure(chip.address7Bit, error)
                },
            ),
        )
    }
}

data class MfiSelfCheckResult(
    val discovery: MfiDiscoveryResult,
    val chip: MfiSelfCheckChip?,
)

data class MfiSelfCheckChip(
    val address7Bit: Int,
    val deviceVersion: Int,
    val protocolMajor: MfiProtocolMajorResult,
)

sealed class MfiProtocolMajorResult {
    abstract val address7Bit: Int

    data class Value(override val address7Bit: Int, val major: Int) : MfiProtocolMajorResult()

    data class MfiFailure(override val address7Bit: Int, val error: MfiException) : MfiProtocolMajorResult()

    data class TransportFailure(
        override val address7Bit: Int,
        val error: I2cTransportException,
    ) : MfiProtocolMajorResult()
}
