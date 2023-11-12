package dev.dexsr.klio.player.android.presentation.root.main

import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import dev.dexsr.klio.android.base.checkInMainLooper
import dev.dexsr.klio.base.compose.rememberWithCustomEquality
import dev.dexsr.klio.base.kt.referentialEqualityFun
import dev.dexsr.klio.player.android.presentation.root.PlaybackControlMainScreenState
import dev.dexsr.klio.player.shared.LocalMediaArtwork
import dev.dexsr.klio.player.shared.PlaybackMediaDescription
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import timber.log.Timber

// Pager Implementation over androidx.compose.foundation pager
// try to mimic YT Music behavior

// copied from compact implementation

@OptIn(ExperimentalFoundationApi::class)
class FoundationDescriptionPagerState(
    private val panelState: PlaybackControlMainScreenState
) {


    @MainThread
    fun connectLayout(
    ): FoundationDescriptionPagerLayoutConnection {
        checkInMainLooper()

        val connection = FoundationDescriptionPagerLayoutConnection(
            panelState = panelState,
        ).apply { init() }

        return connection
    }
}

@OptIn(ExperimentalFoundationApi::class)
class FoundationDescriptionPagerLayoutConnection  constructor(
    private val panelState: PlaybackControlMainScreenState,
) {

    private var disposables = mutableListOf<DisposableHandle>()
    private val rPageCountState = mutableStateOf(0)
    private val coroutineScope = CoroutineScope(SupervisorJob())

    private val savedItemStateMap = mutableMapOf<Int, Map<String, Any>>()
    private val itemStateSaveDelegate = mutableMapOf<Int, () -> Map<String, Any>>()

    private var _userScrollUnlockKey = Any()
    private var sUserScrollPixels: Float = 0f
    private var correctingPage = false
    private var _rCount = 0
    private var _lCount = 0
    private var _direction = 0


    private val _pagerState = object : PagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f
    ) {

        override val pageCount: Int
            get() = rPageCountState.value

        override suspend fun scroll(
            scrollPriority: MutatePriority,
            block: suspend ScrollScope.() -> Unit
        ) {
            if (scrollPriority == MutatePriority.UserInput) {
                super.scroll(scrollPriority) {  }
                object : ScrollScope {
                    override fun scrollBy(pixels: Float): Float {
                        if (correctingPage) {
                            sUserScrollPixels += pixels
                            return pixels
                        }
                        _userScrollMark = true
                        return dispatchRawDelta(pixels)
                    }
                }.run { block() }
                return
            }
            super.scroll(scrollPriority, block)
        }
    }

    private var rActualPage = 0
    private var _userScrollMark = false

    val pagerState: PagerState
        get() = _pagerState

    val userScrollEnabledState = mutableStateOf(true)

    val placeholderPage = mutableStateOf<Int>(2)

    val renderState = mutableStateOf<DescriptionPagerRenderData?>(
        null,
        neverEqualPolicy()
    )

    val userDraggingState = mutableStateOf<Boolean>(false)

    fun mediaDescriptionAsFlow(mediaID: String): Flow<PlaybackMediaDescription?> {
        return panelState.mediaMetadataProvider.descriptionAsFlow(mediaID)
    }

    fun mediaArtworkAsFlow(mediaID: String): Flow<LocalMediaArtwork?> {
        return panelState.mediaMetadataProvider.artworkAsFlow(mediaID)
    }

    fun init() {
        var k: Job? = null
        val dragListenerInstall = Job()
        val pageListenerInstall = Job()
        coroutineScope.launch(Dispatchers.Main) {
            val dragInteraction = mutableListOf<DragInteraction.Start>()
            pagerState.interactionSource.interactions
                .onStart { dragListenerInstall.complete() }
                .collect { interaction ->
                    Timber.d("DEBUG: interaction=$interaction")
                    when (interaction) {
                        is DragInteraction.Start -> dragInteraction.add(interaction)
                        is DragInteraction.Cancel -> dragInteraction.remove(interaction.start)
                        is DragInteraction.Stop -> dragInteraction.remove(interaction.start)
                    }
                    userDraggingState.value = dragInteraction.isNotEmpty()
                }
        }.also { disposables.add(DisposableHandle { it.cancel() }) }

        coroutineScope.launch(Dispatchers.Main) {
            dragListenerInstall.join()
            var dragWaiter: Job? = null
            snapshotFlow { pagerState.currentPage }
                .onStart { pageListenerInstall.complete() }
                .distinctUntilChanged()
                .collect { page ->
                    dragWaiter?.cancel()
                    if (!_userScrollMark) {
                        return@collect
                    }
                    dragWaiter = launch {
                        snapshotFlow { userDraggingState.value }
                            .distinctUntilChanged()
                            .collect drag@ { dragging ->
                                if (dragging || !_userScrollMark) return@drag
                                if (page == rActualPage + 1 + _rCount) {
                                    _rCount++
                                    panelState.playbackController.seekToNextMediaItemAsync()
                                } else if (page == rActualPage - 1 - _lCount) {
                                    _lCount++
                                    panelState.playbackController.seekToPreviousMediaItemAsync()
                                } else if (page != rActualPage) {
                                    snapToCorrectPageSuspend()
                                }
                            }
                    }
                }
        }

        panelState.playbackController.invokeOnTimelineChanged(2) { timeline, step ->
            k?.cancel()
            k = coroutineScope.launch(Dispatchers.Main) {
                Timber.d("DEBUG: onTimelineChanged)_step=$step")
                dragListenerInstall.join()
                pageListenerInstall.join()
                savedItemStateMap.clear()
                val targetPage = rActualPage + step
                val userScrollUnlockKey = Any()
                _userScrollUnlockKey = userScrollUnlockKey
                _rCount = 0
                _lCount = 0
                run scroll@ {
                    val savePage = targetPage
                    if (targetPage > pagerState.pageCount || targetPage < 0) {
                        return@scroll
                    }
                    placeholderPage.value = targetPage
                    animateMoveToPage(targetPage) ; scroller!!.join()
                    savedItemStateMap[savePage] = persistentMapOf<String, Any>()
                        .builder()
                        .apply {
                            itemStateSaveDelegate[targetPage]?.let {
                                val toSave = it.invoke()
                                putAll(toSave)
                            }
                        }
                        .build()
                }
                correctingPage = true
                renderState.value = DescriptionPagerRenderData(
                    timeline = DescriptionPagerTimeline(
                        currentIndex = timeline.currentIndex,
                        items = timeline.items
                    ),
                    savedInstanceState = savedItemStateMap.toMap(),
                    pageOverride = mapOf(timeline.currentIndex to placeholderPage.value),
                    internalData = mapOf("userScrollUnlockKey" to userScrollUnlockKey)
                )
            }
        }.also { disposables.add(it) }
    }

    private var scroller: Job? = null
    private var scrollerMovePage: Int = -1
    private fun animateMoveToPage(
        page: Int
    ) {
        scroller?.cancel()
        scroller = coroutineScope.launch(AndroidUiDispatcher.Main) {
            if (pagerState.pageCount >= page) {
                scrollerMovePage = page
                try {
                    if (pagerState.currentPage == page) return@launch
                    _userScrollMark = false
                    pagerState.animateScrollToPage(page, animationSpec = tween(200))
                } finally {
                    scrollerMovePage = -1
                }
            }
        }
    }

    private fun snapMoveToPage(
        page: Int
    ) {
        scroller?.cancel()
        scroller = coroutineScope.launch(AndroidUiDispatcher.Main) {
            if (pagerState.pageCount >= page) {
                scrollerMovePage = page
                try {
                    _userScrollMark = false
                    pagerState.scrollToPage(page)
                } finally {
                    scrollerMovePage = -1
                }
            }
        }
    }

    private suspend fun snapToCorrectPageSuspend(
    ) {
        checkInMainLooper()
        scroller?.cancel()
        val t = Job()
        scroller = t
        runCatching {
            doSnapToCorrectPage(
            )
        }.fold(
            onSuccess = { t.complete() },
            onFailure = { t.cancel() }
        )
    }

    private suspend fun doSnapToCorrectPage(
    ) {
        withContext(AndroidUiDispatcher.Main) {
            if (pagerState.pageCount > 0) {
                _userScrollMark = false
                pagerState.scrollToPage(
                    renderState.value?.timeline?.currentIndex ?: 0,
                )
                if (userDraggingState.value) {
                    pagerState.dispatchRawDelta(sUserScrollPixels.also { sUserScrollPixels = 0f })
                }
            }
        }
    }

    @AnyThread
    fun preRender(
        renderData: DescriptionPagerRenderData?
    ) {
        rPageCountState.value = renderData?.timeline?.items?.size ?: 0
    }

    private var r: DescriptionPagerRenderData? = null
    @MainThread
    fun postRender(
        renderData: DescriptionPagerRenderData?
    ) {
        checkInMainLooper()
        if (r === renderData) {
            return
        }
        val o = r
        r = renderData
        if (o?.timeline !== renderData?.timeline) {
            rActualPage = (renderData?.timeline?.currentIndex ?: 0).coerceAtLeast(0)
            // the Foundation pager should already be recomposed, snap to the correct page
            coroutineScope.launch(Dispatchers.Main.immediate) {
                try { scroller?.join() } finally {
                    snapToCorrectPageSuspend()
                    if (o?.internalData !== renderData?.internalData) {
                        if (renderData?.internalData?.get("userScrollUnlockKey") == _userScrollUnlockKey) {
                            correctingPage = false
                        }
                    }
                }
            }
        }
    }

    @MainThread
    fun dispose(

    ) {
        checkInMainLooper()
        coroutineScope.cancel()
        disposables.forEach { it.dispose() }
    }

    @MainThread
    fun itemRendered(
        page: Int,
        onSaveInstanceState: () -> Map<String, Any>
    ) {
        checkInMainLooper()
        itemStateSaveDelegate[page] = onSaveInstanceState
    }
}

