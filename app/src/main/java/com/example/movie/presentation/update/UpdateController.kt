package com.example.movie.presentation.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.movie.BuildConfig
import com.example.movie.data.update.ApkUpdateInstaller
import com.example.movie.di.LocalAppContainer
import com.example.movie.domain.model.AppRelease
import com.example.movie.domain.model.DownloadProgress
import com.example.movie.domain.model.UpdateProbe
import com.example.movie.domain.usecase.CheckAppUpdateUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

internal enum class UpdatePhase {
    Ready,
    Downloading,
    Failed,
}

internal enum class UpdateNotice {
    Current,
    Failed,
}

@Stable
internal class UpdateController(
    private val check: CheckAppUpdateUseCase,
    private val installer: ApkUpdateInstaller,
    private val scope: CoroutineScope,
    private val postponed: MutableState<Boolean>,
) {
    var release by mutableStateOf<AppRelease?>(null)
        private set
    var checked by mutableStateOf(false)
        private set
    var sheet by mutableStateOf(false)
        private set
    var phase by mutableStateOf(UpdatePhase.Ready)
        private set
    var progress by mutableStateOf(DownloadProgress())
        private set
    var checking by mutableStateOf(false)
        private set
    var notice by mutableStateOf<UpdateNotice?>(null)
        private set

    val pending: Boolean
        get() = release != null && postponed.value && !sheet

    private var lookupJob: Job? = null
    private var lookupToken = 0
    private var downloadJob: Job? = null

    fun refresh(present: Boolean) {
        val ticket = ++lookupToken
        lookupJob?.cancel()
        checking = present
        if (present) notice = null
        lookupJob = scope.launch {
            try {
                val probe = check.lookup(BuildConfig.VERSION_CODE)
                if (ticket != lookupToken) return@launch
                when (probe) {
                    is UpdateProbe.Available -> {
                        release = probe.release
                        checked = true
                        notice = null
                        if (present || !postponed.value) sheet = true
                    }
                    UpdateProbe.Current -> {
                        release = null
                        checked = true
                        postponed.value = false
                        sheet = false
                        if (present) notice = UpdateNotice.Current
                    }
                    UpdateProbe.Unavailable -> {
                        if (present) notice = UpdateNotice.Failed
                    }
                }
            } finally {
                if (ticket == lookupToken) checking = false
            }
        }
    }

    fun postpone() {
        downloadJob?.cancel()
        downloadJob = null
        phase = UpdatePhase.Ready
        progress = DownloadProgress()
        postponed.value = true
        sheet = false
    }

    fun download(context: Context) {
        val item = release ?: return
        if (!context.packageManager.canRequestPackageInstalls()) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return
        }
        if (downloadJob?.isActive == true && phase == UpdatePhase.Downloading) return
        downloadJob?.cancel()
        downloadJob = scope.launch {
            phase = UpdatePhase.Downloading
            progress = DownloadProgress()
            try {
                val file = installer.download(
                    url = item.apkUrl,
                    destination = File(context.cacheDir, "updates/update.apk"),
                    onProgress = { progress = it },
                )
                installer.install(context, file)
                phase = UpdatePhase.Ready
                progress = DownloadProgress()
                sheet = false
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                phase = UpdatePhase.Failed
            }
        }
    }
}

internal val LocalAppUpdate = staticCompositionLocalOf<UpdateController> {
    error("UpdateController is not provided")
}

@Composable
internal fun rememberUpdateController(): UpdateController {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val postponed = rememberSaveable { mutableStateOf(false) }
    val controller = remember(container) {
        UpdateController(
            check = container.checkAppUpdate,
            installer = container.apkInstaller,
            scope = scope,
            postponed = postponed,
        )
    }
    LaunchedEffect(Unit) {
        controller.refresh(present = false)
    }
    return controller
}
