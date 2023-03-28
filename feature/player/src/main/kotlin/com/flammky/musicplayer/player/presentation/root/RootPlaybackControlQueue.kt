package com.flammky.musicplayer.player.presentation.root

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.flammky.android.medialib.common.mediaitem.AudioFileMetadata
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.providers.metadata.VirtualFileMetadata
import com.flammky.musicplayer.base.R
import com.flammky.musicplayer.base.compose.NoInlineBox
import com.flammky.musicplayer.base.compose.NoInlineColumn
import com.flammky.musicplayer.base.compose.NoInlineRow
import com.flammky.musicplayer.base.media.MediaConstants
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.*
import com.flammky.musicplayer.player.presentation.main.compose.TransitioningCompactPlaybackControl
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import dev.flammky.compose_components.reorderable.ReorderableLazyColumn
import dev.flammky.compose_components.reorderable.itemsIndexed
import dev.flammky.compose_components.reorderable.rememberReorderableLazyListState
import kotlinx.coroutines.launch

@Composable
internal fun BoxScope.RootPlaybackControlQueueScreen(
    composition: RootPlaybackControlQueueScope
) {
    val transitionHeightState = updateTransition(targetState = composition.showSelf, label = "")
        .animateInt(
            label = "Playback Control Transition",
            targetValueByState = { targetShowSelf ->
                if (targetShowSelf) composition.fullyVisibleHeightTarget else 0
            },
            transitionSpec = {
                remember(composition, targetState) {
                    tween(
                        durationMillis = if (targetState) {
                            if (composition.rememberFullyTransitionedState) {
                                // consume
                                composition.rememberFullyTransitionedState = false
                                0
                            } else {
                                350
                            }
                        } else {
                            250
                        },
                        easing = if (targetState) FastOutSlowInEasing else LinearOutSlowInEasing
                    )
                }
            }
        ).apply {
            LaunchedEffect(
                composition, this.value, composition.fullyVisibleHeightTarget,
                block = {
                    composition.apply {
                        visibleHeight = value
                        if (visibleHeight == fullyVisibleHeightTarget) {
                            rememberFullyTransitionedState = true
                        } else if (visibleHeight == 0) {
                            rememberFullyTransitionedState = false
                        }
                    }
                }
            )
        }

    val blockerFraction =
        if (composition.showSelf || composition.visibleHeight > 0) 1f else 0f

    // Inline Box to immediately consume input during transition
    Box(
        modifier = Modifier
            .fillMaxSize(blockerFraction)
            .clickable(
                enabled = true,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        if (composition.visibleHeight > 0) {
            val localDensity = LocalDensity.current
            val absBackgroundColor = Theme.absoluteBackgroundColorAsState().value
            Layout(
                modifier = Modifier
                    .runRemember {
                        Modifier.fillMaxWidth()
                    }
                    .runRemember(
                        localDensity,
                        composition,
                        composition.fullyVisibleHeightTarget,
                        transitionHeightState.value
                    ) {
                        this
                            .height(
                                height = with(localDensity) {
                                    composition.fullyVisibleHeightTarget.toDp()
                                }
                            )
                            .offset(
                                y = with(localDensity) {
                                    (composition.fullyVisibleHeightTarget - transitionHeightState.value).toDp()
                                }
                            )
                    }
                    .runRemember(absBackgroundColor) {
                        this
                            .background(
                                color = absBackgroundColor.copy(alpha = 0.97f)
                            )
                    },
                composition = composition,
                compactControl = {
                    CompactControl(it)
                },
                queueColumn = {
                    it.statusBarInset = with(LocalDensity.current) {
                        WindowInsets.statusBars.getTop(this).toDp()
                    }
                    ReorderableLazyQueueColumn(composition = it)
                }
            )
        }
    }
}

@Composable
private fun Layout(
    modifier: Modifier,
    composition: RootPlaybackControlQueueScope,
    compactControl: @Composable BoxScope.(composition: RootPlaybackControlQueueScope) -> Unit,
    queueColumn: @Composable BoxScope.(composition: RootPlaybackControlQueueScope) -> Unit
) {
    val transitioned by remember(composition) {
        derivedStateOf {
            composition.fullyVisibleHeightTarget >= 0 &&
            composition.visibleHeight >= composition.fullyVisibleHeightTarget
        }
    }
    val contentAlpha by animateFloatAsState(
        targetValue = if (transitioned) {
            1f
        } else {
            0f
        },
        animationSpec = tween(
            durationMillis = if (transitioned
                && Theme.isDarkAsState().value
            ) {
                150
            } else {
                0
            }
        )
    )
    Box(modifier = modifier
        .fillMaxSize()
        .alpha(contentAlpha)
    ) {
        queueColumn(composition)
        compactControl(composition)
    }
}

@Composable
private fun BoxScope.CompactControl(composition: RootPlaybackControlQueueScope) {
    val navigationBarHeight = with(LocalDensity.current) {
        WindowInsets.navigationBars
            .getBottom(this)
            .toDp()
    }
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(navigationBarHeight)
            .background(Theme.backgroundColorAsState().value)
            .align(Alignment.BottomCenter)
    )
    TransitioningCompactPlaybackControl(
        bottomVisibilityVerticalPadding = navigationBarHeight
    ) { height ->
        composition.playbackControlContentPadding = PaddingValues(bottom = height)
    }
}

