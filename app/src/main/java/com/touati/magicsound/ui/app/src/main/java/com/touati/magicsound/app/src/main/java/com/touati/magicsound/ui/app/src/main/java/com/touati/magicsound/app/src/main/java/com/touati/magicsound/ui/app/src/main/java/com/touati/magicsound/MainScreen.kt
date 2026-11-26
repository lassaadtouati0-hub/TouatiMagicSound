package com.touati.magicsound.ui

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.touati.magicsound.data.*
import com.touati.magicsound.service.*
import com.touati.magicsound.ui.theme.*
import com.touati.magicsound.util.UriUtils
import kotlinx.coroutines.launch

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
                SoundTileEntity(number = i, name = "مؤثر $i", audioUri = null, imageUri = null, state = TileState.UNUSED.value, isLooping = false, positionOrder = i)
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
                Button(onClick = { showNewShow = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)), shape = RoundedCornerShape(10.dp)) {
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
            items(tiles, key = { it.id }) { item ->
                MagicTileBox(item, playbackState, dao, playbackController)
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
            dismissButton = { TextButton(onClick = { showNewShow = false }) { Text("إلغاء") } }
        )
    }
}

@Composable
private fun MagicTileBox(item: SoundTileEntity, state: PlaybackState, dao: SoundTileDao, ctrl: PlaybackController) {
    val scope = rememberCoroutineScope()
    val isActive = state.activeTileId == item.id
    val st = TileState.fromInt(item.state)
    val bg = when {
        isActive && (state.isPlaying || state.isPaused) -> TileGreenPlaying
        st == TileState.PLAYING -> TileGreenPlaying
        st == TileState.FINISHED -> TileRedFinished
        else -> TileBlueUnused
    }
    val pos = if (isActive) state.currentPositionMs else 0L
    val dur = if (isActive) state.durationMs else 0L

    Card(
        modifier = Modifier.fillMaxWidth().clickable {
            if (!item.audioUri.isNullOrBlank()) {
                scope.launch { dao.markPlayingAsFinished(); ctrl.playTile(item) }
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${item.number}. ${item.name}", color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    scope.launch {
                        val loop = !item.isLooping
                        dao.update(item.copy(isLooping = loop))
                        if (isActive) ctrl.updateRepeatMode(loop)
                    }
                }) {
                    Icon(Icons.Default.RepeatOne, null, tint = if (item.isLooping) Color.Yellow else Color.White)
                }
            }
            Text("${UriUtils.formatDuration(pos)} / ${UriUtils.formatDuration(dur)}", color = Color.White, fontSize = 11.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = { ctrl.pause() }, enabled = isActive && state.isPlaying, modifier = Modifier.weight(1f)) { Text("إيقاف", fontSize = 11.sp) }
                Button(onClick = { ctrl.resume() }, enabled = isActive && state.isPaused, modifier = Modifier.weight(1f)) { Text("متابعة", fontSize = 11.sp) }
                Button(onClick = { ctrl.restart() }, enabled = isActive, modifier = Modifier.weight(1f)) { Text("إعادة", fontSize = 11.sp) }
            }
        }
    }
}
