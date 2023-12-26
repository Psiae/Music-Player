package com.flammky.musicplayer.player.presentation.root.main.queue

import android.os.Parcel
import android.os.Parcelable
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastLastOrNull
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.NullRequestData
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
import com.flammky.musicplayer.base.media.playback.isUNSET
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.*
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import dev.dexsr.klio.base.compose.SimpleStack
import dev.dexsr.klio.base.kt.castOrNull
import dev.dexsr.klio.base.theme.md3.compose.LocalIsThemeDark
import dev.flammky.compose_components.reorderable.ReorderableLazyColumn
import dev.flammky.compose_components.reorderable.ReorderableLazyItemScope
import dev.flammky.compose_components.reorderable.itemsIndexed
import dev.flammky.compose_components.reorderable.rememberReorderableLazyListState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

@Composable
fun ReorderableQueueLazyColumn(
    transitionState: QueueContainerTransitionState,
    state: ReorderableQueueLazyColumnState,
    backgroundColor: Color = Theme.surfaceVariantColorAsState().value
) {
    val reorder = remember(state) {
        ReorderingApplier(state.intents, state.dataSource)
    }
    val upReorder = rememberUpdatedState(newValue = reorder)
    val upBackgroundColor = rememberUpdatedState(newValue = backgroundColor)
    val upState = rememberUpdatedState(newValue = state)
    val upPadding = rememberUpdatedState(
        PaddingValues(
            top = state.topContentPadding,
            bottom = state.bottomContentPadding
        )
    )
    if (transitionState.rememberFullTransitionRendered) {
        Box {
            RenderQueue(
                reorder = upReorder::value,
                intents = { upState.value.intents },
                dataSource = { upState.value.dataSource },
                contentPadding = upPadding::value,
                backgroundColor = upBackgroundColor::value
            )
            reorder.actualQueueState.value.let { q ->
                if (q.isUNSET || q.list.isNotEmpty()) return@let
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = "EMPTY QUEUE",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Theme.backgroundContentColorAsState().value.copy(alpha = 0.68f)
                )
            }
        }
    }
    DisposableEffect(
        state, reorder,
        effect = {
            val observeQ = reorder.observeQueue()
            onDispose { observeQ.cancel() ; reorder.dispose() }
        }
    )
}

