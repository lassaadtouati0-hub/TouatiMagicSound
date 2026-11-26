package com.touati.magicsound

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.touati.magicsound.data.AppDatabase
import com.touati.magicsound.data.TileState
import com.touati.magicsound.service.PlaybackController
import com.touati.magicsound.ui.MainScreen
import com.touati.magicsound.ui.theme.TouatiMagicSoundTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var playbackController: PlaybackController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.soundTileDao()

        playbackController = PlaybackController(
            context = applicationContext,
            onTileStarted = { startedId ->
                lifecycleScope.launch(Dispatchers.IO) {
                    dao.markPlayingAsFinished()
                    val list = dao.getAllTilesList()
                    list.find { it.id == startedId }?.let { tile ->
                        dao.update(tile.copy(state = TileState.PLAYING.value))
                    }
                }
            },
            onTileFinished = { finishedId ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val list = dao.getAllTilesList()
                    list.find { it.id == finishedId }?.let { tile ->
                        if (tile.state == TileState.PLAYING.value) {
                            dao.update(tile.copy(state = TileState.FINISHED.value))
                        }
                    }
                }
            }
        )

        setContent {
            TouatiMagicSoundTheme {
                MainScreen(
                    dao = dao,
                    playbackController = playbackController
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::playbackController.isInitialized) {
            playbackController.release()
        }
    }
}
