package com.flammky.musicplayer.player.presentation.root.main.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flammky.android.medialib.common.mediaitem.AudioFileMetadata
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.providers.metadata.VirtualFileMetadata
import com.flammky.musicplayer.base.media.MediaConstants
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.flammky.musicplayer.base.media.playback.PlaybackProperties
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.absoluteBackgroundColorAsState
import com.flammky.musicplayer.base.theme.compose.backgroundColorAsState
import com.flammky.musicplayer.base.theme.compose.backgroundContentColorAsState
import com.flammky.musicplayer.player.R
import kotlinx.coroutines.flow.Flow

@Composable
internal fun CompactControl(
    transitionState: QueueContainerTransitionState,
    state: CompactControlState
) {
    val id = observeCurrentQueue(
        state.dataSource.observeQueue
    ).run { list.getOrNull(currentIndex) ?: "" }

    LayoutPlacement(
        state = state,
        transitionState = transitionState,
        artwork = { modifier ->
            ArtworkDisplay(
                modifier = modifier,
                artwork = observeArtworkDisplayData(
                    id = id,
                    observe = state.dataSource.observeArtwork
                )
            )
        },
        description = { modifier ->
            TextDescription(
                modifier = modifier.fillMaxSize(),
                metadata = observeTrackDescription(
                    id = id,
                    observe = state.dataSource.observeMetadata
                )
            )
        },
        propertiesButtons = { modifier ->
            PropertiesControl(
                modifier = modifier,
                properties = observeProperties(state.dataSource.observeProperties),
                seekPrevious = state.intents.seekPrevious,
                play = state.intents.play,
                pause = state.intents.pause,
                seekNext = state.intents.seekNext
            )
        }
    )
}

@Composable
private fun LayoutPlacement(
    state: CompactControlState,
    transitionState: QueueContainerTransitionState,
    artwork: @Composable (Modifier) -> Unit,
    description: @Composable (Modifier) -> Unit,
    propertiesButtons: @Composable (Modifier) -> Unit
) {
    val statusBarHeight = with(LocalDensity.current) {
        WindowInsets.statusBars.getTop(this).toDp()
    }
    state.apply { stagedLayoutHeight = 55.dp + statusBarHeight }
    Column(
        modifier = Modifier
            .background(
                Theme.absoluteBackgroundColorAsState().value
                    .copy(0.1f)
                    .compositeOver(Theme.backgroundColorAsState().value)
            )
            .alpha(if (transitionState.rememberFullTransitionRendered) 1f else 0f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        Spacer(modifier = Modifier.height(statusBarHeight))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp)
                .padding(horizontal = (7.5).dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            artwork(Modifier)
            Spacer(modifier = Modifier.width(5.dp))
            description(Modifier.weight(2f, true))
            Spacer(modifier = Modifier.width(5.dp))
            propertiesButtons(Modifier)
        }
    }

}

@Composable
private fun ArtworkDisplay(
    modifier: Modifier,
    artwork: Any?
) {
    val ctx = LocalContext.current
    AsyncImage(
        modifier = modifier
            .fillMaxHeight()
            .aspectRatio(1f)
            .background(Theme.backgroundColorAsState().value),
        model = remember(artwork, ctx) {
            ImageRequest.Builder(ctx)
                .data(artwork)
                .build()
        },
        contentDescription = "art",
        contentScale = ContentScale.Crop
    )
}

