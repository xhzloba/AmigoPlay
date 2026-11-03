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

    override fun setExtractColors(enabled: Boolean) = write { it.copy(extractColors = enabled) }

    override fun setShowLogos(enabled: Boolean) = write { it.copy(showLogos = enabled) }

    override fun setAutoBackdrop(enabled: Boolean) = write { it.copy(autoBackdrop = enabled) }

    override fun setHaptic(enabled: Boolean) = write { it.copy(haptic = enabled) }

    override fun setGyro(enabled: Boolean) = write { it.copy(gyro = enabled) }

    private fun write(transform: (UserSettings) -> UserSettings) {
        val next = transform(state.value)
        prefs.edit()
            .putBoolean(KEY_COLORS, next.extractColors)
            .putBoolean(KEY_LOGOS, next.showLogos)
            .putBoolean(KEY_BACKDROP, next.autoBackdrop)
            .putBoolean(KEY_HAPTIC, next.haptic)
            .putBoolean(KEY_GYRO, next.gyro)
            .commit()
        state.value = next
    }

    private fun read(): UserSettings = UserSettings(
        extractColors = prefs.getBoolean(KEY_COLORS, true),
        showLogos = prefs.getBoolean(KEY_LOGOS, true),
        autoBackdrop = prefs.getBoolean(KEY_BACKDROP, true),
        haptic = prefs.getBoolean(KEY_HAPTIC, true),
        gyro = prefs.getBoolean(KEY_GYRO, true),
    )

    private companion object {
        const val FILE = "user_settings"
        const val KEY_COLORS = "extract_colors"
        const val KEY_LOGOS = "show_logos"
        const val KEY_BACKDROP = "auto_backdrop"
        const val KEY_HAPTIC = "haptic"
        const val KEY_GYRO = "gyro"
    }
}
