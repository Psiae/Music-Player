package com.flammky.musicplayer.player.presentation.root

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flammky.musicplayer.player.presentation.root.RootPlaybackControlPagerState.Applier.Companion.ComposeLayout
import com.flammky.musicplayer.player.presentation.root.RootPlaybackControlPagerState.CompositionScope.Companion.contentAlpha
import com.flammky.musicplayer.player.presentation.root.RootPlaybackControlPagerState.CompositionScope.Companion.flingBehavior
import com.flammky.musicplayer.player.presentation.root.RootPlaybackControlPagerState.CompositionScope.Companion.layoutKey
import com.flammky.musicplayer.player.presentation.root.RootPlaybackControlPagerState.CompositionScope.Companion.pageCount
import com.flammky.musicplayer.player.presentation.root.RootPlaybackControlPagerState.CompositionScope.Companion.userScrollEnabled
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalPagerApi::class)
@Composable
internal fun BoxScope.RootPlaybackControlPager(
    state: RootPlaybackControlPagerState
) {
    state.applier.ComposeLayout {

        HorizontalPager(
            modifier = Modifier.alpha(contentAlpha()),
            state = pagerLayoutState,
            count = pageCount(),
            flingBehavior = flingBehavior(),
            key = { page -> layoutKey(page = page) },
            userScrollEnabled = userScrollEnabled()
        ) { page ->
           PagerItem(
               artworkFlow = remember(this, page) {
                   observeArtwork(queueData.list[page])
               }
           )
        }
    }
}

@Composable
private fun PagerItem(
    artworkFlow: Flow<Any?>,
) {
    val artwork by artworkFlow.collectAsState(initial = null)
    val context = LocalContext.current
    val req = remember(artwork) {
        ImageRequest.Builder(context)
            .data(artwork)
            .build()
    }
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        AsyncImage(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(280.dp)
                .align(Alignment.Center),
            model = req,
            contentDescription = "art",
            contentScale = ContentScale.Crop
        )
    }
}