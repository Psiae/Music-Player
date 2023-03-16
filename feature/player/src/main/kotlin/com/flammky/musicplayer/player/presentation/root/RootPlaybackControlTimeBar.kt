package com.flammky.musicplayer.player.presentation.root

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.unit.Dp
import kotlin.time.Duration

class RootPlaybackControlTimeBarState() {



    class Applier(
        private val state: RootPlaybackControlTimeBarState
    ) {

        companion object {

            @Composable
            fun ComposeLayout(
                content: @Composable CompositionScope.() -> Unit
            ) {

            }
        }
    }

    class ScrubbingInstance(
        val startPosition: Float,
        val startDuration: Duration,
    ) {

        init {
            positionSanityCheck(startPosition, startDuration)
        }

        private var locked = false

        var latestPosition by mutableStateOf(startPosition)
            private set

        // reconsider this
        var latestDuration by mutableStateOf(startDuration)
            private set

        val finished
            get() = locked

        fun newPosition(
            position: Float,
            duration: Duration
        ) {
            mutationSanityCheck()
            positionSanityCheck(position, duration)
            latestPosition = position
            latestDuration = duration
        }

        private fun mutationSanityCheck() {
            check(!locked) {
                "sanity check: scrub instance is already locked"
            }
        }

        private fun positionSanityCheck(
            position: Float,
            duration: Duration
        ) {
            check(position >= 0f) {
                "sanity check: should not be able to scrub to negative position"
            }
            check(duration >= Duration.ZERO || duration == Duration.INFINITE) {
                "sanity check: Scrub duration must be positive, " +
                        "negative duration must not be scrub-able, " +
                        "for unknown duration but is valid in any case use Infinite instead"
            }
        }

        fun finish() {
            check(!locked)
            locked = true
        }
    }

    class CompositionScope(

    ) {

        val scrubResultStack = mutableStateListOf<ScrubbingInstance>()
        var currentScrubbingInstance by mutableStateOf<ScrubbingInstance?>(null)

        var latestRemotePosition by mutableStateOf(Duration.ZERO)
        var latestRemoteDuration by mutableStateOf(Duration.ZERO)

        var latestComposedProgress: Float? = null
        var latestComposedDuration: Duration? = null

        fun onUserScrubToPosition(
            newPosition: Float,
        ) {
            // introduce Scrub slot ?
            if (currentScrubbingInstance == null) {
                currentScrubbingInstance = ScrubbingInstance(
                    newPosition,
                    latestComposedDuration
                        ?: error(
                            "sanity check: can only scrub on composed duration"
                        )
                )
                return
            }
            currentScrubbingInstance!!.newPosition(
                newPosition,
                latestComposedDuration!!
            )
        }

        fun onScrubFinished(
            // expect ..
        ) {
            val scrubInstance = currentScrubbingInstance
                ?: error(
                    "sanity check: no scrub instance to finish"
                )
            scrubInstance.finish()
        }

        companion object {

            @Composable
            fun CompositionScope.progressDisplayValue(
                layoutWidth: Dp
            ): Float {
                (currentScrubbingInstance ?: scrubResultStack.lastOrNull())?.let { instance ->
                    if (instance.latestDuration.isInfinite()) {
                        return /* IDK what */ instance.latestPosition
                    }
                    return instance.latestPosition
                }
                val remotePos = latestRemotePosition
                val remoteDur = latestRemoteDuration
                val target = if (remotePos == Duration.ZERO || remoteDur == Duration.ZERO) {
                    0f
                } else {
                    (remotePos.inWholeMilliseconds / remoteDur.inWholeMilliseconds).toFloat()
                }
                return animateFloatAsState(
                    targetValue = target,
                    animationSpec = remember { tween(200) }
                ).value
            }
        }
    }
}