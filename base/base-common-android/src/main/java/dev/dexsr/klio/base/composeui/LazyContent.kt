package dev.dexsr.klio.base.composeui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
fun LazyContent(
    trigger: Boolean,
    content: @Composable () -> Unit
) {
    remember {
        mutableStateOf(false)
    }.run {
        if (trigger) value = true
        if (value) content.invoke()
    }
}
