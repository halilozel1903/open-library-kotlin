package io.github.halilozel1903.openlibrary.internal

/** A small thread safe LRU cache of response bodies by url, with a time to live. */
internal class ResponseCache(
    private val maxEntries: Int,
    private val ttlMillis: Long,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private class Entry(val body: String, val storedAt: Long)

    private val map = object : LinkedHashMap<String, Entry>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>?): Boolean = size > maxEntries
    }

    val enabled: Boolean get() = maxEntries > 0

    @Synchronized
    fun get(url: String): String? {
        val entry = map[url] ?: return null
        if (now() - entry.storedAt > ttlMillis) {
            map.remove(url)
            return null
        }
        return entry.body
    }

    @Synchronized
    fun put(url: String, body: String) {
        if (enabled) map[url] = Entry(body, now())
    }

    @Synchronized
    fun remove(url: String) {
        map.remove(url)
    }

    @Synchronized
    fun clear() {
        map.clear()
    }

    @get:Synchronized
    val size: Int get() = map.size
}
