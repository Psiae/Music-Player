package dev.dexsr.klio.base.composeui

import androidx.compose.runtime.*

@Composable
fun <T> rememberUpdatedState(
    newValue: T,
    mutationPolicy: SnapshotMutationPolicy<T>
): State<T> = remember(mutationPolicy) {
    mutableStateOf(newValue, mutationPolicy)
}.apply { value = newValue }

object AlwaysEqualPolicy : SnapshotMutationPolicy<Any?> {

    override fun equivalent(a: Any?, b: Any?): Boolean {
        return true
    }
}
