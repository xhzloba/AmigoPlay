package com.example.movie.domain.model

import kotlinx.coroutines.flow.StateFlow

data class UserSettings(
    val extractColors: Boolean = true,
    val extractBackdrop: Boolean = false,
    val showLogos: Boolean = true,
    val autoBackdrop: Boolean = true,
    val haptic: Boolean = true,
    val gyro: Boolean = true,
    val showPosterTitles: Boolean = false,
)

interface UserSettingsRepository {
    val settings: StateFlow<UserSettings>

    fun setExtractColors(enabled: Boolean)

    fun setExtractBackdrop(enabled: Boolean)

    fun setShowLogos(enabled: Boolean)

    fun setAutoBackdrop(enabled: Boolean)

    fun setHaptic(enabled: Boolean)

    fun setGyro(enabled: Boolean)

    fun setShowPosterTitles(enabled: Boolean)
}
