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

    override fun setExtractBackdrop(enabled: Boolean) = Unit

    override fun setExtractPlex(enabled: Boolean) = Unit

    override fun setPlexBackdrop(enabled: Boolean) = Unit

    override fun setHeroPosterColors(enabled: Boolean) = Unit

    override fun setHeroPosterPage(enabled: Boolean) = Unit

    override fun setShowLogos(enabled: Boolean) = Unit

    override fun setAutoBackdrop(enabled: Boolean) = Unit

    override fun setWatchGlow(enabled: Boolean) = Unit

    override fun setHaptic(enabled: Boolean) = Unit

    override fun setShowPosterTitles(enabled: Boolean) = Unit

    override fun setLargePosters(enabled: Boolean) = Unit

    override fun setPlayerLogo(enabled: Boolean) = Unit

    override fun setPlayerSpeed(speed: Float) = Unit

    override fun setPlayerPicture(mode: String) = Unit

    override fun setPlayerDelayUs(delayUs: Long) = Unit

    override fun setPlayerBrightness(brightness: Float?) = Unit

    override fun setPlayerUnderCamera(enabled: Boolean) = Unit

    override fun setPlayerAutoNext(enabled: Boolean) = Unit

    override fun setPlayerHdr(enabled: Boolean) = Unit

    override fun setWatchedBadge(enabled: Boolean) = Unit

    override fun setWhiteTitles(enabled: Boolean) = Unit

    override fun setHideBottomBarOnScroll(enabled: Boolean) = Unit

    override fun setNavLabels(enabled: Boolean) = Unit

    override fun setSliderShade(enabled: Boolean) = Unit

    override fun setAppTheme(theme: String) = Unit
}
