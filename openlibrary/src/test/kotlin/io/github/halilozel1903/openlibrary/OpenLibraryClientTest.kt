package io.github.halilozel1903.openlibrary

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OpenLibraryClientTest {

    @Test
    fun searchSendsFieldsAndPagingAndDecodesDocs() = runTest {
        val api = TestApi(route = ::standardRoute)
        val result = api.client.search("the left hand of darkness", page = 2, limit = 5)

        val url = api.urls.single()
        assertTrue(url.startsWith("https://openlibrary.org/search.json?q=the%20left%20hand%20of%20darkness"), url)
        assertTrue("fields=key%2Ctitle%2Csubtitle%2Cauthor_name" in url, url)
        assertTrue(url.endsWith("&page=2&limit=5"), url)

        assertEquals(3, result.numFound)
        // The fourth doc has a title that is not a string; it is left out instead of failing the page.
        assertEquals(3, result.docs.size)
        val dune = result.docs.first()
        assertEquals("OL893415W", dune.olid)
        assertEquals("Frank Herbert", dune.authorLine)
        assertEquals(1965, dune.firstPublishYear)
        assertEquals(11481354L, dune.coverId)
        assertEquals("https://covers.openlibrary.org/b/id/11481354-L.jpg?default=false", dune.coverUrl(CoverSize.L))
        assertNull(result.docs[2].coverUrl())
    }

    @Test
    fun searchBuilderSendsFilters() = runTest {
        val api = TestApi(route = ::standardRoute)
        api.client.search {
            title = "Dune"
            author = "Frank Herbert"
            isbn = "978-0-441-17271-9"
            firstPublishYear = 1960..1970
            sort = SearchSort.Editions
            fields = listOf(SearchFields.KEY, SearchFields.TITLE)
            limit = 1
        }
        val url = api.urls.single()
        assertTrue("q=first_publish_year%3A%5B1960%20TO%201970%5D" in url, url)
        assertTrue("title=Dune&author=Frank%20Herbert" in url, url)
        assertTrue("isbn=9780441172719" in url, url)
        assertTrue("sort=editions&fields=key%2Ctitle" in url, url)
    }

    @Test
    fun emptySearchFailsBeforeAnyRequest() = runTest {
        val api = TestApi(route = ::standardRoute)
        assertFailsWith<IllegalArgumentException> { api.client.search { q = "  " } }
        assertTrue(api.urls.isEmpty())
    }

    @Test
    fun searchPagesFollowsUntilTheLastPage() = runTest {
        val api = TestApi { url ->
            when {
                "page=1&" in url -> "search-page-1.json"
                "page=2&" in url -> "search-page-2.json"
                "page=3&" in url -> "search-page-3.json"
                else -> null
            }
        }
        val pages = api.client.searchPages(SearchQuery().apply { q = "book"; limit = 2 }).toList()
        assertEquals(listOf(2, 2, 1), pages.map { it.docs.size })
        assertFalse(pages.last().hasMore)
        assertEquals(3, api.urls.size)
    }

    @Test
    fun workReadsTypedTextDescriptionAndSkipsRemovedCovers() = runTest {
        val api = TestApi(route = ::standardRoute)
        val work = api.client.work("OL27448W")
        assertEquals("The Lord of the Rings", work.title)
        assertTrue(work.description!!.startsWith("Originally published from 1954"))
        assertEquals(14625765L, work.coverId)
        assertEquals(listOf("OL26320A"), work.authorIds)
        assertEquals(1954, work.firstPublishYear)
        assertEquals("2009-10-15T11:34:21.437031", work.created)
        assertEquals("When Mr. Bilbo Baggins of Bag End announced...", work.excerpts.single().excerpt)
        assertEquals("https://openlibrary.org/works/OL27448W.json", api.urls.single())
    }

    @Test
    fun workReadsPlainStringDescriptionAndBareAuthorKeys() = runTest {
        val api = TestApi(route = ::standardRoute)
        val work = api.client.work("https://openlibrary.org/works/OL893415W/Dune")
        assertEquals("Set on the desert planet Arrakis, Dune is the story of the boy Paul Atreides.", work.description)
        assertEquals(listOf("OL79034A", "OL2A"), work.authorIds)
        assertEquals(1965, work.firstPublishYear)
        assertEquals("2009-12-10T01:50:13.818355", work.created)
    }

    @Test
    fun redirectRecordsAreFollowed() = runTest {
        val api = TestApi(route = ::standardRoute)
        val work = api.client.work("/works/OL1W")
        assertEquals("/works/OL27448W", work.key)
        assertEquals(
            listOf("https://openlibrary.org/works/OL1W.json", "https://openlibrary.org/works/OL27448W.json"),
            api.urls,
        )
    }

    @Test
    fun deletedAndMissingRecordsAreNotFound() = runTest {
        val api = TestApi(route = ::standardRoute)
        assertFailsWith<OpenLibraryException.NotFound> { api.client.work("OL2W") }
        assertFailsWith<OpenLibraryException.NotFound> { api.client.work("OL404W") }
    }

    @Test
    fun wrongKindOfIdFailsBeforeAnyRequest() = runTest {
        val api = TestApi(route = ::standardRoute)
        assertFailsWith<IllegalArgumentException> { api.client.work("OL26320A") }
        assertFailsWith<IllegalArgumentException> { api.client.author("dune") }
        assertTrue(api.urls.isEmpty())
    }

    @Test
    fun editionsPageSkipsBrokenEntriesAndReadsTextFields() = runTest {
        val api = TestApi(route = ::standardRoute)
        val page = api.client.editions("OL27448W", limit = 2)
        assertEquals("https://openlibrary.org/works/OL27448W/editions.json?limit=2", api.urls.single())
        assertEquals(3, page.size)
        assertEquals(2, page.entries.size)
        val first = page.entries[0]
        assertEquals("9780358439196", first.isbn)
        assertEquals(2020, first.publishYear)
        assertEquals(listOf("eng"), first.languageCodes)
        assertEquals("OL27448W", first.workId)
        val french = page.entries[1]
        assertNull(french.coverId)
        assertEquals("Translated by Francis Ledoux.", french.notes)
        assertEquals("Quand M. Bilbon Sacquet...", french.firstSentence)
        assertEquals("/works/OL27448W/editions.json?limit=2&offset=2", page.links?.next)
    }

    @Test
    fun editionPagesFollowOffsets() = runTest {
        val api = TestApi(route = ::standardRoute)
        val pages = api.client.editionPages("OL27448W", pageSize = 2).toList()
        assertEquals(listOf(2, 1), pages.map { it.entries.size })
        assertEquals("https://openlibrary.org/works/OL27448W/editions.json?limit=2&offset=2", api.urls.last())
    }

    @Test
    fun isbnIsCleanedAndValidated() = runTest {
        val api = TestApi(route = ::standardRoute)
        val edition = api.client.isbn("0-14-032872-6")
        assertEquals("https://openlibrary.org/isbn/0140328726.json", api.urls.single())
        assertEquals("OL7353617M", edition.olid)
        assertEquals(listOf("1507552"), edition.identifiers["goodreads"])
        assertEquals(1988, edition.publishYear)
        assertFailsWith<IllegalArgumentException> { api.client.isbn("0140328727") }
        assertEquals(1, api.urls.size)
    }

    @Test
    fun authorBioPhotoAndLifespan() = runTest {
        val api = TestApi(route = ::standardRoute)
        val author = api.client.author("OL26320A")
        assertEquals("J.R.R. Tolkien", author.name)
        assertTrue(author.bio!!.startsWith("John Ronald Reuel Tolkien was"))
        assertEquals(6791763L, author.photoId)
        assertEquals("https://covers.openlibrary.org/a/id/6791763-M.jpg?default=false", author.photoUrl())
        assertEquals("1892 – 1973", author.lifespan)
        assertEquals("Q892", author.remoteId("wikidata"))
    }

    @Test
    fun authorsOfAWorkKeepTheWorksOrder() = runTest {
        val api = TestApi(route = ::standardRoute)
        val work = api.client.work("OL893415W")
        val authors = api.client.authorsOf(work)
        assertEquals(listOf("Frank Herbert", "Second Author"), authors.map { it.name })
        assertEquals("Franklin Patrick Herbert Jr. was an American science fiction author.", authors[0].bio)
    }

    @Test
    fun authorWorksAndAuthorSearch() = runTest {
        val api = TestApi(route = ::standardRoute)
        val works = api.client.authorWorks("/authors/OL26320A", limit = 2)
        assertEquals("https://openlibrary.org/authors/OL26320A/works.json?limit=2", api.urls.last())
        assertEquals(450, works.size)
        assertEquals(listOf("The Lord of the Rings", "The Hobbit"), works.entries.map { it.title })
        assertEquals("A hobbit goes there and back again.", works.entries[1].description)

        val authors = api.client.searchAuthors("tolkien")
        assertEquals("https://openlibrary.org/search/authors.json?q=tolkien&limit=20", api.urls.last())
        assertEquals("OL26320A", authors.docs.single().olid)
        assertEquals("The Hobbit", authors.docs.single().topWork)
    }

    @Test
    fun subjectWithDetails() = runTest {
        val api = TestApi(route = ::standardRoute)
        val subject = api.client.subject("Science Fiction", limit = 2, details = true, publishedIn = 1950..1999)
        assertEquals(
            "https://openlibrary.org/subjects/science_fiction.json?limit=2&details=true&published_in=1950-1999",
            api.urls.single(),
        )
        assertEquals("Science fiction", subject.name)
        assertEquals(21345, subject.workCount)
        val dune = subject.works.first()
        assertEquals("Frank Herbert", dune.authorLine)
        assertEquals("https://covers.openlibrary.org/b/id/11481354-M.jpg?default=false", dune.coverUrl())
        assertNull(subject.works[1].coverUrl())
        assertEquals("Isaac Asimov", subject.authors.single().name)
        assertEquals(mapOf(1950 to 120, 1951 to 130), subject.publishingHistoryByYear)
    }

    @Test
    fun prefixedSubjectsKeepTheirColon() = runTest {
        val api = TestApi { null }
        assertFailsWith<OpenLibraryException.NotFound> { api.client.subject("place:London") }
        assertEquals("https://openlibrary.org/subjects/place:london.json?limit=12", api.urls.single())
    }

    @Test
    fun userAgentIsSent() = runTest {
        val api = TestApi(configure = { userAgent = "ShelfApp/2.0 (shelf@example.com)" }, route = ::standardRoute)
        api.client.work("OL27448W")
        assertEquals("ShelfApp/2.0 (shelf@example.com)", api.seen.single().userAgent)

        val default = TestApi(route = ::standardRoute)
        default.client.work("OL27448W")
        assertEquals(OpenLibraryClient.DEFAULT_USER_AGENT, default.seen.single().userAgent)
    }

    @Test
    fun responsesAreCached() = runTest {
        val api = TestApi(route = ::standardRoute)
        api.client.work("OL27448W")
        api.client.work("/works/OL27448W")
        assertEquals(1, api.urls.size)
        api.client.clearCache()
        api.client.work("OL27448W")
        assertEquals(2, api.urls.size)

        val uncached = TestApi(configure = { cacheSize = 0 }, route = ::standardRoute)
        uncached.client.work("OL27448W")
        uncached.client.work("OL27448W")
        assertEquals(2, uncached.urls.size)
    }

    @Test
    fun rateLimitAndServerErrorsAreTyped() = runTest {
        val limited = TestApi(
            status = { HttpStatusCode.TooManyRequests },
            headers = mapOf("Retry-After" to "30"),
            route = ::standardRoute,
        )
        val e = assertFailsWith<OpenLibraryException.RateLimited> { limited.client.work("OL27448W") }
        assertEquals(30L, e.retryAfterSeconds)

        val down = TestApi(status = { HttpStatusCode.ServiceUnavailable }, route = ::standardRoute)
        assertEquals(503, assertFailsWith<OpenLibraryException.Http> { down.client.work("OL27448W") }.status)
    }

    @Test
    fun invalidJsonIsADecodingError() = runTest {
        val client = OpenLibraryClient {
            transport = io.github.halilozel1903.openlibrary.transport.OpenLibraryTransport { "<html>maintenance</html>" }
            maxRequestsPerSecond = 0.0
        }
        assertFailsWith<OpenLibraryException.Decoding> { client.work("OL27448W") }
    }
}
