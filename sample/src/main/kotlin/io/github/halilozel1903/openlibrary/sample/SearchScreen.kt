package io.github.halilozel1903.openlibrary.sample

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.openlibrary.model.SearchDoc
import io.github.halilozel1903.openlibrary.model.SearchResult
import java.text.NumberFormat

private val Suggestions = listOf("Ursula K. Le Guin", "Dune", "Octavia Butler", "The Hobbit", "Station Eleven", "Borges")

/** `/search.json` as a grid of covers. Typing edits [query]; the keyboard's search key submits it. */
@Composable
fun SearchScreen(
    repository: LibraryRepository,
    query: String,
    onQueryChange: (String) -> Unit,
    submitted: String,
    onSubmit: (String) -> Unit,
    onOpen: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            placeholder = { Text("Search titles, authors, ISBNs") },
            leadingIcon = { SearchIcon(MaterialTheme.colorScheme.onSurfaceVariant, Modifier.size(22.dp)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit(query) }),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
        )
        if (submitted.isBlank()) {
            SearchHome(onPick = { onQueryChange(it); onSubmit(it) })
        } else {
            key(submitted) { SearchResults(repository, submitted, onOpen) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchHome(onPick: (String) -> Unit) {
    Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Find your next book", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Search millions of books in Open Library, the open catalog run by the Internet Archive.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Suggestions.forEach { suggestion ->
                SuggestionChip(onClick = { onPick(suggestion) }, label = { Text(suggestion) })
            }
        }
    }
}

@Composable
private fun SearchResults(repository: LibraryRepository, submitted: String, onOpen: (String) -> Unit) {
    var reload by remember { mutableIntStateOf(0) }
    val result by produceState<Load<SearchResult>>(Load.Loading, submitted, reload) {
        value = Load.Loading
        value = load { repository.search(submitted) }
    }
    ReportLoad(result)
    when (val state = result) {
        Load.Loading -> LoadingState()
        is Load.Failed -> ErrorState(state.message, onRetry = { reload++ })
        is Load.Ready -> {
            val docs = state.value.docs
            if (docs.isEmpty()) {
                MessageState("No books found", "Open Library has nothing for “$submitted”. Try fewer words.")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 156.dp),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "${NumberFormat.getIntegerInstance().format(state.value.numFound)} results for “$submitted”",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(docs) { doc ->
                        BookCard(
                            title = doc.title,
                            coverUrl = doc.coverUrl(),
                            author = doc.authorLine,
                            meta = doc.metaLine(),
                            onClick = { onOpen(doc.olid) },
                        )
                    }
                }
            }
        }
    }
}

private fun SearchDoc.metaLine(): String = listOfNotNull(
    firstPublishYear?.toString(),
    editionCount?.let { if (it == 1) "1 edition" else "$it editions" },
).joinToString(" · ")

/** A cover with the title, author and a meta line under it. */
@Composable
fun BookCard(title: String, coverUrl: String?, author: String, meta: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clickable(onClick = onClick)
            .fillMaxWidth(),
    ) {
        BookCover(
            url = coverUrl,
            title = title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (author.isNotEmpty()) {
            Text(
                author,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (meta.isNotEmpty()) {
            Text(
                meta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
            )
        }
    }
}
