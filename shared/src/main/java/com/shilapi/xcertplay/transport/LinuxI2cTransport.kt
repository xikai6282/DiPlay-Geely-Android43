package com.shilapi.xcertplay.transport

import java.io.Closeable
import java.io.IOException

/**
 * I2C transport for a board-provided Linux `/dev/i2c-N` device node.
 *
 * The target build must grant this app access to the node. Android SELinux policy and Unix device
 * permissions are not changed or bypassed here. All methods block and must not run on the main
 * thread.
 */
class LinuxI2cTransport private constructor(
    private val bridge: LinuxI2cBridge,
    private val handle: Int,
) : I2cTransport, Closeable {
    private val lock = Any()
    private var closed = false

    override fun transaction(address7Bit: Int, writeData: ByteArray, readLength: Int): ByteArray =
        synchronized(lock) {
            checkOpen()
            validateTransaction(address7Bit, writeData, readLength)

            try {
                bridge.transaction(handle, address7Bit, writeData, readLength).also { response ->
                    if (response.size != readLength) {
                        throw I2cTransportException.Protocol(
                            "I2C backend returned ${response.size} bytes, expected $readLength",
                        )
                    }
                }
            } catch (error: LinuxI2cNativeException) {
                throw mapNativeError(error)
            }
        }

    override fun close() {
        synchronized(lock) {
            if (closed) return
            closed = true
            try {
                bridge.close(handle)
            } catch (error: LinuxI2cNativeException) {
                throw mapNativeError(error)
            }
        }
    }

    private fun checkOpen() {
        if (closed) {
            throw I2cTransportException.DeviceUnavailable("Linux I2C transport is closed")
        }
    }

    companion object {
        private const val MAX_MESSAGE_LENGTH = 0xffff
        private val I2C_DEVICE_PATH = Regex("/dev/i2c-[0-9]+")

        /** Opens a Linux I2C device node. */
        @Throws(I2cTransportException::class)
        fun open(devicePath: String): LinuxI2cTransport = open(devicePath, NativeLinuxI2cBridge)

        internal fun open(devicePath: String, bridge: LinuxI2cBridge): LinuxI2cTransport {
            if (!I2C_DEVICE_PATH.matches(devicePath)) {
                throw I2cTransportException.InvalidRequest(
                    "I2C device path must be /dev/i2c-N: $devicePath",
                )
            }

            return try {
                LinuxI2cTransport(bridge, bridge.open(devicePath))
            } catch (error: LinuxI2cNativeException) {
                throw mapNativeError(error)
            }
        }

        private fun validateTransaction(address7Bit: Int, writeData: ByteArray, readLength: Int) {
            if (address7Bit !in 0..0x7f) {
                throw I2cTransportException.InvalidRequest(
                    "I2C address must be 7-bit: 0x${address7Bit.toString(16)}",
                )
            }
            if (readLength !in 0..MAX_MESSAGE_LENGTH || writeData.size > MAX_MESSAGE_LENGTH) {
                throw I2cTransportException.InvalidRequest(
                    "I2C message length must be at most $MAX_MESSAGE_LENGTH bytes",
                )
            }
            if (writeData.isEmpty() && readLength == 0) {
                throw I2cTransportException.InvalidRequest("I2C transaction must read or write data")
            }
        }

        private fun mapNativeError(error: LinuxI2cNativeException): I2cTransportException =
            when (error.errnoCode) {
                1, 13 -> I2cTransportException.PermissionDenied(error.message ?: "I2C permission denied")
                110 -> I2cTransportException.TimedOut(error.message ?: "I2C transaction timed out", error)
                121 -> I2cTransportException.Nack(error.message ?: "I2C remote NACK")
                2, 6, 9, 16, 19 -> I2cTransportException.DeviceUnavailable(
                    error.message ?: "I2C device unavailable",
                    error,
                )
                else -> I2cTransportException.Protocol(error.message ?: "I2C native operation failed", error)
            }
    }
}

internal interface LinuxI2cBridge {
    fun open(devicePath: String): Int
    fun transaction(handle: Int, address7Bit: Int, writeData: ByteArray, readLength: Int): ByteArray
    fun close(handle: Int)
}

internal class LinuxI2cNativeException(
    val errnoCode: Int,
    message: String,
) : IOException(message)

private object NativeLinuxI2cBridge : LinuxI2cBridge {
    init {
        System.loadLibrary("xcertplay_i2c")
    }

    override fun open(devicePath: String): Int = LinuxI2cNative.open(devicePath)

    override fun transaction(
        handle: Int,
        address7Bit: Int,
        writeData: ByteArray,
        readLength: Int,
    ): ByteArray = LinuxI2cNative.transaction(handle, address7Bit, writeData, readLength)

    override fun close(handle: Int) = LinuxI2cNative.close(handle)
}

private object LinuxI2cNative {
    external fun open(devicePath: String): Int
    external fun transaction(
        handle: Int,
        address7Bit: Int,
        writeData: ByteArray,
        readLength: Int,
    ): ByteArray
    external fun close(handle: Int)
}
