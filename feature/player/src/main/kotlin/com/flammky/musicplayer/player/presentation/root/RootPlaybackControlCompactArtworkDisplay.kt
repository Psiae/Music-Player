package com.flammky.musicplayer.player.presentation.root

import androidx.compose.runtime.Composable
import coil.compose.AsyncImage
import com.flammky.musicplayer.player.presentation.root.CompactControlArtworkState.Applier.Companion.PrepareComposition
import com.flammky.musicplayer.player.presentation.root.CompactControlArtworkState.Companion.getContentScale
import com.flammky.musicplayer.player.presentation.root.CompactControlArtworkState.Companion.getImageModel
import com.flammky.musicplayer.player.presentation.root.CompactControlArtworkState.Companion.getLayoutModifier

@Composable
fun CompactControlArtwork(
    state: CompactControlArtworkState
) {
    state.applier
        .apply { PrepareComposition() }
    val model = state.getImageModel()
    AsyncImage(
        modifier = state.getLayoutModifier(model),
        model = model,
        contentScale = state.getContentScale(model),
        contentDescription = null
    )
}