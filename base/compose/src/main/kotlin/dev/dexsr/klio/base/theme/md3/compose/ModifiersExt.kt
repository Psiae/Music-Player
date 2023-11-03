package dev.dexsr.klio.base.theme.md3.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.dexsr.klio.base.compose.consumeDownGesture
import dev.dexsr.klio.base.compose.ifUnspecified
import dev.dexsr.klio.base.theme.md3.MD3Theme
import dev.dexsr.klio.base.theme.md3.uxsystem.*
import kotlin.math.ln

fun Modifier.localMaterial3Background(
    transform: (Color) -> Color
) = composed {
    background(transform(MD3Theme.backgroundColorAsState().value))
}

fun Modifier.localMaterial3Background() = composed {
    background(MD3Theme.backgroundColorAsState().value)
}

fun Modifier.localMaterial3Surface(
    color: (Color) -> Color,
    applyInteractiveUxEnforcement: Boolean = true,
    tonalElevation: Dp = 0.dp,
    tonalTint: Color = Color.Unspecified,
    shadowElevation: Dp = 0.dp,
    shape: Shape = RectangleShape,
    borderStroke: BorderStroke? = null,
) = composed {
    Modifier.localMaterial3Surface(
        color = MD3Theme.surfaceColorAsState(transform = color).value,
        applyInteractiveUxEnforcement = applyInteractiveUxEnforcement,
        tonalElevation = tonalElevation,
        tonalTint = tonalTint,
        shadowElevation = shadowElevation,
        shape = shape,
        borderStroke = borderStroke,
    )
}

fun Modifier.localMaterial3Surface(
    color: Color = Color.Unspecified,
    applyInteractiveUxEnforcement: Boolean = true,
    tonalElevation: Dp = 0.dp,
    tonalTint: Color = Color.Unspecified,
    shadowElevation: Dp = 0.dp,
    shape: Shape = RectangleShape,
    borderStroke: BorderStroke? = null,
) = this
    .then(
        if (applyInteractiveUxEnforcement)
            Modifier.interactiveUiElementSizeEnforcement()
        else
            Modifier
    )
    .shadow(
        shadowElevation,
        shape,
        clip = false
    )
    .then(
        if (borderStroke != null)
            Modifier.border(borderStroke, shape)
        else
            Modifier
    )
    .clip(shape)
    .composed {
        val specifiedColor = color.ifUnspecified { MD3Theme.surfaceColorAsState().value }
        Modifier.background(
            color = if (!tonalTint.isUnspecified)
                surfaceColorAtElevation(
                    surface = specifiedColor,
                    tint = tonalTint,
                    elevation = tonalElevation
                )
            else
                specifiedColor
        )
    }
    .consumeDownGesture()

fun Modifier.interactiveUiElementSizeEnforcement() = sizeIn(
    minWidth = MATERIAL3_INTERACTIVE_COMPONENT_MINIMUM_SIZE_DP.dp,
    minHeight = MATERIAL3_INTERACTIVE_COMPONENT_MINIMUM_SIZE_DP.dp
)

fun Modifier.interactiveUiElementAlphaEnforcement(
    isContent: Boolean,
    enabled: Boolean
) = alpha(
    // locals ?
    alpha = when {
        enabled -> 1f
        isContent -> MATERIAL3_INTERACTIVE_COMPONENT_SURFACE_CONTENT_DISABLED_ALPHA
        else -> MATERIAL3_INTERACTIVE_COMPONENT_SURFACE_DISABLED_ALPHA
    }
)

fun Modifier.interactiveUiElementTextAlphaEnforcement(
    isContent: Boolean,
    enabled: Boolean
) = alpha(
    alpha = when {
        enabled -> 1f
        isContent -> MATERIAL3_INTERACTIVE_TEXT_COMPONENT_SURFACE_CONTENT_DISABLED_ALPHA
        else -> MATERIAL3_INTERACTIVE_TEXT_COMPONENT_SURFACE_DISABLED_ALPHA
    }
)

private fun surfaceColorAtElevation(
    surface: Color,
    tint: Color,
    elevation: Dp,
): Color {
    if (elevation == 0.dp) return surface
    val alpha = ((4.5f * ln(elevation.value + 1)) + 2f) / 100f
    return tint.copy(alpha = alpha).compositeOver(surface)
}
