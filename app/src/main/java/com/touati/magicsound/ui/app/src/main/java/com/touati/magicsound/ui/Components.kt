package com.touati.magicsound.ui

import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.touati.magicsound.data.*
import com.touati.magicsound.service.PlaybackState
import com.touati.magicsound.ui.theme.*
import com.touati.magicsound.util.UriUtils

@Composable
fun SoundTileCard(
    tile: SoundTileEntity,
    playbackState: PlaybackState,
    onTileClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onRestartClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleLoop: () -> Unit
) {
    val isActive = playbackState.activeTileId == tile.id
    val stateEnum = TileState.fromInt(tile.state)
    val bgColor = when {
        isActive && (playbackState.isPlaying || playbackState.isPaused) -> TileGreenPlaying
        stateEnum == TileState.PLAYING -> TileGreenPlaying
        stateEnum == TileState.FINISHED -> TileRedFinished
        else -> TileBlueUnused
    }
    val pos = if (isActive) playbackState.currentPositionMs else 0L
    val dur = if (isActive) playbackState.durationMs else 0L
    val hasAudio = !tile.audioUri.isNullOrBlank()

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onTileClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(if (isActive) 3.dp else 1.dp, if (isActive) Color.Yellow else Color.White.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${tile.number}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier.size(50.dp).clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!tile.imageUri.isNullOrBlank()) {
                        AsyncImage(model = Uri.parse(tile.imageUri), contentDescription = tile.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(26.dp))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(tile.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (hasAudio) "جاهز للتشغيل" else "بدون ملف صوتي", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, maxLines = 1)
                }
                IconButton(
                    onClick = onToggleLoop,
                    modifier = Modifier.clip(CircleShape).background(if (tile.isLooping) Color.White else Color.Black.copy(alpha = 0.25f))
                ) {
                    Icon(Icons.Default.RepeatOne, "تكرار", tint = if (tile.isLooping) bgColor else Color.White)
                }
            }
            Spacer(Modifier.height(6.dp))
            Slider(
                value = if (dur > 0) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f,
                onValueChange = { if (isActive && dur > 0) onSeek((it * dur).toLong()) },
                enabled = isActive && dur > 0,
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.3f)),
                modifier = Modifier.height(22.dp)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(UriUtils.formatDuration(pos), color = Color.White, fontSize = 11.sp)
                Text(UriUtils.formatDuration(dur), color = Color.White, fontSize = 11.sp)
            }
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onPauseClick,
                    enabled = isActive && playbackState.isPlaying,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.45f), contentColor = Color.White),
                    contentPadding = PaddingValues(4.dp),
                    modifier = Modifier.weight(1f)
                ) { Text("إيقاف", fontSize = 12.sp) }
                Button(
                    onClick = onResumeClick,
                    enabled = isActive && playbackState.isPaused,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.45f), contentColor = Color.White),
                    contentPadding = PaddingValues(4.dp),
                    modifier = Modifier.weight(1f)
                ) { Text("متابعة", fontSize = 12.sp) }
                Button(
                    onClick = onRestartClick,
                    enabled = isActive,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.45f), contentColor = Color.White),
                    contentPadding = PaddingValues(4.dp),
                    modifier = Modifier.weight(1f)
                ) { Text("إعادة", fontSize = 12.sp) }
            }
        }
    }
}
