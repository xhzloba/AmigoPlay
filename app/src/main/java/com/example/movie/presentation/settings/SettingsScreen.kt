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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.example.movie.ui.theme.CinemaBlack
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
        onExtractColors = repository::setExtractColors,
        onShowLogos = repository::setShowLogos,
        onShowPosterTitles = repository::setShowPosterTitles,
        onAutoBackdrop = repository::setAutoBackdrop,
        onHaptic = repository::setHaptic,
        onGyro = repository::setGyro,
    )
}

@Composable
private fun SettingsScreen(
    settings: UserSettings,
    onExtractColors: (Boolean) -> Unit,
    onShowLogos: (Boolean) -> Unit,
    onShowPosterTitles: (Boolean) -> Unit,
    onAutoBackdrop: (Boolean) -> Unit,
    onHaptic: (Boolean) -> Unit,
    onGyro: (Boolean) -> Unit,
) {
    var sectionName by rememberSaveable { mutableStateOf(SettingsSection.Interface.name) }
    val section = SettingsSection.valueOf(sectionName)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CinemaBlack)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(top = BrandTopInset),
    ) {
        SettingsTabs(
            selected = section,
            onSelect = { sectionName = it.name },
        )
        Spacer(Modifier.height(28.dp))
        when (section) {
            SettingsSection.Interface -> {
                SettingRow(
                    title = stringResource(R.string.settings_extract_colors),
                    hint = stringResource(R.string.settings_extract_colors_hint),
                    checked = settings.extractColors,
                    onChecked = onExtractColors,
                )
                SettingsDivider()
                SettingRow(
                    title = stringResource(R.string.settings_poster_titles),
                    hint = stringResource(R.string.settings_poster_titles_hint),
                    checked = settings.showPosterTitles,
                    onChecked = onShowPosterTitles,
                )
                SettingsDivider()
                SettingRow(
                    title = stringResource(R.string.settings_logos),
                    hint = stringResource(R.string.settings_logos_hint),
                    checked = settings.showLogos,
                    onChecked = onShowLogos,
                )
                SettingsDivider()
                SettingRow(
                    title = stringResource(R.string.settings_backdrop_auto),
                    hint = stringResource(R.string.settings_backdrop_auto_hint),
                    checked = settings.autoBackdrop,
                    onChecked = onAutoBackdrop,
                )
            }
            SettingsSection.Sensors -> {
                SettingRow(
                    title = stringResource(R.string.settings_haptic),
                    hint = stringResource(R.string.settings_haptic_hint),
                    checked = settings.haptic,
                    onChecked = onHaptic,
                )
                SettingsDivider()
                SettingRow(
                    title = stringResource(R.string.settings_gyro),
                    hint = stringResource(R.string.settings_gyro_hint),
                    checked = settings.gyro,
                    onChecked = onGyro,
                )
            }
            SettingsSection.About -> AboutPage(Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Composable
private fun AboutPage(modifier: Modifier = Modifier) {
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
        val updates = LocalAppUpdate.current
        val context = LocalContext.current
        val tick = rememberHapticTick()
        Row(
            modifier = Modifier.padding(horizontal = PosterGridInset),
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
        var shown by rememberSaveable { mutableIntStateOf(ReleasePage) }
        val visible = Changelog.releases.take(shown)
        visible.forEachIndexed { index, release ->
            if (index > 0) {
                Spacer(Modifier.height(8.dp))
                SettingsDivider()
                Spacer(Modifier.height(28.dp))
            }
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
        when {
            shown < Changelog.releases.size -> ReleaseAction(
                text = stringResource(R.string.changelog_more),
                onClick = { shown = (shown + ReleasePage).coerceAtMost(Changelog.releases.size) },
            )
            Changelog.releases.size > ReleasePage -> ReleaseAction(
                text = stringResource(R.string.changelog_collapse),
                onClick = { shown = ReleasePage },
            )
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
            .background(CinemaSurface)
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

private const val ReleasePage = 5

@Composable
private fun ReleaseAction(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        modifier = Modifier
            .padding(start = PosterGridInset, end = PosterGridInset, top = 4.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        color = VersionBlue,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    )
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

@Composable
private fun SettingRow(
    title: String,
    hint: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onChecked(!checked) },
            )
            .padding(horizontal = PosterGridInset, vertical = 18.dp),
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
    val travel by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = spring(dampingRatio = 1f, stiffness = 500f),
        label = "switch",
    )
    Box(
        modifier = Modifier
            .width(48.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (checked) Color.White else Color(0xFF2C2C30))
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
