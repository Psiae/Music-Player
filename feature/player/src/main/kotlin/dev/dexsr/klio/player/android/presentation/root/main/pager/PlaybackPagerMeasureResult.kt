package dev.dexsr.klio.player.android.presentation.root.main.pager

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.unit.IntSize

class PlaybackPagerMeasureResult(
    measureResult: MeasureResult,
    override val orientation: Orientation,
    override val pageCount: Int,
    override val closestPageToSnapPosition: PlaybackPagerPageInfo?,
    override val visiblePagesInfo: List<PlaybackPagerPageInfo>,
    override val viewportStartOffset: Int,
    override val viewportEndOffset: Int,
    override val afterContentPadding: Int,
    override val pageSize: Int,
    override val firstVisiblePage: PlaybackPagerMeasuredPage?,
    override val firstVisiblePageOffset: Int,
    val consumedScroll: Float,
    val canScrollForward: Boolean,
): PlaybackPagerLayoutInfo, MeasureResult by measureResult {

    override val beforeContentPadding: Int
        get() = -viewportStartOffset

    override val viewportSize: IntSize
        get() = IntSize(width, height)
}