package com.flammky.musicplayer.player.presentation.queue

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.flammky.musicplayer.base.media.playback.OldPlaybackQueue
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.backgroundContentColorAsState
import com.flammky.musicplayer.base.theme.compose.isDarkAsState
import com.flammky.musicplayer.base.theme.compose.surfaceVariantColorAsState
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.player.presentation.controller.PlaybackController
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Duration

@Composable
internal fun MainCurrentPlaybackQueueScreen(
    modifier: Modifier
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

    with(LocalDensity.current) {
        Spacer(
            modifier = Modifier.height(
                WindowInsets.statusBars.getTop(this).toDp()
            )
        )
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
        items(maskedQueue.list.size) { index ->
            val id = maskedQueue.list[index]
            val playing = index == maskedQueue.currentIndex
            Box(
                modifier = if (playing) {
                    Modifier.background(
                        Color.White.copy(alpha = 0.3f)
                    )
                } else {
                    Modifier
                }
            ) {
                QueueItem(
                    id = id,
                    viewModel = vms,
                ) play@ {
                    if (index != maskedQueue.currentIndex) {
                        controller.requestSeekAsync(index, Duration.ZERO)
                    }
                }
                if (!Theme.isDarkAsState().value) {
                    Divider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 5.dp),
                        color = Theme.surfaceVariantColorAsState().value
                    )
                }
            }
        }
        item {
           with(LocalDensity.current) {
               Spacer(
                   modifier = Modifier.height(
                       WindowInsets.navigationBars.getBottom(this).toDp()
                   )
               )
           }
        }
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
    play: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(60.dp)
            .clickable { play() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ItemArtworkCard(id, viewModel)

            Spacer(modifier = Modifier.width(8.dp))

            ItemTextDescription(
                modifier = Modifier.weight(1f, true),
                id = id,
                viewModel = viewModel
            )

            Spacer(modifier = Modifier.width(8.dp))

            val moreResId =
                if (isSystemInDarkTheme()) {
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
                )
            }
        }
    }
}

private val ART_LOADING = Any()

@Composable
private fun ItemArtworkCard(
    id: String,
    viewModel: QueueViewModel
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
                    visible = imageModel.data === ART_LOADING,
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

    LaunchedEffect(
        key1 = id,
        key2 = viewModel,
        block = {
            val obs = viewModel.observeArtwork(id)
            artState.value = ART_LOADING
            obs.collect {
                artState.value = it
            }
        }
    )
}

@Composable
private fun ItemTextDescription(
    modifier: Modifier,
    id: String,
    viewModel: QueueViewModel,
    textColor: Color = Theme.backgroundContentColorAsState().value
) {
    val metadataState = remember { mutableStateOf<AudioMetadata?>(AudioMetadata.UNSET) }

    LaunchedEffect(
        key1 = id,
        key2 = viewModel,
        block = {
            viewModel.observeMetadata(id).collect {
                metadataState.value = it as? AudioMetadata
            }
        }
    )

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

    val separator = String("\u00b7".toByteArray(Charsets.UTF_8))

    Column(
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
        Text(
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
        Text(
            text = formattedDuration + " " +
                    separator + " " +
                    (metadataState.value?.albumArtistName ?: metadataState.value?.artistName ?: ""),
            style = style2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun localShimmerSurface(): Color {
    val darkTheme = isSystemInDarkTheme()
    return remember(darkTheme) {
        if (darkTheme) Color(0xFF323232) else Color(0xFFE6E6E6)
    }
}

@Composable
fun localShimmerColor(): Color {
    val darkTheme = isSystemInDarkTheme()
    return remember(darkTheme) {
        if (darkTheme) Color(0xFF414141) else Color(0xFFEBEBEB)
    }
}