package io.github.halilozel1903.openlibrary

/** The kinds of Open Library records, with the letter their ids end in. */
public enum class OpenLibraryKind(public val suffix: Char, public val path: String) {
    /** A work: the book as an idea, with all its editions. `OL27448W`, `/works/OL27448W`. */
    Work('W', "works"),

    /** An edition: one published book (`/books/` in Open Library). `OL7353617M`, `/books/OL7353617M`. */
    Edition('M', "books"),

    /** An author. `OL26320A`, `/authors/OL26320A`. */
    Author('A', "authors"),
    ;

    public companion object {
        /** The kind of [olid] from its last letter, or `null` when it is not an Open Library id. */
        public fun of(olid: String): OpenLibraryKind? {
            val id = OpenLibraryIds.find(olid) ?: return null
            return entries.firstOrNull { it.suffix == id.last() }
        }
    }
}

/** Parses Open Library ids out of keys and urls. */
public object OpenLibraryIds {
    private val pattern = Regex("""OL\d+[WMA]""")

    /**
     * The first Open Library id in [text]: `"OL27448W"`, `"/works/OL27448W"`,
     * `"https://openlibrary.org/works/OL27448W/The_Lord_of_the_Rings"` all give `"OL27448W"`. `null` when there is none.
     */
    public fun find(text: String): String? = pattern.find(text.uppercase())?.value

    /**
     * The id in [text], checked to be of [kind]. Throws [IllegalArgumentException] for anything else, so a typo
     * fails before a request is made.
     */
    public fun parse(text: String, kind: OpenLibraryKind): String {
        val id = find(text)
        require(id != null && id.last() == kind.suffix) {
            "\"$text\" is not an Open Library ${kind.name.lowercase()} id (like OL123${kind.suffix})"
        }
        return id
    }

    /** The record key, like `/works/OL27448W`. */
    public fun key(olid: String, kind: OpenLibraryKind): String = "/${kind.path}/${parse(olid, kind)}"

    /** The page of the record on openlibrary.org, for a browser. */
    public fun webUrl(olid: String, kind: OpenLibraryKind): String = "https://openlibrary.org${key(olid, kind)}"
}

