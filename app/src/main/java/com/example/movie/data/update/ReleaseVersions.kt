package com.example.movie.data.update

internal object ReleaseVersions {
    private val Semver = Regex("""(\d+)\.(\d+)\.(\d+)""")

    fun codeOf(label: String): Int? {
        val match = Semver.find(label) ?: return null
        val major = match.groupValues[1].toIntOrNull() ?: return null
        val minor = match.groupValues[2].toIntOrNull() ?: return null
        val patch = match.groupValues[3].toIntOrNull() ?: return null
        if (major > 99 || minor > 99 || patch > 99) return null
        return major * 10000 + minor * 100 + patch
    }

    fun nameOf(label: String): String? = Semver.find(label)?.value
}
