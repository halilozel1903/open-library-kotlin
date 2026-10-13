package io.github.halilozel1903.openlibrary.model

import io.github.halilozel1903.openlibrary.CoverSize
import io.github.halilozel1903.openlibrary.OpenLibraryCovers
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** An author, `GET /authors/{id}.json`. [bio] reads the same as a string or as a typed text value. */
@Serializable
public data class Author(
    public val key: String,
    public val name: String = "",
    @SerialName("personal_name") public val personalName: String? = null,
    @SerialName("fuller_name") public val fullerName: String? = null,
    @SerialName("alternate_names") public val alternateNames: List<String> = emptyList(),
    public val title: String? = null,
    @SerialName("birth_date") public val birthDate: String? = null,
    @SerialName("death_date") public val deathDate: String? = null,
    /** A free-form date or period, used when birth and death dates are unknown. */
    public val date: String? = null,
    @Serializable(with = OpenLibraryTextSerializer::class)
    public val bio: String? = null,
    /** Photo ids for the Covers API. Removed photos can leave `-1` entries; use [photoId]. */
    public val photos: List<Long> = emptyList(),
    public val links: List<Link> = emptyList(),
    public val wikipedia: String? = null,
    /** Ids at other sources, such as `wikidata`, `viaf` or `isni`. */
    @SerialName("remote_ids") public val remoteIds: JsonObject? = null,
    public val type: KeyRef? = null,
    @SerialName("latest_revision") public val latestRevision: Int? = null,
    public val revision: Int? = null,
    @Serializable(with = OpenLibraryTextSerializer::class)
    public val created: String? = null,
    @Serializable(with = OpenLibraryTextSerializer::class)
    @SerialName("last_modified") public val lastModified: String? = null,
) {
    /** The author's OLID, such as `OL26320A`. */
    public val olid: String get() = key.substringAfterLast('/')

    /** The first real photo id, or `null` when there is no photo. */
    public val photoId: Long? get() = firstPositive(photos)

    /** `"1892 – 1973"`, `"born 1948"`, the free-form [date], or `null` when nothing is known. */
    public val lifespan: String?
        get() {
            val born = birthDate?.let { yearIn(it)?.toString() ?: it }
            val died = deathDate?.let { yearIn(it)?.toString() ?: it }
            return when {
                born != null && died != null -> "$born – $died"
                born != null -> "born $born"
                died != null -> "died $died"
                else -> date
            }
        }

    /** The id at another source: `remoteId("wikidata")`. */
    public fun remoteId(source: String): String? = (remoteIds?.get(source) as? JsonPrimitive)?.contentOrNull

    /** The photo url of [photoId] (with `default=false`), or `null` when there is no photo. */
    public fun photoUrl(size: CoverSize = CoverSize.M): String? = photoId?.let { OpenLibraryCovers.author(it, size) }

    /** The author's page on openlibrary.org. */
    public val webUrl: String get() = "https://openlibrary.org$key"
}

/** A page of an author's works, `GET /authors/{id}/works.json`. */
@Serializable
public data class AuthorWorksPage(
    public val links: PageLinks? = null,
    /** How many works the author has in total. */
    public val size: Int = 0,
    @Serializable(with = WorkListSerializer::class)
    public val entries: List<Work> = emptyList(),
)

internal object WorkListSerializer : LenientListSerializer<Work>(Work.serializer())
