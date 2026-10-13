package io.github.halilozel1903.openlibrary.sample

/** The app's sections. */
enum class Destination(val label: String) {
    Search("Search"),
    Subjects("Subjects"),
    ReadingList("Reading list"),
}

/**
 * Screenshot scenes, picked with the `scene` intent extra. They load live data from Open Library through the same
 * client as a normal launch; nothing is bundled or faked.
 */
enum class Scene(val key: String, val destination: Destination) {
    /** Search results for [SEARCH_QUERY] with their covers. */
    Search("search", Destination.Search),

    /** The work found by searching [WORK_TITLE] by [WORK_AUTHOR], with its authors and editions. */
    Work("work", Destination.Search),

    /** The [SUBJECT] shelf with details. */
    Subject("subject", Destination.Subjects),

    /** A reading list of [READING_LIST] books, each looked up live by title and author. */
    Reading("reading", Destination.ReadingList),
    ;

    companion object {
        const val SEARCH_QUERY = "dune"
        const val WORK_TITLE = "Dune"
        const val WORK_AUTHOR = "Frank Herbert"
        const val SUBJECT = "science_fiction"

        /** Title, author and the status each gets on the list. */
        val READING_LIST: List<Triple<String, String, ReadingStatus>> = listOf(
            Triple("Dune", "Frank Herbert", ReadingStatus.Reading),
            Triple("The Left Hand of Darkness", "Ursula K. Le Guin", ReadingStatus.WantToRead),
            Triple("Kindred", "Octavia E. Butler", ReadingStatus.Reading),
            Triple("Foundation", "Isaac Asimov", ReadingStatus.Finished),
            Triple("Neuromancer", "William Gibson", ReadingStatus.WantToRead),
            Triple("The Hobbit", "J.R.R. Tolkien", ReadingStatus.Finished),
        )

        fun from(key: String?): Scene? = entries.firstOrNull { it.key == key }
    }
}
