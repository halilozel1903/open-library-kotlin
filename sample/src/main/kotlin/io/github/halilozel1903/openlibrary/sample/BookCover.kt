package io.github.halilozel1903.openlibrary.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import kotlinx.coroutines.delay

/** How often a failed image is requested again before it counts as failed. */
private const val ImageRetries = 3

/** Cloth colors for books without a cover, picked by title so a book always gets the same one. */
private val ClothColors = listOf(
    Color(0xFF2F5D50), Color(0xFF8C3B3B), Color(0xFF3D4F7A), Color(0xFF8A6A1F), Color(0xFF5B3F6B), Color(0xFF44633A),
)

/**
 * A book cover from the Covers API. [url] comes from `coverUrl()` of a work, edition or search result, which asks
 * for `default=false`, so a missing cover fails instead of loading a blank image. Without a url the book gets a
 * plain cloth binding with its title, like a library hardback. Screenshot scenes wait until every cover with a url
 * has loaded.
 */
@Composable
fun BookCover(url: String?, title: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(6.dp)
    Box(modifier.clip(shape), contentAlignment = Alignment.Center) {
        if (url.isNullOrEmpty()) {
            ClothCover(title)
        } else {
            RemoteImage(url = url, description = "Cover of $title", placeholder = { ClothCover(title, showTitle = false) })
        }
    }
}

/** An author photo from the Covers API, round, with initials while it loads or when there is none. */
@Composable
fun AuthorPhoto(url: String?, name: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        val initials: @Composable () -> Unit = {
            Text(
                name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() },
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        if (url.isNullOrEmpty()) initials() else RemoteImage(url = url, description = name, placeholder = initials)
    }
}

@Composable
private fun ClothCover(title: String, showTitle: Boolean = true) {
    val color = ClothColors[(title.hashCode() and Int.MAX_VALUE) % ClothColors.size]
    Box(
        Modifier
            .fillMaxSize()
            .background(color),
    ) {
        Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.Center) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.5f)))
            if (showTitle) {
                Text(
                    title,
                    color = Color.White,
                    fontFamily = FontFamily.Serif,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
            } else {
                Box(Modifier.height(24.dp))
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.5f)))
        }
    }
}

/** Loads [url] with Coil, retrying a failed load a few times, and reports to the scene monitor. */
@Composable
private fun RemoteImage(url: String, description: String, placeholder: @Composable () -> Unit) {
    val monitor = LocalSceneMonitor.current
    var attempt by remember(url) { mutableIntStateOf(0) }
    var status by remember(url) { mutableStateOf(ImageStatus.Loading) }
    if (monitor != null) {
        val id = remember { Any() }
        DisposableEffect(monitor, id) { onDispose { monitor.image(id, null) } }
        // snapshotFlow starts with the current value, so a load that finished before this effect ran still counts.
        // A failure that will be retried still counts as loading.
        LaunchedEffect(monitor, id, url) {
            snapshotFlow {
                if (status == ImageStatus.Failed && attempt < ImageRetries) ImageStatus.Loading else status
            }.collect { monitor.image(id, it) }
        }
    }
    if (status == ImageStatus.Failed && attempt < ImageRetries) {
        LaunchedEffect(url, attempt) {
            delay(1_000L shl attempt)
            status = ImageStatus.Loading
            attempt++
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (status != ImageStatus.Loaded) placeholder()
        key(url, attempt) {
            AsyncImage(
                model = url,
                contentDescription = description,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onState = { state ->
                    status = when (state) {
                        is AsyncImagePainter.State.Success -> ImageStatus.Loaded
                        is AsyncImagePainter.State.Error -> ImageStatus.Failed
                        else -> ImageStatus.Loading
                    }
                },
            )
        }
    }
}
