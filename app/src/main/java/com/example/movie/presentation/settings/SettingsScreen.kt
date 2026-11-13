package com.example.movie.presentation.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.view.HapticFeedbackConstants
import com.example.movie.BuildConfig
import com.example.movie.R
import com.example.movie.domain.model.UserSettings
import com.example.movie.presentation.haptic.rememberHapticTick
import com.example.movie.presentation.home.components.BrandLogoBleed
import com.example.movie.presentation.home.components.BrandTopInset
import com.example.movie.presentation.home.components.PosterGridInset
import com.example.movie.presentation.update.LocalAppUpdate
import com.example.movie.presentation.update.UpdateAlert
import com.example.movie.presentation.update.UpdateBadge
import com.example.movie.presentation.update.UpdateNotice
import com.example.movie.presentation.update.UpdatePhase
import com.example.movie.presentation.update.downloadStatus
import com.example.movie.presentation.home.components.CategoryTabFontSize
import com.example.movie.presentation.home.components.CategoryTabLetterSpacing
import com.example.movie.presentation.home.components.CategoryTabLineHeight
import com.example.movie.domain.model.AppRelease
import com.example.movie.domain.model.DownloadProgress
import com.example.movie.presentation.common.HandleSheet
import com.example.movie.presentation.common.THEME_CINEMA
import com.example.movie.presentation.common.THEME_GRAPHITE
import com.example.movie.presentation.common.appBackground
import com.example.movie.ui.theme.CinemaMuted
import com.example.movie.ui.theme.CinemaSurface

private enum class SettingsSection {
    Interface,
    Sensors,
    About,
    ;

    val label: Int
        get() = when (this) {
            Interface -> R.string.settings_interface
            Sensors -> R.string.settings_sensors
            About -> R.string.settings_about
        }
}

@Composable
fun SettingsRoute() {
    val repository = LocalUserSettings.current
    val settings by repository.settings.collectAsStateWithLifecycle()
    SettingsScreen(
        settings = settings,
        onExtractBackdrop = repository::setExtractBackdrop,
        onExtractPlex = repository::setExtractPlex,
        onPlexBackdrop = repository::setPlexBackdrop,
        onHeroPosterColors = repository::setHeroPosterColors,
        onHeroPosterPage = repository::setHeroPosterPage,
        onShowLogos = repository::setShowLogos,
        onWatchedBadge = repository::setWatchedBadge,
        onShowPosterTitles = repository::setShowPosterTitles,
        onWhiteTitles = repository::setWhiteTitles,
        onHideBottomBarOnScroll = repository::setHideBottomBarOnScroll,
        onNavLabels = repository::setNavLabels,
        onLargePosters = repository::setLargePosters,
        onAutoBackdrop = repository::setAutoBackdrop,
        onWatchGlow = repository::setWatchGlow,
        onHaptic = repository::setHaptic,
        onAppTheme = repository::setAppTheme,
    )
}

