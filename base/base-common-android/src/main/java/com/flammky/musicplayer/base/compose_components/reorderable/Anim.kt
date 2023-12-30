package dev.flammky.compose_components.reorderable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.AndroidUiDispatcher
import dev.flammky.compose_components.core.SnapshotRead
import dev.flammky.compose_components.core.SnapshotWriter
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.coroutineContext

class DragCancelAnimation(
    private val spec: FiniteAnimationSpec<Offset>
) {
    private val animatable = Animatable(Offset.Zero, Offset.VectorConverter)

    val animatedOffset: Offset
        @SnapshotRead get() = animatable.value

    var cancellingItemPosition by mutableStateOf<dev.flammky.compose_components.reorderable.ItemPosition?>(null)
        @SnapshotRead get
        @SnapshotWriter private set

    internal suspend fun dragCancelled(position: dev.flammky.compose_components.reorderable.ItemPosition, offset: Offset) {
        check(coroutineContext[ContinuationInterceptor] is AndroidUiDispatcher)
        cancellingItemPosition = position
        animatable.snapTo(offset)
        animatable.animateTo(
            Offset.Zero,
            spec,
        )
        cancellingItemPosition = null
    }
}