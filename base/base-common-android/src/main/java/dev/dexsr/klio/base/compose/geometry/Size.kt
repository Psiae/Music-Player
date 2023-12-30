package dev.dexsr.klio.base.compose.geometry

import androidx.compose.ui.geometry.Size
import dev.dexsr.klio.base.geometry.Vector2D

operator fun Size.plus(vector2D: Vector2D): Size {
    return Size(width = width + vector2D.x, height = height + vector2D.y)
}
