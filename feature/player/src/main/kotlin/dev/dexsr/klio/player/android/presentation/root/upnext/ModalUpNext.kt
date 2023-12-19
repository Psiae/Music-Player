package dev.dexsr.klio.player.android.presentation.root.upnext

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flammky.musicplayer.base.compose.LocalLayoutVisibility
import com.flammky.musicplayer.player.presentation.root.main.ComposeBackPressRegistry
import dev.dexsr.klio.base.compose.clickable
import dev.dexsr.klio.base.compose.horizontalBiasAlignment
import dev.dexsr.klio.base.compose.ifUnspecified
import dev.dexsr.klio.base.theme.md3.MD3Spec
import dev.dexsr.klio.base.theme.md3.MD3Theme
import dev.dexsr.klio.base.theme.md3.components.sheet.BottomSheetDragHandleAlpha
import dev.dexsr.klio.base.theme.md3.components.sheet.bottomSheet
import dev.dexsr.klio.base.theme.md3.compose.*
import dev.dexsr.klio.player.android.presentation.root.main.PlaybackControlScreenState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpNextBottomSheet(
    modifier: Modifier = Modifier,
    container: PlaybackControlScreenState
) {
    val sheetState = rememberBottomSheetScaffoldState()
    val dragHandleHeightState = remember {
        mutableStateOf(0)
    }
    val upPeekHeight = rememberUpdatedState(
        with(LocalDensity.current) {
            dragHandleHeightState.value.toDp()
                .coerceAtLeast(MD3Spec.bottomSheet.PeekHeightDp.toDp())
                .plus(WindowInsets.navigationBars.getBottom(this).toDp())
        }
    )
    BoxWithConstraints(
        modifier = modifier
            .statusBarsPadding()
    ) {
        val upSheetMaxHeight = rememberUpdatedState(newValue = maxHeight)
        BottomSheetScaffold(
            scaffoldState = sheetState,
            containerColor = Color.Transparent,
            content = {
            },
            sheetDragHandle = {
                Column(
                    modifier = Modifier
                        .onSizeChanged { dragHandleHeightState.value = it.height }
                ) {
                    Box(
                        Modifier
                            .composed {
                                val scope = rememberCoroutineScope()
                                clickable(
                                    indication = null,
                                    enabled = sheetState.bottomSheetState.currentValue ==
                                            sheetState.bottomSheetState.targetValue &&
                                            sheetState.bottomSheetState.currentValue != SheetValue.Hidden,
                                    onClick = {
                                        scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                            if (sheetState.bottomSheetState.currentValue != sheetState.bottomSheetState.targetValue) {
                                                return@launch
                                            }
                                            when (sheetState.bottomSheetState.currentValue) {
                                                SheetValue.PartiallyExpanded -> {
                                                    sheetState.bottomSheetState.expand()
                                                }

                                                SheetValue.Expanded -> {
                                                    sheetState.bottomSheetState.partialExpand()
                                                }

                                                else -> Unit
                                            }
                                        }
                                    }
                                )
                            }
                            .padding(
                                top = MD3Spec.bottomSheet.DragHandleVerticalPadding.div(2).dp,
                                bottom = MD3Theme.dpPaddingIncrementsOf(2)
                            )
                            .size(
                                width = MD3Spec.bottomSheet.DragHandleWidthDp.dp,
                                height = MD3Spec.bottomSheet.DragHandleHeightDp.dp
                            )
                            .align(
                                horizontalBiasAlignment(
                                    bias = MD3Spec.bottomSheet.DragHandleAlignmentHorizontalBias,
                                )
                            )
                            .alpha(MD3Spec.bottomSheet.DragHandleAlpha)
                            .background(
                                color = MD3Theme
                                    .colorFromToken(
                                        tokenStr = MD3Spec.bottomSheet.DragHandleColorToken
                                    )
                                    .ifUnspecified {
                                        MD3Theme.surfaceContentColorAsState().value
                                    },
                                shape = RoundedCornerShape(28.dp)
                            )
                    )
                    BasicText(
                        modifier = Modifier
                            .composed {
                                val scope = rememberCoroutineScope()
                                clickable(
                                    indication = LocalIndication.current,
                                    enabled = sheetState.bottomSheetState.currentValue ==
                                            sheetState.bottomSheetState.targetValue &&
                                            sheetState.bottomSheetState.currentValue != SheetValue.Hidden,
                                    onClick = {
                                        scope.launch(start = CoroutineStart.UNDISPATCHED) {
                                            if (sheetState.bottomSheetState.currentValue != sheetState.bottomSheetState.targetValue) {
                                                return@launch
                                            }
                                            when (sheetState.bottomSheetState.currentValue) {
                                                SheetValue.PartiallyExpanded -> {
                                                    sheetState.bottomSheetState.expand()
                                                }

                                                else -> Unit
                                            }
                                        }
                                    }
                                )
                            }
                            .padding(
                                vertical = MD3Theme.dpPaddingIncrementsOf(2),
                                horizontal = MD3Theme.dpPaddingIncrementsOf(3)
                            )
                            .align(Alignment.CenterHorizontally),
                        text = "UP NEXT",
                        style = run {
                            val alphaState = remember {
                                mutableFloatStateOf(0.68f)
                            }
                            val density = LocalDensity.current
                            LaunchedEffect(
                                sheetState,
                                density,
                                block = {
                                    snapshotFlow { sheetState.bottomSheetState.requireOffset() }
                                        .collect { offset ->
                                            alphaState.floatValue = with(density) {
                                                if (
                                                    upSheetMaxHeight.value
                                                        .minus(offset.roundToInt().toDp())
                                                        .compareTo(upPeekHeight.value) == 1
                                                ) {
                                                    1f
                                                } else {
                                                    0.68f
                                                }
                                            }
                                        }
                                }
                            )
                            MaterialTheme3.typography.labelLarge
                                .copy(
                                    color = MD3Theme.surfaceContentColorAsState().value
                                        .copy(
                                            alpha = alphaState.floatValue
                                        ),
                                    fontWeight = FontWeight.SemiBold,
                                )
                        }
                    )
                }
            },
            sheetPeekHeight = upPeekHeight.value,
            sheetContent = {
                val density = LocalDensity.current
                val alphaState = remember {
                    mutableFloatStateOf(0f)
                }
                val showState = remember {
                    mutableStateOf(false)
                }
                val background = MD3Theme.surfaceColorAtElevation(elevation = 1.dp)
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(background)
                        .alpha(alphaState.floatValue)
                ) {
                    ReorderableQueue(
                        modifier = Modifier,
                        container = container,
                        visible = showState.value,
                        bottomSpacing = LocalLayoutVisibility.Bottom.current,
                        backgroundColor = MD3Theme.surfaceColorAtElevation(elevation = 1.dp)
                    )
                    LaunchedEffect(sheetState, density, block = {
                        snapshotFlow {
                            // read states
                            dragHandleHeightState.value
                            upSheetMaxHeight.value
                            upPeekHeight.value
                            sheetState.bottomSheetState.requireOffset()
                        }.collect { offset ->
                            with(density) {
                                val sheetHeightPx = upSheetMaxHeight.value.roundToPx()
                                val peekHeightPx = upPeekHeight.value.roundToPx()
                                val heightPx =  (sheetHeightPx - offset).roundToInt()
                                alphaState.floatValue = run {
                                    if (sheetHeightPx == 0) {
                                        0f
                                    } else {
                                        heightPx
                                            .minus(peekHeightPx)
                                            .div(sheetHeightPx * 0.65f)
                                            .coerceIn(0f, 1f)
                                    }
                                }
                                showState.value = heightPx.compareTo(peekHeightPx) == 1
                            }
                        }
                    })
                }
            }
        )
    }

    DisposableEffect(
        container,
        sheetState,
        effect = {
            val coroutineScope = CoroutineScope(SupervisorJob())
            val consumer = ComposeBackPressRegistry.BackPressConsumer {
                if (sheetState.bottomSheetState.targetValue == SheetValue.Expanded) {
                    coroutineScope.launch(AndroidUiDispatcher.Main) {
                        // seriously, why don't they expose the mutatePriority ?
                        sheetState.bottomSheetState.partialExpand()
                    }
                    return@BackPressConsumer true
                }
                false
            }
            container.backPressRegistry.registerBackPressConsumer(consumer)
            onDispose {
                container.backPressRegistry.unregisterBackPressConsumer(consumer)
                coroutineScope.cancel()
            }
        }
    )
}