data class DescriptionPagerTimeline(
    val currentIndex: Int,
    val items: ImmutableList<String>
)

data class DescriptionPagerRenderData(
    val timeline: DescriptionPagerTimeline?,
    val savedInstanceState: Map<Int, Map<String, Any>>,
    val pageOverride: Map<Int, Int>,
    val internalData: Map<String, Any>
)

@Composable
fun FoundationDescriptionPager(
    modifier: Modifier,
    state: FoundationDescriptionPagerState,
    contentPadding: PaddingValues
) {

    val layoutConnectionState = remember {
        mutableStateOf<FoundationDescriptionPagerLayoutConnection?>(null)
    }

    layoutConnectionState.value
        ?.let { layoutConnection ->
            FoundationHorizontalPager(
                modifier = modifier.fillMaxSize(),
                layoutConnection = layoutConnection,
                contentPadding = contentPadding
            )
        }

    DisposableEffect(
        state,
        effect = {
            val connection = state.connectLayout()
                .also { layoutConnectionState.value = it }

            onDispose { connection.dispose() }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FoundationHorizontalPager(
    modifier: Modifier,
    layoutConnection: FoundationDescriptionPagerLayoutConnection,
    contentPadding: PaddingValues
) {
    val render = layoutConnection.renderState.value
    layoutConnection.preRender(render)
    val savedInstanceStateKey = rememberWithCustomEquality(
        key = render?.savedInstanceState,
        keyEquality = referentialEqualityFun()
    ) {
        Any()
    }
    HorizontalPager(
        modifier = modifier.fillMaxSize(),
        state = layoutConnection.pagerState,
        flingBehavior = PagerDefaults.flingBehavior(
            state = layoutConnection.pagerState,
            pagerSnapDistance = PagerSnapDistance.atMost(1)
        ),
        userScrollEnabled = layoutConnection.userScrollEnabledState.value,
        beyondBoundsPageCount = Int.MAX_VALUE,
    ) { pageIndex ->
        val mediaID = render!!.timeline!!.items[pageIndex]
        FoundationDescriptionPagerItem(
            modifier = Modifier,
            layoutConnection = layoutConnection,
            page = pageIndex,
            mediaID = mediaID,
            savedInstanceStateKey = savedInstanceStateKey,
            savedInstanceState = render.savedInstanceState[render.pageOverride[pageIndex] ?: pageIndex],
            contentPadding = contentPadding
        )
    }
    SideEffect {
        layoutConnection.postRender(render)
    }
}

@Composable
private inline fun FoundationDescriptionPagerItem(
    modifier: Modifier = Modifier,
    page: Int,
    mediaID: String,
    layoutConnection: FoundationDescriptionPagerLayoutConnection,
    savedInstanceStateKey: Any,
    savedInstanceState: Map<String, Any>?,
    contentPadding: PaddingValues
) {

    val artwork = remember(savedInstanceStateKey) {
        mutableStateOf(
            savedInstanceState?.get("MediaArtwork") as? LocalMediaArtwork
        )
    }.apply {
        LaunchedEffect(this, layoutConnection, mediaID) {
            layoutConnection.mediaArtworkAsFlow(mediaID).collect { value = it }
        }
    }.value

    val ctx = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        // we can provide a custom painter instead
        AsyncImage(
            modifier = Modifier
                .fillMaxSize(),
            model = remember(ctx, artwork) {
                ImageRequest.Builder(ctx)
                    .data(artwork?.image?.value)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .memoryCacheKey(mediaID)
                    .build()
            },
            contentDescription = "art",
            contentScale = if (artwork?.allowTransform == true) {
                ContentScale.Crop
            } else {
                ContentScale.Fit
            }
        )
    }


    SideEffect {
        layoutConnection.itemRendered(page) {
            persistentMapOf<String, Any>()
                .builder()
                .apply {
                    artwork?.let { put("MediaArtwork", artwork) }
                }
                .build()
        }
    }
}