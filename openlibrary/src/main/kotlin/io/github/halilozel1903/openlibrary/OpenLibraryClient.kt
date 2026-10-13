package io.github.halilozel1903.openlibrary

import io.github.halilozel1903.openlibrary.internal.RateLimiter
import io.github.halilozel1903.openlibrary.internal.ResponseCache
import io.github.halilozel1903.openlibrary.internal.buildUrl
import io.github.halilozel1903.openlibrary.internal.encodePathSegment
import io.github.halilozel1903.openlibrary.model.Author
import io.github.halilozel1903.openlibrary.model.AuthorSearchResult
import io.github.halilozel1903.openlibrary.model.AuthorWorksPage
import io.github.halilozel1903.openlibrary.model.Edition
import io.github.halilozel1903.openlibrary.model.EditionPage
import io.github.halilozel1903.openlibrary.model.SearchResult
import io.github.halilozel1903.openlibrary.model.Subject
import io.github.halilozel1903.openlibrary.model.Work
import io.github.halilozel1903.openlibrary.transport.KtorTransport
import io.github.halilozel1903.openlibrary.transport.OpenLibraryTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * A coroutine client for the [Open Library API](https://openlibrary.org/developers/api).
 *
 * ```kotlin
 * val client = OpenLibraryClient {
 *     userAgent = "MyBookApp/1.0 (books@example.com)"
 * }
 * val results = client.search("the left hand of darkness")
 * val work = client.work(results.docs.first().key)
 * val editions = client.editions(work.olid, limit = 10)
 * val cover = work.coverUrl(CoverSize.L)
 * ```
 *
 * Every function is main-safe and throws an [OpenLibraryException] on failure. Ids can be given as OLIDs
 * (`OL27448W`), keys (`/works/OL27448W`) or openlibrary.org urls. Responses are cached in memory by url, network
 * requests are spaced by [OpenLibraryConfig.maxRequestsPerSecond], and works or authors that were merged into
 * another record (`/type/redirect`) are followed to the record they point to. Close the client when you no longer
 * need it.
 */
public class OpenLibraryClient(configure: OpenLibraryConfig.() -> Unit = {}) : AutoCloseable {

    private val config = OpenLibraryConfig().apply(configure)

    /** The API root, without a trailing slash. */
    public val baseUrl: String = config.baseUrl.trimEnd('/')

    private val transport: OpenLibraryTransport = config.transport ?: KtorTransport(
        httpClient = config.httpClient,
        engine = config.engine,
        userAgent = config.userAgent,
        requestTimeoutMillis = config.requestTimeoutMillis,
        retries = config.retries,
    )
    private val cache = ResponseCache(config.cacheSize, config.cacheTtl.inWholeMilliseconds)
    private val limiter = RateLimiter(config.maxRequestsPerSecond)

    // region Search

    /** `GET /search.json` with [query]. */
    public suspend fun search(query: SearchQuery): SearchResult =
        fetch(buildUrl("$baseUrl/search.json", query.parameters()), SearchResult.serializer())

    /** `GET /search.json`, with the query set up in [build]. */
    public suspend fun search(build: SearchQuery.() -> Unit): SearchResult = search(SearchQuery().apply(build))

    /** `GET /search.json?q=...`: free text search with the [SearchFields.Default] fields. */
    public suspend fun search(
        q: String,
        page: Int = 1,
        limit: Int = 20,
        fields: List<String>? = SearchFields.Default,
    ): SearchResult = search {
        this.q = q
        this.page = page
        this.limit = limit
        this.fields = fields
    }

    /**
     * Every page of [query] from its `page`, requested lazily as the flow is collected. Stops after the last page
     * or after [maxPages]. Use `take(n)` to stop earlier.
     */
    public fun searchPages(query: SearchQuery, maxPages: Int = Int.MAX_VALUE): Flow<SearchResult> = flow {
        var page = query.page
        var emitted = 0
        while (emitted < maxPages) {
            val result = search(query.copy(page = page))
            emit(result)
            emitted++
            if (!result.hasMore) break
            page++
        }
    }

    /** `GET /search/authors.json?q=...`. */
    public suspend fun searchAuthors(q: String, limit: Int = 20, offset: Int = 0): AuthorSearchResult {
        require(q.isNotBlank()) { "The search query is empty" }
        require(limit in 1..1000) { "limit must be 1 to 1000, was $limit" }
        require(offset >= 0) { "offset must not be negative, was $offset" }
        val url = buildUrl(
            "$baseUrl/search/authors.json",
            listOf("q" to q.trim(), "limit" to limit.toString(), "offset" to offset.takeIf { it > 0 }?.toString()),
        )
        return fetch(url, AuthorSearchResult.serializer())
    }

    // endregion

    // region Works, editions and authors

    /** `GET /works/{id}.json`. Merged works are followed to the work they redirect to. */
    public suspend fun work(id: String): Work =
        record("$baseUrl${OpenLibraryIds.key(id, OpenLibraryKind.Work)}.json", Work.serializer())

    /** `GET /works/{id}/editions.json`: one page of the work's editions, [limit] at a time from [offset]. */
    public suspend fun editions(workId: String, limit: Int = 50, offset: Int = 0): EditionPage {
        require(limit in 1..1000) { "limit must be 1 to 1000, was $limit" }
        require(offset >= 0) { "offset must not be negative, was $offset" }
        val key = OpenLibraryIds.key(workId, OpenLibraryKind.Work)
        val url = buildUrl(
            "$baseUrl$key/editions.json",
            listOf("limit" to limit.toString(), "offset" to offset.takeIf { it > 0 }?.toString()),
        )
        return fetch(url, EditionPage.serializer())
    }

    /** Every page of a work's editions, following the `links.next` of each page lazily. */
    public fun editionPages(workId: String, pageSize: Int = 50): Flow<EditionPage> = flow {
        var page = editions(workId, limit = pageSize)
        emit(page)
        var offset = page.entries.size
        while (page.entries.isNotEmpty() && page.links?.next != null && offset < page.size) {
            page = editions(workId, limit = pageSize, offset = offset)
            emit(page)
            offset += page.entries.size
        }
    }

    /** `GET /books/{id}.json`: one edition by its OLID (`OL7353617M`). */
    public suspend fun edition(id: String): Edition =
        record("$baseUrl${OpenLibraryIds.key(id, OpenLibraryKind.Edition)}.json", Edition.serializer())

    /**
     * `GET /isbn/{isbn}.json`: the edition with this ISBN-10 or ISBN-13. Hyphens are ignored; an invalid check digit
     * throws [IllegalArgumentException] before any request. Open Library redirects to `/books/{id}.json`.
     */
    public suspend fun isbn(isbn: String): Edition {
        require(Isbn.isValid(isbn)) { "\"$isbn\" is not a valid ISBN-10 or ISBN-13" }
        return record("$baseUrl/isbn/${Isbn.clean(isbn)}.json", Edition.serializer())
    }

    /** The work of [edition], or `null` when the edition is not linked to one. */
    public suspend fun workOf(edition: Edition): Work? = edition.workId?.let { work(it) }

    /** `GET /authors/{id}.json`. Merged authors are followed to the author they redirect to. */
    public suspend fun author(id: String): Author =
        record("$baseUrl${OpenLibraryIds.key(id, OpenLibraryKind.Author)}.json", Author.serializer())

    /** The authors of [work], loaded in parallel, in the work's order. */
    public suspend fun authorsOf(work: Work): List<Author> = coroutineScope {
        work.authorIds.distinct().map { async { author(it) } }.awaitAll()
    }

    /** `GET /authors/{id}/works.json`: one page of the author's works. */
    public suspend fun authorWorks(authorId: String, limit: Int = 50, offset: Int = 0): AuthorWorksPage {
        require(limit in 1..1000) { "limit must be 1 to 1000, was $limit" }
        require(offset >= 0) { "offset must not be negative, was $offset" }
        val key = OpenLibraryIds.key(authorId, OpenLibraryKind.Author)
        val url = buildUrl(
            "$baseUrl$key/works.json",
            listOf("limit" to limit.toString(), "offset" to offset.takeIf { it > 0 }?.toString()),
        )
        return fetch(url, AuthorWorksPage.serializer())
    }

    // endregion

    // region Subjects

    /**
     * `GET /subjects/{name}.json`. [name] is a subject such as `"science_fiction"` or `"Science fiction"` (it is
     * lower-cased and spaces become underscores), or a prefixed one: `"place:london"`, `"person:sherlock_holmes"`,
     * `"time:20th_century"`.
     *
     * @param details also return related subjects, people, places, times, top authors and publishing history.
     * @param ebooks only works that have an ebook.
     * @param publishedIn only works published in these years.
     */
    public suspend fun subject(
        name: String,
        limit: Int = 12,
        offset: Int = 0,
        details: Boolean = false,
        ebooks: Boolean = false,
        publishedIn: IntRange? = null,
    ): Subject {
        require(limit in 1..1000) { "limit must be 1 to 1000, was $limit" }
        require(offset >= 0) { "offset must not be negative, was $offset" }
        val slug = subjectSlug(name)
        require(slug.isNotEmpty()) { "The subject is empty" }
        val url = buildUrl(
            "$baseUrl/subjects/${encodePathSegment(slug).replace("%3A", ":")}.json",
            listOf(
                "limit" to limit.toString(),
                "offset" to offset.takeIf { it > 0 }?.toString(),
                "details" to "true".takeIf { details },
                "ebooks" to "true".takeIf { ebooks },
                "published_in" to publishedIn?.let { "${it.first}-${it.last}" },
            ),
        )
        return fetch(url, Subject.serializer())
    }

    // endregion

    // region Raw access

    /** Loads and decodes any Open Library url with [deserializer], through the cache and the rate limit. */
    public suspend fun <T> fetch(url: String, deserializer: DeserializationStrategy<T>): T {
        val body = load(url)
        return decode(url, body, deserializer)
    }

    /** Forgets every cached response. */
    public fun clearCache() {
        cache.clear()
    }

    override fun close() {
        (transport as? AutoCloseable)?.close()
    }

    // endregion

    /** Loads a record and follows `/type/redirect` records (merged works and authors), up to a few hops. */
    private suspend fun <T> record(url: String, deserializer: DeserializationStrategy<T>): T {
        var current = url
        repeat(MAX_REDIRECTS + 1) {
            val body = load(current)
            val element = parse(current, body)
            val record = element as? JsonObject
            val type = (record?.get("type") as? JsonObject)?.get("key")
            when ((type as? JsonPrimitive)?.contentOrNull) {
                "/type/redirect" -> {
                    val location = (record["location"] as? JsonPrimitive)?.contentOrNull
                        ?: throw OpenLibraryException.NotFound(current)
                    current = "$baseUrl$location.json"
                }
                "/type/delete" -> throw OpenLibraryException.NotFound(current)
                else -> return decode(current, element, deserializer)
            }
        }
        throw OpenLibraryException.NotFound(current)
    }

    private suspend fun load(url: String): String {
        cache.get(url)?.let { return it }
        limiter.acquire()
        val body = withContext(Dispatchers.IO) { transport.get(url) }
        cache.put(url, body)
        return body
    }

    private fun parse(url: String, body: String): JsonElement = try {
        json.parseToJsonElement(body)
    } catch (e: SerializationException) {
        cache.remove(url)
        throw OpenLibraryException.Decoding(url, e)
    }

    private fun <T> decode(url: String, body: String, deserializer: DeserializationStrategy<T>): T =
        decode(url, parse(url, body), deserializer)

    private fun <T> decode(url: String, element: JsonElement, deserializer: DeserializationStrategy<T>): T = try {
        json.decodeFromJsonElement(deserializer, element)
    } catch (e: IllegalArgumentException) {
        // SerializationException is an IllegalArgumentException.
        cache.remove(url)
        throw OpenLibraryException.Decoding(url, e)
    }

    public companion object {
        /** The public Open Library. */
        public const val DEFAULT_BASE_URL: String = "https://openlibrary.org"

        /** Sent when [OpenLibraryConfig.userAgent] is not set. Set your own app name and contact. */
        public const val DEFAULT_USER_AGENT: String =
            "open-library-kotlin/1.0.0 (+https://github.com/halilozel1903/open-library-kotlin)"

        private const val MAX_REDIRECTS = 3

        /** `"Science Fiction"` gives `"science_fiction"`; prefixes such as `place:` are kept. */
        public fun subjectSlug(name: String): String =
            name.trim().lowercase().replace(Regex("""\s+"""), "_")

        internal val json: Json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            explicitNulls = false
            isLenient = true
        }
    }
}
