package dev.dexsr.klio.player.android.presentation.root.main.pager

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.fastAll
import kotlinx.coroutines.coroutineScope

/**
 * A modifier to detect up and down events in a Pager.
 */
@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.dragDirectionDetector(state: PlaybackPagerController) =
    this then Modifier.pointerInput(state) {
        coroutineScope {
            awaitEachGesture {
                val downEvent =
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                var upEventOrCancellation: PointerInputChange? = null
                while (upEventOrCancellation == null) {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    if (event.changes.fastAll { it.changedToUp() }) {
                        // All pointers are up
                        upEventOrCancellation = event.changes[0]
                    }
                }

                state.upDownDifference = upEventOrCancellation.position - downEvent.position
            }
        }
    }