package com.example.movie.domain.model

import kotlinx.coroutines.flow.StateFlow

data class UserSettings(
    val extractColors: Boolean = true,
    val showLogos: Boolean = true,
    val autoBackdrop: Boolean = true,
    val haptic: Boolean = true,
    val gyro: Boolean = true,
)

interface UserSettingsRepository {
    val settings: StateFlow<UserSettings>

    fun setExtractColors(enabled: Boolean)

    fun setShowLogos(enabled: Boolean)

    fun setAutoBackdrop(enabled: Boolean)

    fun setHaptic(enabled: Boolean)

    fun setGyro(enabled: Boolean)
}
