package dev.flammky.compose_components.core

import androidx.compose.ui.geometry.Offset

internal fun horizontalOffset(value: Float): Offset = Offset(x = value, y = 0f)
internal fun verticalOffset(value: Float): Offset = Offset(x = 0f, y = value)