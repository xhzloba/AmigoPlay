package com.example.movie.presentation.settings

import androidx.compose.runtime.staticCompositionLocalOf
import com.example.movie.domain.model.UserSettings
import com.example.movie.domain.model.UserSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

val LocalUserSettings = staticCompositionLocalOf<UserSettingsRepository> {
    DefaultUserSettings
}

private object DefaultUserSettings : UserSettingsRepository {
    override val settings: StateFlow<UserSettings> = MutableStateFlow(UserSettings())

    override fun setExtractColors(enabled: Boolean) = Unit

    override fun setShowLogos(enabled: Boolean) = Unit

    override fun setHaptic(enabled: Boolean) = Unit

    override fun setGyro(enabled: Boolean) = Unit
}
