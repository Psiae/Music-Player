package dev.flammky.compose_components.reorderable

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.*
import com.flammky.musicplayer.base.compose_components.reorderable.ReorderResultHandle
import dev.flammky.compose_components.core.SnapshotReader

internal class ReorderableLazyListItemProvider(
    private val state: ReorderableLazyListState,
    private val baseContentState: State<@SnapshotReader ReorderableLazyListScope.() -> Unit>
) {

    private var overrideScope by mutableStateOf<MaskedReorderableLazyListScope?>(null)

    private val baseScope by derivedStateOf {
        // consider possibility of rebasing
        overrideScope = null
        RealReorderableLazyListScope(state).apply(baseContentState.value)
    }

    private val maskedScope by derivedStateOf {
        val over = overrideScope
        val base = baseScope
        over ?: base
    }

    fun provideToLayout(lazyListScope: LazyListScope) {
        val provideScope = maskedScope
        provideScope.intervals.forEach { interval ->
            lazyListScope.items(
                interval.items.size,
                { i -> provideScope.itemOfIndex(interval.itemStartIndex + i)!!.key },
                { i -> provideScope.itemOfIndex(interval.itemStartIndex + i)!!.type },
            ) { i ->
                val composition = maskedScope
                rememberInternalReorderableLazyItemScope(
                    composition = composition,
                    displayIndex = i
                ).run {
                    ComposeContent()
                }
            }
        }
    }

    fun indexOfKey(key: Any): Int = maskedScope.indexOfKey(key)

    fun onStartReorder(
        composition: InternalReorderableLazyListScope,
        from: ItemPosition
    ): Boolean {
        if (composition != baseScope) {
            return false
        }
        overrideScope = MaskedReorderableLazyListScope(baseScope)
        return true
    }

    fun onMove(
        composition: InternalReorderableLazyListScope,
        from: ItemPosition,
        new: ItemPosition
    ): Boolean {
        val overrideScope = this.overrideScope
        if (overrideScope == null || composition != baseScope) {
            return false
        }
        this.overrideScope = overrideScope.onMove(from, new)
        return true
    }

    fun onEndReorder(
        composition: InternalReorderableLazyListScope,
        cancelled: Boolean,
        from: ItemPosition,
        to: ItemPosition
    ): ReorderResultHandle? {
        if (this.baseScope != composition) return null
				return object : ReorderResultHandle {
						override fun done() {
							if (this@ReorderableLazyListItemProvider.baseScope != composition) return
							this@ReorderableLazyListItemProvider.overrideScope = null
						}
				}
    }
}

@Composable
internal fun rememberReorderableLazyListItemProvider(
    state: ReorderableLazyListState,
    baseContent: @SnapshotReader ReorderableLazyListScope.() -> Unit
): ReorderableLazyListItemProvider {

    val updatedBaseContent = rememberUpdatedState(newValue = baseContent)

    return remember(state) {
        ReorderableLazyListItemProvider(state, updatedBaseContent)
    }
}
