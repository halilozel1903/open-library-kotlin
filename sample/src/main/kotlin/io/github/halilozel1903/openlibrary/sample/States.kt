package io.github.halilozel1903.openlibrary.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.openlibrary.OpenLibraryException
import kotlinx.coroutines.CancellationException

/** The state of something loading. */
sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Ready<T>(val value: T) : Load<T>
    data class Failed(val message: String) : Load<Nothing>
}

/** Runs [block] and wraps its result or a readable error. */
suspend fun <T> load(block: suspend () -> T): Load<T> = try {
    Load.Ready(block())
} catch (e: CancellationException) {
    throw e
} catch (e: OpenLibraryException.NotFound) {
    Load.Failed("Open Library has no record for this.")
} catch (e: OpenLibraryException.RateLimited) {
    Load.Failed("Open Library is busy. Try again in a moment.")
} catch (e: OpenLibraryException.Network) {
    Load.Failed("Could not reach Open Library. Check your connection.")
} catch (e: OpenLibraryException) {
    Load.Failed(e.message ?: "Open Library returned an error.")
} catch (e: NoSuchElementException) {
    Load.Failed(e.message ?: "Nothing was found.")
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun MessageState(title: String, message: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Column(
        modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        action?.invoke()
    }
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    MessageState("Something went wrong", message, modifier) {
        Button(onClick = onRetry) { Text("Try again") }
    }
}
