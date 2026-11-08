package com.example.movie.presentation.detail

import android.graphics.Matrix
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.movie.data.player.LIFT_USER_AGENT

@Composable
internal fun BackdropTrailer(
    url: String,
    audio: String?,
    referer: String?,
    modifier: Modifier = Modifier,
    onReady: () -> Unit = {},
    onFailed: () -> Unit,
) {
    val failed by rememberUpdatedState(onFailed)
    val ready by rememberUpdatedState(onReady)
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextureView(context).apply {
                isOpaque = true
                var frameSeen = false
                val player = MediaPlayer()
                val soundtrack = if (audio != null) MediaPlayer() else null
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build()
                val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(attrs)
                    .build()
                tag = TrailerPlayback(player, soundtrack, focus)
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
                        player.setSurface(Surface(surface))
                        player.setAudioAttributes(attrs)
                        player.setVolume(1f, 1f)
                        player.isLooping = true
                        player.setOnVideoSizeChangedListener { _, videoWidth, videoHeight ->
                            cropTop(videoWidth, videoHeight)
                        }
                        player.setOnPreparedListener { ready ->
                            cropTop(ready.videoWidth, ready.videoHeight)
                            ready.setVolume(1f, 1f)
                            context.getSystemService(AudioManager::class.java)?.requestAudioFocus(focus)
                            ready.start()
                        }
                        player.setOnCompletionListener { ready ->
                            ready.seekTo(0)
                            ready.start()
                        }
                        player.setOnErrorListener { _, _, _ ->
                            failed()
                            true
                        }
                        val headers = HashMap<String, String>()
                        headers["User-Agent"] = LIFT_USER_AGENT
                        val origin = referer?.let(Uri::parse)
                        if (origin?.host != null) headers["Referer"] = "${origin.scheme}://${origin.host}/"
                        val started = runCatching {
                            player.setDataSource(context, Uri.parse(url), headers)
                            player.prepareAsync()
                        }
                        if (started.isFailure) failed()
                        if (soundtrack != null && audio != null) {
                            soundtrack.setAudioAttributes(attrs)
                            soundtrack.isLooping = true
                            soundtrack.setVolume(1f, 1f)
                            soundtrack.setOnPreparedListener { it.start() }
                            soundtrack.setOnErrorListener { _, _, _ -> true }
                            runCatching {
                                soundtrack.setDataSource(context, Uri.parse(audio), headers)
                                soundtrack.prepareAsync()
                            }
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
                        cropTop(player.videoWidth, player.videoHeight)
                    }

                    override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
                        runCatching { player.setSurface(null) }
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {
                        if (frameSeen) return
                        frameSeen = true
                        ready()
                    }
                }
            }
        },
        onRelease = { view ->
            val playback = view.tag as? TrailerPlayback ?: return@AndroidView
            view.tag = null
            view.surfaceTextureListener = null
            view.context.getSystemService(AudioManager::class.java)?.abandonAudioFocusRequest(playback.focus)
            runCatching { playback.soundtrack?.reset() }
            playback.soundtrack?.release()
            runCatching { playback.player.reset() }
            playback.player.release()
        },
    )
}

private class TrailerPlayback(
    val player: MediaPlayer,
    val soundtrack: MediaPlayer?,
    val focus: AudioFocusRequest,
)

private fun TextureView.cropTop(videoWidth: Int, videoHeight: Int) {
    if (videoWidth <= 0 || videoHeight <= 0) return
    post {
        if (width <= 0 || height <= 0) return@post
        val viewW = width.toFloat()
        val viewH = height.toFloat()
        val videoRatio = videoWidth.toFloat() / videoHeight
        val viewRatio = viewW / viewH
        val zoom = if (videoRatio > viewRatio) {
            viewH * videoRatio / viewW
        } else {
            viewW / (viewH * videoRatio)
        }
        val pivotY = if (videoRatio > viewRatio) viewH / 2f else 0f
        setTransform(Matrix().apply { setScale(zoom, zoom, viewW / 2f, pivotY) })
    }
}
