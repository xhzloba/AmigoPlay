package com.example.movie.domain.model

import kotlinx.coroutines.flow.StateFlow

data class UserSettings(
    val extractColors: Boolean = true,
    val extractBackdrop: Boolean = false,
    val showLogos: Boolean = true,
    val autoBackdrop: Boolean = true,
    val watchGlow: Boolean = false,
    val haptic: Boolean = true,
    val gyro: Boolean = true,
    val showPosterTitles: Boolean = false,
    val playerLogo: Boolean = true,
)

interface UserSettingsRepository {
    val settings: StateFlow<UserSettings>

    fun setExtractColors(enabled: Boolean)

    fun setExtractBackdrop(enabled: Boolean)

    fun setShowLogos(enabled: Boolean)

    fun setAutoBackdrop(enabled: Boolean)

    fun setWatchGlow(enabled: Boolean)

    fun setHaptic(enabled: Boolean)

    fun setGyro(enabled: Boolean)

    fun setShowPosterTitles(enabled: Boolean)

    fun setPlayerLogo(enabled: Boolean)
}
