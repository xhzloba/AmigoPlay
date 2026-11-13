package com.example.movie.domain.model

import kotlinx.coroutines.flow.StateFlow

data class UserSettings(
    val extractBackdrop: Boolean = false,
    val extractPlex: Boolean = true,
    val plexBackdrop: Boolean = true,
    val heroPosterColors: Boolean = false,
    /** true = UltraBlur на всю главную; false = только зона слайдера (как раньше). */
    val heroPosterPage: Boolean = true,
    val showLogos: Boolean = true,
    val autoBackdrop: Boolean = true,
    val watchGlow: Boolean = false,
    val haptic: Boolean = true,
    val showPosterTitles: Boolean = true,
    val largePosters: Boolean = false,
    val playerLogo: Boolean = true,
    val playerSpeed: Float = 1f,
    val playerPicture: String = "fit",
    val playerDelayUs: Long = 0L,
    val playerBrightness: Float? = null,
    val playerUnderCamera: Boolean = false,
    val playerAutoNext: Boolean = true,
    val playerHdr: Boolean = true,
    val watchedBadge: Boolean = true,
    val whiteTitles: Boolean = true,
    /** Прятать нижнее меню при скролле вниз (главная, каталог, полный список). */
    val hideBottomBarOnScroll: Boolean = true,
    val appTheme: String = "cinema",
)

interface UserSettingsRepository {
    val settings: StateFlow<UserSettings>

    fun setExtractBackdrop(enabled: Boolean)

    fun setExtractPlex(enabled: Boolean)

    fun setPlexBackdrop(enabled: Boolean)

    fun setHeroPosterColors(enabled: Boolean)

    fun setHeroPosterPage(enabled: Boolean)

    fun setShowLogos(enabled: Boolean)

    fun setAutoBackdrop(enabled: Boolean)

    fun setWatchGlow(enabled: Boolean)

    fun setHaptic(enabled: Boolean)

    fun setShowPosterTitles(enabled: Boolean)

    fun setLargePosters(enabled: Boolean)

    fun setPlayerLogo(enabled: Boolean)

    fun setPlayerSpeed(speed: Float)

    fun setPlayerPicture(mode: String)

    fun setPlayerDelayUs(delayUs: Long)

    fun setPlayerBrightness(brightness: Float?)

    fun setPlayerUnderCamera(enabled: Boolean)

    fun setPlayerAutoNext(enabled: Boolean)

    fun setPlayerHdr(enabled: Boolean)

    fun setWatchedBadge(enabled: Boolean)

    fun setWhiteTitles(enabled: Boolean)

    fun setHideBottomBarOnScroll(enabled: Boolean)

    fun setAppTheme(theme: String)
}