@Composable
private fun RenderQueue(
    reorder: () -> ReorderingApplier,
    dataSource: () -> ReorderableQueueLazyColumnDataSource,
    intents: () -> ReorderableQueueLazyColumnIntents,
    contentPadding: () -> PaddingValues,
    backgroundColor: () -> Color
) {
    val upReorder = rememberUpdatedState(newValue = reorder())
    val upQueue = rememberUpdatedState(newValue = upReorder.value.actualQueueState.value)
    val queue = upQueue.value
    if (upQueue.value == OldPlaybackQueue.UNSET) {
        return Column(modifier = Modifier.fillMaxSize()) {}
    }
    val lazyListState: LazyListState = rememberLazyListState(
        initialFirstVisibleItemIndex = (upQueue.value.currentIndex),
        initialFirstVisibleItemScrollOffset = 0
    )
    val upBackgroundColor = rememberUpdatedState(newValue = backgroundColor)
    val upIntents = rememberUpdatedState(newValue = intents())
    val upDataSource = rememberUpdatedState(newValue = dataSource())
    val state = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        onDragStart = start@ { true },
        onDragEnd = end@ { cancelled, from, to, handle ->
            val fromID = from.key.castOrNull<QueueItemPositionKey>()?.idInQueue
            val toID = to.key.castOrNull<QueueItemPositionKey>()?.idInQueue
            if (cancelled || fromID == null || toID == null) {
                handle.done()
                return@end
            }
            reorder().dispatchMove(from.index, fromID, to.index, toID).invokeOnCompletion { handle.done() }
        },
    )
    ReorderableLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(upBackgroundColor.value.invoke())
            },
        state = state,
        contentPadding = contentPadding(),
        content = remember(queue) {
            {
                itemsIndexed(
                    queue.list,
                    { _, item ->
                        QueueItemPositionKey(qID = "", idInQueue = item)
                    },
                    contentType = { _, _ -> "type" }
                ) { index, item ->
                    val q = queue
                    val backgroundColor = upBackgroundColor.value.invoke()
                    DraggableQueueItem(
                        getBackgroundColor = upBackgroundColor.value,
                        observeTrackArtwork = { upDataSource.value.observeTrackArtwork(item) },
                        observeTrackMetadata = { upDataSource.value.observeTrackMetadata(item) },
                        play = {
                            upIntents.value.requestSeekIndexAsync(
                                q.currentIndex,
                                q.list.getOrNull(q.currentIndex) ?: "",
                                index,
                                item
                            )
                        },
                        currentlyPlaying = index == q.currentIndex,
                        behindCurrentlyPlaying = index < q.currentIndex,
                        withDivider = !LocalIsThemeDark.current && q.list.getOrNull(index + 1) != null
                    )
                }
            }
        }
    )
    LaunchedEffect(
        key1 = queue.list.getOrNull(queue.currentIndex),
        block = {
            if (!state.remeasure()) return@LaunchedEffect
            val index = queue.currentIndex
            val id = queue.list.getOrNull(index) ?: return@LaunchedEffect
            with(state.lazyListState.layoutInfo) {
                val firstFullyVisibleItem = visibleItemsInfo.fastFirstOrNull {
                    it.offset >= viewportStartOffset
                } ?: return@LaunchedEffect
                val lastFullyVisibleItem = visibleItemsInfo.fastLastOrNull {
                    it.offset + it.size <= viewportEndOffset + viewportStartOffset - afterContentPadding
                } ?: return@LaunchedEffect
                if (index in firstFullyVisibleItem.index .. lastFullyVisibleItem.index) {
                    // fully visible, do nothing
                    return@with
                }
                if (index < firstFullyVisibleItem.index) {
                    // not fully visible, bring it down
                    state.lazyListState.animateScrollToItem(index)
                    return@with
                }
                if (index <= visibleItemsInfo.last().index) {
                    // not fully visible, bring it up
                    state.lazyListState.animateScrollToItem(
                        index = firstFullyVisibleItem.index,
                        scrollOffset = run {
                            val endOffset = viewportEndOffset + viewportStartOffset - afterContentPadding
                            val itemInInfo = visibleItemsInfo[index - visibleItemsInfo.first().index]
                            val itemEndBottom = itemInInfo.offset + itemInInfo.size
                            val offsetToFirstFullyVisibleItem = firstFullyVisibleItem.offset
                            (itemEndBottom - offsetToFirstFullyVisibleItem ) - endOffset
                        }.coerceAtLeast(0)
                    )
                    return@with
                }
                // TODO: find a better way
                // not visible at all, scroll until it's visible
                state.lazyListState.scrollToItem(
                    index = index,
                    scrollOffset = -(viewportEndOffset - 1)
                )
                with(state.lazyListState.layoutInfo) {
                    val firstFullyVisibleItem = requireNotNull(
                        visibleItemsInfo.fastFirstOrNull {
                            it.offset >= viewportStartOffset
                        }
                    )
                    state.lazyListState.scrollToItem(
                        index = firstFullyVisibleItem.index,
                        scrollOffset = run {
                            val endOffset = viewportEndOffset + viewportStartOffset - afterContentPadding
                            val itemInInfo = visibleItemsInfo[index - visibleItemsInfo.first().index]
                            val itemEndBottom = itemInInfo.offset + itemInInfo.size
                            val offsetToFirstFullyVisibleItem = firstFullyVisibleItem.offset
                            (itemEndBottom - offsetToFirstFullyVisibleItem ) - endOffset
                        }.coerceAtLeast(0)
                    )
                }
            }
        }
    )
}

