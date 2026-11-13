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

    override fun setExtractBackdrop(enabled: Boolean) = write {
        it.copy(
            extractBackdrop = enabled,
            extractPlex = if (enabled) false else it.extractPlex,
        )
    }

    override fun setExtractPlex(enabled: Boolean) = write {
        it.copy(
            extractPlex = enabled,
            extractBackdrop = if (enabled) false else it.extractBackdrop,
        )
    }

    override fun setPlexBackdrop(enabled: Boolean) = write { it.copy(plexBackdrop = enabled) }

    override fun setHeroPosterColors(enabled: Boolean) = write { it.copy(heroPosterColors = enabled) }

    override fun setHeroPosterPage(enabled: Boolean) = write { it.copy(heroPosterPage = enabled) }

    override fun setShowLogos(enabled: Boolean) = write { it.copy(showLogos = enabled) }

    override fun setAutoBackdrop(enabled: Boolean) = write { it.copy(autoBackdrop = enabled) }

    override fun setWatchGlow(enabled: Boolean) = write { it.copy(watchGlow = enabled) }

    override fun setHaptic(enabled: Boolean) = write { it.copy(haptic = enabled) }

    override fun setShowPosterTitles(enabled: Boolean) = write { it.copy(showPosterTitles = enabled) }

    override fun setLargePosters(enabled: Boolean) = write { it.copy(largePosters = enabled) }

    override fun setPlayerLogo(enabled: Boolean) = write { it.copy(playerLogo = enabled) }

    override fun setPlayerSpeed(speed: Float) = write { it.copy(playerSpeed = speed) }

    override fun setPlayerPicture(mode: String) = write { it.copy(playerPicture = mode) }

    override fun setPlayerDelayUs(delayUs: Long) = write { it.copy(playerDelayUs = delayUs) }

    override fun setPlayerBrightness(brightness: Float?) = write { it.copy(playerBrightness = brightness) }

    override fun setPlayerUnderCamera(enabled: Boolean) = write { it.copy(playerUnderCamera = enabled) }

    override fun setPlayerAutoNext(enabled: Boolean) = write { it.copy(playerAutoNext = enabled) }

    override fun setPlayerHdr(enabled: Boolean) = write { it.copy(playerHdr = enabled) }

    override fun setWatchedBadge(enabled: Boolean) = write { it.copy(watchedBadge = enabled) }

    override fun setWhiteTitles(enabled: Boolean) = write { it.copy(whiteTitles = enabled) }

    override fun setHideBottomBarOnScroll(enabled: Boolean) = write { it.copy(hideBottomBarOnScroll = enabled) }

    override fun setNavLabels(enabled: Boolean) = write { it.copy(navLabels = enabled) }

    override fun setAppTheme(theme: String) = write {
        it.copy(appTheme = if (theme == "graphite") "graphite" else "cinema")
    }

    private fun write(transform: (UserSettings) -> UserSettings) {
        val next = transform(state.value)
        prefs.edit()
            .putBoolean(KEY_COLORS, false)
            .putBoolean(KEY_BACKDROP_COLORS, next.extractBackdrop)
            .putBoolean(KEY_PLEX, next.extractPlex)
            .putBoolean(KEY_PLEX_BACKDROP, next.plexBackdrop)
            .putBoolean(KEY_HERO_COLORS, next.heroPosterColors)
            .putBoolean(KEY_HERO_PAGE, next.heroPosterPage)
            .putBoolean(KEY_LOGOS, next.showLogos)
            .putBoolean(KEY_BACKDROP, next.autoBackdrop)
            .putBoolean(KEY_WATCH_GLOW, next.watchGlow)
            .putBoolean(KEY_HAPTIC, next.haptic)
            .putBoolean(KEY_POSTER_TITLES, next.showPosterTitles)
            .putBoolean(KEY_LARGE_POSTERS, next.largePosters)
            .putBoolean(KEY_PLAYER_LOGO, next.playerLogo)
            .putFloat(KEY_PLAYER_SPEED, next.playerSpeed)
            .putString(KEY_PLAYER_PICTURE, next.playerPicture)
            .putLong(KEY_PLAYER_DELAY, next.playerDelayUs)
            .putFloat(KEY_PLAYER_BRIGHTNESS, next.playerBrightness ?: -1f)
            .putBoolean(KEY_PLAYER_UNDER_CAMERA, next.playerUnderCamera)
            .putBoolean(KEY_PLAYER_AUTO_NEXT, next.playerAutoNext)
            .putBoolean(KEY_PLAYER_HDR, next.playerHdr)
            .putBoolean(KEY_WATCHED_BADGE, next.watchedBadge)
            .putBoolean(KEY_WHITE_TITLES, next.whiteTitles)
            .putBoolean(KEY_HIDE_BAR, next.hideBottomBarOnScroll)
            .putBoolean(KEY_NAV_LABELS, next.navLabels)
            .putString(KEY_THEME, next.appTheme)
            .commit()
        state.value = next
    }

    private fun read(): UserSettings {
        if (!prefs.contains(KEY_FRAME_DEFAULT)) {
            val oldPosterDefault = prefs.getBoolean(KEY_COLORS, true) &&
                !prefs.getBoolean(KEY_BACKDROP_COLORS, false)
            prefs.edit()
                .putBoolean(KEY_FRAME_DEFAULT, true)
                .apply {
                    if (oldPosterDefault) {
                        putBoolean(KEY_COLORS, false)
                        putBoolean(KEY_BACKDROP_COLORS, true)
                    }
                }
                .commit()
        }
        if (!prefs.contains(KEY_PLEX_DEFAULT)) {
            prefs.edit()
                .putBoolean(KEY_PLEX_DEFAULT, true)
                .putBoolean(KEY_PLEX, true)
                .putBoolean(KEY_COLORS, false)
                .putBoolean(KEY_BACKDROP_COLORS, false)
                .commit()
        }
        if (!prefs.contains(KEY_DEFAULTS_230)) {
            prefs.edit()
                .putBoolean(KEY_DEFAULTS_230, true)
                .putBoolean(KEY_WHITE_TITLES, true)
                .putBoolean(KEY_POSTER_TITLES, true)
                .putBoolean(KEY_HERO_COLORS, false)
                .putBoolean(KEY_PLEX, true)
                .putBoolean(KEY_PLEX_BACKDROP, true)
                .putBoolean(KEY_BACKDROP_COLORS, false)
                .commit()
        }
        if (prefs.getBoolean(KEY_COLORS, false)) {
            prefs.edit().putBoolean(KEY_COLORS, false).commit()
        }
        return UserSettings(
        extractBackdrop = prefs.getBoolean(KEY_BACKDROP_COLORS, false),
        extractPlex = prefs.getBoolean(KEY_PLEX, true),
        plexBackdrop = prefs.getBoolean(KEY_PLEX_BACKDROP, true),
        heroPosterColors = prefs.getBoolean(KEY_HERO_COLORS, false),
        heroPosterPage = prefs.getBoolean(KEY_HERO_PAGE, true),
        showLogos = prefs.getBoolean(KEY_LOGOS, true),
        autoBackdrop = prefs.getBoolean(KEY_BACKDROP, true),
        watchGlow = prefs.getBoolean(KEY_WATCH_GLOW, false),
        haptic = prefs.getBoolean(KEY_HAPTIC, true),
        showPosterTitles = prefs.getBoolean(KEY_POSTER_TITLES, true),
        largePosters = prefs.getBoolean(KEY_LARGE_POSTERS, false),
        playerLogo = prefs.getBoolean(KEY_PLAYER_LOGO, true),
        playerSpeed = prefs.getFloat(KEY_PLAYER_SPEED, 1f),
        playerPicture = prefs.getString(KEY_PLAYER_PICTURE, "fit") ?: "fit",
        playerDelayUs = prefs.getLong(KEY_PLAYER_DELAY, 0L),
        playerBrightness = prefs.getFloat(KEY_PLAYER_BRIGHTNESS, -1f).takeIf { it in 0f..1f },
        playerUnderCamera = prefs.getBoolean(KEY_PLAYER_UNDER_CAMERA, false),
        playerAutoNext = prefs.getBoolean(KEY_PLAYER_AUTO_NEXT, true),
        playerHdr = prefs.getBoolean(KEY_PLAYER_HDR, true),
        watchedBadge = prefs.getBoolean(KEY_WATCHED_BADGE, true),
        whiteTitles = prefs.getBoolean(KEY_WHITE_TITLES, true),
        hideBottomBarOnScroll = prefs.getBoolean(KEY_HIDE_BAR, true),
        navLabels = prefs.getBoolean(KEY_NAV_LABELS, true),
        appTheme = prefs.getString(KEY_THEME, "cinema") ?: "cinema",
        )
    }

    private companion object {
        const val FILE = "user_settings"
        const val KEY_COLORS = "extract_colors"
        const val KEY_BACKDROP_COLORS = "extract_backdrop"
        const val KEY_PLEX = "extract_plex"
        const val KEY_PLEX_BACKDROP = "plex_backdrop"
        const val KEY_PLEX_DEFAULT = "plex_color_default"
        const val KEY_FRAME_DEFAULT = "frame_color_default"
        const val KEY_DEFAULTS_230 = "defaults_2_30"
        const val KEY_HERO_COLORS = "hero_poster_colors"
        const val KEY_HERO_PAGE = "hero_poster_page"
        const val KEY_LOGOS = "show_logos"
        const val KEY_BACKDROP = "auto_backdrop"
        const val KEY_WATCH_GLOW = "watch_glow"
        const val KEY_HAPTIC = "haptic"
        const val KEY_POSTER_TITLES = "poster_titles"
        const val KEY_LARGE_POSTERS = "large_posters"
        const val KEY_PLAYER_LOGO = "player_logo"
        const val KEY_PLAYER_SPEED = "player_speed"
        const val KEY_PLAYER_PICTURE = "player_picture"
        const val KEY_PLAYER_DELAY = "player_delay_us"
        const val KEY_PLAYER_BRIGHTNESS = "player_brightness"
        const val KEY_PLAYER_UNDER_CAMERA = "player_under_camera"
        const val KEY_PLAYER_AUTO_NEXT = "player_auto_next"
        const val KEY_PLAYER_HDR = "player_hdr"
        const val KEY_WATCHED_BADGE = "watched_badge"
        const val KEY_WHITE_TITLES = "white_titles"
        const val KEY_HIDE_BAR = "hide_bottom_bar_scroll"
        const val KEY_NAV_LABELS = "nav_labels"
        const val KEY_THEME = "app_theme"
    }
}
