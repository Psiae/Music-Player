package dev.dexsr.klio.player.android.presentation.root.compact

import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.dexsr.klio.android.base.checkInMainLooper
import dev.dexsr.klio.base.compose.rememberWithCustomEquality
import dev.dexsr.klio.base.kt.referentialEqualityFun
import dev.dexsr.klio.base.theme.md3.compose.MaterialTheme3
import dev.dexsr.klio.player.shared.PlaybackMediaDescription
import dev.dexsr.klio.player.android.presentation.root.RootCompactPlaybackControlPanelState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow

// Pager Implementation over androidx.compose.foundation pager

@OptIn(ExperimentalFoundationApi::class)
class FoundationDescriptionPagerState(
    private val panelState: RootCompactPlaybackControlPanelState
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
    private val panelState: RootCompactPlaybackControlPanelState,
) {

    private var disposables = mutableListOf<DisposableHandle>()
    private val rPageCountState = mutableStateOf(0)
    private val coroutineScope = CoroutineScope(SupervisorJob())

    private val savedItemStateMap = mutableMapOf<Int, Map<String, Any>>()
    private val itemStateSaveDelegate = mutableMapOf<Int, () -> Map<String, Any>>()

    @Volatile
    private var targetStep = -1

    private val _pagerState = object : PagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f
    ) {

        override val isScrollInProgress: Boolean
            get() = super.isScrollInProgress
        override val pageCount: Int
            get() = rPageCountState.value

        override fun dispatchRawDelta(delta: Float): Float {
            return super.dispatchRawDelta(delta)
        }

        override suspend fun scroll(
            scrollPriority: MutatePriority,
            block: suspend ScrollScope.() -> Unit
        ) {
            super.scroll(scrollPriority, block)
        }
    }

    val pagerState: PagerState
        get() = _pagerState

    val userScrollEnabledState = mutableStateOf(false)
    val isSurfaceDarkState = derivedStateOf { panelState.isSurfaceDark }

    val renderTimelineState = mutableStateOf<DescriptionPagerTimeline?>(null)
    val placeholderPage = mutableStateOf<Int>(2)

    val renderState = mutableStateOf<DescriptionPagerRenderData?>(
        null,
        neverEqualPolicy()
    )

    fun mediaDescriptionAsFlow(mediaID: String): Flow<PlaybackMediaDescription?> {
        return panelState.mediaMetadataProvider.descriptionAsFlow(mediaID)
    }

    fun init() {
        var k: Job? = null
        panelState.playbackController.invokeOnMoveToNextMediaItem { step ->
            k?.cancel()
            k = coroutineScope.launch(Dispatchers.Main) {
                savedItemStateMap.clear()
                val targetPage = pagerState.currentPage + step
                run scroll@ {
                    val savePage = targetPage
                    if (targetPage > pagerState.pageCount) {
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
                val tl = panelState.playbackController.getPlaybackTimelineAsync(2).await()
                renderState.value = DescriptionPagerRenderData(
                    timeline = DescriptionPagerTimeline(
                        currentIndex = tl.currentIndex,
                        items = tl.items
                    ),
                    savedInstanceState = savedItemStateMap.toMap(),
                    pageOverride = mapOf(tl.currentIndex to placeholderPage.value)
                )
            }
        }.also { disposables.add(it) }

        panelState.playbackController.invokeOnMoveToPreviousMediaItem { step ->
            k?.cancel()
            k = coroutineScope.launch(Dispatchers.Main) {
                savedItemStateMap.clear()
                val targetPage = pagerState.currentPage - step
                run scroll@ {
                    if (targetPage < 0) {
                        return@scroll
                    }
                    val savePage = targetPage
                    placeholderPage.value = targetPage
                    animateMoveToPage(targetPage) ; scroller!!.join()
                    savedItemStateMap[savePage] = persistentMapOf<String, Any>()
                        .builder()
                        .apply {
                            itemStateSaveDelegate[targetPage]?.let {
                                putAll(it.invoke())
                            }
                        }
                        .build()
                }
                val tl = panelState.playbackController.getPlaybackTimelineAsync(2).await()
                renderState.value = DescriptionPagerRenderData(
                    timeline = DescriptionPagerTimeline(
                        currentIndex = tl.currentIndex,
                        items = tl.items
                    ),
                    savedInstanceState = savedItemStateMap.toMap(),
                    pageOverride = mapOf(tl.currentIndex to placeholderPage.value)
                )
            }
        }.also { disposables.add(it) }

        panelState.playbackController.invokeOnTimelineChanged(2) { timeline ->
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
                    pagerState.scrollToPage(page)
                } finally {
                    scrollerMovePage = -1
                }
            }
        }
    }

    private suspend fun snapToMiddleSuspend() {
        checkInMainLooper()
        scroller?.cancel()
        val t = Job()
        scroller = t
        runCatching {
            doSnapToMiddleSuspend()
        }.fold(
            onSuccess = { t.complete() },
            onFailure = { t.cancel() }
        )
    }

    private suspend fun doSnapToMiddleSuspend() {
        withContext(AndroidUiDispatcher.Main) {
            if (pagerState.pageCount > 0) {
                pagerState.scrollToPage(renderState.value?.timeline?.currentIndex ?: 0)
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
        // the Foundation pager should already be recomposed, snap to the correct page
        coroutineScope.launch(Dispatchers.Main.immediate) {
            r = renderData
            try { scroller?.join() } finally { snapToMiddleSuspend() }
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
    val pageOverride: Map<Int, Int>
)

@Composable
fun FoundationDescriptionPagerState(
    modifier: Modifier,
    state: FoundationDescriptionPagerState
) {

    val layoutConnectionState = remember {
        mutableStateOf<FoundationDescriptionPagerLayoutConnection?>(null)
    }

    layoutConnectionState.value
        ?.let { layoutConnection ->
            FoundationHorizontalPager(
                modifier = modifier.fillMaxSize(),
                layoutConnection = layoutConnection
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
) {
    val render = layoutConnection.renderState.value
    layoutConnection.preRender(render)
    val key = rememberWithCustomEquality(
        key = render,
        keyEquality = referentialEqualityFun()
    ) {
        Any()
    }
    HorizontalPager(
        modifier = modifier.fillMaxSize(),
        state = layoutConnection.pagerState,
        flingBehavior = PagerDefaults.flingBehavior(state = layoutConnection.pagerState),
        userScrollEnabled = layoutConnection.userScrollEnabledState.value,
        beyondBoundsPageCount = Int.MAX_VALUE,
    ) { pageIndex ->
        val mediaID = render!!.timeline!!.items[pageIndex]
        FoundationDescriptionPagerItem(
            modifier = Modifier,
            layoutConnection = layoutConnection,
            page = pageIndex,
            mediaID = mediaID,
            savedInstanceStateKey = key,
            savedInstanceState = render.savedInstanceState[render.pageOverride[pageIndex] ?: pageIndex]
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
    savedInstanceState: Map<String, Any>?
) {

    val metadata = remember(savedInstanceStateKey) {
        mutableStateOf(
            savedInstanceState?.get("MediaDescription") as? PlaybackMediaDescription
        )
    }.apply {
        LaunchedEffect(this, layoutConnection, mediaID) {
            layoutConnection.mediaDescriptionAsFlow(mediaID).collect { value = it }
        }
    }.value

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        // or we can just add more base ratio of the background on the palette
        val textColorState = remember {
            mutableStateOf(Color.Unspecified)
        }.apply {
            value =
                if (layoutConnection.isSurfaceDarkState.value) {
                    Color(0xFFFFFFFF)
                } else {
                    Color(0xFF101010)
                }
        }

        BasicText(
            text = metadata?.title ?: "",
            style = MaterialTheme3.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = textColorState.value,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(1.dp))

        BasicText(
            text = metadata?.subtitle ?: "",
            style = MaterialTheme3.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = textColorState.value,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    SideEffect {
        layoutConnection.itemRendered(page) {
            persistentMapOf<String, Any>()
                .builder()
                .apply {
                    metadata?.let { put("MediaDescription", metadata) }
                }
                .build()
        }
    }
}