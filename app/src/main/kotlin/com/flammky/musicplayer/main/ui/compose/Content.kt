@file:Suppress("NOTHING_TO_INLINE")

package com.flammky.musicplayer.main.ui.compose

import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import com.flammky.musicplayer.main.MainActivity
import com.flammky.musicplayer.ui.main.compose.ComposeContent

fun MainActivity.setContent() = setContent { ThemedContent() }

@Composable
private inline fun MainActivity.ThemedContent() = MaterialDesign3Theme { ComposeContent() }
