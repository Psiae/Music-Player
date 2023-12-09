package dev.dexsr.klio.player.android.presentation.root.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.unit.IntSize
import dev.dexsr.klio.base.UNSET

interface PlaybackPagerLayoutInfo : UNSET<PlaybackPagerLayoutInfo> {

    override val UNSET: PlaybackPagerLayoutInfo get() = Companion.UNSET

    val viewportSize: IntSize
    val visiblePagesInfo: List<PlaybackPagerPageInfo>
    val closestPageToSnapPosition: PlaybackPagerPageInfo?
    val beforeContentPadding: Int
    val afterContentPadding: Int
    val pageSize: Int
    val pageCount: Int
    val orientation: Orientation
    val viewportStartOffset: Int
    val viewportEndOffset: Int

    val firstVisiblePage: PlaybackPagerMeasuredPage?
    val firstVisiblePageOffset: Int

    companion object : UNSET<PlaybackPagerLayoutInfo> {

        override val UNSET = object : PlaybackPagerLayoutInfo {

            override val visiblePagesInfo: List<PlaybackPagerPageInfo>
                get() = emptyList()
            override val closestPageToSnapPosition: PlaybackPagerPageInfo = PlaybackPagerPageInfo(
                index = -1,
                offset = 0,
                size = 0
            )
            override val viewportSize: IntSize
                get() = IntSize.Zero
            override val beforeContentPadding: Int
                get() = 0
            override val afterContentPadding: Int
                get() = 0
            override val pageSize: Int
                get() = 0
            override val pageCount: Int
                get() = 0
            override val orientation: Orientation
                get() = Orientation.Vertical
            override val viewportStartOffset: Int
                get() = 0
            override val viewportEndOffset: Int
                get() = 0
            override val firstVisiblePage: PlaybackPagerMeasuredPage?
                get() = null
            override val firstVisiblePageOffset: Int
                get() = 0
        }
    }
}

@ExperimentalFoundationApi
internal val PlaybackPagerLayoutInfo.mainAxisViewportSize: Int
    get() = if (orientation == Orientation.Vertical) viewportSize.height else viewportSize.width