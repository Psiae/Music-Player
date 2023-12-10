package dev.dexsr.klio.player.android.presentation.root.main

import androidx.collection.LruCache
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.flammky.musicplayer.player.BuildConfig
import dev.dexsr.klio.base.isUNSET
import dev.dexsr.klio.base.kt.cast
import dev.dexsr.klio.base.theme.md3.MD3Theme
import dev.dexsr.klio.base.theme.md3.compose.MaterialTheme3
import dev.dexsr.klio.base.theme.md3.compose.dpPaddingIncrementsOf
import dev.dexsr.klio.base.theme.md3.compose.surfaceVariantContentColorAsState
import dev.dexsr.klio.player.android.presentation.root.MediaMetadataProvider
import dev.dexsr.klio.player.shared.LocalMediaArtwork

@Composable
fun PlaybackPagerItemLayout(
    modifier: Modifier,
    mediaMetadataProvider: MediaMetadataProvider,
    contentPadding: PaddingValues,
    scope: PlaybackPagerItemScope,
    cache: LruCache<String, Any>
) {
    with(scope) {
        val artwork = remember(mediaMetadataProvider, mediaID) {
            mutableStateOf<LocalMediaArtwork?>(
                cache.get(mediaID)
                    ?.cast<Map<String, Any>>()?.get("artwork")
                    ?.cast<LocalMediaArtwork?>() ?: LocalMediaArtwork.UNSET
            )
        }.apply {
            LaunchedEffect(this) {
                mediaMetadataProvider.artworkAsFlow(mediaID).collect { value = it }
            }
        }.value

        val ctx = LocalContext.current

        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                modifier = Modifier
                    .fillMaxSize(),
                model = remember(ctx, artwork) {
                    ImageRequest.Builder(ctx)
                        .data(artwork?.image?.value)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        // cache-key GEN
                        .memoryCacheKey(artwork?.takeIf { !it.isUNSET }?.let { mediaID })
                        .build()
                },
                contentDescription = "art",
                contentScale = if (artwork?.allowTransform == true) {
                    ContentScale.Crop
                } else {
                    ContentScale.Fit
                }
            )
            if (BuildConfig.DEBUG) {
                BasicText(
                    modifier = Modifier.align(Alignment.Center).padding(MD3Theme.dpPaddingIncrementsOf(2)),
                    text = page.toString(),
                    style = MaterialTheme3.typography.headlineLarge.copy(color = MD3Theme.surfaceVariantContentColorAsState().value, fontSize = 100.sp)
                )
            }
        }

        LaunchedEffect(
            artwork,
            cache,
            block = {
                val map = cache.get(mediaID)?.cast<Map<String, Any>>()?.toMutableMap()
                    ?: mutableMapOf<String, Any>()
                artwork?.let { map["artwork"] = artwork }
                cache.put(mediaID, map)
            }
        )
    }
}