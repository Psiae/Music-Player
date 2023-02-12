package dev.flammky.compose_components.reorderable

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId

internal data class ReorderDragStart(
    val id: PointerId,
    val composition: InternalReorderableLazyListScope,
    val slop: Offset,
    val selfIndex: Int,
    val selfKey: Any
)