@Composable
private fun SettingsScreen(
    settings: UserSettings,
    onExtractBackdrop: (Boolean) -> Unit,
    onExtractPlex: (Boolean) -> Unit,
    onPlexBackdrop: (Boolean) -> Unit,
    onHeroPosterColors: (Boolean) -> Unit,
    onHeroPosterPage: (Boolean) -> Unit,
    onShowLogos: (Boolean) -> Unit,
    onWatchedBadge: (Boolean) -> Unit,
    onShowPosterTitles: (Boolean) -> Unit,
    onWhiteTitles: (Boolean) -> Unit,
    onHideBottomBarOnScroll: (Boolean) -> Unit,
    onNavLabels: (Boolean) -> Unit,
    onLargePosters: (Boolean) -> Unit,
    onAutoBackdrop: (Boolean) -> Unit,
    onWatchGlow: (Boolean) -> Unit,
    onHaptic: (Boolean) -> Unit,
    onAppTheme: (String) -> Unit,
) {
    var sectionName by rememberSaveable { mutableStateOf(SettingsSection.Interface.name) }
    val section = SettingsSection.valueOf(sectionName)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .appBackground()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(top = BrandTopInset),
    ) {
        SettingsTabs(
            selected = section,
            onSelect = { sectionName = it.name },
        )
        Spacer(Modifier.height(28.dp))
        when (section) {
            SettingsSection.Interface -> Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 96.dp),
            ) {
                ThemeGroup(
                    selected = settings.appTheme,
                    onSelect = onAppTheme,
                )
                Spacer(Modifier.height(22.dp))
                SettingGroup(
                    title = stringResource(R.string.settings_group_posters),
                    accent = PosterBlue,
                    items = listOf(
                        SettingItem(
                            title = stringResource(R.string.settings_large_posters),
                            hint = stringResource(R.string.settings_large_posters_hint),
                            checked = settings.largePosters,
                            onChecked = onLargePosters,
                        ),
                        SettingItem(
                            title = stringResource(R.string.settings_poster_titles),
                            hint = stringResource(R.string.settings_poster_titles_hint),
                            checked = settings.showPosterTitles,
                            onChecked = onShowPosterTitles,
                        ),
                        SettingItem(
                            title = stringResource(R.string.settings_white_titles),
                            hint = stringResource(R.string.settings_white_titles_hint),
                            checked = settings.whiteTitles,
                            onChecked = onWhiteTitles,
                        ),
                        SettingItem(
                            title = stringResource(R.string.settings_watched_badge),
                            hint = stringResource(R.string.settings_watched_badge_hint),
                            checked = settings.watchedBadge,
                            onChecked = onWatchedBadge,
                        ),
                    ),
                )
                Spacer(Modifier.height(22.dp))
                SettingGroup(
                    title = stringResource(R.string.settings_group_navigation),
                    accent = PosterBlue,
                    items = listOf(
                        SettingItem(
                            title = stringResource(R.string.settings_nav_labels),
                            hint = stringResource(R.string.settings_nav_labels_hint),
                            checked = settings.navLabels,
                            onChecked = onNavLabels,
                        ),
                        SettingItem(
                            title = stringResource(R.string.settings_hide_bar_scroll),
                            hint = stringResource(R.string.settings_hide_bar_scroll_hint),
                            checked = settings.hideBottomBarOnScroll,
                            onChecked = onHideBottomBarOnScroll,
                        ),
                    ),
                )
                Spacer(Modifier.height(22.dp))
                SettingGroup(
                    title = stringResource(R.string.settings_group_color),
                    accent = ColorViolet,
                    items = listOfNotNull(
                        SettingItem(
                            title = stringResource(R.string.settings_hero_colors),
                            hint = stringResource(R.string.settings_hero_colors_hint),
                            checked = settings.heroPosterColors,
                            onChecked = onHeroPosterColors,
                        ),
                        if (settings.heroPosterColors) {
                            SettingItem(
                                title = stringResource(R.string.settings_hero_page),
                                hint = stringResource(R.string.settings_hero_page_hint),
                                checked = settings.heroPosterPage,
                                onChecked = onHeroPosterPage,
                            )
                        } else {
                            null
                        },
                        SettingItem(
                            title = stringResource(R.string.settings_extract_plex),
                            hint = stringResource(R.string.settings_extract_plex_hint),
                            checked = settings.extractPlex,
                            onChecked = onExtractPlex,
                        ),
                        if (settings.extractPlex) {
                            SettingItem(
                                title = stringResource(R.string.settings_plex_backdrop),
                                hint = stringResource(R.string.settings_plex_backdrop_hint),
                                checked = settings.plexBackdrop,
                                onChecked = onPlexBackdrop,
                            )
                        } else {
                            null
                        },
                        SettingItem(
                            title = stringResource(R.string.settings_extract_backdrop),
                            hint = stringResource(R.string.settings_extract_backdrop_hint),
                            checked = settings.extractBackdrop,
                            onChecked = onExtractBackdrop,
                        ),
                    ),
                )
                Spacer(Modifier.height(22.dp))
                SettingGroup(
                    title = stringResource(R.string.settings_group_detail),
                    accent = DetailGreen,
                    items = listOf(
                        SettingItem(
                            title = stringResource(R.string.settings_logos),
                            hint = stringResource(R.string.settings_logos_hint),
                            checked = settings.showLogos,
                            onChecked = onShowLogos,
                        ),
                        SettingItem(
                            title = stringResource(R.string.settings_backdrop_auto),
                            hint = stringResource(R.string.settings_backdrop_auto_hint),
                            checked = settings.autoBackdrop,
                            onChecked = onAutoBackdrop,
                        ),
                        SettingItem(
                            title = stringResource(R.string.settings_watch_glow),
                            hint = stringResource(R.string.settings_watch_glow_hint),
                            checked = settings.watchGlow,
                            onChecked = onWatchGlow,
                        ),
                    ),
                )
            }
            SettingsSection.Sensors -> {
                SettingGroup(
                    title = stringResource(R.string.settings_sensors),
                    accent = SensorAmber,
                    items = listOf(
                        SettingItem(
                            title = stringResource(R.string.settings_haptic),
                            hint = stringResource(R.string.settings_haptic_hint),
                            checked = settings.haptic,
                            onChecked = onHaptic,
                            armHaptic = true,
                        ),
                    ),
                )
            }
            SettingsSection.About -> AboutPage(Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Composable
private fun AboutPage(modifier: Modifier = Modifier) {
    var featuresOpen by rememberSaveable { mutableStateOf(false) }
    val updates = LocalAppUpdate.current
    val context = LocalContext.current
    val tick = rememberHapticTick()
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 96.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.amigoplay_logo),
            contentDescription = "AmigoPlay",
            modifier = Modifier
                .padding(start = PosterGridInset - BrandLogoBleed, end = PosterGridInset)
                .height(36.dp)
                .aspectRatio(754f / 186f, matchHeightConstraintsFirst = true),
            alignment = Alignment.CenterStart,
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.settings_version),
            modifier = Modifier.padding(horizontal = PosterGridInset),
            color = CinemaMuted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PosterGridInset),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = BuildConfig.VERSION_NAME,
                color = VersionBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 38.sp,
                letterSpacing = (-0.6).sp,
            )
            if (updates.checked && updates.release == null) {
                Text(
                    text = stringResource(R.string.update_current),
                    modifier = Modifier.padding(start = 12.dp),
                    color = CinemaMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.settings_about_description),
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        tick?.invoke()
                        featuresOpen = true
                    },
                ),
                color = VersionBlue,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                lineHeight = 22.sp,
            )
        }
        val offer = updates.release
        if (offer != null) {
            Spacer(Modifier.height(16.dp))
            UpdateBanner(
                release = offer,
                phase = updates.phase,
                progress = updates.progress,
                onClick = {
                    tick?.invoke()
                    updates.download(context)
                },
            )
        }
        Text(
            text = if (updates.checking) {
                stringResource(R.string.update_checking)
            } else {
                stringResource(R.string.update_check)
            },
            modifier = Modifier
                .padding(start = PosterGridInset, end = PosterGridInset, top = if (offer != null) 14.dp else 18.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = !updates.checking,
                    onClick = {
                        tick?.invoke()
                        updates.refresh(present = true)
                    },
                ),
            color = if (updates.checking) CinemaMuted else VersionBlue,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        )
        when (updates.notice) {
            UpdateNotice.Current -> NoticeLine(
                text = stringResource(R.string.update_current_notice),
                color = CinemaMuted,
            )
            UpdateNotice.Failed -> NoticeLine(
                text = stringResource(R.string.update_check_failed),
                color = Color(0xFFE7B3B3),
            )
            null -> Unit
        }
        Spacer(Modifier.height(28.dp))
        Changelog.releases.firstOrNull()?.let { release ->
            Text(
                text = release.version,
                modifier = Modifier.padding(horizontal = PosterGridInset),
                color = VersionBlue,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                letterSpacing = (-0.2).sp,
            )
            if (release.added.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                ChangeLabel(stringResource(R.string.changelog_added))
                release.added.forEach { ChangeLine(it) }
            }
            if (release.fixed.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                ChangeLabel(stringResource(R.string.changelog_fixed))
                release.fixed.forEach { ChangeLine(it) }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
    if (featuresOpen) {
        FeaturesSheet(onDismiss = { featuresOpen = false })
    }
}

@Composable
private fun FeaturesSheet(onDismiss: () -> Unit) {
    HandleSheet(onDismiss = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.settings_about_features),
                modifier = Modifier.padding(horizontal = 22.dp),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(14.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp),
            ) {
                Changelog.features.forEachIndexed { index, line ->
                    if (index > 0) Spacer(Modifier.height(12.dp))
                    Text(
                        text = "— $line",
                        color = ChangeText,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun NoticeLine(text: String, color: Color) {
    Text(
        text = text,
        modifier = Modifier.padding(start = PosterGridInset, end = PosterGridInset, top = 8.dp),
        color = color,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )
}

@Composable
private fun UpdateBanner(
    release: AppRelease,
    phase: UpdatePhase,
    progress: DownloadProgress,
    onClick: () -> Unit,
) {
    val fraction by animateFloatAsState(
        targetValue = progress.fraction,
        animationSpec = tween(120),
        label = "bannerProgress",
    )
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .padding(horizontal = PosterGridInset)
            .fillMaxWidth()
            .clip(shape)
            .background(settingsPanel())
            .border(0.5.dp, Color.White.copy(alpha = 0.10f), shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = phase != UpdatePhase.Downloading,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(UpdateAlert),
            )
            Text(
                text = stringResource(R.string.update_available),
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f),
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                lineHeight = 20.sp,
            )
            Text(
                text = release.versionName,
                color = VersionBlue,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                lineHeight = 20.sp,
            )
        }
        val hint = when (phase) {
            UpdatePhase.Downloading -> downloadStatus(progress)
            UpdatePhase.Failed -> stringResource(R.string.update_failed)
            UpdatePhase.Ready -> stringResource(R.string.update_available_hint)
        }
        Text(
            text = hint,
            modifier = Modifier.padding(top = 4.dp),
            color = if (phase == UpdatePhase.Failed) Color(0xFFE7B3B3) else CinemaMuted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
        if (phase == UpdatePhase.Downloading) {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.12f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .background(Color.White),
                )
            }
        }
    }
}

