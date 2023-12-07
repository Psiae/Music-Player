package dev.dexsr.klio.player.android.presentation.root.main.pager.gesture

import androidx.compose.ui.Modifier
import dev.dexsr.klio.player.android.presentation.root.main.pager.PlaybackPagerContentInViewModifier

internal fun Modifier.bringContentInView(
    modifier: PlaybackPagerContentInViewModifier
): Modifier = this.then(modifier.modifier)