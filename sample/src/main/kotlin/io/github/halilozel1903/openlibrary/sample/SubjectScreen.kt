package io.github.halilozel1903.openlibrary.sample

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.openlibrary.OpenLibraryClient
import io.github.halilozel1903.openlibrary.model.Subject
import io.github.halilozel1903.openlibrary.model.SubjectFacet
import java.text.NumberFormat

/** Subjects offered as chips; any Open Library subject works. */
private val Shelves = listOf(
    "science_fiction" to "Science fiction",
    "fantasy" to "Fantasy",
    "mystery_and_detective_stories" to "Mystery",
    "historical_fiction" to "Historical fiction",
    "poetry" to "Poetry",
    "biography" to "Biography",
    "cooking" to "Cooking",
)

/** `/subjects/{name}.json?details=true` as a shelf of covers, with top authors and related subjects beside it. */
@Composable
fun SubjectScreen(
    repository: LibraryRepository,
    subject: String,
    onSubjectChange: (String) -> Unit,
    wide: Boolean,
    onOpen: (String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Shelves.forEach { (slug, label) ->
                FilterChip(selected = slug == subject, onClick = { onSubjectChange(slug) }, label = { Text(label) })
            }
        }
        key(subject) { SubjectShelf(repository, subject, wide, onSubjectChange, onOpen) }
    }
}

@Composable
private fun SubjectShelf(
    repository: LibraryRepository,
    subject: String,
    wide: Boolean,
    onSubjectChange: (String) -> Unit,
    onOpen: (String) -> Unit,
) {
    var reload by remember { mutableIntStateOf(0) }
    val shelf by produceState<Load<Subject>>(Load.Loading, subject, reload) {
        value = Load.Loading
        value = load { repository.subject(subject) }
    }
    ReportLoad(shelf)
    when (val state = shelf) {
        Load.Loading -> LoadingState()
        is Load.Failed -> ErrorState(state.message, onRetry = { reload++ })
        is Load.Ready -> {
            val data = state.value
            if (wide) {
                Row(Modifier.fillMaxSize()) {
                    WorksGrid(data, onOpen, Modifier.weight(1f))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .width(300.dp)
                            .fillMaxHeight()
                            .padding(end = 20.dp, bottom = 20.dp),
                    ) {
                        Details(data, onSubjectChange, Modifier.verticalScroll(rememberScrollState()).padding(20.dp))
                    }
                }
            } else {
                WorksGrid(data, onOpen, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun WorksGrid(data: Subject, onOpen: (String) -> Unit, modifier: Modifier) {
    val number = NumberFormat.getIntegerInstance()
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 140.dp),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = modifier,
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Text(data.name, style = MaterialTheme.typography.displaySmall)
                Text(
                    listOfNotNull(
                        "${number.format(data.workCount)} works",
                        data.ebookCount?.let { "${number.format(it)} ebooks" },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(data.works) { work ->
            BookCard(
                title = work.title,
                coverUrl = work.coverUrl(),
                author = work.authorLine,
                meta = listOfNotNull(
                    work.firstPublishYear?.toString(),
                    work.editionCount?.let { if (it == 1) "1 edition" else "$it editions" },
                ).joinToString(" · "),
                onClick = { onOpen(work.olid) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Details(data: Subject, onSubjectChange: (String) -> Unit, modifier: Modifier) {
    val number = NumberFormat.getIntegerInstance()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (data.authors.isNotEmpty()) {
            Text("Top authors", style = MaterialTheme.typography.titleLarge)
            data.authors.take(8).forEach { author ->
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        author.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${number.format(author.count)} works",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        FacetSection("Related subjects", data.subjects, onSubjectChange)
        FacetSection("Places", data.places, onSubjectChange)
        FacetSection("Times", data.times, onSubjectChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FacetSection(title: String, facets: List<SubjectFacet>, onSubjectChange: (String) -> Unit) {
    val shown = facets.filter { it.key != null }.take(8)
    if (shown.isEmpty()) return
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        shown.forEach { facet ->
            FilterChip(
                selected = false,
                onClick = { onSubjectChange(OpenLibraryClient.subjectSlug(facet.key!!.removePrefix("/subjects/"))) },
                label = { Text(facet.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            )
        }
    }
}
