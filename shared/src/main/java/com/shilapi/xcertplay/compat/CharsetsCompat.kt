package com.shilapi.xcertplay.compat

import java.nio.charset.Charset

/** StandardCharsets was added in API 19; these names work on Android 4.3. */
object CharsetsCompat {
    val UTF_8: Charset = Charset.forName("UTF-8")
    val US_ASCII: Charset = Charset.forName("US-ASCII")
}