private val VersionBlue = Color(0xFF7EC8FF)
private val ChangeText = Color(0xFFD2D2D8)

@Composable
private fun ChangeLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = PosterGridInset),
        color = CinemaMuted,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    )
}

@Composable
private fun ChangeLine(text: String) {
    Row(
        modifier = Modifier.padding(start = PosterGridInset, end = PosterGridInset, top = 8.dp),
    ) {
        Text(
            text = "—",
            color = ChangeText,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 8.dp),
            color = ChangeText,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        )
    }
}

@Composable
private fun SettingsTabs(
    selected: SettingsSection,
    onSelect: (SettingsSection) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = PosterGridInset),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items(SettingsSection.entries, key = { it.name }) { section ->
            val on = section == selected
            val badge = section == SettingsSection.About && LocalAppUpdate.current.pending
            Row(
                modifier = Modifier.clickable(
                    interactionSource = remember(section) { MutableInteractionSource() },
                    indication = null,
                    onClick = { onSelect(section) },
                ),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = stringResource(section.label),
                    color = if (on) Color.White else Color(0xFF8A8A90),
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                    fontSize = CategoryTabFontSize,
                    lineHeight = CategoryTabLineHeight,
                    letterSpacing = CategoryTabLetterSpacing,
                )
                if (badge) {
                    UpdateBadge(Modifier.padding(start = 5.dp))
                }
            }
        }
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = PosterGridInset)
            .fillMaxWidth()
            .height(0.5.dp)
            .background(Color.White.copy(alpha = 0.08f)),
    )
}