@Composable
private fun TextDescription(
    modifier: Modifier,
    metadata: MediaMetadata?
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = remember(metadata) {
                if (metadata === MediaMetadata.UNSET) {
                    ""
                } else {
                    metadata?.title
                        ?: metadata.run {
                            (this as? AudioFileMetadata)?.file
                                ?.let { fileMetadata ->
                                    fileMetadata.fileName
                                        ?.ifBlank {
                                            (fileMetadata as? VirtualFileMetadata)?.uri?.toString()
                                        }
                                        ?: ""
                                }
                                ?.ifEmpty { "TITLE_EMPTY" }?.ifBlank { "TITLE_BLANK" }
                        }
                        ?: "TITLE_NONE"
                }
            },
            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
            fontWeight = FontWeight.Bold,
            color = Theme.backgroundContentColorAsState().value,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(1.dp))

        Text(
            text = remember(metadata) {
                if (metadata === MediaMetadata.UNSET) {
                    ""
                } else {
                    (metadata as? AudioMetadata)
                        ?.let {
                            it.albumArtistName ?: it.artistName
                        }
                        ?.ifEmpty { "TITLE_EMPTY" }?.ifBlank { "TITLE_BLANK" }
                        ?: "SUBTITLE_NONE"
                }

            },
            fontSize = MaterialTheme.typography.labelMedium.fontSize,
            fontWeight = FontWeight.SemiBold,
            color = Theme.backgroundContentColorAsState().value,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PropertiesControl(
    modifier: Modifier,
    properties: PlaybackProperties,
    seekPrevious: () -> Unit,
    play: () -> Unit,
    pause: () -> Unit,
    seekNext: () -> Unit,
) {
    val prevInteractionSource = remember {
        MutableInteractionSource()
    }
    val pwrInteractionSource = remember {
        MutableInteractionSource()
    }
    val nextInteractionSource = remember {
        MutableInteractionSource()
    }
    Row(
        modifier,
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
        ) {
            Icon(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(if (prevInteractionSource.collectIsPressedAsState().value) 21.dp else 24.dp)
                    .clickable(
                        enabled = properties.canSeekPrevious,
                        interactionSource = prevInteractionSource,
                        indication = null,
                        onClick = seekPrevious
                    ),
                painter = painterResource(
                    id = R.drawable.ios_glyph_seek_previous_100
                ),
                contentDescription = "prev",
                tint = Theme.backgroundContentColorAsState().value
            )
        }
        Spacer(modifier = Modifier.width(5.dp))
        Box(
            modifier = Modifier
                .size(30.dp)
                .clickable(
                    enabled = !properties.playWhenReady || properties.canPlay,
                    interactionSource = pwrInteractionSource,
                    indication = null,
                    onClick = { if (properties.playWhenReady) pause() else play() }
                )
        ) {
            Icon(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(if (pwrInteractionSource.collectIsPressedAsState().value) 24.dp else 27.dp),
                painter = painterResource(
                    id = if (properties.playWhenReady) {
                        R.drawable.ios_glyph_pause_100
                    } else {
                        R.drawable.ios_glyph_play_100
                    }
                ),
                contentDescription = if (properties.playWhenReady) "pause" else "play",
                tint = Theme.backgroundContentColorAsState().value
            )
        }
        Spacer(modifier = Modifier.width(5.dp))
        Box(
            modifier = Modifier
                .size(30.dp)
        ) {
            Icon(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(if (nextInteractionSource.collectIsPressedAsState().value) 21.dp else 24.dp)
                    .clickable(
                        enabled = properties.canSeekNext,
                        interactionSource = nextInteractionSource,
                        indication = null,
                        onClick = seekNext
                    ),
                painter = painterResource(
                    id = R.drawable.ios_glyph_seek_next_100
                ),
                contentDescription = "next",
                tint = Theme.backgroundContentColorAsState().value
            )
        }
    }
}

@Composable
private fun observeCurrentQueue(
    observe: () -> Flow<OldPlaybackQueue>
): OldPlaybackQueue {
    return remember(observe) {
        observe()
    }.collectAsState(initial = OldPlaybackQueue.UNSET).value
}

@Composable
private fun observeArtworkDisplayData(
    id: String,
    observe: (String) -> Flow<Any?>
): Any? {
    return remember(id, observe) {
        observe(id)
    }.collectAsState(initial = MediaConstants.ARTWORK_UNSET).value
}

@Composable
private fun observeTrackDescription(
    id: String,
    observe: (String) -> Flow<MediaMetadata?>
): MediaMetadata? {
    return remember(id, observe) {
        observe(id)
    }.collectAsState(initial = MediaMetadata.UNSET).value
}

@Composable
private fun observeProperties(
    observe: () -> Flow<PlaybackProperties>
): PlaybackProperties = observe().collectAsState(initial = PlaybackProperties.UNSET).value