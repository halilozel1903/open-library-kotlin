package io.github.halilozel1903.openlibrary

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

/** Reads `src/test/resources/fixtures/{name}`. */
fun fixture(name: String): String =
    requireNotNull(object {}.javaClass.getResource("/fixtures/$name")) { "Missing fixture $name" }.readText()

/** A request the mock engine saw. */
data class Seen(val url: String, val userAgent: String?)

/**
 * A client on a Ktor [MockEngine]. [route] maps a full request url to a fixture file name, or `null` for 404.
 * Every request is recorded in [seen]. Rate limiting is off and retries are off, so tests stay fast.
 */
class TestApi(
    private val status: (String) -> HttpStatusCode = { HttpStatusCode.OK },
    private val headers: Map<String, String> = emptyMap(),
    configure: OpenLibraryConfig.() -> Unit = {},
    private val route: (String) -> String?,
) {
    val seen = mutableListOf<Seen>()

    val engine = MockEngine { request ->
        val url = request.url.toString()
        synchronized(seen) { seen += Seen(url, request.headers[HttpHeaders.UserAgent]) }
        val code = status(url)
        val file = route(url)
        val jsonHeaders = headersOf(
            *(listOf(HttpHeaders.ContentType to "application/json") + headers.toList())
                .map { it.first to listOf(it.second) }.toTypedArray(),
        )
        when {
            code != HttpStatusCode.OK -> respond("{}", code, jsonHeaders)
            file == null -> respond("{\"error\": \"notfound\"}", HttpStatusCode.NotFound, jsonHeaders)
            else -> respond(fixture(file), HttpStatusCode.OK, jsonHeaders)
        }
    }

    val client = OpenLibraryClient {
        engine = this@TestApi.engine
        maxRequestsPerSecond = 0.0
        retries = 0
        configure()
    }

    val urls: List<String> get() = synchronized(seen) { seen.map { it.url } }
}

/** Routes the standard fixtures by url. */
fun standardRoute(url: String): String? {
    val path = url.removePrefix(OpenLibraryClient.DEFAULT_BASE_URL)
    return when {
        path.startsWith("/search.json") -> "search-dune.json"
        path.startsWith("/search/authors.json") -> "search-authors-tolkien.json"
        path.startsWith("/works/OL27448W/editions.json") && "offset=2" in path -> "editions-OL27448W-offset2.json"
        path.startsWith("/works/OL27448W/editions.json") -> "editions-OL27448W.json"
        path.startsWith("/authors/OL26320A/works.json") -> "author-works-OL26320A.json"
        path.startsWith("/subjects/science_fiction.json") -> "subject-science_fiction.json"
        path.startsWith("/isbn/") -> "book-OL7353617M.json"
        path.startsWith("/books/") -> "book-${path.removePrefix("/books/").substringBefore(".json")}.json"
        path.startsWith("/works/") -> "work-${path.removePrefix("/works/").substringBefore(".json")}.json"
            .takeIf { object {}.javaClass.getResource("/fixtures/$it") != null }
        path.startsWith("/authors/") -> "author-${path.removePrefix("/authors/").substringBefore(".json")}.json"
            .takeIf { object {}.javaClass.getResource("/fixtures/$it") != null }
        else -> null
    }
}
