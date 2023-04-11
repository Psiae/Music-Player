package com.flammky.musicplayer.player.presentation.root.main

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flammky.android.medialib.common.mediaitem.AudioFileMetadata
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.providers.metadata.VirtualFileMetadata
import com.flammky.musicplayer.base.compose.SnapshotRead
import com.flammky.musicplayer.base.compose.SnapshotReader
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.surfaceContentColorAsState
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Composable
fun rememberPlaybackTrackDescriptionState(
    key: Any,
    observeCurrentMetadata: () -> Flow<MediaMetadata?>
): PlaybackDescriptionState {
    return remember(key) {
        PlaybackDescriptionState(observeCurrentMetadata)
    }
}

@Composable
fun PlaybackTrackDescription(
    state: PlaybackDescriptionState
) = state.coordinator.ComposeLayout(
    text = @SnapshotRead {
        provideTitleRenderer {
            // TODO: coordinator should provide theme, and possibly overflow
            Text(
                text = text,
                color = Theme.surfaceContentColorAsState().value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        provideSubtitleRenderer {
            // TODO: coordinator should provide theme, and possibly overflow
            Text(
                text = text,
                color = Theme.surfaceContentColorAsState().value,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
)

class PlaybackDescriptionState(
    observeCurrentMetadata: () -> Flow<MediaMetadata?>
) {

    val coordinator = PlaybackDescriptionCoordinator(
        observeCurrentMetadata
    )
}

class PlaybackDescriptionCoordinator(
    private val observeCurrentMetadata: () -> Flow<MediaMetadata?>
) {

    val layoutCoordinator = PlaybackDescriptionLayoutCoordinator()

    interface TextDescriptionScope {

        fun provideTitleRenderer(
            factory: @Composable TitleRenderScope.() -> Unit
        )

        fun provideSubtitleRenderer(
            factory: @Composable SubtitleRenderScope.() -> Unit
        )
    }

    interface TitleRenderScope {
        val text: String
    }

    interface SubtitleRenderScope {
        val text: String
    }

    private class TitleRenderScopeImpl(
        override val text: String
    ) : TitleRenderScope {

    }

    private class SubtitleRenderScopeImpl(
        override val text: String
    ) : SubtitleRenderScope {

    }

    private class TextDescriptionScopeImpl(
        private val observeCurrentMetadata: () -> Flow<MediaMetadata?>
    ) : TextDescriptionScope {

        private var _metadata by mutableStateOf<MediaMetadata?>(MediaMetadata.UNSET)
        private var metadataReaderCount by mutableStateOf(0)
        private var metadataKey: Any? = null

        var title by mutableStateOf<@Composable () -> Unit>({})
            private set

        var subtitle by mutableStateOf<@Composable () -> Unit>({})
            private set


        override fun provideTitleRenderer(factory: @Composable TitleRenderScope.() -> Unit) {
            this.title = @Composable {
                observeTitleRenderScope().factory()
            }
        }

        override fun provideSubtitleRenderer(factory: @Composable SubtitleRenderScope.() -> Unit) {
            this.subtitle = @Composable {
                observeSubtitleRenderScope().factory()
            }
        }

        @Composable
        fun attachCompositionTree(): TextDescriptionScopeImpl {
            val uiCoroutineScope = rememberCoroutineScope()

            DisposableEffect(
                key1 = this,
                effect = {
                    val supervisor = SupervisorJob()
                    metadataKey = supervisor

                    uiCoroutineScope.launch(supervisor) {
                        var currentCollector: Job? = null
                        snapshotFlow { metadataReaderCount }
                            .collect { count ->
                                if (count == 0) {
                                    currentCollector?.cancel()
                                    return@collect
                                }
                                currentCollector = launch {
                                    observeCurrentMetadata()
                                        .collect { metadata ->
                                            if (metadataKey == supervisor) _metadata = metadata
                                        }
                                }
                            }
                    }


                    onDispose {
                        supervisor.cancel()
                        if (metadataKey == supervisor) _metadata = null
                    }
                }
            )

            return this
        }

        @Composable
        private fun observeTitleRenderScope(): TitleRenderScopeImpl {
            DisposableEffect(
                key1 = this,
                effect = {
                    metadataReaderCount++
                    onDispose { metadataReaderCount-- }
                }
            )
            val metadata = _metadata
            return TitleRenderScopeImpl(
                text = if (metadata === MediaMetadata.UNSET) {
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
            )
        }

        @Composable
        private fun observeSubtitleRenderScope(): SubtitleRenderScopeImpl {
            DisposableEffect(
                key1 = this,
                effect = {
                    metadataReaderCount++
                    onDispose { metadataReaderCount-- }
                }
            )
            val metadata = _metadata
            return SubtitleRenderScopeImpl(
                text = if (metadata === MediaMetadata.UNSET) {
                    ""
                } else {
                    (metadata as? AudioMetadata)
                        ?.let {
                            it.albumArtistName ?: it.artistName
                        }
                        ?.ifEmpty { "TITLE_EMPTY" }?.ifBlank { "TITLE_BLANK" }
                        ?: "SUBTITLE_NONE"
                }
            )
        }

    }

    @Composable
    fun ComposeLayout(
        text: @SnapshotReader TextDescriptionScope.() -> Unit
    ) {
        val upText = rememberUpdatedState(text)
        val renderer = remember(this) {
            val impl = TextDescriptionScopeImpl(observeCurrentMetadata)
            derivedStateOf { impl.apply(upText.value) }
        }.value.attachCompositionTree()
        with(layoutCoordinator) {
            PlaceLayout(
                layout = {
                    provideLayoutData(
                        title = renderer.title,
                        subtitle = renderer.subtitle
                    )
                }
            )
        }
    }
}

class PlaybackDescriptionLayoutCoordinator() {

    interface TextDescriptionLayoutScope {

        fun provideLayoutData(
            title: @Composable () -> Unit,
            subtitle: @Composable () -> Unit
        )
    }

    private class TextDescriptionLayoutScopeImpl() : TextDescriptionLayoutScope {

        var title by mutableStateOf<@Composable () -> Unit>({})
            private set

        var subtitle by mutableStateOf<@Composable () -> Unit>({})
            private set

        override fun provideLayoutData(
            title: @Composable () -> Unit,
            subtitle: @Composable () -> Unit
        ) {
            this.title = title
            this.subtitle = subtitle
        }
    }

    @Composable
    fun PlaceLayout(
        layout: TextDescriptionLayoutScope.() -> Unit
    ) {
        val upLayout = rememberUpdatedState(newValue = layout)
        val scope = remember(this) {
            val impl = TextDescriptionLayoutScopeImpl()
            derivedStateOf { impl.apply(upLayout.value) }
        }.value
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = remember {
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.8f)
                        .align(Alignment.Center)
                },
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                scope.title()
                scope.subtitle()
            }
        }
    }
}