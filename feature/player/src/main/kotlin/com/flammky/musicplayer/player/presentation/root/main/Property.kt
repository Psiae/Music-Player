package com.flammky.musicplayer.player.presentation.root.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.flammky.musicplayer.base.compose.SnapshotRead
import com.flammky.musicplayer.base.compose.SnapshotReader
import com.flammky.musicplayer.base.media.playback.PlaybackProperties
import com.flammky.musicplayer.base.media.playback.RepeatMode
import com.flammky.musicplayer.base.media.playback.ShuffleMode
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.backgroundContentColorAsState
import com.flammky.musicplayer.player.R
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Composable
fun rememberPlaybackPropertyControlState(
    key: Any,
    propertiesFlow: () -> Flow<PlaybackProperties>,
    play: () -> Unit,
    pause: () -> Unit,
    toggleShuffleMode: () -> Unit,
    toggleRepeatMode: () -> Unit,
    seekNext: () -> Unit,
    seekPrevious: () -> Unit
): PlaybackPropertyControlState {
    return remember(key) {
        PlaybackPropertyControlState(
            propertiesFlow,
            play,
            pause,
            toggleShuffleMode,
            toggleRepeatMode,
            seekNext,
            seekPrevious
        )
    }
}

@Composable
fun PlaybackPropertyControl(
    state: PlaybackPropertyControlState
) = state.coordinator.ComposeContent(
    buttons = @SnapshotRead {
        provideShuffleButtonRenderer { modifier ->
            Box(modifier = modifier.buttonModifier()) {
                Icon(
                    modifier = Modifier
                        .iconModifier()
                        .align(Alignment.Center),
                    painter = painterResource(
                        id = R.drawable.ios_glyph_shuffle_100
                    ),
                    contentDescription = "shuffle",
                    tint = iconTint()
                )
            }
        }
        provideSeekPreviousButtonRenderer { modifier ->
            Box(modifier = modifier.containerModifier()) {
                Icon(
                    modifier = Modifier
                        .iconModifier()
                        .align(Alignment.Center),
                    painter = painterResource(
                        id = R.drawable.ios_glyph_seek_previous_100
                    ),
                    contentDescription = "prev",
                    tint = iconTint()
                )
            }
        }
        providePlayWhenReadyButtonRenderer { modifier ->
            Box(modifier = modifier.containerModifier()) {
                Icon(
                    modifier = Modifier
                        .iconModifier()
                        .align(Alignment.Center),
                    painter = painterResource(
                        id = if (drawPauseButton) {
                            R.drawable.ios_glyph_pause_100
                        } else {
                            R.drawable.ios_glyph_play_100
                        }
                    ),
                    contentDescription = if (drawPauseButton) "pause" else "play",
                    tint = iconTint()
                )
            }
        }
        provideSeekNextButtonRenderer { modifier ->
            Box(modifier = modifier.containerModifier()) {
                Icon(
                    modifier = Modifier
                        .iconModifier()
                        .align(Alignment.Center),
                    painter = painterResource(
                        id = R.drawable.ios_glyph_seek_next_100
                    ),
                    contentDescription = "next",
                    tint = iconTint()
                )
            }
        }
        provideRepeatButtonRenderer { modifier ->
            Box(modifier = modifier.containerModifier()) {
                Icon(
                    modifier = Modifier
                        .iconModifier()
                        .align(Alignment.Center),
                    painter = painterResource(
                        if (drawRepeatOne) R.drawable.ios_glyph_repeat_one_100
                        else R.drawable.ios_glyph_repeat_100
                    ),
                    contentDescription = "repeat",
                    tint = iconTint()
                )
            }
        }
    }
)

class PlaybackPropertyControlState(
   propertiesFlow: () -> Flow<PlaybackProperties>,
   play: () -> Unit,
   pause: () -> Unit,
   toggleShuffleMode: () -> Unit,
   toggleRepeatMode: () -> Unit,
   seekNext: () -> Unit,
   seekPrevious: () -> Unit
) {
    val coordinator = PlaybackPropertyControlCoordinator(
        this,
        propertiesFlow,
        play,
        pause,
        toggleShuffleMode,
        toggleRepeatMode,
        seekNext,
        seekPrevious
    )
}

