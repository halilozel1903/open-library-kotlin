package io.github.halilozel1903.openlibrary

/** Every failure of [OpenLibraryClient]. Catch the subclasses you care about, or all of them. */
public sealed class OpenLibraryException(
    /** The requested url. */
    public val url: String,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {

    /** Open Library answered 404, or the record was deleted: there is nothing with that id. */
    public class NotFound(url: String) : OpenLibraryException(url, "Not found: $url")

    /**
     * Open Library answered 429: too many requests. [retryAfterSeconds] is the `Retry-After` header when it was sent.
     * Identify your app with [OpenLibraryConfig.userAgent] and keep [OpenLibraryConfig.maxRequestsPerSecond] low.
     */
    public class RateLimited(url: String, public val retryAfterSeconds: Long?) :
        OpenLibraryException(url, "Rate limited by Open Library (HTTP 429) for $url")

    /** Open Library answered with another unsuccessful [status] (5xx, ...). */
    public class Http(url: String, public val status: Int) : OpenLibraryException(url, "HTTP $status for $url")

    /** The request failed before a response arrived (offline, timeout, DNS). */
    public class Network(url: String, cause: Throwable) :
        OpenLibraryException(url, "Request failed for $url: ${cause.message ?: cause::class.simpleName}", cause)

    /** The response was not the JSON this library expects. */
    public class Decoding(url: String, cause: Throwable) :
        OpenLibraryException(url, "Could not decode the response of $url: ${cause.message}", cause)
}
