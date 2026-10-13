package io.github.halilozel1903.openlibrary.model

import io.github.halilozel1903.openlibrary.CoverSize
import io.github.halilozel1903.openlibrary.OpenLibraryCovers
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A work, `GET /works/{id}.json`: the book as an idea (*Dune*), with all its editions. Fields that are text in
 * Open Library (`description`, `created`, ...) read the same whether they arrive as a string or as
 * `{"type": "/type/text", "value": "..."}`.
 */
@Serializable
public data class Work(
    public val key: String,
    public val title: String = "",
    public val subtitle: String? = null,
    public val authors: List<WorkAuthor> = emptyList(),
    @Serializable(with = OpenLibraryTextSerializer::class)
    public val description: String? = null,
    /** Cover ids, best first. Removed covers can leave `-1` entries; use [coverId]. */
    public val covers: List<Long> = emptyList(),
    public val subjects: List<String> = emptyList(),
    @SerialName("subject_places") public val subjectPlaces: List<String> = emptyList(),
    @SerialName("subject_people") public val subjectPeople: List<String> = emptyList(),
    @SerialName("subject_times") public val subjectTimes: List<String> = emptyList(),
    @SerialName("first_publish_date") public val firstPublishDate: String? = null,
    public val links: List<Link> = emptyList(),
    @Serializable(with = ExcerptListSerializer::class)
    public val excerpts: List<Excerpt> = emptyList(),
    public val type: KeyRef? = null,
    @SerialName("latest_revision") public val latestRevision: Int? = null,
    public val revision: Int? = null,
    @Serializable(with = OpenLibraryTextSerializer::class)
    public val created: String? = null,
    @Serializable(with = OpenLibraryTextSerializer::class)
    @SerialName("last_modified") public val lastModified: String? = null,
) {
    /** The work's OLID, such as `OL27448W`. */
    public val olid: String get() = key.substringAfterLast('/')

    /** The first real cover id, or `null` when the work has no cover. */
    public val coverId: Long? get() = firstPositive(covers)

    /** The author OLIDs, in order (`OL26320A`). */
    public val authorIds: List<String> get() = authors.mapNotNull { it.author?.id?.takeIf(String::isNotEmpty) }

    /** The year in [firstPublishDate], when it has one. */
    public val firstPublishYear: Int? get() = yearIn(firstPublishDate)

    /** The cover url of [coverId] (with `default=false`), or `null` when there is no cover. */
    public fun coverUrl(size: CoverSize = CoverSize.M): String? = coverId?.let { OpenLibraryCovers.book(it, size) }

    /** The work's page on openlibrary.org. */
    public val webUrl: String get() = "https://openlibrary.org$key"
}

/** One author of a [Work]: `{"author": {"key": "/authors/OL26320A"}, "type": {"key": "/type/author_role"}}`. */
@Serializable
public data class WorkAuthor(
    public val author: KeyRef? = null,
    public val type: KeyRef? = null,
)

/** An excerpt of a work. */
@Serializable
public data class Excerpt(
    @Serializable(with = OpenLibraryTextSerializer::class)
    public val excerpt: String = "",
    public val comment: String? = null,
    public val author: KeyRef? = null,
)

internal object ExcerptListSerializer : LenientListSerializer<Excerpt>(Excerpt.serializer())