class PlaybackPropertyControlCoordinator(
    private val state: PlaybackPropertyControlState,
    private val propertiesFlow: () -> Flow<PlaybackProperties>,
    private val play: () -> Unit,
    private val pause: () -> Unit,
    private val toggleShuffleMode: () -> Unit,
    private val toggleRepeatMode: () -> Unit,
    private val seekNext: () -> Unit,
    private val seekPrevious: () -> Unit
) {

    private val layoutCoordinator = PlaybackPropertyControlLayoutCoordinator()

    interface ShuffleButtonRenderScope {

        fun Modifier.buttonModifier(): Modifier

        fun Modifier.iconModifier(): Modifier

        @Composable
        fun iconTint(): Color
    }

    interface SeekPreviousRenderScope {

        fun Modifier.containerModifier(): Modifier

        fun Modifier.iconModifier(): Modifier

        @Composable
        fun iconTint(): Color
    }

    interface PlayWhenReadyRenderScope {

        val drawPauseButton: Boolean

        fun Modifier.containerModifier(): Modifier

        fun Modifier.iconModifier(): Modifier

        @Composable
        fun iconTint(): Color
    }

    interface SeekNextRenderScope {

        fun Modifier.containerModifier(): Modifier

        fun Modifier.iconModifier(): Modifier

        @Composable
        fun iconTint(): Color
    }

    interface RepeatButtonRenderScope {

        val drawRepeatOff: Boolean

        val drawRepeatOne: Boolean

        val drawRepeatAll: Boolean

        fun Modifier.containerModifier(): Modifier

        fun Modifier.iconModifier(): Modifier

        @Composable
        fun iconTint(): Color
    }

    interface PropertyControlScope {

        fun provideShuffleButtonRenderer(
            content: @Composable ShuffleButtonRenderScope.(Modifier) -> Unit
        )
        fun provideSeekPreviousButtonRenderer(
            content: @Composable SeekPreviousRenderScope.(Modifier) -> Unit
        )
        fun providePlayWhenReadyButtonRenderer(
            content: @Composable PlayWhenReadyRenderScope.(Modifier) -> Unit
        )
        fun provideSeekNextButtonRenderer(
            content: @Composable SeekNextRenderScope.(Modifier) -> Unit
        )
        fun provideRepeatButtonRenderer(
            content: @Composable RepeatButtonRenderScope.(Modifier) -> Unit
        )
    }

    private class PropertyControlScopeImpl(
        private val propertiesFlow: () -> Flow<PlaybackProperties>,
        private val play: () -> Unit,
        private val pause: () -> Unit,
        private val toggleShuffleMode: () -> Unit,
        private val toggleRepeatMode: () -> Unit,
        private val seekNext: () -> Unit,
        private val seekPrevious: () -> Unit
    ) : PropertyControlScope {

        var shuffleButton by mutableStateOf<@Composable () -> Unit>({})
            private set

        var previousButton by mutableStateOf<@Composable () -> Unit>({})
            private set

        var playWhenReadyButton by mutableStateOf<@Composable () -> Unit>({})
            private set

        var nextButton by mutableStateOf<@Composable () -> Unit>({})
            private set

        var repeatButton by mutableStateOf<@Composable () -> Unit>({})
            private set

        var shuffleButtonModifierFactory by mutableStateOf<() -> Modifier>({ Modifier })
            private set

        var previousButtonModifierFactory by mutableStateOf<() -> Modifier>({ Modifier })
            private set

        var playWhenReadyButtonModifierFactory by mutableStateOf<() -> Modifier>({ Modifier })
            private set

        var nextButtonModifierFactory by mutableStateOf<() -> Modifier>({ Modifier })
            private set

        var repeatButtonModifierFactory by mutableStateOf<() -> Modifier>({ Modifier })
            private set

        fun attachModifierFactories(
            shuffleButtonModifierFactory: () -> Modifier,
            previousButtonModifierFactory: () -> Modifier,
            playWhenReadyButtonModifierFactory: () -> Modifier,
            nextButtonModifierFactory: () -> Modifier,
            repeatButtonModifierFactory: () -> Modifier
        ) {
            this.shuffleButtonModifierFactory = shuffleButtonModifierFactory
            this.previousButtonModifierFactory = previousButtonModifierFactory
            this.playWhenReadyButtonModifierFactory = playWhenReadyButtonModifierFactory
            this.nextButtonModifierFactory = nextButtonModifierFactory
            this.repeatButtonModifierFactory = repeatButtonModifierFactory
        }

        override fun provideShuffleButtonRenderer(
            content: @Composable ShuffleButtonRenderScope.(Modifier) -> Unit
        ) {
            shuffleButton = @Composable {
                observeShuffleButtonRenderScope()
                    .content(shuffleButtonModifierFactory())
            }
        }

        override fun provideSeekPreviousButtonRenderer(
            content: @Composable SeekPreviousRenderScope.(Modifier) -> Unit
        ) {
            previousButton = @Composable {
                observePreviousButtonRenderScope()
                    .content(previousButtonModifierFactory())
            }
        }

        override fun providePlayWhenReadyButtonRenderer(
            content: @Composable PlayWhenReadyRenderScope.(Modifier) -> Unit
        ) {
            playWhenReadyButton = @Composable {
                observePlayWhenReadyButtonRenderScope()
                    .content(playWhenReadyButtonModifierFactory())
            }
        }

        override fun provideSeekNextButtonRenderer(
            content: @Composable SeekNextRenderScope.(Modifier) -> Unit
        ) {
            nextButton = @Composable {
                observeNextButtonRenderScope()
                    .content(nextButtonModifierFactory())
            }
        }

        override fun provideRepeatButtonRenderer(
            content: @Composable RepeatButtonRenderScope.(Modifier) -> Unit
        ) {
            repeatButton = @Composable {
                observeRepeatButtonRenderScope()
                    .content(repeatButtonModifierFactory())
            }
        }

        @Composable
        private fun observeShuffleButtonRenderScope(): ShuffleButtonRenderScope {

            val interactionSource = remember {
                MutableInteractionSource()
            }

            val shuffleModeState = remember(this) {
                mutableStateOf(false)
            }

            val allowToggleShuffleModeState = remember(this) {
                mutableStateOf(false)
            }

            val coroutineScope = rememberCoroutineScope()

            DisposableEffect(
                key1 = this,
                effect = {
                    val supervisor = SupervisorJob()

                    coroutineScope.launch(supervisor) {
                        propertiesFlow()
                            .collect {
                                shuffleModeState.value = it.shuffleMode == ShuffleMode.ON
                                allowToggleShuffleModeState.value = if (it.shuffleMode == ShuffleMode.ON) {
                                    it.canShuffleOff
                                } else {
                                    it.canShuffleOn
                                }
                            }
                    }

                    onDispose { supervisor.cancel() }
                }
            )

            return object : ShuffleButtonRenderScope {

                val canToggle = allowToggleShuffleModeState.value
                val on = shuffleModeState.value

                override fun Modifier.buttonModifier(): Modifier {
                    return composed {
                        size(40.dp)
                            .clickable(
                                enabled = canToggle,
                                interactionSource = interactionSource,
                                indication = null,
                                onClickLabel = "shuffle",
                                role = Role.Button,
                                onClick = toggleShuffleMode
                            )
                    }
                }

                override fun Modifier.iconModifier(): Modifier {
                    return composed {
                        size(
                            if (interactionSource.collectIsPressedAsState().value) 27.dp else 30.dp
                        )
                    }
                }

                @Composable
                override fun iconTint(): Color = if (on && canToggle) {
                    Theme.backgroundContentColorAsState().value
                } else {
                    Color(0xFF787878)
                }
            }
        }

        @Composable
        private fun observePreviousButtonRenderScope(): SeekPreviousRenderScope {

            val interactionSource = remember {
                MutableInteractionSource()
            }

            val canSeekPreviousState = remember {
                mutableStateOf(false)
            }

            val coroutineScope = rememberCoroutineScope()

            DisposableEffect(
                key1 = this,
                effect = {
                    val supervisor = SupervisorJob()

                    coroutineScope.launch(supervisor) {
                        propertiesFlow()
                            .collect {
                                canSeekPreviousState.value = it.canSeekPrevious
                            }
                    }

                    onDispose { supervisor.cancel() }
                }
            )

            return object : SeekPreviousRenderScope {

                val canSeekPrevious = canSeekPreviousState.value

                override fun Modifier.containerModifier(): Modifier {
                    return composed {
                        size(40.dp)
                            .clickable(
                                enabled = canSeekPrevious,
                                interactionSource = interactionSource,
                                indication = null,
                                onClickLabel = if (canSeekPrevious) "prev" else "no_prev",
                                role = Role.Button,
                                onClick = seekPrevious
                            )
                    }
                }

                override fun Modifier.iconModifier(): Modifier {
                    return composed {
                        size(
                            if (interactionSource.collectIsPressedAsState().value) 32.dp else 35.dp
                        )
                    }
                }

                @Composable
                override fun iconTint(): Color = if (canSeekPrevious) {
                    Theme.backgroundContentColorAsState().value
                } else {
                    Color(0xFF787878)
                }
            }
        }

        @Composable
        private fun observePlayWhenReadyButtonRenderScope(): PlayWhenReadyRenderScope {

            val interactionSource = remember {
                MutableInteractionSource()
            }

            val playWhenReadyState = remember {
                mutableStateOf(false)
            }

            val canPlayState = remember {
                mutableStateOf(false)
            }

            val canPauseState = remember {
                mutableStateOf(false)
            }

            val coroutineScope = rememberCoroutineScope()

            DisposableEffect(
                key1 = this,
                effect = {
                    val supervisor = SupervisorJob()

                    coroutineScope.launch(supervisor) {
                        propertiesFlow()
                            .collect {
                                playWhenReadyState.value = it.playWhenReady
                                canPlayState.value = it.canPlay
                                canPauseState.value = true
                            }
                    }

                    onDispose { supervisor.cancel() }
                }
            )

            return object : PlayWhenReadyRenderScope {

                val canPlay = canPlayState.value
                val canPause = canPauseState.value

                override val drawPauseButton: Boolean = playWhenReadyState.value

                override fun Modifier.containerModifier(): Modifier {
                    return composed {
                        size(40.dp)
                            .clickable(
                                enabled = if (drawPauseButton) canPause else canPlay,
                                interactionSource = interactionSource,
                                indication = null,
                                onClickLabel = if (drawPauseButton) "pause" else "play",
                                role = Role.Button,
                                onClick = if (drawPauseButton) pause else play
                            )
                    }
                }

                override fun Modifier.iconModifier(): Modifier {
                    return composed {
                        size(
                            if (interactionSource.collectIsPressedAsState().value) 37.dp else 40.dp
                        )
                    }
                }

                @Composable
                override fun iconTint(): Color = if (drawPauseButton) {
                    if (canPause) Theme.backgroundContentColorAsState().value else Color(0xFF787878)
                } else {
                    if (canPlay) Theme.backgroundContentColorAsState().value else Color(0xFF787878)
                }
            }
        }

        @Composable
        private fun observeNextButtonRenderScope(): SeekNextRenderScope {

            val interactionSource = remember {
                MutableInteractionSource()
            }

            val canSeekNextState = remember {
                mutableStateOf(false)
            }

            val coroutineScope = rememberCoroutineScope()

            DisposableEffect(
                key1 = this,
                effect = {
                    val supervisor = SupervisorJob()

                    coroutineScope.launch(supervisor) {
                        propertiesFlow()
                            .collect {
                                canSeekNextState.value = it.canSeekNext
                            }
                    }

                    onDispose { supervisor.cancel() }
                }
            )


            return object : SeekNextRenderScope {

                val canSeekNext = canSeekNextState.value

                override fun Modifier.containerModifier(): Modifier {
                    return composed {
                        size(40.dp)
                            .clickable(
                                enabled = canSeekNext,
                                interactionSource = interactionSource,
                                indication = null,
                                onClickLabel = "seek_next",
                                role = Role.Button,
                                onClick = seekNext
                            )
                    }
                }

                override fun Modifier.iconModifier(): Modifier {
                    return composed {
                        size(if (interactionSource.collectIsPressedAsState().value) 32.dp else 35.dp)
                    }
                }

                @Composable
                override fun iconTint(): Color = if (canSeekNext) {
                    Theme.backgroundContentColorAsState().value
                } else {
                    Color(0xFF787878)
                }
            }
        }

        @Composable
        private fun observeRepeatButtonRenderScope(): RepeatButtonRenderScope {

            val interactionSource = remember(this) {
                MutableInteractionSource()
            }

            val repeatModeState = remember(this) {
                mutableStateOf<RepeatMode>(RepeatMode.OFF)
            }

            val canToggleRepeatModeState = remember(this) {
                mutableStateOf(false)
            }

            val coroutineScope = rememberCoroutineScope()

            DisposableEffect(
                key1 = this,
                effect = {
                    val supervisor = SupervisorJob()

                    coroutineScope.launch(supervisor) {
                        propertiesFlow()
                            .collect {
                                repeatModeState.value = it.repeatMode
                                canToggleRepeatModeState.value = when (it.repeatMode) {
                                    RepeatMode.ALL -> it.canRepeatOff
                                    RepeatMode.OFF -> it.canRepeatOne
                                    RepeatMode.ONE -> it.canRepeatAll
                                }
                            }
                    }

                    onDispose { supervisor.cancel() }
                }
            )

            return object : RepeatButtonRenderScope {

                val repeatMode = repeatModeState.value
                val canToggleNext = canToggleRepeatModeState.value

                override val drawRepeatOff: Boolean = repeatMode == RepeatMode.OFF
                override val drawRepeatOne: Boolean = repeatMode == RepeatMode.ONE
                override val drawRepeatAll: Boolean = repeatMode == RepeatMode.ALL

                override fun Modifier.containerModifier(): Modifier {
                    return composed {
                        size(40.dp)
                            .clickable(
                                enabled = canToggleNext,
                                interactionSource = interactionSource,
                                indication = null,
                                onClickLabel = "toggle_repeat",
                                role = Role.Button,
                                onClick = toggleRepeatMode
                            )
                    }
                }

                override fun Modifier.iconModifier(): Modifier {
                    return composed {
                        size(
                            if (interactionSource.collectIsPressedAsState().value) 27.dp else 30.dp
                        )
                    }
                }

                @Composable
                override fun iconTint(): Color = if (canToggleNext && !drawRepeatOff) {
                    Theme.backgroundContentColorAsState().value
                } else {
                    Color(0xFF787878)
                }
            }
        }
    }

    @Composable
    fun ComposeContent(
        buttons: @SnapshotReader PropertyControlScope.() -> Unit
    ) {
        val upButtons = rememberUpdatedState(buttons)
        val scopeState = remember(this) {
            val impl = PropertyControlScopeImpl(
                propertiesFlow,
                play,
                pause,
                toggleShuffleMode,
                toggleRepeatMode,
                seekNext,
                seekPrevious
            )
            derivedStateOf { impl.apply(upButtons.value) }
        }
        with(layoutCoordinator) {
            PlaceButtons(
                buttons = @SnapshotRead {
                    val scope = scopeState.value
                    provideButtonPlaceable(
                        shuffleButton = scope.shuffleButton,
                        previousButton = scope.previousButton,
                        playWhenReadyButton = scope.playWhenReadyButton,
                        nextButton = scope.nextButton,
                        repeatButton = scope.repeatButton
                    )
                }
            )
        }
    }

    @Composable
    private fun rememberPropertyControlScope(): PropertyControlScopeImpl {
        return remember(this) {
            PropertyControlScopeImpl(
                propertiesFlow = propertiesFlow,
                play, pause, toggleShuffleMode, toggleRepeatMode, seekNext, seekPrevious
            )
        }
    }
}

