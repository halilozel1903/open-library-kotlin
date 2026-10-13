package io.github.halilozel1903.openlibrary.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.openlibrary.CoverSize
import io.github.halilozel1903.openlibrary.model.Author
import io.github.halilozel1903.openlibrary.model.Edition
import java.text.NumberFormat

/** A work with its authors and first editions: `/works/{id}.json`, `/authors/{id}.json` and `/works/{id}/editions.json`. */
@Composable
fun WorkScreen(
    repository: LibraryRepository,
    workId: String,
    readingList: ReadingListStore,
    wide: Boolean,
    onBack: () -> Unit,
) {
    var reload by remember { mutableIntStateOf(0) }
    val detail by produceState<Load<WorkDetail>>(Load.Loading, workId, reload) {
        value = Load.Loading
        value = load { repository.workDetail(workId) }
    }
    ReportLoad(detail)
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                BackIcon(MaterialTheme.colorScheme.onSurface, Modifier.size(24.dp))
            }
            Text("Back", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        when (val state = detail) {
            Load.Loading -> LoadingState()
            is Load.Failed -> ErrorState(state.message, onRetry = { reload++ })
            is Load.Ready -> WorkContent(state.value, readingList, wide)
        }
    }
}

@Composable
private fun WorkContent(detail: WorkDetail, readingList: ReadingListStore, wide: Boolean) {
    val books by readingList.books.collectAsState()
    val saved = books.firstOrNull { it.workId == detail.work.olid }
    val onStatus: (ReadingStatus) -> Unit = { status ->
        if (saved?.status == status) {
            readingList.remove(detail.work.olid)
        } else {
            readingList.save(
                SavedBook.from(
                    detail.work,
                    authors = detail.authors.joinToString(", ") { it.name },
                    status = status,
                    addedAt = saved?.addedAt ?: System.currentTimeMillis(),
                ),
            )
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (wide && maxWidth >= 960.dp) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
            ) {
                Column(
                    Modifier
                        .width(260.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    BookCover(
                        url = detail.work.coverUrl(CoverSize.L),
                        title = detail.work.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(2f / 3f),
                    )
                    ReadingControls(saved?.status, onStatus)
                }
                LazyColumn(
                    Modifier.weight(1.15f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    item { TitleBlock(detail) }
                    item { Facts(detail) }
                    items(detail.authors) { AuthorRow(it) }
                    detail.work.description?.let { description ->
                        item { Description(description) }
                    }
                    item { Subjects(detail.work.subjects) }
                }
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    EditionsHeader(detail)
                    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                        items(detail.editions.entries) { edition ->
                            EditionRow(edition)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        BookCover(
                            url = detail.work.coverUrl(CoverSize.L),
                            title = detail.work.title,
                            modifier = Modifier
                                .width(128.dp)
                                .aspectRatio(2f / 3f),
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            TitleBlock(detail)
                        }
                    }
                }
                item { Facts(detail) }
                item { ReadingControls(saved?.status, onStatus) }
                items(detail.authors) { AuthorRow(it) }
                detail.work.description?.let { description ->
                    item { Description(description, maxLines = 8) }
                }
                item { Subjects(detail.work.subjects) }
                item { EditionsHeader(detail) }
                items(detail.editions.entries) { edition ->
                    EditionRow(edition)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun TitleBlock(detail: WorkDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(detail.work.title, style = MaterialTheme.typography.headlineLarge)
        detail.work.subtitle?.let {
            Text(it, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (detail.authors.isNotEmpty()) {
            Text(
                "by " + detail.authors.joinToString(", ") { it.name },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Facts(detail: WorkDetail) {
    val number = NumberFormat.getIntegerInstance()
    FlowRow(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        detail.work.firstPublishYear?.let { Fact("First published", it.toString()) }
        Fact("Editions", number.format(detail.editions.size))
        if (detail.work.subjects.isNotEmpty()) Fact("Subjects", number.format(detail.work.subjects.size))
        Fact("Work", detail.work.olid)
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReadingControls(status: ReadingStatus?, onStatus: (ReadingStatus) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            if (status == null) "Add to your reading list" else "On your reading list",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReadingStatus.entries.forEach { option ->
                FilterChip(
                    selected = status == option,
                    onClick = { onStatus(option) },
                    label = { Text(option.label, maxLines = 1) },
                )
            }
        }
    }
}

@Composable
private fun AuthorRow(author: Author) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        AuthorPhoto(author.photoUrl(CoverSize.M), author.name, Modifier.size(56.dp))
        Column(Modifier.weight(1f)) {
            Text(author.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            author.lifespan?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            author.bio?.let {
                Text(
                    cleanText(it),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun Description(text: String, maxLines: Int = 14) {
    Text(
        cleanText(text),
        style = MaterialTheme.typography.bodyLarge,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Subjects(subjects: List<String>) {
    if (subjects.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        subjects.take(12).forEach { Pill(it) }
    }
}

/** A small rounded label. */
@Composable
fun Pill(text: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun EditionsHeader(detail: WorkDetail) {
    Column(Modifier.padding(bottom = 8.dp)) {
        Text("Editions", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Showing ${detail.editions.entries.size} of ${NumberFormat.getIntegerInstance().format(detail.editions.size)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EditionRow(edition: Edition) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BookCover(edition.coverUrl(CoverSize.M), edition.title, Modifier.width(52.dp).height(78.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(edition.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val published = listOfNotNull(edition.publishers.firstOrNull(), edition.publishDate).joinToString(" · ")
            if (published.isNotEmpty()) {
                Text(published, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            val physical = listOfNotNull(
                edition.numberOfPages?.let { "$it pages" },
                edition.physicalFormat,
                edition.languageCodes.takeIf { it.isNotEmpty() }?.joinToString("/") { it.uppercase() },
            ).joinToString(" · ")
            if (physical.isNotEmpty()) {
                Text(physical, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            edition.isbn?.let {
                Text("ISBN $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/**
 * Open Library descriptions are user edited Markdown-ish text, often with a source footer and reference links.
 * Keep the prose: drop everything after a `----------` rule, reference definitions and `[text][1]` link syntax.
 */
fun cleanText(text: String): String = text
    .substringBefore("\n----")
    .lines()
    .filterNot { Regex("""^\s*\[\d+]:\s*\S+""").containsMatchIn(it) }
    .joinToString("\n")
    .replace(Regex("""\[([^\]]+)]\[\d+]"""), "$1")
    .replace(Regex("""\[([^\]]+)]\((https?://[^)]+)\)"""), "$1")
    .replace(Regex("""\(\s*\[?source]?[^)]*\)""", RegexOption.IGNORE_CASE), "")
    .replace("\r", "")
    .replace(Regex("""\n{3,}"""), "\n\n")
    .trim()