@Composable
private fun ReorderableLazyItemScope.DraggableQueueItem(
    getBackgroundColor: () -> Color,
    observeTrackArtwork: () -> Flow<Any?>,
    observeTrackMetadata: () -> Flow<MediaMetadata?>,
    play: () -> Unit,
    currentlyPlaying: Boolean,
    behindCurrentlyPlaying: Boolean,
    withDivider: Boolean,
) {
    Timber.d("RECOMPOSE_DraggableQueueItem")
    Box(
        modifier = run {
            val dragging = info.dragging
            val backgroundColor = getBackgroundColor()
            remember(this, currentlyPlaying, backgroundColor, dragging, behindCurrentlyPlaying) {
                var acc: Modifier = Modifier
                acc = acc
                    .reorderingItemVisualModifiers()
                if (dragging) {
                    acc = acc
                        .drawBehind {
                            drawRect(
                                color = Color.White.copy(alpha = 0.35f).compositeOver(backgroundColor)
                            )
                        }
                } else if (currentlyPlaying) {
                    acc = acc
                        .drawBehind {
                            drawRect(
                                color = Color.White.copy(alpha = 0.25f).compositeOver(backgroundColor)
                            )
                        }
                }
                if (behindCurrentlyPlaying) {
                    acc = acc
                        .alpha(0.6f)
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
            QueueItemContent(
                observeTrackMetadata = observeTrackMetadata,
                observeTrackArtwork = observeTrackArtwork,
                play = play
            )
        }
        if (withDivider) {
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

@Composable
private fun QueueItemContent(
    observeTrackArtwork: () -> Flow<Any?>,
    observeTrackMetadata: () -> Flow<MediaMetadata?>,
    play: () -> Unit
) {
    val artwork by remember(observeTrackArtwork) {
        mutableStateOf<Any?>(null)
    }.apply {
        LaunchedEffect(key1 = this, block = {
            observeTrackArtwork()
                .collect {
                    value = it
                }
        })
    }
    val metadata by remember(observeTrackMetadata) {
        mutableStateOf<MediaMetadata?>(AudioMetadata.UNSET)
    }.apply {
        LaunchedEffect(key1 = this, block = {
            observeTrackMetadata()
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
                    visible = imageModel.data === NullRequestData,
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
private fun localShimmerSurface(): Color {
    val svar = Theme.surfaceVariantColorAsState().value
    val s = Theme.surfaceColorAsState().value
    return remember(svar, s) {
        s.copy(alpha = 0.45f).compositeOver(svar)
    }
}

@Composable
private fun localShimmerColor(): Color {
    val sf = localShimmerSurface()
    val content = Theme.backgroundContentColorAsState().value
    return remember(sf, content) {
        content.copy(alpha = 0.6f).compositeOver(sf)
    }
}

private class ReorderingApplier(
    private val intents: ReorderableQueueLazyColumnIntents,
    private val source: ReorderableQueueLazyColumnDataSource,
) {

    private val coroutineScope = CoroutineScope(SupervisorJob())

    private var _observeJobCount = 0
    private var _observeJob: Job? = null

    private val _queueState = mutableStateOf<OldPlaybackQueue>(value = OldPlaybackQueue.UNSET)
    private val _reorderedQueueState = mutableStateOf<ReorderedQueue?>(value = null)
    private var ignoreObserve = false
    private var ignoreObserveStagedQueue: OldPlaybackQueue? = null

    val actualQueueState = derivedStateOf {
        _queueState.value
    }
    val reorderedQueueState = derivedStateOf {
        _reorderedQueueState.value?.modified
    }
    val maskedQueueState = derivedStateOf {
        reorderedQueueState.value ?: actualQueueState.value
    }



    fun observeQueue(): Job = coroutineScope.launch(Dispatchers.Main.immediate) {
        if (_observeJob == null) {
            _observeJob = coroutineScope.launch(Dispatchers.Main.immediate) {
                source.observeQueue()
                    .collect {
                        if (ignoreObserve) {
                            ignoreObserveStagedQueue = it
                            return@collect
                        }
                        _queueState.value = it
                    }
            }
        }
        runCatching {
            _observeJobCount++
            _observeJob!!.join()
        }.onFailure { ex ->
            if (ex !is CancellationException) throw ex
            if (--_observeJobCount == 0) _observeJob!!.cancel()
        }
    }

    private var dispatchMoveC = 0
    fun dispatchMove(
        from: Int,
        expectFromID: String,
        to: Int,
        expectToID: String
    ): Job {
        dispatchMoveC++
        ignoreObserve = true
        return coroutineScope.launch(Dispatchers.Main.immediate) {
            intents.requestMoveQueueItemAsync(
                from,
                expectFromID,
                to,
                expectToID
            ).await()
        }.apply {
            invokeOnCompletion {
                if (--dispatchMoveC == 0) {
                    ignoreObserve = false
                    ignoreObserveStagedQueue?.let {
                        ignoreObserveStagedQueue = null
                        _queueState.value = it
                    }
                }
            }
        }
    }

    fun dispose() {
        coroutineScope.cancel()
    }

    @Immutable
    private data class ReorderedQueue(
        val base: OldPlaybackQueue,
        val modified: OldPlaybackQueue,
        val node: Pair<Int, Int>
    )
}

private data class QueueItemPositionKey(
    val qID: String,
    val idInQueue: String,
) : Parcelable {
    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(qID)
        dest.writeString(idInQueue)
    }

    companion object {

        @JvmField
        @Suppress("unused")
        val CREATOR = object : Parcelable.Creator<QueueItemPositionKey> {

            override fun createFromParcel(source: Parcel): QueueItemPositionKey {
                return QueueItemPositionKey(
                    source.readString()!!,
                    source.readString()!!,
                )
            }

            override fun newArray(size: Int): Array<QueueItemPositionKey?> {
                return arrayOfNulls(size)
            }
        }
    }
}