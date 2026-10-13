package io.github.halilozel1903.openlibrary

import io.github.halilozel1903.openlibrary.transport.OpenLibraryTransport
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/** Settings for [OpenLibraryClient], set in its builder block. */
public class OpenLibraryConfig internal constructor() {
    /** The API root. Change it for a mirror or a local Open Library instance. */
    public var baseUrl: String = OpenLibraryClient.DEFAULT_BASE_URL

    /**
     * Sent with every request. Open Library asks apps to identify themselves with a name and a way to contact
     * them, so heavy or broken traffic can be traced to you instead of blocked. Use something like
     * `"MyBookApp/2.1 (books@example.com)"`.
     */
    public var userAgent: String = OpenLibraryClient.DEFAULT_USER_AGENT

    /**
     * The client spaces its network requests so no more than this many start per second. Responses served from the
     * cache do not count. `0` or less turns the limit off. The default keeps a single app well inside Open
     * Library's limits.
     */
    public var maxRequestsPerSecond: Double = 3.0

    /**
     * The Ktor engine. `null` uses CIO. Pass `OkHttp.create()` (add `io.ktor:ktor-client-okhttp`) to share your
     * app's OkHttp stack, or a `MockEngine` in tests. The client does not close an engine you pass.
     */
    public var engine: HttpClientEngine? = null

    /** A ready Ktor client to use as is, instead of [engine]. [requestTimeoutMillis] and [retries] then do not apply. */
    public var httpClient: HttpClient? = null

    /** Replaces HTTP entirely, for example to serve JSON files in tests or previews. */
    public var transport: OpenLibraryTransport? = null

    /** How many responses the in-memory cache keeps. `0` turns caching off. */
    public var cacheSize: Int = 200

    /** How long a cached response is used. */
    public var cacheTtl: Duration = 10.minutes

    /** Timeout of one request, in milliseconds. Open Library can be slow under load. */
    public var requestTimeoutMillis: Long = 30_000

    /** Retries on network errors, 429 and 5xx responses, with exponential backoff that honors `Retry-After`. */
    public var retries: Int = 2
}
