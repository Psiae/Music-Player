package com.flammky.musicplayer.core.sdk

import android.os.Build

object Tiramisu : AndroidAPI() {
    const val BUILD_CODE_INT = Build.VERSION_CODES.TIRAMISU

    override val buildcode: BuildCode = BuildCode(BUILD_CODE_INT)
}

val AndroidAPI.tiramisu
    get() = Tiramisu