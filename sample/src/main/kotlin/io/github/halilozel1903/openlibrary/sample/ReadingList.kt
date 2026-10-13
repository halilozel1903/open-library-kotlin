package io.github.halilozel1903.openlibrary.sample

import android.content.Context
import io.github.halilozel1903.openlibrary.CoverSize
import io.github.halilozel1903.openlibrary.OpenLibraryCovers
import io.github.halilozel1903.openlibrary.model.SearchDoc
import io.github.halilozel1903.openlibrary.model.Work
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

enum class ReadingStatus(val label: String) {
    WantToRead("Want to read"),
    Reading("Reading"),
    Finished("Finished"),
    ;

    /** The next status, for a chip that cycles through them. */
    val next: ReadingStatus get() = entries[(ordinal + 1) % entries.size]
}

/** A book on the reading list: a snapshot of the work, so the list shows without a network request. */
data class SavedBook(
    val workId: String,
    val title: String,
    val authors: String,
    val coverId: Long? = null,
    val year: Int? = null,
    val status: ReadingStatus = ReadingStatus.WantToRead,
    val addedAt: Long = 0,
) {
    fun coverUrl(size: CoverSize = CoverSize.M): String? = coverId?.takeIf { it > 0 }?.let { OpenLibraryCovers.book(it, size) }

    companion object {
        fun from(doc: SearchDoc, status: ReadingStatus, addedAt: Long) = SavedBook(
            workId = doc.olid,
            title = doc.title,
            authors = doc.authorLine,
            coverId = doc.coverId,
            year = doc.firstPublishYear,
            status = status,
            addedAt = addedAt,
        )

        fun from(work: Work, authors: String, status: ReadingStatus, addedAt: Long) = SavedBook(
            workId = work.olid,
            title = work.title,
            authors = authors,
            coverId = work.coverId,
            year = work.firstPublishYear,
            status = status,
            addedAt = addedAt,
        )

        fun fromJson(json: JSONObject) = SavedBook(
            workId = json.getString("workId"),
            title = json.optString("title"),
            authors = json.optString("authors"),
            coverId = json.optLong("coverId", 0L).takeIf { it > 0 },
            year = json.optInt("year", 0).takeIf { it > 0 },
            status = ReadingStatus.entries.firstOrNull { it.name == json.optString("status") } ?: ReadingStatus.WantToRead,
            addedAt = json.optLong("addedAt", 0L),
        )
    }

    fun toJson(): JSONObject = JSONObject()
        .put("workId", workId)
        .put("title", title)
        .put("authors", authors)
        .put("coverId", coverId ?: 0L)
        .put("year", year ?: 0)
        .put("status", status.name)
        .put("addedAt", addedAt)
}

/**
 * The reading list, stored on the device as JSON (org.json) in SharedPreferences. Newest first. The UI observes [books].
 */
class ReadingListStore private constructor(context: Context, name: String) {
    private val prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())

    val books: StateFlow<List<SavedBook>> = state.asStateFlow()

    fun get(workId: String): SavedBook? = state.value.firstOrNull { it.workId == workId }

    /** Adds [book], or updates it when the work is already on the list. */
    @Synchronized
    fun save(book: SavedBook) {
        val others = state.value.filterNot { it.workId == book.workId }
        val existing = get(book.workId)
        write(if (existing != null) state.value.map { if (it.workId == book.workId) book else it } else listOf(book) + others)
    }

    @Synchronized
    fun setStatus(workId: String, status: ReadingStatus) {
        write(state.value.map { if (it.workId == workId) it.copy(status = status) else it })
    }

    @Synchronized
    fun remove(workId: String) {
        write(state.value.filterNot { it.workId == workId })
    }

    @Synchronized
    fun replaceAll(books: List<SavedBook>) {
        write(books.distinctBy { it.workId })
    }

    private fun read(): List<SavedBook> {
        val text = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val array = JSONArray(text)
            List(array.length()) { SavedBook.fromJson(array.getJSONObject(it)) }
        } catch (e: JSONException) {
            emptyList()
        }
    }

    private fun write(books: List<SavedBook>) {
        val array = JSONArray()
        books.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY, array.toString()).apply()
        state.value = books
    }

    companion object {
        private const val KEY = "books"
        private var app: ReadingListStore? = null
        private var scenes: ReadingListStore? = null

        @Synchronized
        fun forApp(context: Context): ReadingListStore =
            app ?: ReadingListStore(context.applicationContext, "reading_list").also { app = it }

        /** Screenshot scenes keep their own list, so they never touch the one you built. */
        @Synchronized
        fun forScenes(context: Context): ReadingListStore =
            scenes ?: ReadingListStore(context.applicationContext, "reading_list_scenes").also { scenes = it }
    }
}
