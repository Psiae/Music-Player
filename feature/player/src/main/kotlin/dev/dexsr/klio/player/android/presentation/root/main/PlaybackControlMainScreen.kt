package dev.dexsr.klio.player.android.presentation.root.main

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy
import androidx.compose.ui.util.fastSumBy
import com.flammky.musicplayer.base.compose.LocalLayoutVisibility
import dev.dexsr.klio.base.theme.md3.compose.localMaterial3Background
import kotlin.math.max

@Composable
fun PlaybackControlMainScreen(
    state: PlaybackControlMainScreenState,
) {

    val topSpacing = LocalLayoutVisibility.Top.current
    val bottomSpacing = LocalLayoutVisibility.Bottom.current

    SubcomposeLayout(
        modifier = Modifier
            .fillMaxSize()
            .localMaterial3Background()
            .verticalScroll(state = rememberScrollState())
    ) { constraints ->

        val contentConstraints = constraints
            .copy(minWidth = 0, minHeight = 0)

        val toolbar = subcompose("toolbar") {
            PlaybackControlMainScreenToolbar(
                modifier = Modifier
                    .height(56.dp)
                    .padding(vertical = 8.dp, horizontal = 15.dp),
                state = state
            )
        }.fastMap { it.measure(constraints = contentConstraints) }

        val pager = subcompose("pager") {
            PlaybackControlMainScreenPager(
                state = state,
                contentPadding = PaddingValues(horizontal = 40.dp - 6.dp)
            )
        }.fastMap { it.measure(constraints = contentConstraints) }

        val description = subcompose("description") {
            PlaybackControlMainScreenDescription(
                modifier = Modifier.padding(horizontal = 40.dp),
                state = state
            )
        }.fastMap { it.measure(constraints = contentConstraints) }

        val timeBar = subcompose("timebar") {
            PlaybackControlMainScreenTimeBar(
                modifier = Modifier.padding(horizontal = 40.dp - 6.dp),
                containerState = state,
            )
        }.fastMap { it.measure(constraints = contentConstraints) }

        val controls = subcompose("controls") {
            PlaybackControlMainScreenControls(
                modifier = Modifier.padding(horizontal = 40.dp - 6.dp),
                state = state
            )
        }.fastMap { it.measure(constraints = contentConstraints) }

        val lyric = subcompose("lyric") {
            PlaybackControlMainScreenLyric(state = state)
        }.fastMap { it.measure(constraints = contentConstraints) }

        val measuresToSpacing = arrayListOf(
            toolbar to 4.dp.roundToPx(),
            pager to 16.dp.roundToPx(),
            description to 16.dp.roundToPx(),
            timeBar to 16.dp.roundToPx(),
            controls to 16.dp.roundToPx(),
            lyric to 0.dp.roundToPx()
        )
        var layoutTopOffset = topSpacing.roundToPx()
        var layoutBottomOffset = bottomSpacing.roundToPx()
        layout(
            max(
                measuresToSpacing
                    .fastMaxBy { item ->
                        item.first
                            .fastMaxBy { it.width }?.width ?: 0
                            .plus(item.second)
                    }
                    ?.let { item ->
                        item.first
                            .fastMaxBy { it.width }?.width ?: 0
                            .plus(item.second)
                    }
                    ?: 0,
                constraints.minWidth
            ),
            max(
                measuresToSpacing
                    .fastSumBy { item ->
                        item.first
                            .fastMaxBy { it.height }?.height ?: 0
                            .plus(item.second)
                    }
                    .plus(layoutTopOffset)
                    .plus(layoutBottomOffset),
                constraints.minHeight
            )
        ) {
            measuresToSpacing.fastForEach { item ->
                var h = 0
                item.first.fastForEach { placeable ->
                    placeable.place(0, layoutTopOffset, 0f)
                    if (placeable.height > h) h = placeable.height
                }
                layoutTopOffset += h + item.second
            }
        }
    }
}



