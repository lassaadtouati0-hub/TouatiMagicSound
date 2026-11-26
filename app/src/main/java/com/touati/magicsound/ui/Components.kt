package com.touati.magicsound.ui

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.touati.magicsound.data.SoundTileEntity
import com.touati.magicsound.data.TileState
import com.touati.magicsound.service.PlaybackState
import com.touati.magicsound.ui.theme.TileBlueUnused
import com.touati.magicsound.ui.theme.TileGreenPlaying
import com.touati.magicsound.ui.theme.TileRedFinished
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
    val isCurrentActiveTile = playbackState.activeTileId == tile.id
    val tileStateEnum = TileState.fromInt(tile.state)

    val cardBackgroundColor = when {
        isCurrentActiveTile && (playbackState.isPlaying || playbackState.isPaused) -> TileGreenPlaying
        tileStateEnum == TileState.PLAYING -> TileGreenPlaying
        tileStateEnum == TileState.FINISHED -> TileRedFinished
        else -> TileBlueUnused
    }

    val currentPos = if (isCurrentActiveTile) playbackState.currentPositionMs else 0L
    val totalDur = if (isCurrentActiveTile) playbackState.durationMs else 0L
    val hasAudio = !tile.audioUri.isNullOrBlank()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTileClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor),
        border = BorderStroke(
            width = if (isCurrentActiveTile) 3.dp else 1.dp,
            color = if (isCurrentActiveTile) Color.Yellow else Color.White.copy(alpha = 0.25f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${tile.number}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!tile.imageUri.isNullOrBlank()) {
                        AsyncImage(
                            model = Uri.parse(tile.imageUri),
                            contentDescription = tile.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tile.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (hasAudio) "جاهز للتشغيل" else "بدون ملف صوتي (اضبطه من الإعدادات)",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onToggleLoop,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (tile.isLooping) Color.White else Color.Black.copy(alpha = 0.25f)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.RepeatOne,
                        contentDescription = "تكرار",
                        tint = if (tile.isLooping) cardBackgroundColor else Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = if (totalDur > 0) (currentPos.toFloat() / totalDur.toFloat()).coerceIn(0f, 1f) else 0f,
                    onValueChange = { fraction ->
                        if (isCurrentActiveTile && totalDur > 0) {
                            onSeek((fraction * totalDur).toLong())
                        }
                    },
                    enabled = isCurrentActiveTile && totalDur > 0,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.height(24.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = UriUtils.formatDuration(currentPos),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = UriUtils.formatDuration(totalDur),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPauseClick,
                    enabled = isCurrentActiveTile && playbackState.isPlaying,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black.copy(alpha = 0.45f),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("إيقاف مؤقت", fontSize = 12.sp, maxLines = 1)
                }

                Button(
                    onClick = onResumeClick,
                    enabled = isCurrentActiveTile && playbackState.isPaused,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black.copy(alpha = 0.45f),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("استكمال", fontSize = 12.sp, maxLines = 1)
                }

                Button(
                    onClick = onRestartClick,
                    enabled = hasAudio,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black.copy(alpha = 0.45f),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
