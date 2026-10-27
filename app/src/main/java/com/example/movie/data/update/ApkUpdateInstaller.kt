package com.example.movie.data.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.movie.domain.model.DownloadProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

class ApkUpdateInstaller(
    private val client: OkHttpClient,
) {
    suspend fun download(
        url: String,
        destination: File,
        onProgress: (DownloadProgress) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "AmigoPlay")
            .header("Accept", "application/octet-stream")
            .build()
        val partial = File(destination.parentFile, "${destination.name}.part")
        destination.parentFile?.mkdirs()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("http ${response.code}")
            val body = response.body ?: error("empty")
            val total = body.contentLength()
            body.byteStream().use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var read = 0L
                    var lastRead = 0L
                    var speed = 0L
                    var lastTick = System.nanoTime()
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        read += count
                        val now = System.nanoTime()
                        val done = total > 0 && read == total
                        if (!done && now - lastTick < 150_000_000L) continue
                        val dt = now - lastTick
                        if (dt > 0) {
                            val instant = ((read - lastRead) * 1_000_000_000L) / dt
                            speed = if (speed == 0L) instant else (speed * 3 + instant) / 4
                        }
                        lastTick = now
                        lastRead = read
                        val snapshot = DownloadProgress(read, total, speed)
                        withContext(Dispatchers.Main) { onProgress(snapshot) }
                    }
                }
            }
        }
        if (destination.exists()) destination.delete()
        if (!partial.renameTo(destination)) error("rename")
        destination
    }

    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
