package com.flammky.musicplayer.player.presentation.root

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.flammky.android.medialib.common.mediaitem.AudioFileMetadata
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.providers.metadata.VirtualFileMetadata
import com.flammky.musicplayer.base.compose.NoInlineBox
import com.flammky.musicplayer.base.compose.SnapshotRead
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.*
import com.flammky.musicplayer.player.presentation.root.CompactControlPagerState.Applier.Companion.ComposeLayout
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerDefaults
import dev.chrisbanes.snapper.ExperimentalSnapperApi
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalPagerApi::class, ExperimentalSnapperApi::class)
@Composable
fun CompactControlPager(
    state: CompactControlPagerState,
) {
    val applier = state.applier
    NoInlineBox(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize()
    ) {
        applier.ComposeLayout {
            HorizontalPager(
                modifier = Modifier.fillMaxSize(),
                state = layoutState,
                count = queueData.list.size,
                flingBehavior = PagerDefaults.flingBehavior(
                    state = layoutState,
                    snapIndex = PagerDefaults.singlePageSnapIndex
                ),
                userScrollEnabled = userScrollEnabled
            ) {
                PagerItem(
                    metadataFlow = state.observeMetadata(queueData.list[it]),
                    darkSurface = state.isSurfaceDark
                )
            }
        }
    }
}

@Composable
private fun PagerItem(
    metadataFlow: Flow<MediaMetadata?>,
    darkSurface: @SnapshotRead () -> Boolean
) {

    val metadata =
        metadataFlow.collectAsState(initial = MediaMetadata.UNSET).value

    val textColor =
        if (darkSurface()) {
            Theme.darkSurfaceContentColorAsState().value
        } else {
            Theme.lightSurfaceContentColorAsState().value
        }

    Column(
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth(),
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
            ,
            textAlign = TextAlign.Start,
            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(1.dp))

        Text(
            modifier = Modifier
                .fillMaxWidth(),
            text = if (metadata === MediaMetadata.UNSET) {
                ""
            } else {
                (metadata as? AudioMetadata)
                    ?.let {
                        it.albumArtistName ?: it.artistName
                    }
                    ?.ifEmpty { "TITLE_EMPTY" }?.ifBlank { "TITLE_BLANK" }
                    ?: "SUBTITLE_NONE"
            },
            textAlign = TextAlign.Start,
            fontSize = MaterialTheme.typography.labelMedium.fontSize,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun localShimmerSurface(backgroundDark: Boolean): Color {
    val svar =
        if (backgroundDark) {
            Theme.darkSurfaceVariantColorAsState().value
        } else {
            Theme.lightSurfaceVariantColorAsState().value
        }
    val s =
        if (backgroundDark) {
            Theme.darkSurfaceColorAsState().value
        } else {
            Theme.lightSurfaceColorAsState().value
        }
    return remember(svar, s) {
        s.copy(alpha = 0.45f).compositeOver(svar)
    }
}

@Composable
fun localShimmerColor(backgroundDark: Boolean): Color {
    val sf = localShimmerSurface(backgroundDark)
    val content =
        if (backgroundDark) {
            Theme.darkBackgroundContentColorAsState().value
        } else {
            Theme.lightBackgroundContentColorAsState().value
        }
    return remember(sf, content) {
        content.copy(alpha = 0.6f).compositeOver(sf)
    }
}