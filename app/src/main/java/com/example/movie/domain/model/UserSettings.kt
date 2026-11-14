package com.example.movie.domain.model

import kotlinx.coroutines.flow.StateFlow

data class UserSettings(
    val extractBackdrop: Boolean = false,
    val extractPlex: Boolean = true,
    val plexBackdrop: Boolean = true,
    /** UltraBlur на главной с обложки слайдера. По дефолту вкл (полки скрыты — только слайдер). */
    val heroPosterColors: Boolean = true,
    /** true = UltraBlur на всю главную; false = только зона слайдера. */
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
    /** Подписи под иконками нижнего меню. */
    val navLabels: Boolean = false,
    /** Ретушь по бокам полок (слайдеров) на главной и в каталоге. */
    val sliderShade: Boolean = false,
    /** Скрытые полки главной (id из HomeRails). Главный слайдер всегда. По умолчанию все скрыты. */
    val hiddenHomeRails: Set<String> = DefaultHiddenHomeRails,
    /** Ссылка «Зачем это?» под плашкой «Вид полок». */
    val hideRailsLookHint: Boolean = false,
    val appTheme: String = "cinema",
) {
    fun homeRailVisible(id: String): Boolean = id !in hiddenHomeRails
}

/** Совпадает с HomeRails.All — по дефолту на главной только слайдер. */
val DefaultHiddenHomeRails: Set<String> = setOf(
    "recommendations",
    "history",
    "continue",
    "favorites",
    "watched",
    "watching",
    "best",
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

    fun setNavLabels(enabled: Boolean)

    fun setSliderShade(enabled: Boolean)

    fun setHomeRailVisible(id: String, visible: Boolean)

    fun setHideRailsLookHint(hidden: Boolean)

    fun setAppTheme(theme: String)
}
