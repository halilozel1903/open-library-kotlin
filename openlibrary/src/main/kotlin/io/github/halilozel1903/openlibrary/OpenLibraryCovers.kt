package io.github.halilozel1903.openlibrary

import io.github.halilozel1903.openlibrary.internal.encodePathSegment

/** Cover sizes of the Covers API. */
public enum class CoverSize(public val code: Char) {
    /** Small, about 45 pixels wide: list thumbnails. */
    S('S'),

    /** Medium, about 180 pixels wide: grids and cards. */
    M('M'),

    /** Large, the original upload, often 500 pixels wide or more: detail screens. */
    L('L'),
}

/**
 * Builds image urls of the [Covers API](https://openlibrary.org/dev/docs/api/covers) (`covers.openlibrary.org`).
 * No request is made; hand the url to Coil, Glide or any image loader.
 *
 * By default the urls end in `?default=false`, so a missing cover answers 404 instead of a blank 1x1 image and your
 * image loader can show its own placeholder. Pass `default = true` to get the blank image instead.
 *
 * Open Library rate limits cover lookups by ISBN, OCLC, LCCN and other identifiers (100 requests per 5 minutes per
 * IP), but not lookups by cover id or OLID. Prefer [book] with a cover id (`cover_i` of a search result, `covers` of
 * a work or edition, `cover_id` of a subject work) and [author] with a photo id.
 */
public object OpenLibraryCovers {
    /** The Covers API root. */
    public const val BASE_URL: String = "https://covers.openlibrary.org"

    /** A book cover by its cover id: `/b/id/{id}-{size}.jpg`. Not rate limited. */
    public fun book(coverId: Long, size: CoverSize = CoverSize.M, default: Boolean = false): String {
        require(coverId > 0) { "Cover ids are positive, was $coverId" }
        return url("b", "id", coverId.toString(), size, default)
    }

    /** A book cover by ISBN-10 or ISBN-13: `/b/isbn/{isbn}-{size}.jpg`. Rate limited by Open Library. */
    public fun bookByIsbn(isbn: String, size: CoverSize = CoverSize.M, default: Boolean = false): String {
        val clean = Isbn.clean(isbn)
        require(clean.isNotEmpty()) { "The ISBN is empty" }
        return url("b", "isbn", clean, size, default)
    }

    /** A book cover by edition OLID (`OL7353617M`): `/b/olid/{olid}-{size}.jpg`. Not rate limited. */
    public fun bookByOlid(editionOlid: String, size: CoverSize = CoverSize.M, default: Boolean = false): String =
        url("b", "olid", OpenLibraryIds.parse(editionOlid, OpenLibraryKind.Edition), size, default)

    /**
     * A book cover by another identifier: `oclc`, `lccn`, `goodreads` or `librarything` (or `isbn`, `olid`, `id`).
     * Lookups other than `id` and `olid` are rate limited by Open Library.
     */
    public fun bookBy(identifier: String, value: String, size: CoverSize = CoverSize.M, default: Boolean = false): String {
        require(identifier in setOf("isbn", "oclc", "lccn", "olid", "id", "goodreads", "librarything")) {
            "Unknown cover identifier \"$identifier\""
        }
        require(value.isNotBlank()) { "The $identifier is empty" }
        return url("b", identifier, value.trim(), size, default)
    }

    /** An author photo by its photo id (`photos` of an author): `/a/id/{id}-{size}.jpg`. Not rate limited. */
    public fun author(photoId: Long, size: CoverSize = CoverSize.M, default: Boolean = false): String {
        require(photoId > 0) { "Photo ids are positive, was $photoId" }
        return url("a", "id", photoId.toString(), size, default)
    }

    /** An author photo by author OLID (`OL26320A`): `/a/olid/{olid}-{size}.jpg`. Not rate limited. */
    public fun authorByOlid(authorOlid: String, size: CoverSize = CoverSize.M, default: Boolean = false): String =
        url("a", "olid", OpenLibraryIds.parse(authorOlid, OpenLibraryKind.Author), size, default)

    private fun url(kind: String, key: String, value: String, size: CoverSize, default: Boolean): String {
        val base = "$BASE_URL/$kind/$key/${encodePathSegment(value)}-${size.code}.jpg"
        return if (default) base else "$base?default=false"
    }
}
