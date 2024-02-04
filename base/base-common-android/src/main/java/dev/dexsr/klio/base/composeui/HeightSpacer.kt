package dev.dexsr.klio.base.composeui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@Composable
@NonRestartableComposable
fun HeightSpacer(height: Dp) = Spacer(modifier = Modifier.height(height))

@NonRestartableComposable
@Composable
fun WidthSpacer(width: Dp) = Spacer(modifier = Modifier.height(width))
