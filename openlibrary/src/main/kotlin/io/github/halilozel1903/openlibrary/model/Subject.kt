package io.github.halilozel1903.openlibrary.model

import io.github.halilozel1903.openlibrary.CoverSize
import io.github.halilozel1903.openlibrary.OpenLibraryCovers
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A subject, `GET /subjects/{name}.json`: works filed under it, and with `details=true` the related subjects,
 * people, places, times, top authors, publishers and publishing history.
 */
@Serializable
public data class Subject(
    /** `/subjects/science_fiction`, `/subjects/place:london`, ... */
    public val key: String = "",
    /** The display name, `Science fiction`. */
    public val name: String = "",
    /** `subject`, `place`, `person` or `time`. */
    @SerialName("subject_type") public val subjectType: String? = null,
    @SerialName("work_count") public val workCount: Int = 0,
    @SerialName("ebook_count") public val ebookCount: Int? = null,
    @Serializable(with = SubjectWorkListSerializer::class)
    public val works: List<SubjectWork> = emptyList(),
    // Only with details=true:
    public val subjects: List<SubjectFacet> = emptyList(),
    public val places: List<SubjectFacet> = emptyList(),
    public val people: List<SubjectFacet> = emptyList(),
    public val times: List<SubjectFacet> = emptyList(),
    public val authors: List<SubjectFacet> = emptyList(),
    public val publishers: List<SubjectFacet> = emptyList(),
    /** `[[year, count], ...]` as sent; see [publishingHistoryByYear]. */
    @SerialName("publishing_history") public val publishingHistory: List<List<Int>> = emptyList(),
) {
    /** The publishing history as year to number of editions, oldest first. */
    public val publishingHistoryByYear: Map<Int, Int>
        get() = publishingHistory.filter { it.size >= 2 }.associate { it[0] to it[1] }.toSortedMap()
}

/** A work in a [Subject]. Note the cover field is `cover_id` here, not `cover_i` as in search. */
@Serializable
public data class SubjectWork(
    /** The work key, `/works/OL27448W`. */
    public val key: String = "",
    public val title: String = "",
    @SerialName("edition_count") public val editionCount: Int? = null,
    @SerialName("cover_id") public val coverId: Long? = null,
    @SerialName("cover_edition_key") public val coverEditionKey: String? = null,
    public val subject: List<String> = emptyList(),
    public val authors: List<SubjectAuthor> = emptyList(),
    @SerialName("first_publish_year") public val firstPublishYear: Int? = null,
    @SerialName("has_fulltext") public val hasFulltext: Boolean? = null,
    @SerialName("public_scan") public val publicScan: Boolean? = null,
    /** The Internet Archive identifier of a scanned edition, when there is one. */
    public val ia: String? = null,
) {
    /** The work OLID, `OL27448W`. */
    public val olid: String get() = key.substringAfterLast('/')

    /** The authors joined for display. */
    public val authorLine: String get() = authors.joinToString(", ") { it.name }

    /** The cover url of [coverId] (with `default=false`), or `null` when there is no cover. */
    public fun coverUrl(size: CoverSize = CoverSize.M): String? =
        coverId?.takeIf { it > 0 }?.let { OpenLibraryCovers.book(it, size) }
}

/** An author of a [SubjectWork]: `{"key": "/authors/OL79034A", "name": "Frank Herbert"}`. */
@Serializable
public data class SubjectAuthor(
    public val key: String = "",
    public val name: String = "",
) {
    public val olid: String get() = key.substringAfterLast('/')
}

/** A related subject, place, person, time, author or publisher of a [Subject], with how many works share it. */
@Serializable
public data class SubjectFacet(
    /** `/subjects/fiction`, `/authors/OL79034A`, ...; publishers have no key. */
    public val key: String? = null,
    public val name: String = "",
    public val count: Int = 0,
)

internal object SubjectWorkListSerializer : LenientListSerializer<SubjectWork>(SubjectWork.serializer())
