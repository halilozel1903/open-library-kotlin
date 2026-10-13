package io.github.halilozel1903.openlibrary.sample

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** The reading list stored on the device, filtered by status. Tapping a status chip moves the book on. */
@Composable
fun ReadingListScreen(
    books: List<SavedBook>,
    loading: Boolean,
    wide: Boolean,
    onOpen: (String) -> Unit,
    onStatusChange: (String, ReadingStatus) -> Unit,
    onRemove: (String) -> Unit,
) {
    // null shows every book.
    var filter by rememberSaveable { mutableStateOf<ReadingStatus?>(null) }
    if (loading) {
        LoadingState()
        return
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp)) {
            Text("Reading list", style = MaterialTheme.typography.headlineMedium)
            Text(
                "${books.size} ${if (books.size == 1) "book" else "books"} · saved on this device",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("All ${books.size}") })
            ReadingStatus.entries.forEach { status ->
                val count = books.count { it.status == status }
                FilterChip(
                    selected = filter == status,
                    onClick = { filter = if (filter == status) null else status },
                    label = { Text("${status.label} $count") },
                )
            }
        }
        val shown = books.filter { filter == null || it.status == filter }
        if (books.isEmpty()) {
            MessageState("Your reading list is empty", "Search for a book or browse a subject, then add it to your list.")
        } else {
            LazyVerticalGrid(
                columns = if (wide) GridCells.Adaptive(minSize = 380.dp) else GridCells.Fixed(1),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(shown, key = { it.workId }) { book ->
                    SavedBookRow(
                        book = book,
                        onClick = { onOpen(book.workId) },
                        onCycleStatus = { onStatusChange(book.workId, book.status.next) },
                        onRemove = { onRemove(book.workId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedBookRow(book: SavedBook, onClick: () -> Unit, onCycleStatus: () -> Unit, onRemove: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BookCover(book.coverUrl(), book.title, Modifier.width(64.dp).height(96.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(book.authors.takeIf { it.isNotEmpty() }, book.year?.toString()).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AssistChip(
                    onClick = onCycleStatus,
                    label = { Text(book.status.label) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = book.status.containerColor(),
                        labelColor = book.status.contentColor(),
                    ),
                    border = null,
                )
            }
            IconButton(onClick = onRemove, modifier = Modifier.align(Alignment.Top)) {
                CloseIcon(MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ReadingStatus.containerColor(): Color = when (this) {
    ReadingStatus.WantToRead -> MaterialTheme.colorScheme.tertiaryContainer
    ReadingStatus.Reading -> MaterialTheme.colorScheme.primaryContainer
    ReadingStatus.Finished -> MaterialTheme.colorScheme.secondaryContainer
}

@Composable
private fun ReadingStatus.contentColor(): Color = when (this) {
    ReadingStatus.WantToRead -> MaterialTheme.colorScheme.onTertiaryContainer
    ReadingStatus.Reading -> MaterialTheme.colorScheme.onPrimaryContainer
    ReadingStatus.Finished -> MaterialTheme.colorScheme.onSecondaryContainer
}
