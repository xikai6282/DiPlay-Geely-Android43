package com.shilapi.xcertplay.compat

import org.bouncycastle.util.encoders.Base64 as BcBase64

/** java.util.Base64 is API 26; BouncyCastle is already bundled and supports Android 4.3. */
object Base64Compat {
    fun encodeToString(data: ByteArray): String = BcBase64.toBase64String(data)
    fun decode(encoded: String): ByteArray = BcBase64.decode(encoded)
    fun decodeMime(encoded: ByteArray): ByteArray = BcBase64.decode(String(encoded, CharsetsCompat.US_ASCII))

    /** Mirrors the MIME encoder's 64-character lines and caller-provided separator. */
    fun encodeMimeLines(data: ByteArray, lineSeparator: ByteArray): String =
        BcBase64.toBase64String(data).chunked(64)
            .joinToString(String(lineSeparator, CharsetsCompat.US_ASCII))
}
