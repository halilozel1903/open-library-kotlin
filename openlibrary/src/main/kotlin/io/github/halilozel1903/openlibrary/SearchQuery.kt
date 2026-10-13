package io.github.halilozel1903.openlibrary

/** Sort orders of `/search.json`. [Relevance] sends no `sort` parameter. */
public enum class SearchSort(public val apiValue: String?) {
    Relevance(null),
    New("new"),
    Old("old"),
    Title("title"),
    Editions("editions"),
    Rating("rating"),
    WantToRead("want_to_read"),
    CurrentlyReading("currently_reading"),
    AlreadyRead("already_read"),
    Random("random"),
}

/** Field names for [SearchQuery.fields]. */
public object SearchFields {
    public const val KEY: String = "key"
    public const val TITLE: String = "title"
    public const val SUBTITLE: String = "subtitle"
    public const val AUTHOR_NAME: String = "author_name"
    public const val AUTHOR_KEY: String = "author_key"
    public const val FIRST_PUBLISH_YEAR: String = "first_publish_year"
    public const val PUBLISH_YEAR: String = "publish_year"
    public const val COVER_ID: String = "cover_i"
    public const val COVER_EDITION_KEY: String = "cover_edition_key"
    public const val EDITION_COUNT: String = "edition_count"
    public const val EDITION_KEY: String = "edition_key"
    public const val ISBN: String = "isbn"
    public const val LANGUAGE: String = "language"
    public const val SUBJECT: String = "subject"
    public const val PUBLISHER: String = "publisher"
    public const val NUMBER_OF_PAGES_MEDIAN: String = "number_of_pages_median"
    public const val RATINGS_AVERAGE: String = "ratings_average"
    public const val RATINGS_COUNT: String = "ratings_count"
    public const val WANT_TO_READ_COUNT: String = "want_to_read_count"
    public const val CURRENTLY_READING_COUNT: String = "currently_reading_count"
    public const val ALREADY_READ_COUNT: String = "already_read_count"
    public const val EBOOK_ACCESS: String = "ebook_access"
    public const val HAS_FULLTEXT: String = "has_fulltext"

    /** Everything Open Library has (`*`). Responses get large: `isbn`, `subject` and `edition_key` can be long. */
    public val All: List<String> = listOf("*")

    /**
     * What a results list needs: key, title, subtitle, authors, first publish year, cover, edition count, page
     * count, ratings and ebook access. Small responses, fast to load.
     */
    public val Default: List<String> = listOf(
        KEY, TITLE, SUBTITLE, AUTHOR_NAME, AUTHOR_KEY, FIRST_PUBLISH_YEAR, COVER_ID, COVER_EDITION_KEY,
        EDITION_COUNT, NUMBER_OF_PAGES_MEDIAN, RATINGS_AVERAGE, RATINGS_COUNT, EBOOK_ACCESS,
    )
}

/**
 * A `/search.json` query. Set any mix of [q] and the field filters; at least one must be set.
 *
 * ```kotlin
 * client.search {
 *     title = "dune"
 *     author = "frank herbert"
 *     sort = SearchSort.Editions
 *     limit = 10
 * }
 * ```
 */
public class SearchQuery {
    /** Free text, in Open Library's query syntax (`"tolkien hobbit"`, `"subject:fantasy"`). */
    public var q: String? = null
    public var title: String? = null
    public var author: String? = null
    public var subject: String? = null
    public var place: String? = null
    public var person: String? = null
    public var publisher: String? = null
    /** Any ISBN of an edition; hyphens are removed. */
    public var isbn: String? = null
    /** Only works with an edition in this language, a MARC code such as `eng`, `fre`, `tur`. */
    public var language: String? = null
    /** Only works first published in these years. */
    public var firstPublishYear: IntRange? = null
    public var sort: SearchSort = SearchSort.Relevance
    /** Which fields Open Library returns; `null` returns its own default set. */
    public var fields: List<String>? = SearchFields.Default
    /** The 1-based page of [limit] results. */
    public var page: Int = 1
    /** Results per page, 1 to 1000. */
    public var limit: Int = 20
    /** Prefer titles in this language (ISO 639-1, `en`, `de`) when a work has translations. */
    public var lang: String? = null

    /** A copy with another [page]. */
    public fun copy(page: Int = this.page): SearchQuery {
        val source = this
        return SearchQuery().apply {
            q = source.q; title = source.title; author = source.author; subject = source.subject
            place = source.place; person = source.person; publisher = source.publisher; isbn = source.isbn
            language = source.language; firstPublishYear = source.firstPublishYear; sort = source.sort
            fields = source.fields; limit = source.limit; lang = source.lang
            this.page = page
        }
    }

    /** The query parameters, in a stable order. Throws [IllegalArgumentException] for an empty query. */
    public fun parameters(): List<Pair<String, String?>> {
        require(page >= 1) { "page starts at 1, was $page" }
        require(limit in 1..1000) { "limit must be 1 to 1000, was $limit" }
        val years = firstPublishYear?.let { "first_publish_year:[${it.first} TO ${it.last}]" }
        val text = listOfNotNull(q?.trim()?.takeIf(String::isNotEmpty), years).joinToString(" ").takeIf(String::isNotEmpty)
        val filters = listOf(
            "title" to title, "author" to author, "subject" to subject, "place" to place, "person" to person,
            "publisher" to publisher, "isbn" to isbn?.let(Isbn::clean), "language" to language,
        ).map { (k, v) -> k to v?.trim()?.takeIf(String::isNotEmpty) }
        require(text != null || filters.any { it.second != null }) { "The search query is empty" }
        return buildList<Pair<String, String?>> {
            add("q" to text)
            addAll(filters)
            add("sort" to sort.apiValue)
            add("fields" to fields?.joinToString(","))
            add("lang" to lang)
            add("page" to page.toString())
            add("limit" to limit.toString())
        }
    }
}
