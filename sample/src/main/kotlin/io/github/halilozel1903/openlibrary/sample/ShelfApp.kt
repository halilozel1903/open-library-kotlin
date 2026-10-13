package io.github.halilozel1903.openlibrary.sample

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The whole app: Search, Subjects and the Reading list, with a navigation rail on tablets and a bottom bar on
 * phones. Opening a book shows its work screen in place of the section. Screenshot [scene]s start on a fixed
 * section and report to a [SceneMonitor] once their data and covers have loaded.
 */
@Composable
fun ShelfApp(scene: Scene?, repository: LibraryRepository, readingList: ReadingListStore) {
    val monitor = remember(scene) { scene?.let { SceneMonitor(it) } }
    monitor?.Report()

    var destination by rememberSaveable { mutableStateOf(scene?.destination ?: Destination.Search) }
    var openWork by rememberSaveable { mutableStateOf<String?>(null) }
    val startQuery = if (scene == Scene.Search) Scene.SEARCH_QUERY else ""
    var query by rememberSaveable { mutableStateOf(startQuery) }
    var submitted by rememberSaveable { mutableStateOf(startQuery) }
    var subject by rememberSaveable { mutableStateOf(Scene.SUBJECT) }
    val books by readingList.books.collectAsState()

    CompositionLocalProvider(LocalSceneMonitor provides monitor) {
        // The work scene finds Dune with a live search, then opens it like a tap on a search result would.
        var sceneReload by remember { mutableIntStateOf(0) }
        val sceneSetup by produceState<Load<String?>>(Load.Loading, scene, sceneReload) {
            value = Load.Loading
            value = when (scene) {
                Scene.Work -> load { repository.findWork(Scene.WORK_TITLE, Scene.WORK_AUTHOR).olid }
                Scene.Reading -> load {
                    readingList.replaceAll(repository.sceneReadingList(System.currentTimeMillis()))
                    null
                }
                else -> Load.Ready(null)
            }
        }
        if (scene == Scene.Work || scene == Scene.Reading) ReportLoad(sceneSetup)
        LaunchedEffect(sceneSetup) {
            val id = (sceneSetup as? Load.Ready)?.value
            if (scene == Scene.Work && id != null && openWork == null) openWork = id
        }

        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            val wide = maxWidth >= 840.dp
            val select: (Destination) -> Unit = {
                destination = it
                openWork = null
            }
            val content: @Composable () -> Unit = {
                val work = openWork
                val setup = sceneSetup
                when {
                    setup is Load.Failed -> ErrorState(setup.message, onRetry = { sceneReload++ })
                    scene == Scene.Work && work == null -> LoadingState()
                    work != null -> {
                        BackHandler { openWork = null }
                        WorkScreen(
                            repository = repository,
                            workId = work,
                            readingList = readingList,
                            wide = wide,
                            onBack = { openWork = null },
                        )
                    }
                    destination == Destination.Search -> SearchScreen(
                        repository = repository,
                        query = query,
                        onQueryChange = { query = it },
                        submitted = submitted,
                        onSubmit = { submitted = it.trim() },
                        onOpen = { openWork = it },
                    )
                    destination == Destination.Subjects -> SubjectScreen(
                        repository = repository,
                        subject = subject,
                        onSubjectChange = { subject = it },
                        wide = wide,
                        onOpen = { openWork = it },
                    )
                    else -> ReadingListScreen(
                        books = books,
                        loading = scene == Scene.Reading && setup !is Load.Ready,
                        wide = wide,
                        onOpen = { openWork = it },
                        onStatusChange = readingList::setStatus,
                        onRemove = readingList::remove,
                    )
                }
            }
            if (wide) {
                Row(Modifier.fillMaxSize()) {
                    ShelfRail(destination, books.size, select)
                    Box(Modifier.weight(1f).fillMaxHeight()) { content() }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxSize()) { content() }
                    ShelfBottomBar(destination, select)
                }
            }
        }
    }
}

@Composable
private fun DestinationIcon(destination: Destination, selected: Boolean) {
    val color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val modifier = Modifier.size(24.dp)
    when (destination) {
        Destination.Search -> SearchIcon(color, modifier)
        Destination.Subjects -> ShelfIcon(color, modifier)
        Destination.ReadingList -> BookmarkIcon(color, modifier, filled = selected)
    }
}

@Composable
private fun ShelfRail(current: Destination, saved: Int, onSelect: (Destination) -> Unit) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        windowInsets = WindowInsets(0, 0, 0, 0),
        header = {
            Text(
                "Shelf",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(12.dp))
        },
    ) {
        Destination.entries.forEach { destination ->
            NavigationRailItem(
                selected = destination == current,
                onClick = { onSelect(destination) },
                icon = { DestinationIcon(destination, destination == current) },
                label = {
                    Text(if (destination == Destination.ReadingList && saved > 0) "${destination.label} ($saved)" else destination.label)
                },
            )
        }
    }
}

@Composable
private fun ShelfBottomBar(current: Destination, onSelect: (Destination) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        Destination.entries.forEach { destination ->
            NavigationBarItem(
                selected = destination == current,
                onClick = { onSelect(destination) },
                icon = { DestinationIcon(destination, destination == current) },
                label = { Text(destination.label) },
            )
        }
    }
}
