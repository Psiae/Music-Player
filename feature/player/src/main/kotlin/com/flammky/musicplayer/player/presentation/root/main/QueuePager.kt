package com.flammky.musicplayer.player.presentation.root.main

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.flammky.musicplayer.player.presentation.root.runRemember
import com.google.accompanist.pager.*
import dev.chrisbanes.snapper.ExperimentalSnapperApi
import dev.flammky.compose_components.core.SnapshotRead
import kotlinx.coroutines.Job
import coil.request.ImageRequest as CoilImageRequest

@OptIn(ExperimentalPagerApi::class)
@Composable
fun QueuePager(
    state: QueuePagerState
) = state.coordinator.SetContent(
    pager = { RenderPager { PagerLayout() } }
)

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun QueuePagerCoordinator.PagerLayoutScope.PagerLayout() {
    HorizontalPager(
        modifier = run {
            val contentAlpha = contentAlpha()
            Modifier.runRemember(contentAlpha) { alpha(contentAlpha) }
        },
        count = pageCount(),
        state = pagerState(),
        flingBehavior = flingBehavior(),
        key = { pageKey(page = it) },
        userScrollEnabled = userScrollEnabled()
    ) { index ->
        PageItem(
            index = index,
            content = {
                PlaceImage {
                    AsyncImage(
                        model = coilImageRequest(),
                        contentDescription = "artwork",
                        contentScale = contentScale()
                    )
                }
            }
        )
    }
}

class QueuePagerState {
    val coordinator = QueuePagerCoordinator()
}

@OptIn(ExperimentalPagerApi::class)
class QueuePagerCoordinator() {

    private val layoutCoordinator = QueuePagerLayoutCoordinator()

    interface PagerLayoutScope {

        @Composable
        fun pagerState(): PagerState

        @Composable
        fun pageCount(): Int

        @Composable
        fun contentAlpha(): Float

        @Composable
        fun flingBehavior(): FlingBehavior

        @SnapshotRead
        fun pageKey(page: Int): Any

        @Composable
        fun userScrollEnabled(): Boolean

        @Composable
        fun PagerScope.PageItem(
            index: Int,
            content: @Composable PagerItemLayoutScope.() -> Unit
        )

        fun RenderPager(
            content: @Composable () -> Unit
        )
    }

    interface PagerItemLayoutScope {

        @Composable
        fun PlaceImage(image: @Composable () -> Unit)

        @Composable
        fun coilImageRequest(): CoilImageRequest

        @Composable
        fun contentScale(): ContentScale
    }

    class PagerCompositionInstance(
        private val queueData: OldPlaybackQueue,
        private val ancestor: PagerCompositionInstance? = null,
        private val layoutCoordinator: QueuePagerLayoutCoordinator
    ): PagerLayoutScope {

        private val initialPageCorrectionJob = Job()
        private val initialPageFullCorrectionJob = Job()
        private val swipeListenerInstallJob = Job()

        private var isComposed by mutableStateOf(false)
        private var readyForSwipe by mutableStateOf(false)

        private var pagerState: PagerState by mutableStateOf(
            ancestor?.pagerState
                ?: PagerState(queueData.currentIndex.coerceAtLeast(0))
        )

        @Composable
        override fun pagerState(): PagerState {
            return this.pagerState
        }

        @Composable
        override fun pageCount(): Int {
            return queueData.list.size
        }

        @Composable
        override fun contentAlpha(): Float {
            val pageCorrection by run {
                remember(this) {
                    mutableStateOf(false)
                }.apply {
                    value = ancestor?.initialPageCorrectionJob?.isCompleted == true
                    initialPageCorrectionJob.invokeOnCompletion { value = true }
                }
            }
            return if (pageCorrection) {
                1f
            } else {
                0f
            }
        }

        @OptIn(ExperimentalSnapperApi::class)
        @Composable
        override fun flingBehavior(): FlingBehavior {
            return PagerDefaults.flingBehavior(
                state = pagerState,
                snapIndex = PagerDefaults.singlePageSnapIndex
            )
        }

        override fun pageKey(page: Int): Any {
            return queueData.list[page]
        }

        @Composable
        override fun userScrollEnabled(): Boolean {
            return readyForSwipe
        }

        @Composable
        override fun PagerScope.PageItem(
            index: Int,
            content: @Composable PagerItemLayoutScope.() -> Unit
        ) {
            var imagePlaceable by remember {
                mutableStateOf<@Composable () -> Unit>({})
            }
            remember(
                this@PagerCompositionInstance,
                this@PageItem,
                index
            ) {
                object : PagerItemLayoutScope {

                    @Composable
                    override fun PlaceImage(image: @Composable () -> Unit) {
                        imagePlaceable = image
                    }

                    @Composable
                    override fun coilImageRequest(): CoilImageRequest {
                        val ctx = LocalContext.current
                        TODO("Not yet implemented")
                    }

                    @Composable
                    override fun contentScale(): ContentScale {
                        TODO("Not yet implemented")
                    }
                }
            }.run {
                content()
            }
        }

        private var pagerContent by mutableStateOf<@Composable () -> Unit>({})

        override fun RenderPager(
            content: @Composable () -> Unit
        ) {
            with(layoutCoordinator) {
                pagerContent = { PlacePagerLayout { content() } }
            }
        }
    }

    @Composable
    fun SetContent(
        pager: PagerLayoutScope.() -> Unit
    ) {
        observeForCompositionInstance()
            .run {
                pager()
            }
    }

    @Composable
    fun observeForCompositionInstance(): PagerCompositionInstance {

        val returns = remember {
            mutableStateOf(
                PagerCompositionInstance(
                    OldPlaybackQueue.UNSET,
                    null,
                    QueuePagerLayoutCoordinator()
                )
            )
        }

        DisposableEffect(
            key1 = this,
            effect = {

                onDispose {  }
            }
        )

        return returns.value
    }
}

class QueuePagerLayoutCoordinator {

    interface PagerLayoutScope {
        @Composable
        fun RenderItem(content: @Composable PagerLayoutItemScope.() -> Unit)
    }

    interface PagerLayoutItemScope {
        @Composable
        fun RenderImage(content: @Composable () -> Unit)
    }

    private class PagerContainer {

        @Composable
        fun Render(
            content: @Composable PagerLayoutScope.() -> Unit
        ) {
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                val layoutScope = remember(this) {
                    object : PagerLayoutScope {
                        @Composable
                        override fun RenderItem(content: @Composable PagerLayoutItemScope.() -> Unit) {
                            val itemScope = remember(this) {
                                object : PagerLayoutItemScope {
                                    @Composable
                                    override fun RenderImage(content: @Composable () -> Unit) {
                                        Box(
                                            modifier = Modifier
                                                .width(maxWidth * 0.8f)
                                                .height(maxHeight)
                                        ) {
                                            content()
                                        }
                                    }
                                }
                            }
                            itemScope.run { content() }
                        }
                    }
                }
                layoutScope.run { content() }
            }
        }
    }

    @Composable
    fun PlacePagerLayout(
        layout: @Composable PagerLayoutScope.() -> Unit
    ) {
        remember(this) {
            PagerContainer()
        }.run {
            Render { layout() }
        }
    }
}