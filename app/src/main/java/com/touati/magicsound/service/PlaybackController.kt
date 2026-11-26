package com.touati.magicsound.service

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.touati.magicsound.data.SoundTileEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackState(
    val activeTileId: Long? = null,
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val errorMessage: String? = null
)

class PlaybackController(
    private val context: Context,
    private val onTileFinished: (Long) -> Unit,
    private val onTileStarted: (Long) -> Unit
) {
    private var mediaController: MediaController? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    init {
        connectToService()
    }

    private fun connectToService() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, AudioPlaybackService::class.java)
        )
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            try {
                mediaController = controllerFuture.get()
                setupPlayerListener()
            } catch (e: Exception) {
                _playbackState.value = _playbackState.value.copy(
                    errorMessage = "تعذر الاتصال بخدمة الصوت: ${e.localizedMessage}"
                )
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun setupPlayerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val activeId = _playbackState.value.activeTileId
                if (activeId != null) {
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = isPlaying,
                        isPaused = !isPlaying && (mediaController?.playbackState == Player.STATE_READY)
                    )
                    if (isPlaying) {
                        startProgressTracker()
                    } else {
                        stopProgressTracker()
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val activeId = _playbackState.value.activeTileId ?: return
                if (playbackState == Player.STATE_READY) {
                    val dur = mediaController?.duration?.coerceAtLeast(0L) ?: 0L
                    _playbackState.value = _playbackState.value.copy(durationMs = dur)
                } else if (playbackState == Player.STATE_ENDED) {
                    stopProgressTracker()
                    _playbackState.value = PlaybackState(
                        activeTileId = activeId,
                        isPlaying = false,
                        isPaused = false,
                        currentPositionMs = 0L,
                        durationMs = _playbackState.value.durationMs
                    )
                    onTileFinished(activeId)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val activeId = _playbackState.value.activeTileId
                stopProgressTracker()
                _playbackState.value = PlaybackState(
                    errorMessage = "خطأ في تشغيل الملف الصوتي. تأكد من أن الملف لم يُنقل أو يُحذف من الهاتف."
                )
                if (activeId != null) {
                    onTileFinished(activeId)
                }
            }
        })
    }

    fun playTile(tile: SoundTileEntity) {
        val uriString = tile.audioUri
        if (uriString.isNullOrBlank()) {
            _playbackState.value = _playbackState.value.copy(
                errorMessage = "لا يوجد ملف صوتي مرتبط بالمربع (${tile.number} - ${tile.name}). أضفه من الإعدادات."
            )
            return
        }

        val controller = mediaController ?: run {
            connectToService()
            _playbackState.value = _playbackState.value.copy(
                errorMessage = "جاري تجهيز مشغل الصوت، اضغط مرة أخرى."
            )
            return
        }

        try {
            val previousId = _playbackState.value.activeTileId
            if (previousId != null && previousId != tile.id) {
                onTileFinished(previousId)
            }

            val metadata = MediaMetadata.Builder()
                .setTitle("${tile.number}. ${tile.name}")
                .setArtist("Touati Magic Sound")
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(Uri.parse(uriString))
                .setMediaMetadata(metadata)
                .build()

            controller.stop()
            controller.clearMediaItems()
            controller.setMediaItem(mediaItem)
            controller.repeatMode = if (tile.isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            controller.prepare()
            controller.play()

            _playbackState.value = PlaybackState(
                activeTileId = tile.id,
                isPlaying = true,
                isPaused = false,
                currentPositionMs = 0L,
                durationMs = 0L,
                errorMessage = null
            )
            onTileStarted(tile.id)
        } catch (e: Exception) {
            _playbackState.value = _playbackState.value.copy(
                errorMessage = "فشل تشغيل الملف: ${e.localizedMessage}"
            )
        }
    }

    fun pause() {
        mediaController?.pause()
    }

    fun resume() {
        mediaController?.play()
    }

    fun restart(tile: SoundTileEntity) {
        val controller = mediaController ?: return
        if (_playbackState.value.activeTileId == tile.id) {
            controller.seekTo(0L)
            controller.play()
            onTileStarted(tile.id)
        } else {
            playTile(tile)
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
    }

    fun updateRepeatMode(tileId: Long, isLooping: Boolean) {
        if (_playbackState.value.activeTileId == tileId) {
            mediaController?.repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        }
    }

    fun stopAll() {
        stopProgressTracker()
        mediaController?.stop()
        mediaController?.clearMediaItems()
        _playbackState.value = PlaybackState()
    }

    fun clearError() {
        _playbackState.value = _playbackState.value.copy(errorMessage = null)
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val controller = mediaController
                if (controller != null && controller.isPlaying) {
                    _playbackState.value = _playbackState.value.copy(
                        currentPositionMs = controller.currentPosition.coerceAtLeast(0L),
                        durationMs = controller.duration.coerceAtLeast(0L)
                    )
                }
                delay(250L)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
    }

    fun release() {
        stopProgressTracker()
        scope.cancel()
        mediaController?.release()
    }
}
