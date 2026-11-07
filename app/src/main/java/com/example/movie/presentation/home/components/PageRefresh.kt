package com.example.movie.presentation.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.example.movie.ui.theme.PlayBlue
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PageRefresh(
    refreshing: Boolean,
    onRefresh: () -> Unit,
    indicatorOffset: Dp,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val pull = rememberPullToRefreshState()
    val gate = rememberRefreshGate()
    Box(
        modifier.pullToRefresh(
            isRefreshing = refreshing,
            state = pull,
            enabled = enabled,
            onRefresh = onRefresh,
        ),
    ) {
        Box(Modifier.fillMaxSize().nestedScroll(gate)) {
            content()
        }
        PullToRefreshDefaults.Indicator(
            state = pull,
            isRefreshing = refreshing,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = indicatorOffset),
            containerColor = Color(0xFF2C2C2E),
            color = PlayBlue,
        )
    }
}

@Composable
private fun rememberRefreshGate(): NestedScrollConnection {
    val deadzone = with(LocalDensity.current) { 32.dp.toPx() }
    return remember(deadzone) {
        object : NestedScrollConnection {
            var held = 0f
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source != NestedScrollSource.UserInput || available.y <= 0f) {
                    if (available.y < 0f) held = 0f
                    return Offset.Zero
                }
                if (held >= deadzone) return Offset.Zero
                val take = min(available.y, deadzone - held)
                held += take
                return Offset(0f, take)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                held = 0f
                return Velocity.Zero
            }
        }
    }
}
