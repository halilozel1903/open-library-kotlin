package io.github.halilozel1903.openlibrary.sample

import io.github.halilozel1903.openlibrary.OpenLibraryClient
import io.github.halilozel1903.openlibrary.OpenLibraryException
import io.github.halilozel1903.openlibrary.SearchFields
import io.github.halilozel1903.openlibrary.model.Author
import io.github.halilozel1903.openlibrary.model.EditionPage
import io.github.halilozel1903.openlibrary.model.SearchDoc
import io.github.halilozel1903.openlibrary.model.SearchResult
import io.github.halilozel1903.openlibrary.model.Subject
import io.github.halilozel1903.openlibrary.model.Work
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** Everything the work screen shows. */
data class WorkDetail(val work: Work, val authors: List<Author>, val editions: EditionPage)

/**
 * The sample's only data source: the live Open Library, through [OpenLibraryClient] with its cache, rate limit and
 * retries.
 *
 * When [strict], a failing extra section (an author) fails the whole screen instead of being left out. Screenshot
 * scenes use this, so a half loaded screen fails CI rather than ending up in the README.
 */
class LibraryRepository(private val client: OpenLibraryClient, private val strict: Boolean = false) {

    suspend fun search(query: String): SearchResult = client.search(query, limit = 40)

    suspend fun workDetail(workId: String): WorkDetail = coroutineScope {
        val work = client.work(workId)
        // A merged work redirects; ask for the editions of the work it points to.
        val editions = async { client.editions(work.olid, limit = 12) }
        val authors = work.authorIds.distinct().take(MAX_AUTHORS).map { id ->
            async {
                try {
                    client.author(id)
                } catch (e: OpenLibraryException) {
                    if (strict) throw e
                    null
                }
            }
        }
        WorkDetail(work, authors.awaitAll().filterNotNull(), editions.await())
    }

    suspend fun subject(name: String): Subject = client.subject(name, limit = 24, details = true)

    /** The best match for a title and an author, from `/search.json` with only a few fields. */
    suspend fun findWork(title: String, author: String): SearchDoc =
        client.search {
            this.title = title
            this.author = author
            fields = SearchFields.Default
            limit = 1
        }.docs.firstOrNull() ?: throw NoSuchElementException("Open Library found no \"$title\" by $author.")

    /** Looks up each title of [Scene.READING_LIST] live, for the reading list scene. */
    suspend fun sceneReadingList(now: Long): List<SavedBook> =
        Scene.READING_LIST.mapIndexed { index, (title, author, status) ->
            SavedBook.from(findWork(title, author), status, addedAt = now - index * DAY_MILLIS)
        }

    companion object {
        private const val MAX_AUTHORS = 3
        private const val DAY_MILLIS = 86_400_000L
    }
}

/** The app's repositories, created on first use. */
object Repositories {
    private var live: LibraryRepository? = null
    private var scenes: LibraryRepository? = null

    private fun client(retries: Int) = OpenLibraryClient {
        // Shares OkHttp with Coil.
        engine = OkHttp.create()
        // Open Library asks apps to say who they are and how to reach them.
        userAgent = "Shelf/1.0.0 (open-library-kotlin sample; +https://github.com/halilozel1903/open-library-kotlin)"
        requestTimeoutMillis = 45_000
        this.retries = retries
    }

    /** Normal use: a failing extra section is left out. */
    @Synchronized
    fun live(): LibraryRepository =
        live ?: LibraryRepository(client(retries = 2)).also { live = it }

    /** Screenshot scenes: more retries for a busy CI network, and every section must load. */
    @Synchronized
    fun scenes(): LibraryRepository =
        scenes ?: LibraryRepository(client(retries = 4), strict = true).also { scenes = it }
}
