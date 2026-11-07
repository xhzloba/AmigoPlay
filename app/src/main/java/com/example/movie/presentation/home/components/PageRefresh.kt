package com.example.movie.presentation.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.example.movie.ui.theme.PlayBlue

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
    Box(
        modifier.pullToRefresh(
            isRefreshing = refreshing,
            state = pull,
            enabled = enabled,
            onRefresh = onRefresh,
        ),
    ) {
        content()
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