class PlaybackPropertyControlLayoutCoordinator() {

    interface ButtonsLayoutScope {
        fun provideButtonPlaceable(
            shuffleButton: @Composable () -> Unit,
            previousButton: @Composable () -> Unit,
            playWhenReadyButton: @Composable () -> Unit,
            nextButton: @Composable () -> Unit,
            repeatButton: @Composable () -> Unit
        )
    }

    private class ButtonsLayoutData(
        val shuffleButton: @Composable () -> Unit,
        val previousButton: @Composable () -> Unit,
        val playWhenReadyButton: @Composable () -> Unit,
        val nextButton: @Composable () -> Unit,
        val repeatButton: @Composable () -> Unit
    )

    private class ButtonsLayoutScopeImpl : ButtonsLayoutScope {

        var layoutData by mutableStateOf<ButtonsLayoutData?>(null)
            private set

        override fun provideButtonPlaceable(
            shuffleButton: @Composable () -> Unit,
            previousButton: @Composable () -> Unit,
            playWhenReadyButton: @Composable () -> Unit,
            nextButton: @Composable () -> Unit,
            repeatButton: @Composable () -> Unit
        ) {
            this.layoutData = ButtonsLayoutData(
                shuffleButton = shuffleButton,
                previousButton = previousButton,
                playWhenReadyButton = playWhenReadyButton,
                nextButton = nextButton,
                repeatButton = repeatButton
            )
        }
    }

    @Composable
    fun PlaceButtons(
        buttons: @SnapshotReader ButtonsLayoutScope.() -> Unit
    ) = BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val scope = remember(this@PlaybackPropertyControlLayoutCoordinator) {
            val impl = ButtonsLayoutScopeImpl()
            derivedStateOf { impl.apply(buttons) }
        }.value
        Row(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            scope.layoutData?.let {
                with(it) {
                    shuffleButton()
                    previousButton()
                    playWhenReadyButton()
                    nextButton()
                    repeatButton()
                }
            }
        }
    }
}