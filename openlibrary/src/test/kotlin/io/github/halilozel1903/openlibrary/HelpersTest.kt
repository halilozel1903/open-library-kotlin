package io.github.halilozel1903.openlibrary

import io.github.halilozel1903.openlibrary.internal.RateLimiter
import io.github.halilozel1903.openlibrary.internal.ResponseCache
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HelpersTest {

    @Test
    fun coverUrls() {
        assertEquals("https://covers.openlibrary.org/b/id/240727-S.jpg?default=false", OpenLibraryCovers.book(240727, CoverSize.S))
        assertEquals("https://covers.openlibrary.org/b/id/240727-M.jpg", OpenLibraryCovers.book(240727, default = true))
        assertEquals(
            "https://covers.openlibrary.org/b/isbn/9780385533225-L.jpg?default=false",
            OpenLibraryCovers.bookByIsbn("978-0-385-53322-5", CoverSize.L),
        )
        assertEquals("https://covers.openlibrary.org/b/olid/OL7353617M-M.jpg?default=false", OpenLibraryCovers.bookByOlid("/books/OL7353617M"))
        assertEquals("https://covers.openlibrary.org/a/olid/OL26320A-S.jpg?default=false", OpenLibraryCovers.authorByOlid("OL26320A", CoverSize.S))
        assertEquals("https://covers.openlibrary.org/a/id/6791763-L.jpg?default=false", OpenLibraryCovers.author(6791763, CoverSize.L))
        assertEquals("https://covers.openlibrary.org/b/goodreads/1507552-M.jpg?default=false", OpenLibraryCovers.bookBy("goodreads", "1507552"))
        assertFailsWith<IllegalArgumentException> { OpenLibraryCovers.book(-1) }
        assertFailsWith<IllegalArgumentException> { OpenLibraryCovers.bookByOlid("OL26320A") }
        assertFailsWith<IllegalArgumentException> { OpenLibraryCovers.bookBy("asin", "x") }
    }

    @Test
    fun ids() {
        assertEquals("OL27448W", OpenLibraryIds.find("https://openlibrary.org/works/OL27448W/The_Lord_of_the_Rings"))
        assertEquals("OL26320A", OpenLibraryIds.parse("/authors/ol26320a", OpenLibraryKind.Author))
        assertEquals("/books/OL7353617M", OpenLibraryIds.key("OL7353617M", OpenLibraryKind.Edition))
        assertEquals(OpenLibraryKind.Work, OpenLibraryKind.of("OL1W"))
        assertNull(OpenLibraryIds.find("dune"))
        assertFailsWith<IllegalArgumentException> { OpenLibraryIds.parse("OL1W", OpenLibraryKind.Author) }
    }

    @Test
    fun isbn() {
        assertTrue(Isbn.isValid("0-14-032872-6"))
        assertTrue(Isbn.isValid("978 0 14 032872 1"))
        assertTrue(Isbn.isValid("080442957X"))
        assertFalse(Isbn.isValid("0140328727"))
        assertFalse(Isbn.isValid("12345"))
        assertEquals("9780140328721", Isbn.toIsbn13("0140328726"))
        assertEquals("0140328726", Isbn.toIsbn10("9780140328721"))
        assertEquals("080442957X", Isbn.toIsbn10("9780804429573"))
        assertNull(Isbn.toIsbn10("9791032305690"))
    }

    @Test
    fun subjectSlugs() {
        assertEquals("science_fiction", OpenLibraryClient.subjectSlug(" Science  Fiction "))
        assertEquals("place:london", OpenLibraryClient.subjectSlug("place:London"))
    }

    @Test
    fun cacheExpires() {
        var now = 0L
        val cache = ResponseCache(maxEntries = 2, ttlMillis = 100, now = { now })
        cache.put("a", "1")
        cache.put("b", "2")
        cache.get("a")
        cache.put("c", "3") // evicts b, the least recently used
        assertNull(cache.get("b"))
        assertEquals("1", cache.get("a"))
        now = 101
        assertNull(cache.get("a"))
    }

    @Test
    fun rateLimiterSpacesSlots() = runTest {
        var now = 0L
        val limiter = RateLimiter(perSecond = 4.0, nowNanos = { now })
        assertEquals(listOf(0L, 250L, 500L), List(3) { limiter.reserve() })
        now = 2_000_000_000L
        assertEquals(0L, limiter.reserve())
        assertEquals(0L, RateLimiter(0.0).reserve())
    }
}
