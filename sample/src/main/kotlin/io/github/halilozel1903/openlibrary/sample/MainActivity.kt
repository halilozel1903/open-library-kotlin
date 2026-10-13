package io.github.halilozel1903.openlibrary.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier

/**
 * Shelf, a reading list app built on open-library-kotlin. It searches the live Open Library, shows works with their
 * authors and editions, browses subjects, and keeps a reading list on the device. `scripts/screenshots.sh` starts
 * it with `--es scene <scene>` to open a fixed screen, loaded live like a normal launch:
 *
 * - `search`: results for "dune" with their covers
 * - `work`: Frank Herbert's Dune with its author and editions
 * - `subject`: the science fiction shelf with top authors and related subjects
 * - `reading`: a reading list of six classics, each looked up live
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val scene = Scene.from(intent.getStringExtra(EXTRA_SCENE))
        val context = applicationContext
        setContent {
            ShelfTheme(dark = isSystemInDarkTheme()) {
                // The Surface makes text default to onBackground, so it stays readable in dark mode.
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    ShelfApp(
                        scene = scene,
                        repository = if (scene != null) Repositories.scenes() else Repositories.live(),
                        readingList = if (scene != null) ReadingListStore.forScenes(context) else ReadingListStore.forApp(context),
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_SCENE = "scene"
    }
}
