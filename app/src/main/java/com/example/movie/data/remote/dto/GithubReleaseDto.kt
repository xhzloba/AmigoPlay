package com.example.movie.data.remote.dto

import com.example.movie.data.update.ReleaseVersions
import com.example.movie.domain.model.AppRelease
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class GithubReleaseDto(
    @SerialName("tag_name") val tagName: String = "",
    val name: String = "",
    val body: String = "",
    val draft: Boolean = false,
    val assets: List<GithubAssetDto> = emptyList(),
)

@Serializable
internal data class GithubAssetDto(
    val name: String = "",
    @SerialName("browser_download_url") val browserDownloadUrl: String = "",
    @SerialName("content_type") val contentType: String = "",
)

internal fun GithubReleaseDto.toRelease(currentCode: Int): AppRelease? {
    if (draft) return null
    val code = ReleaseVersions.codeOf(tagName) ?: ReleaseVersions.codeOf(name) ?: return null
    if (code <= currentCode) return null
    val apk = assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) && it.browserDownloadUrl.isNotBlank() }
        ?: assets.firstOrNull {
            it.contentType == "application/vnd.android.package-archive" && it.browserDownloadUrl.isNotBlank()
        }
        ?: return null
    val versionName = ReleaseVersions.nameOf(tagName) ?: ReleaseVersions.nameOf(name) ?: return null
    return AppRelease(
        versionName = versionName,
        versionCode = code,
        notes = body.trim().replace("\r\n", "\n"),
        apkUrl = apk.browserDownloadUrl,
    )
}
