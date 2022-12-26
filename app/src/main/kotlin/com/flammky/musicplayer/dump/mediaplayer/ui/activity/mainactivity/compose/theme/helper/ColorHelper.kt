package com.flammky.musicplayer.dump.mediaplayer.ui.activity.mainactivity.compose.theme.helper

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
import kotlin.math.ln

object ColorHelper {

    @Composable
    fun getTonedSurface(el: Int = 2): Color {
        val alpha = ((4.5f * ln( x = (el).dp.value + 1) ) + 2f) / 100f
        val surface = MaterialTheme.colorScheme.surface
        val primary = MaterialTheme.colorScheme.primary
        return alpha.let { primary.copy(it).compositeOver(surface) }
    }
}
