package com.example.movie.data.settings

import android.content.Context
import com.example.movie.domain.model.UserSettings
import com.example.movie.domain.model.UserSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserSettingsStore(context: Context) : UserSettingsRepository {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())

    override val settings: StateFlow<UserSettings> = state.asStateFlow()

    override fun setExtractColors(enabled: Boolean) = write {
        it.copy(
            extractColors = enabled,
            extractBackdrop = if (enabled) false else it.extractBackdrop,
        )
    }

    override fun setExtractBackdrop(enabled: Boolean) = write {
        it.copy(
            extractBackdrop = enabled,
            extractColors = if (enabled) false else it.extractColors,
        )
    }

    override fun setShowLogos(enabled: Boolean) = write { it.copy(showLogos = enabled) }

    override fun setAutoBackdrop(enabled: Boolean) = write { it.copy(autoBackdrop = enabled) }

    override fun setWatchGlow(enabled: Boolean) = write { it.copy(watchGlow = enabled) }

    override fun setHaptic(enabled: Boolean) = write { it.copy(haptic = enabled) }

    override fun setGyro(enabled: Boolean) = write { it.copy(gyro = enabled) }

    override fun setShowPosterTitles(enabled: Boolean) = write { it.copy(showPosterTitles = enabled) }

    override fun setLargePosters(enabled: Boolean) = write { it.copy(largePosters = enabled) }

    override fun setPlayerLogo(enabled: Boolean) = write { it.copy(playerLogo = enabled) }

    override fun setWatchedBadge(enabled: Boolean) = write { it.copy(watchedBadge = enabled) }

    private fun write(transform: (UserSettings) -> UserSettings) {
        val next = transform(state.value)
        prefs.edit()
            .putBoolean(KEY_COLORS, next.extractColors)
            .putBoolean(KEY_BACKDROP_COLORS, next.extractBackdrop)
            .putBoolean(KEY_LOGOS, next.showLogos)
            .putBoolean(KEY_BACKDROP, next.autoBackdrop)
            .putBoolean(KEY_WATCH_GLOW, next.watchGlow)
            .putBoolean(KEY_HAPTIC, next.haptic)
            .putBoolean(KEY_GYRO, next.gyro)
            .putBoolean(KEY_POSTER_TITLES, next.showPosterTitles)
            .putBoolean(KEY_LARGE_POSTERS, next.largePosters)
            .putBoolean(KEY_PLAYER_LOGO, next.playerLogo)
            .putBoolean(KEY_WATCHED_BADGE, next.watchedBadge)
            .commit()
        state.value = next
    }

    private fun read(): UserSettings = UserSettings(
        extractColors = prefs.getBoolean(KEY_COLORS, true),
        extractBackdrop = prefs.getBoolean(KEY_BACKDROP_COLORS, false),
        showLogos = prefs.getBoolean(KEY_LOGOS, true),
        autoBackdrop = prefs.getBoolean(KEY_BACKDROP, true),
        watchGlow = prefs.getBoolean(KEY_WATCH_GLOW, false),
        haptic = prefs.getBoolean(KEY_HAPTIC, true),
        gyro = prefs.getBoolean(KEY_GYRO, true),
        showPosterTitles = prefs.getBoolean(KEY_POSTER_TITLES, false),
        largePosters = prefs.getBoolean(KEY_LARGE_POSTERS, false),
        playerLogo = prefs.getBoolean(KEY_PLAYER_LOGO, true),
        watchedBadge = prefs.getBoolean(KEY_WATCHED_BADGE, true),
    )

    private companion object {
        const val FILE = "user_settings"
        const val KEY_COLORS = "extract_colors"
        const val KEY_BACKDROP_COLORS = "extract_backdrop"
        const val KEY_LOGOS = "show_logos"
        const val KEY_BACKDROP = "auto_backdrop"
        const val KEY_WATCH_GLOW = "watch_glow"
        const val KEY_HAPTIC = "haptic"
        const val KEY_GYRO = "gyro"
        const val KEY_POSTER_TITLES = "poster_titles"
        const val KEY_LARGE_POSTERS = "large_posters"
        const val KEY_PLAYER_LOGO = "player_logo"
        const val KEY_WATCHED_BADGE = "watched_badge"
    }
}
