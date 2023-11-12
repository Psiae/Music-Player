package dev.dexsr.klio.player.android.presentation.root

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.dexsr.klio.player.android.presentation.root.main.FoundationDescriptionPager
import dev.dexsr.klio.player.android.presentation.root.main.FoundationDescriptionPagerState

@Composable
fun PlaybackControlMainScreenPager(
    state: PlaybackControlMainScreenState,
    contentPadding: PaddingValues
) {
    // TODO: impl
    BoxWithConstraints(
        modifier = Modifier
            .height(300.dp)
            .fillMaxWidth()
    ) {
        FoundationDescriptionPager(
            modifier = Modifier,
            state = remember(state) {
                FoundationDescriptionPagerState(state)
            },
            contentPadding = contentPadding
        )
    }
}