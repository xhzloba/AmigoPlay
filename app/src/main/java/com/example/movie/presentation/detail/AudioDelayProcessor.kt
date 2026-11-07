package com.example.movie.presentation.detail

import androidx.annotation.OptIn
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.min

@OptIn(UnstableApi::class)
internal class AudioDelayProcessor : BaseAudioProcessor() {
    @Volatile
    var delayUs: Long = 0

    private var pendingSilence = 0
    private var pendingSkip = 0

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat =
        inputAudioFormat

    override fun onFlush(streamMetadata: AudioProcessor.StreamMetadata) {
        val format = inputAudioFormat
        val bytes = if (format.bytesPerFrame <= 0 || format.sampleRate <= 0 || delayUs == 0L) {
            0
        } else {
            val frames = abs(delayUs) * format.sampleRate / 1_000_000L
            (frames * format.bytesPerFrame).toInt()
        }
        if (delayUs > 0) {
            pendingSilence = bytes
            pendingSkip = 0
        } else {
            pendingSkip = bytes
            pendingSilence = 0
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (pendingSkip > 0 && inputBuffer.hasRemaining()) {
            val skip = min(pendingSkip, inputBuffer.remaining())
            inputBuffer.position(inputBuffer.position() + skip)
            pendingSkip -= skip
        }
        val incoming = inputBuffer.remaining()
        if (incoming == 0 && pendingSilence == 0) return
        val output = replaceOutputBuffer(incoming + pendingSilence)
        if (pendingSilence > 0) {
            output.put(ByteArray(pendingSilence))
            pendingSilence = 0
        }
        if (incoming > 0) output.put(inputBuffer)
        output.flip()
    }
}