private val ThemeInk = Color(0xFF8AA0B8)
private val ThemeInkGraphite = Color(0xFFA8B4C4)
private val PosterBlue = Color(0xFF3B6EF5)
private val PosterBlueGraphite = Color(0xFF6B8CFF)
private val ColorVioletGraphite = Color(0xFFB08CFF)
private val DetailGreenGraphite = Color(0xFF2BC48A)
private val SensorAmberGraphite = Color(0xFFE0A83A)
/** Приподнятая плашка на фоне graphite, чтобы не сливалась с #1A1E26. */
private val GraphitePanel = Color(0xFF2A2E38)

@Composable
private fun settingsPanel(): Color {
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    return if (settings.appTheme == THEME_GRAPHITE) GraphitePanel else CinemaSurface
}

@Composable
private fun ThemeGroup(
    selected: String,
    onSelect: (String) -> Unit,
) {
    val tick = rememberHapticTick()
    val graphite = selected == THEME_GRAPHITE
    val next = if (graphite) THEME_CINEMA else THEME_GRAPHITE
    fun apply() {
        tick?.invoke()
        onSelect(next)
    }
    Text(
        text = stringResource(R.string.settings_group_theme),
        modifier = Modifier.padding(start = PosterGridInset + 4.dp, end = PosterGridInset, bottom = 8.dp),
        color = if (graphite) ThemeInkGraphite else ThemeInk,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    )
    Box(
        modifier = Modifier
            .padding(horizontal = PosterGridInset)
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (graphite) GraphitePanel else Color.White.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = ::apply,
                ),
        )
        Text(
            text = stringResource(if (graphite) R.string.settings_theme_graphite else R.string.settings_theme_cinema),
            modifier = Modifier.padding(horizontal = 52.dp),
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        ThemeKey(
            glyph = "−",
            graphite = graphite,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp),
            onClick = ::apply,
        )
        ThemeKey(
            glyph = "+",
            graphite = graphite,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp),
            onClick = ::apply,
        )
    }
}

