package dev.dexsr.klio.player.android.presentation.root.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.flammky.common.kotlin.collection.mutable.forEachClear
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.surfaceContentColorAsState
import dev.dexsr.klio.base.theme.md3.MD3Theme
import dev.dexsr.klio.base.theme.md3.compose.dpPaddingIncrementsOf
import kotlinx.coroutines.*

@Composable
fun PlaybackControlMainScreenDescription(
    modifier: Modifier,
    state: PlaybackControlMainScreenState
) {
    val titleTextState = remember(state) {
        mutableStateOf("")
    }
    val subtitleTextState = remember(state) {
        mutableStateOf("")
    }
    val noTitleState = remember(state) {
        mutableStateOf(false)
    }
    val noSubtitleState = remember(state) {
        mutableStateOf(false)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {

        Text(
            modifier = Modifier
                // TODO: define spec
                .alpha(alpha = if (noTitleState.value) 0.38f else 1f),
            text = titleTextState.value,
            color = Theme.surfaceContentColorAsState().value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(MD3Theme.dpPaddingIncrementsOf(1)))
        Text(
            modifier = Modifier
                // TODO: define spec
                .alpha(alpha = if (noSubtitleState.value) 0.38f else 1f),
            text = subtitleTextState.value,
            color = Theme.surfaceContentColorAsState().value,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
    DisposableEffect(
        state,
        effect = {
            val coroutineScope = CoroutineScope(SupervisorJob())
            var k: Job? = null
            val disposableHandles = mutableListOf<DisposableHandle>()
            state.playbackController.invokeOnTimelineChanged(1) { timeline, step ->
                k?.cancel()
                k = coroutineScope.launch {
                    titleTextState.value = ""
                    subtitleTextState.value = ""
                    val mediaID = timeline.items.getOrNull(timeline.currentIndex) ?: return@launch
                    state.mediaMetadataProvider.descriptionAsFlow(mediaID)
                        .collect { description ->
                            titleTextState.value = description?.title
                                ?.also { noTitleState.value = false }
                                ?: "NO TITLE"
                                    .also { noTitleState.value = true }
                            subtitleTextState.value = description?.subtitle
                                ?.also { noSubtitleState.value = false }
                                ?: "NO SUBTITLE"
                                    .also { noSubtitleState.value = true }
                        }
                }
            }.also { disposableHandles.add(it) }

            onDispose {
                coroutineScope.cancel()
                disposableHandles.forEachClear { it.dispose() }
            }
        }
    )
}