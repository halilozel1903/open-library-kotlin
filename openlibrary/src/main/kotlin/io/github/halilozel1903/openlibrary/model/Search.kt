package io.github.halilozel1903.openlibrary.model

import io.github.halilozel1903.openlibrary.CoverSize
import io.github.halilozel1903.openlibrary.OpenLibraryCovers
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A page of `GET /search.json`. Only the fields you asked for with `fields=` are set; the others keep their
 * defaults (empty lists and `null`).
 */
@Serializable
public data class SearchResult(
    /** How many works match in total. */
    @SerialName("numFound") public val numFound: Int = 0,
    /** The index of the first doc of this page. */
    public val start: Int = 0,
    @SerialName("numFoundExact") public val numFoundExact: Boolean? = null,
    /** The query as Open Library understood it. */
    public val q: String? = null,
    @Serializable(with = SearchDocListSerializer::class)
    public val docs: List<SearchDoc> = emptyList(),
) {
    /** Whether there are more results after this page. */
    public val hasMore: Boolean get() = docs.isNotEmpty() && start + docs.size < numFound
}

/** One work in a [SearchResult]. */
@Serializable
public data class SearchDoc(
    /** The work key, `/works/OL27448W`. */
    public val key: String = "",
    public val title: String = "",
    public val subtitle: String? = null,
    @SerialName("author_name") public val authorNames: List<String> = emptyList(),
    /** Author OLIDs without a prefix, `OL26320A`. */
    @SerialName("author_key") public val authorKeys: List<String> = emptyList(),
    @SerialName("first_publish_year") public val firstPublishYear: Int? = null,
    @SerialName("publish_year") public val publishYears: List<Int> = emptyList(),
    /** The cover id for [OpenLibraryCovers.book]. */
    @SerialName("cover_i") public val coverId: Long? = null,
    @SerialName("cover_edition_key") public val coverEditionKey: String? = null,
    @SerialName("edition_count") public val editionCount: Int? = null,
    @SerialName("edition_key") public val editionKeys: List<String> = emptyList(),
    public val isbn: List<String> = emptyList(),
    /** Language codes such as `eng`. */
    public val language: List<String> = emptyList(),
    public val subject: List<String> = emptyList(),
    public val publisher: List<String> = emptyList(),
    @SerialName("number_of_pages_median") public val numberOfPagesMedian: Int? = null,
    @SerialName("ratings_average") public val ratingsAverage: Double? = null,
    @SerialName("ratings_count") public val ratingsCount: Int? = null,
    @SerialName("want_to_read_count") public val wantToReadCount: Int? = null,
    @SerialName("currently_reading_count") public val currentlyReadingCount: Int? = null,
    @SerialName("already_read_count") public val alreadyReadCount: Int? = null,
    /** `public`, `borrowable`, `printdisabled` or `no_ebook`. */
    @SerialName("ebook_access") public val ebookAccess: String? = null,
    @SerialName("has_fulltext") public val hasFulltext: Boolean? = null,
) {
    /** The work OLID, `OL27448W`. */
    public val olid: String get() = key.substringAfterLast('/')

    /** The authors joined for display: `"Frank Herbert"`, `"Neil Gaiman, Terry Pratchett"`. */
    public val authorLine: String get() = authorNames.joinToString(", ")

    /** The cover url of [coverId] (with `default=false`), or `null` when there is no cover. */
    public fun coverUrl(size: CoverSize = CoverSize.M): String? =
        coverId?.takeIf { it > 0 }?.let { OpenLibraryCovers.book(it, size) }
}

internal object SearchDocListSerializer : LenientListSerializer<SearchDoc>(SearchDoc.serializer())

/** A page of `GET /search/authors.json`. */
@Serializable
public data class AuthorSearchResult(
    @SerialName("numFound") public val numFound: Int = 0,
    public val start: Int = 0,
    @Serializable(with = AuthorDocListSerializer::class)
    public val docs: List<AuthorDoc> = emptyList(),
)

/** One author in an [AuthorSearchResult]. */
@Serializable
public data class AuthorDoc(
    /** The author OLID without a prefix, `OL26320A`. */
    public val key: String = "",
    public val name: String = "",
    @SerialName("alternate_names") public val alternateNames: List<String> = emptyList(),
    @SerialName("birth_date") public val birthDate: String? = null,
    @SerialName("death_date") public val deathDate: String? = null,
    @SerialName("top_work") public val topWork: String? = null,
    @SerialName("work_count") public val workCount: Int? = null,
    @SerialName("top_subjects") public val topSubjects: List<String> = emptyList(),
) {
    /** The author OLID, `OL26320A`. */
    public val olid: String get() = key.substringAfterLast('/')
}

internal object AuthorDocListSerializer : LenientListSerializer<AuthorDoc>(AuthorDoc.serializer())
