package com.lahacks2026.pretriage.privacy

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Bidirectional placeholder ↔ original-value map. Survives across the de-id round-trip
 * so we can re-identify the response before showing it to the user.
 *
 * Privacy invariant: this map MUST never leave the device. Encryption-at-rest uses
 * `MasterKey` + `EncryptedSharedPreferences` in production builds; the demo uses an
 * in-memory variant since the session dies with the activity anyway.
 */
interface PhiTokenMap {
    fun put(category: String, original: String): String
    fun resolve(text: String): String
    fun snapshot(): Map<String, String>
    fun clear()
}

class InMemoryPhiTokenMap : PhiTokenMap {
    private val byPlaceholder = ConcurrentHashMap<String, String>()
    private val byOriginal = ConcurrentHashMap<String, String>()
    private val counters = ConcurrentHashMap<String, AtomicInteger>()

    override fun put(category: String, original: String): String {
        byOriginal[original]?.let { return it }
        val n = counters.computeIfAbsent(category) { AtomicInteger(0) }.incrementAndGet()
        val placeholder = "[${category}_$n]"
        byPlaceholder[placeholder] = original
        byOriginal[original] = placeholder
        return placeholder
    }

    override fun resolve(text: String): String {
        var out = text
        for ((placeholder, original) in byPlaceholder) {
            out = out.replace(placeholder, original)
        }
        return out
    }

    override fun snapshot(): Map<String, String> = byPlaceholder.toMap()

    override fun clear() {
        byPlaceholder.clear()
        byOriginal.clear()
        counters.clear()
    }
}
