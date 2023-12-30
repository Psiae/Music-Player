package dev.dexsr.klio.base.di.compose

import androidx.compose.runtime.staticCompositionLocalOf
import dev.dexsr.klio.base.di.RuntimeDependencyInjector

val LocalDependencyInjector = staticCompositionLocalOf<RuntimeDependencyInjector> {
    error("RuntimeDependencyInjector not provided")
}