@Composable
private fun ThemeKey(
    glyph: String,
    graphite: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (graphite) Color.White.copy(alpha = 0.12f)
                else Color.White.copy(alpha = 0.14f),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = glyph,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
private val ColorViolet = Color(0xFF9B6BFF)
private val DetailGreen = Color(0xFF14A36A)
private val SensorAmber = Color(0xFFD4920A)

private data class SettingItem(
    val title: String,
    val hint: String,
    val checked: Boolean,
    val onChecked: (Boolean) -> Unit,
    val armHaptic: Boolean = false,
)

@Composable
private fun SettingGroup(
    title: String,
    accent: Color,
    items: List<SettingItem>,
) {
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val graphite = settings.appTheme == THEME_GRAPHITE
    val titleColor = when {
        !graphite -> accent
        accent == PosterBlue -> PosterBlueGraphite
        accent == ColorViolet -> ColorVioletGraphite
        accent == DetailGreen -> DetailGreenGraphite
        accent == SensorAmber -> SensorAmberGraphite
        else -> accent
    }
    Text(
        text = title,
        modifier = Modifier.padding(start = PosterGridInset + 4.dp, end = PosterGridInset, bottom = 8.dp),
        color = titleColor,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    )
    Column(
        modifier = Modifier
            .padding(horizontal = PosterGridInset)
            .clip(RoundedCornerShape(20.dp))
            .background(settingsPanel()),
    ) {
        items.forEachIndexed { index, item ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp)
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(Color.White.copy(alpha = 0.08f)),
                )
            }
            SettingRow(
                title = item.title,
                hint = item.hint,
                checked = item.checked,
                onChecked = item.onChecked,
                armHaptic = item.armHaptic,
            )
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    hint: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    armHaptic: Boolean = false,
) {
    val tick = rememberHapticTick()
    val view = LocalView.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (tick != null) {
                        tick()
                    } else if (armHaptic && !checked) {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                    onChecked(!checked)
                },
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp),
        ) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                letterSpacing = (-0.2).sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = hint,
                color = CinemaMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
        CinemaSwitch(checked)
    }
}

@Composable
private fun CinemaSwitch(checked: Boolean) {
    val settings by LocalUserSettings.current.settings.collectAsStateWithLifecycle()
    val graphite = settings.appTheme == THEME_GRAPHITE
    val travel by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = spring(dampingRatio = 1f, stiffness = 500f),
        label = "switch",
    )
    val offTrack = if (graphite) Color(0xFF3A3F4A) else Color(0xFF2C2C30)
    Box(
        modifier = Modifier
            .width(48.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (checked) Color.White else offTrack)
            .padding(3.dp),
    ) {
        Box(
            modifier = Modifier
                .offset(x = travel)
                .size(22.dp)
                .clip(CircleShape)
                .background(if (checked) Color.Black else Color(0xFF8A8A90)),
        )
    }
}
