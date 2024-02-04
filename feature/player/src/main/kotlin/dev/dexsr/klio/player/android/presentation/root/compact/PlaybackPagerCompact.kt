package dev.dexsr.klio.player.android.presentation.root.compact

import androidx.collection.LruCache
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.dexsr.klio.base.composeui.clickable
import dev.dexsr.klio.base.kt.cast
import dev.dexsr.klio.base.theme.md3.compose.MaterialTheme3
import dev.dexsr.klio.player.android.presentation.root.main.RootCompactMediaMetadataProvider
import dev.dexsr.klio.player.android.presentation.root.main.pager.LazyPlaybackPagerState
import dev.dexsr.klio.player.android.presentation.root.main.pager.PlaybackPager
import dev.dexsr.klio.player.android.presentation.root.main.pager.PlaybackPagerEagerRangePlaceholder
import dev.dexsr.klio.player.android.presentation.root.main.pager.PlaybackPagerItemScope
import dev.dexsr.klio.player.shared.PlaybackMediaDescription

@Composable
fun PlaybackPagerCompact(
    modifier: Modifier,
    state: LazyPlaybackPagerState,
    mediaMetadataProvider: RootCompactMediaMetadataProvider,
    lightContent: Boolean,
    onItemClicked: (() -> Unit)?
) {
    val cache = remember {
        LruCache<String, Any>(1 + PlaybackPagerEagerRangePlaceholder * 2)
    }
    val upMediaMetadataProvider = rememberUpdatedState(newValue = mediaMetadataProvider)
    val upLightContent = rememberUpdatedState(newValue = lightContent)
    val upOnItemClicked = rememberUpdatedState(newValue = onItemClicked)
    PlaybackPager(
        modifier = modifier.fillMaxSize(),
        state = state,
        isVertical = false,
        eagerRange = PlaybackPagerEagerRangePlaceholder,
        itemLayout = remember {
            {
                PlaybackPagerCompactItem(
                    modifier = Modifier,
                    cache = cache,
                    mediaMetadataProvider = upMediaMetadataProvider.value,
                    lightContent = upLightContent.value,
                    onItemClicked = upOnItemClicked.value
                )
            }
        }
    )
}


@Composable
fun PlaybackPagerItemScope.PlaybackPagerCompactItem(
    modifier: Modifier = Modifier,
    cache: LruCache<String, Any>,
    mediaMetadataProvider: RootCompactMediaMetadataProvider,
    lightContent: Boolean,
    onItemClicked: (() -> Unit)?
) {
    val description = remember(mediaMetadataProvider, mediaID) {
        mutableStateOf<PlaybackMediaDescription?>(
            cache.get(mediaID)
                ?.cast<Map<String, Any>>()?.get("description")
                ?.cast<PlaybackMediaDescription?>() ?: PlaybackMediaDescription.UNSET
        )
    }.apply {
        LaunchedEffect(this) {
            mediaMetadataProvider.descriptionAsFlow(mediaID).collect { value = it }
        }
    }.value

    Column(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                enabled = onItemClicked != null,
                onClick = onItemClicked ?: {}
            ),
        verticalArrangement = Arrangement.Center
    ) {
        // or we can just add more base ratio of the background on the palette
        val textColorState = remember {
            mutableStateOf(Color.Unspecified)
        }.apply {
            value =
                if (lightContent) {
                    Color(0xFFFFFFFF)
                } else {
                    Color(0xFF101010)
                }
        }

        BasicText(
            text = description?.title ?: "",
            style = MaterialTheme3.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = textColorState.value,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(1.dp))

        BasicText(
            text = description?.subtitle ?: "",
            style = MaterialTheme3.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = textColorState.value,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    LaunchedEffect(
        description,
        cache,
        block = {
            val map = cache.get(mediaID)?.cast<Map<String, Any>>()?.toMutableMap()
                ?: mutableMapOf<String, Any>()
            description?.let { map["description"] = description }
            cache.put(mediaID, map)
        }
    )
}