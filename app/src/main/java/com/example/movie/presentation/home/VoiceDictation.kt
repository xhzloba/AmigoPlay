package com.example.movie.presentation.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.movie.R

internal class VoiceDictation(
    val listening: Boolean,
    val toggle: () -> Unit,
)

@Composable
internal fun rememberVoiceDictation(
    onResult: (String) -> Unit,
): VoiceDictation {
    val context = LocalContext.current
    val prompt = context.getString(R.string.search_voice)
    val onResultState by rememberUpdatedState(onResult)
    var listening by remember { mutableStateOf(false) }
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    fun toast(res: Int) {
        Toast.makeText(context, res, Toast.LENGTH_SHORT).show()
    }

    fun stopEngine() {
        runCatching {
            recognizer?.stopListening()
            recognizer?.cancel()
        }
        listening = false
    }

    fun destroyEngine() {
        stopEngine()
        runCatching { recognizer?.destroy() }
        recognizer = null
    }

    val intentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        listening = false
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()
        if (spoken.isNotEmpty()) {
            onResultState(spoken)
        } else {
            toast(R.string.search_voice_no_match)
        }
    }

    fun speechIntent(): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }

    fun launchSystemUi() {
        val intent = speechIntent()
        val resolved = intent.resolveActivity(context.packageManager)
        if (resolved == null) {
            listening = false
            toast(R.string.search_voice_unavailable)
            return
        }
        listening = true
        runCatching { intentLauncher.launch(intent) }
            .onFailure {
                listening = false
                toast(R.string.search_voice_unavailable)
            }
    }

    fun startEngine() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            launchSystemUi()
            return
        }
        destroyEngine()
        val engine = runCatching { SpeechRecognizer.createSpeechRecognizer(context) }.getOrNull()
        if (engine == null) {
            launchSystemUi()
            return
        }
        recognizer = engine
        engine.setRecognitionListener(
            object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    listening = true
                }

                override fun onBeginningOfSpeech() = Unit

                override fun onRmsChanged(rmsdB: Float) = Unit

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() = Unit

                override fun onPartialResults(partialResults: Bundle?) = Unit

                override fun onEvent(eventType: Int, params: Bundle?) = Unit

                override fun onError(error: Int) {
                    listening = false
                    when (error) {
                        SpeechRecognizer.ERROR_CLIENT,
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
                        -> Unit
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                        SpeechRecognizer.ERROR_NO_MATCH,
                        -> toast(R.string.search_voice_no_match)
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS,
                        -> toast(R.string.search_voice_permission)
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                        -> toast(R.string.search_voice_network)
                        else -> toast(R.string.search_voice_failed)
                    }
                }

                override fun onResults(results: Bundle?) {
                    listening = false
                    val spoken = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        ?.trim()
                        .orEmpty()
                    if (spoken.isNotEmpty()) {
                        onResultState(spoken)
                    } else {
                        toast(R.string.search_voice_no_match)
                    }
                }
            },
        )
        listening = true
        runCatching { engine.startListening(speechIntent()) }
            .onFailure {
                destroyEngine()
                launchSystemUi()
            }
    }

    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            startEngine()
        } else {
            listening = false
            toast(R.string.search_voice_permission)
        }
    }

    DisposableEffect(Unit) {
        onDispose { destroyEngine() }
    }

    return VoiceDictation(
        listening = listening,
        toggle = {
            if (listening) {
                destroyEngine()
            } else {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO,
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    startEngine()
                } else {
                    permission.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        },
    )
}
