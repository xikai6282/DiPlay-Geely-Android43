package com.shilapi.xcertplay.compat

import java.util.concurrent.ConcurrentHashMap

/** ConcurrentHashMap.computeIfAbsent was added to Android much later than Java 7. */
fun <K : Any, V : Any> ConcurrentHashMap<K, V>.getOrPutCompat(key: K, create: () -> V): V = synchronized(this) {
    get(key) ?: create().also { put(key, it) }
}
