package com.flammky.musicplayer.player.presentation.root

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flammky.android.medialib.common.mediaitem.AudioFileMetadata
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.providers.metadata.VirtualFileMetadata
import com.flammky.musicplayer.base.compose.SnapshotRead
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.darkSurfaceContentColorAsState
import com.flammky.musicplayer.base.theme.compose.lightSurfaceContentColorAsState
import com.flammky.musicplayer.player.presentation.root.CompactControlPagerState.Applier.Companion.PrepareCompositionInline
import com.flammky.musicplayer.player.presentation.root.CompactControlPagerState.LayoutComposition.Companion.OnComposingLayout
import com.flammky.musicplayer.player.presentation.root.CompactControlPagerState.LayoutComposition.Companion.OnLayoutComposed
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerDefaults
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import dev.chrisbanes.snapper.ExperimentalSnapperApi
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalPagerApi::class, ExperimentalSnapperApi::class)
@Composable
fun CompactControlPager(
    state: CompactControlPagerState,
) {
    state.applier
        .apply { PrepareCompositionInline() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize()
    ) {
        val layoutData = state.currentLayoutComposition
            ?: return@Box
        layoutData.OnComposingLayout()
        HorizontalPager(
            modifier = Modifier.fillMaxSize(),
            state = state.layoutState,
            count = layoutData.queueData.list.size,
            flingBehavior = PagerDefaults.flingBehavior(
                state = state.layoutState,
                snapIndex = PagerDefaults.singlePageSnapIndex
            ),
            userScrollEnabled = layoutData.userScrollReady
        ) {
            PagerItem(
                metadataFlow = state.observeMetadata(layoutData.queueData.list[it]),
                darkSurface = state.isSurfaceDark
            )
        }
        layoutData.OnLayoutComposed()
    }
}

@Composable
fun PagerItem(
    metadataFlow: Flow<MediaMetadata?>,
    darkSurface: @SnapshotRead () -> Boolean
) {
    val metadata =
        metadataFlow.collectAsState(initial = null).value
    val textColor =
        if (darkSurface()) {
            Theme.darkSurfaceContentColorAsState().value
        } else {
            Theme.lightSurfaceContentColorAsState().value
        }

    Text(
        text = metadata?.title
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
            ,
        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
        fontWeight = FontWeight.Bold,
        color = textColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )

    Spacer(modifier = Modifier.height(1.dp))

    Text(
        modifier = Modifier
            .placeholder(
                visible = metadata == null,
                color = localShimmerSurface(),
                highlight = localShimmerColor().let { color ->
                    remember(color) { PlaceholderHighlight.shimmer(color) }
                }
            ),
        text = (metadata as? AudioMetadata)
            ?.let {
                it.albumArtistName ?: it.artistName
            }
            ?.ifEmpty { "TITLE_EMPTY" }?.ifBlank { "TITLE_BLANK" }
            ?: "SUBTITLE_NONE",
        fontSize = MaterialTheme.typography.labelMedium.fontSize,
        fontWeight = FontWeight.SemiBold,
        color = textColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}