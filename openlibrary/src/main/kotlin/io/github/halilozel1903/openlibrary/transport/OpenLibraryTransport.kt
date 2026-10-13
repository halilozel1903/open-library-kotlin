package io.github.halilozel1903.openlibrary.transport

/**
 * Fetches the body of an Open Library url. The client uses a Ktor based transport by default; supply your own
 * through `OpenLibraryConfig.transport` to serve responses from files, a database or another HTTP stack.
 *
 * Throw [io.github.halilozel1903.openlibrary.OpenLibraryException.NotFound] for missing records; other exceptions
 * are reported as [io.github.halilozel1903.openlibrary.OpenLibraryException.Network].
 */
public fun interface OpenLibraryTransport {
    public suspend fun get(url: String): String
}
