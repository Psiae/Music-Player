package com.flammky.musicplayer.base.compose

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// consider naming this SubCompose instead
@Composable
fun NoInline(content: @Composable () -> Unit) = content()

@Composable
fun SubCompose(block: @Composable () -> Unit) = block()

@Composable
fun SubComposeBox(
	modifier: Modifier = Modifier,
	contentAlignment: Alignment = Alignment.TopStart,
	propagateMinConstraints: Boolean = false,
	content: @Composable BoxScope.() -> Unit
) = Box(modifier, contentAlignment, propagateMinConstraints, content)

@Composable
fun NoInlineBox(modifier: Modifier) = Box(modifier)

@Composable
fun NoInlineBox(
	modifier: Modifier = Modifier,
	contentAlignment: Alignment = Alignment.TopStart,
	propagateMinConstraints: Boolean = false,
	content: @Composable BoxScope.() -> Unit
) = Box(modifier, contentAlignment, propagateMinConstraints, content)

@Composable
fun NoInlineColumn(
	modifier: Modifier = Modifier,
	verticalArrangement: Arrangement.Vertical = Arrangement.Top,
	horizontalAlignment: Alignment.Horizontal = Alignment.Start,
	content: @Composable ColumnScope.() -> Unit
) = Column(modifier, verticalArrangement, horizontalAlignment, content)

@Composable
fun NoInlineRow(
	modifier: Modifier,
	horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
	verticalAlignment: Alignment.Vertical = Alignment.Top,
	content: @Composable RowScope.() -> Unit
) = Row(modifier, horizontalArrangement, verticalAlignment, content)
