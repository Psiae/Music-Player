package dev.dexsr.klio.base.debug

import com.flammky.musicplayer.base.BuildConfig


// TODO: linter

object DebugResourceUsage {

    init {
        if (!BuildConfig.DEBUG) error("DebugResourceUsage on non-debug build")
    }
}

object DebugBlock {

    init {
        if (!BuildConfig.DEBUG) error("DebugBlock on non-debug build")
    }
}

inline fun <T> debugResourceUsage(
    block: DebugResourceUsage.() -> T
): T = DebugResourceUsage.block()

inline fun <T> debugBlock(
    block: DebugBlock.() -> T
) = DebugBlock.block()
