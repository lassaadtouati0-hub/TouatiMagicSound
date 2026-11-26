package com.touati.magicsound.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.touati.magicsound.data.*
import com.touati.magicsound.service.PlaybackController
import kotlinx.coroutines.*
import org.json.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(dao: SoundTileDao, playbackController: PlaybackController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tiles by dao.getAllTilesFlow().collectAsState(initial = emptyList())
    val playbackState by playbackController.playbackState.collectAsState()
    var showNewShow by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (dao.getCount() == 0) {
            val initList = (1..20).map { i ->
                SoundTileEntity(
                    number = i,
                    name = "مؤثر $i",
                    audioUri = null,
                    imageUri = null,
                    state = TileState.UNUSED.value,
                    isLooping = false,
                    positionOrder = i
                )
            }
            dao.insertAll(initList)
        }
    }

    LaunchedEffect(playbackState.errorMessage) {
        playbackState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            playbackController.clearError()
        }
    }

    Scaffold(
        containerColor = Color(0xFF0B1224),
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF101B34)).statusBarsPadding().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Touati Magic Sound", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Button(
                    onClick = { showNewShow = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("عرض جديد", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { pad ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(165.dp),
            contentPadding = PaddingValues(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize().padding(pad)
        ) {
            items(tiles, key = { it.id }) { tile ->
                SoundTileCard(
                    tile = tile,
                    playbackState = playbackState,
                    onTileClick = {
                        if (!tile.audioUri.isNullOrBlank()) {
                            scope.launch {
                                dao.markPlayingAsFinished()
                                playbackController.playTile(tile)
                            }
                        }
                    },
                    onPauseClick = { playbackController.pause() },
                    onResumeClick = { playbackController.resume() },
                    onRestartClick = { playbackController.restart() },
                    onSeek = { pos -> playbackController.seekTo(pos) },
                    onToggleLoop = {
                        scope.launch {
                            val loop = !tile.isLooping
                            dao.update(tile.copy(isLooping = loop))
                            if (playbackState.activeTileId == tile.id) {
                                playbackController.updateRepeatMode(loop)
                            }
                        }
                    }
                )
            }
        }
    }

    if (showNewShow) {
        AlertDialog(
            onDismissRequest = { showNewShow = false },
            title = { Text("عرض جديد") },
            text = { Text("إيقاف الصوت وإعادة كل المربعات للأزرق؟") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        playbackController.stopAll()
                        dao.resetAllStatesToUnused()
                        showNewShow = false
                    }
                }) { Text("نعم") }
            },
            dismissButton = {
                TextButton(onClick = { showNewShow = false }) { Text("إلغاء") }
            }
        )
    }
}