@Composable
private fun BoxScope.ReorderableLazyQueueColumn(composition: RootPlaybackControlQueueScope) {
    val coroutineScope = rememberCoroutineScope()
    val queueOverrideState = remember(composition) {
        mutableStateOf<OldPlaybackQueue?>(null)
    }
    val maskedQueueState = remember(composition) {
        derivedStateOf { queueOverrideState.value ?: composition.currentQueue }
    }.apply {
        DisposableEffect(key1 = this, effect = {
            composition.incrementQueueReaderCount()
            onDispose { composition.decrementQueueReaderCount() }
        })
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            NoInlineBox() contentBox@ {
                val queue = maskedQueueState.value
                if (queue == OldPlaybackQueue.UNSET) return@contentBox
                val lazyListState: LazyListState = rememberLazyListState(
                    initialFirstVisibleItemIndex = (queue.currentIndex),
                    initialFirstVisibleItemScrollOffset = 0
                )
                ReorderableLazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Theme.backgroundColorAsState().value),
                    state = rememberReorderableLazyListState(
                        lazyListState = lazyListState,
                        onDragStart = { true },
                        onDragEnd = remember(composition) {
                            end@ { cancelled, from, to, handle ->
                                if (cancelled || from.index == to.index || from.key == to.key) {
                                    return@end handle.done()
                                }
                                val expectFromId = (from.key as String)
                                val expectToId = (to.key as String)
                                composition.requestMoveQueueItemAsync(
                                    from.index,
                                    expectFromId,
                                    to.index,
                                    expectToId
                                ).run {
                                    coroutineScope.launch {
                                        try {
                                            await()
                                        } finally {
                                            handle.done()
                                        }
                                    }
                                }
                            }
                        },
                    ),
                    contentPadding = composition.columnVisibilityContentPadding,
                ) content@ {
                    val maskedQueue = maskedQueueState.value
                    if (maskedQueue === OldPlaybackQueue.UNSET) {
                        return@content
                    }
                    itemsIndexed(
                        maskedQueue.list,
                        { _, item -> item }
                    ) { index, item ->
                        val id = item
                        val playing = index == maskedQueue.currentIndex
                        Box(
                            modifier = run {
                                val svc = Theme.surfaceVariantColorAsState().value
                                val bck = Theme.backgroundColorAsState().value
                                val dragging = info.dragging
                                remember(this, playing, maskedQueue.currentIndex, dragging) {
                                    var acc: Modifier = Modifier
                                    acc = acc.reorderingItemVisualModifiers()
                                    acc = acc.background(
                                        color = if (playing) {
                                            svc.copy(alpha = 0.6f).compositeOver(bck)
                                        } else if (dragging) {
                                            svc.copy(0.4f).compositeOver(bck)
                                        } else {
                                            bck
                                        }
                                    )
                                    if (index < maskedQueue.currentIndex) {
                                        acc = acc.alpha(0.6f)
                                    }
                                    acc
                                }
                            }
                        ) {
                            Row {
                                NoInlineBox(
                                    modifier = Modifier
                                        .height(60.dp)
                                        .width(35.dp)
                                ) {
                                    Icon(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .align(Alignment.Center)
                                            .reorderInput(),
                                        painter = painterResource(id = R.drawable.drag_handle_material_fill),
                                        contentDescription = "Drag_Queue_Item",
                                        tint = Theme.backgroundContentColorAsState().value
                                    )
                                }
                                QueueItem(
                                    id = id,
                                    composition = composition,
                                ) play@ {
                                    if (index != maskedQueue.currentIndex) {
                                        composition.requestSeekIndexAsync(
                                            maskedQueue.currentIndex,
                                            maskedQueue.list[maskedQueue.currentIndex],
                                            index,
                                            item,
                                        )
                                    }
                                }
                            }
                            if (!Theme.isDarkAsState().value) {
                                Divider(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 5.dp)
                                        .align(Alignment.BottomCenter),
                                    color = Theme.surfaceVariantColorAsState().value
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueItem(
    id: String,
    composition: RootPlaybackControlQueueScope,
    play: () -> Unit
) {
    val artwork by remember(id, composition) {
        mutableStateOf<Any?>(null)
    }.apply {
        LaunchedEffect(key1 = this, block = {
            composition.observeTrackArtwork(id)
                .collect {
                    value = it
                }
        })
    }
    val metadata by remember(id, composition) {
        mutableStateOf<MediaMetadata?>(AudioMetadata.UNSET)
    }.apply {
        LaunchedEffect(key1 = this, block = {
            composition.observeTrackMetadata(id)
                .collect {
                    value = it
                }
        })
    }
    Box(
        modifier = Modifier
            .height(60.dp)
            .clickable { play() },
        contentAlignment = Alignment.Center
    ) {
        NoInlineRow(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            ItemArtworkCard(artwork)

            Spacer(modifier = Modifier.width(8.dp))

            ItemTextDescription(
                modifier = Modifier.weight(1f, true),
                metadata = metadata
            )

            Spacer(modifier = Modifier.width(8.dp))

            val moreResId =
                if (Theme.isDarkAsState().value) {
                    R.drawable.more_vert_48px
                } else {
                    R.drawable.more_vert_48px_dark
                }

            val context = LocalContext.current

            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .fillMaxHeight()
                    .aspectRatio(1f, true)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable {
                        Toast
                            .makeText(context.applicationContext, "Coming Soon", Toast.LENGTH_SHORT)
                            .show()
                    }
            ) {
                Icon(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxSize(0.5f)
                        .aspectRatio(1f),
                    painter = painterResource(id = moreResId),
                    contentDescription = "More",
                    tint = Theme.backgroundContentColorAsState().value
                )
            }
        }
    }
}

private val ART_UNSET = Any()

@Composable
private fun ItemArtworkCard(
    artwork: Any?
) {
    val context = LocalContext.current

    val art = remember(artwork) {
        artwork?.takeIf { !MediaConstants.isNoArtwork(it) } ?: MediaConstants.NO_ARTWORK
    }

    val shape: Shape = remember {
        RoundedCornerShape(5)
    }

    Card(
        modifier = Modifier
            .fillMaxHeight()
            .aspectRatio(1f, true)
            .clip(shape),
        shape = shape,
        elevation = CardDefaults.elevatedCardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {

        val imageModel = remember(art) {
            ImageRequest.Builder(context)
                .data(art)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build()
        }

        AsyncImage(
            modifier = Modifier
                .fillMaxSize()
                .placeholder(
                    visible = imageModel.data === ART_UNSET,
                    color = localShimmerSurface(),
                    shape = shape,
                    highlight = PlaceholderHighlight.shimmer(highlightColor = localShimmerColor())
                )
                .clip(shape),
            model = imageModel,
            contentDescription = "Artwork",
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun ItemTextDescription(
    modifier: Modifier,
    metadata: MediaMetadata?,
    textColor: Color = Theme.backgroundContentColorAsState().value
) {
    NoInlineColumn(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.Center
    ) {
        if (metadata !is AudioMetadata?) {
            return@NoInlineColumn
        }
        val style = with(MaterialTheme.typography.bodyMedium) {
            copy(
                color = textColor,
                fontWeight = FontWeight.Medium
            )
        }
        val style2 = with(MaterialTheme.typography.bodySmall) {
            copy(
                color = textColor,
                fontWeight = FontWeight.Normal
            )
        }
        val placeHolderState = remember(metadata) {
            derivedStateOf { metadata === AudioMetadata.UNSET }
        }
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .placeholder(
                    visible = placeHolderState.value,
                    color = localShimmerSurface(),
                    highlight = PlaceholderHighlight.shimmer(localShimmerColor())
                ),
            text = metadata
                ?.let { metadata ->
                    metadata.title
                        ?: (metadata as? AudioFileMetadata)?.let { afm ->
                            afm.file.fileName
                                ?: (afm.file as? VirtualFileMetadata)?.uri?.toString()
                        }
                }
                ?: "",
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(3.dp))

        val formattedDuration = remember(metadata) {
            val seconds = metadata?.duration?.inWholeSeconds ?: 0
            if (seconds > 3600) {
                String.format(
                    "%02d:%02d:%02d",
                    seconds / 3600,
                    seconds % 3600 / 60,
                    seconds % 60
                )
            } else {
                String.format(
                    "%02d:%02d",
                    seconds / 60,
                    seconds % 60
                )
            }
        }

        val separator = remember {
            String("\u00b7".toByteArray(Charsets.UTF_8))
        }

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .placeholder(
                    visible = placeHolderState.value,
                    color = localShimmerSurface(),
                    highlight = PlaceholderHighlight.shimmer(localShimmerColor())
                ),
            text = formattedDuration + " " +
                    separator + " " +
                    (metadata?.albumArtistName ?: metadata?.artistName ?: ""),
            style = style2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun localShimmerSurface(): Color {
    val svar = Theme.surfaceVariantColorAsState().value
    val s = Theme.surfaceColorAsState().value
    return remember(svar, s) {
        s.copy(alpha = 0.45f).compositeOver(svar)
    }
}

@Composable
fun localShimmerColor(): Color {
    val sf = localShimmerSurface()
    val content = Theme.backgroundContentColorAsState().value
    return remember(sf, content) {
        content.copy(alpha = 0.6f).compositeOver(sf)
    }
}