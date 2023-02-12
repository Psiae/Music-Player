package dev.flammky.compose_components.reorderable

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.flammky.compose_components.core.SnapshotReader

@Composable
fun ReorderableLazyColumn(
    modifier: Modifier = Modifier,
    state: ReorderableLazyListState,
    contentPadding: PaddingValues = PaddingValues(),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    content: @SnapshotReader ReorderableLazyListScope.() -> Unit
) {

    val itemProvider = rememberReorderableLazyListItemProvider(state, content)

    LazyColumn(
        modifier = modifier.then(state.applier.lazyLayoutModifiers),
        state = state.lazyListState,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled
    ) scope@ {
        state.applier.onLazyListScope(this, itemProvider)
    }
}