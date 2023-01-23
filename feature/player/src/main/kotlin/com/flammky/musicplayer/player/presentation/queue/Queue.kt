package com.flammky.musicplayer.player.presentation.queue

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.flammky.android.medialib.common.mediaitem.AudioFileMetadata
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.providers.metadata.VirtualFileMetadata
import com.flammky.musicplayer.base.compose.NoInlineColumn
import com.flammky.musicplayer.base.compose.NoInlineRow
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.*
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.player.presentation.controller.PlaybackController
import com.flammky.musicplayer.player.presentation.main.compose.TransitioningCompactPlaybackControl
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.time.Duration

@Composable
internal fun MainCurrentPlaybackQueueScreen(
    modifier: Modifier,
    transitionedState: State<Boolean>
) = Column(modifier = modifier.fillMaxSize()) {
    val lazyListState: LazyListState = rememberLazyListState()
    val vms: QueueViewModel = hiltViewModel()
    val userState = remember {
        mutableStateOf<User?>(null)
    }
    val controllerState = remember {
        mutableStateOf<PlaybackController?>(null)
    }
    val queueState = remember {
        mutableStateOf<OldPlaybackQueue>(OldPlaybackQueue.UNSET)
    }
    val queueOverrideState = remember {
        mutableStateOf<OldPlaybackQueue?>(null)
    }
    val maskedQueueState = remember {
        derivedStateOf { queueOverrideState.value ?: queueState.value }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        val statusBarHeight = with(LocalDensity.current) {
            WindowInsets.statusBars
                .getTop(this)
                .toDp()
        }
        val navigationBarHeight = with(LocalDensity.current) {
            WindowInsets.navigationBars
                .getBottom(this)
                .toDp()
        }
        val controlHeightState = remember {
            mutableStateOf(0.dp)
        }
        Box contentBox@ {
            val deferLoadState = remember {
                derivedStateOf { !transitionedState.value }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
            ) content@ {
                val user = userState.value
                val controller = controllerState.value
                val maskedQueue = maskedQueueState.value
                if (user == null ||
                    controller == null ||
                    user != controller.user ||
                    maskedQueue === OldPlaybackQueue.UNSET
                ) {
                    return@content
                }
                item {
                    Spacer(
                        modifier = Modifier.height(statusBarHeight)
                    )
                }
                items(maskedQueue.list.size) { index ->
                    val id = maskedQueue.list[index]
                    val playing = index == maskedQueue.currentIndex
                    Box(
                        modifier = if (playing) {
                            Modifier.background(
                                Theme.surfaceVariantColorAsState().value.copy(alpha = 0.6f)
                            )
                        } else {
                            Modifier
                        }
                    ) {
                        QueueItem(
                            id = id,
                            viewModel = vms,
                            deferLoadState = deferLoadState
                        ) play@ {
                            if (index != maskedQueue.currentIndex) {
                                controller.requestSeekAsync(index, Duration.ZERO)
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
                item {
                    Spacer(
                        modifier = Modifier.height(
                            maxOf(controlHeightState.value, navigationBarHeight)
                        )
                    )
                }
            }
            // temporary
            TransitioningCompactPlaybackControl(
                bottomVisibilityVerticalPadding = navigationBarHeight
            ) { yOffset ->
                controlHeightState.value = yOffset.unaryMinus()
            }
        }
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(statusBarHeight)
                .background(Theme.backgroundColorAsState().value)
                .align(Alignment.TopCenter)
        )
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(navigationBarHeight)
                .background(Theme.backgroundColorAsState().value)
                .align(Alignment.BottomCenter)
        )
    }

    LaunchedEffect(key1 = Unit, block = {
        vms.observeUser().collect {
            Timber.d("QueueScreen collect user=$it")
            userState.value = it
            controllerState.value = null
            queueState.value = OldPlaybackQueue.UNSET
        }
    })

    val coroutineScope = rememberCoroutineScope()
    DisposableEffect(key1 = userState.value, effect = {
        val user = userState.value
            ?: return@DisposableEffect onDispose {  }
        val supervisor = SupervisorJob()
        val controller = vms.createPlaybackController(user)
        controllerState.value = controller
            .apply {
                val collector = createPlaybackObserver(supervisor).createQueueCollector()
                coroutineScope.launch(supervisor) {
                    collector.startCollect().join()
                    collector.queueStateFlow.collect {
                        Timber.d("QueueScreen collect queue=$it")
                        queueState.value = it
                    }
                }
            }
        onDispose {
            supervisor.cancel()
            controller.dispose()
        }
    })
}

@Composable
private fun QueueItem(
    id: String,
    viewModel: QueueViewModel,
    deferLoadState: State<Boolean>,
    play: () -> Unit
) {
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

            ItemArtworkCard(id, deferLoadState, viewModel)

            Spacer(modifier = Modifier.width(8.dp))

            ItemTextDescription(
                modifier = Modifier.weight(1f, true),
                id = id,
                viewModel = viewModel,
                deferLoadState = deferLoadState
            )

            if (deferLoadState.value) {
                return@NoInlineRow
            }

            Spacer(modifier = Modifier.width(8.dp))

            val moreResId =
                if (Theme.isDarkAsState().value) {
                    com.flammky.musicplayer.base.R.drawable.more_vert_48px
                } else {
                    com.flammky.musicplayer.base.R.drawable.more_vert_48px_dark
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

private val ART_LOADING = Any()

@Composable
private fun ItemArtworkCard(
    id: String,
    deferLoadState: State<Boolean>,
    viewModel: QueueViewModel,
) {
    val context = LocalContext.current

    val artState = remember {
        mutableStateOf<Any?>(null)
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


        val imageModel = remember(artState.value) {
            ImageRequest.Builder(context)
                .data(artState.value)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build()
        }

        AsyncImage(
            modifier = Modifier
                .fillMaxSize()
                .placeholder(
                    visible = imageModel.data === ART_LOADING || deferLoadState.value,
                    color = localShimmerSurface(),
                    shape = shape,
                    highlight = PlaceholderHighlight.shimmer(highlightColor = localShimmerColor())
                )
                .clip(shape),
            model = imageModel,
            contentDescription = "Artwork",
            contentScale = ContentScale.Crop
        )

        if (deferLoadState.value) {
            return@Card
        }

        LaunchedEffect(
            key1 = id,
            key2 = viewModel,
            block = {
                withContext(Dispatchers.Main) {
                    val obs = viewModel.observeArtwork(id)
                    artState.value = ART_LOADING
                    obs.collect {
                        artState.value = it
                    }
                }
            }
        )
    }
}

@Composable
private fun ItemTextDescription(
    modifier: Modifier,
    id: String,
    viewModel: QueueViewModel,
    deferLoadState: State<Boolean>,
    textColor: Color = Theme.backgroundContentColorAsState().value
) {
    val metadataState = remember { mutableStateOf<AudioMetadata?>(AudioMetadata.UNSET) }

    NoInlineColumn(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.Center
    ) {
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
        val placeHolderState = remember {
            derivedStateOf { metadataState.value === AudioMetadata.UNSET || deferLoadState.value }
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
            text = remember(metadataState.value) {
                metadataState.value
                    ?.let { metadata ->
                        metadata.title
                            ?: (metadata as? AudioFileMetadata)?.let { afm ->
                                afm.file.fileName
                                    ?: (afm.file as? VirtualFileMetadata)?.uri?.toString()
                            }
                    }
                    ?: ""
            },
            style = style,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(3.dp))

        val formattedDuration = remember(metadataState.value?.duration) {
            val seconds = metadataState.value?.duration?.inWholeSeconds ?: 0
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
                    (metadataState.value?.albumArtistName ?: metadataState.value?.artistName ?: ""),
            style = style2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        if (deferLoadState.value) {
            return@NoInlineColumn
        }

        LaunchedEffect(
            key1 = id,
            key2 = viewModel,
            block = {
                withContext(coroutineContext + Dispatchers.Main) {
                    viewModel.observeMetadata(id).collect {
                        metadataState.value = it as? AudioMetadata
                    }
                }
            }
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