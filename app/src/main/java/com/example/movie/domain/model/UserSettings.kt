package com.example.movie.domain.model

import kotlinx.coroutines.flow.StateFlow

data class UserSettings(
    val extractColors: Boolean = true,
    val extractBackdrop: Boolean = false,
    val heroPosterColors: Boolean = true,
    val showLogos: Boolean = true,
    val autoBackdrop: Boolean = true,
    val watchGlow: Boolean = false,
    val haptic: Boolean = true,
    val showPosterTitles: Boolean = false,
    val largePosters: Boolean = false,
    val playerLogo: Boolean = true,
    val watchedBadge: Boolean = true,
)

interface UserSettingsRepository {
    val settings: StateFlow<UserSettings>

    fun setExtractColors(enabled: Boolean)

    fun setExtractBackdrop(enabled: Boolean)

    fun setHeroPosterColors(enabled: Boolean)

    fun setShowLogos(enabled: Boolean)

    fun setAutoBackdrop(enabled: Boolean)

    fun setWatchGlow(enabled: Boolean)

    fun setHaptic(enabled: Boolean)

    fun setShowPosterTitles(enabled: Boolean)

    fun setLargePosters(enabled: Boolean)

    fun setPlayerLogo(enabled: Boolean)

    fun setWatchedBadge(enabled: Boolean)
}
