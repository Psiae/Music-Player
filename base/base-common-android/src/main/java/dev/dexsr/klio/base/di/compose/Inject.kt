package dev.dexsr.klio.base.di.compose

import androidx.compose.runtime.Composable
import dev.dexsr.klio.base.di.requireInject

// TODO: verify that we can just do `return LocalDependencyInjector.current`
@Composable
inline fun <reified T: Any> runtimeInject(): T = LocalDependencyInjector.current.requireInject()
