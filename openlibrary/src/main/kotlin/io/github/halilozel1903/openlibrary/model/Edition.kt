package io.github.halilozel1903.openlibrary.model

import io.github.halilozel1903.openlibrary.CoverSize
import io.github.halilozel1903.openlibrary.OpenLibraryCovers
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * An edition, `GET /books/{id}.json` or `GET /isbn/{isbn}.json`: one published book, with its publisher, date,
 * page count and ISBNs. Its [works] link back to the [Work].
 */
@Serializable
public data class Edition(
    public val key: String,
    public val title: String = "",
    public val subtitle: String? = null,
    @SerialName("full_title") public val fullTitle: String? = null,
    @SerialName("by_statement") public val byStatement: String? = null,
    public val publishers: List<String> = emptyList(),
    @SerialName("publish_date") public val publishDate: String? = null,
    @SerialName("publish_places") public val publishPlaces: List<String> = emptyList(),
    @SerialName("number_of_pages") public val numberOfPages: Int? = null,
    public val pagination: String? = null,
    @SerialName("physical_format") public val physicalFormat: String? = null,
    @SerialName("edition_name") public val editionName: String? = null,
    @SerialName("isbn_10") public val isbn10: List<String> = emptyList(),
    @SerialName("isbn_13") public val isbn13: List<String> = emptyList(),
    public val lccn: List<String> = emptyList(),
    @SerialName("oclc_numbers") public val oclcNumbers: List<String> = emptyList(),
    /** Other identifiers by source, such as `goodreads` or `librarything`. */
    public val identifiers: Map<String, List<String>> = emptyMap(),
    /** Cover ids, best first. Removed covers can leave `-1` entries; use [coverId]. */
    public val covers: List<Long> = emptyList(),
    public val languages: List<KeyRef> = emptyList(),
    public val works: List<KeyRef> = emptyList(),
    public val authors: List<KeyRef> = emptyList(),
    public val series: List<String> = emptyList(),
    public val subjects: List<String> = emptyList(),
    @Serializable(with = OpenLibraryTextSerializer::class)
    public val description: String? = null,
    @Serializable(with = OpenLibraryTextSerializer::class)
    public val notes: String? = null,
    @Serializable(with = OpenLibraryTextSerializer::class)
    @SerialName("first_sentence") public val firstSentence: String? = null,
    public val type: KeyRef? = null,
    @SerialName("latest_revision") public val latestRevision: Int? = null,
    public val revision: Int? = null,
    @Serializable(with = OpenLibraryTextSerializer::class)
    public val created: String? = null,
    @Serializable(with = OpenLibraryTextSerializer::class)
    @SerialName("last_modified") public val lastModified: String? = null,
    /** Classifications such as `dewey_decimal_class`, as sent. */
    public val classifications: JsonObject? = null,
) {
    /** The edition's OLID, such as `OL7353617M`. */
    public val olid: String get() = key.substringAfterLast('/')

    /** The first real cover id, or `null` when the edition has no cover. */
    public val coverId: Long? get() = firstPositive(covers)

    /** The ISBN-13, else the ISBN-10, else `null`. */
    public val isbn: String? get() = isbn13.firstOrNull() ?: isbn10.firstOrNull()

    /** Language codes such as `eng` or `fre`. */
    public val languageCodes: List<String> get() = languages.map { it.id }

    /** The OLID of the edition's work, such as `OL27448W`. */
    public val workId: String? get() = works.firstOrNull()?.id

    /** The year in [publishDate] (`"1990"`, `"Aug 01, 1990"`, ...). */
    public val publishYear: Int? get() = yearIn(publishDate)

    /** The cover url of [coverId] (with `default=false`), or `null` when there is no cover. */
    public fun coverUrl(size: CoverSize = CoverSize.M): String? = coverId?.let { OpenLibraryCovers.book(it, size) }

    /** The edition's page on openlibrary.org. */
    public val webUrl: String get() = "https://openlibrary.org$key"
}

/** A page of a work's editions, `GET /works/{id}/editions.json`. */
@Serializable
public data class EditionPage(
    public val links: PageLinks? = null,
    /** How many editions the work has in total. */
    public val size: Int = 0,
    @Serializable(with = EditionListSerializer::class)
    public val entries: List<Edition> = emptyList(),
)

internal object EditionListSerializer : LenientListSerializer<Edition>(Edition.serializer())
