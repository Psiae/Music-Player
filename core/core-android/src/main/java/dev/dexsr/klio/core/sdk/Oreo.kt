package com.flammky.musicplayer.core.sdk

import android.os.Build
import dev.dexsr.klio.core.sdk.AndroidAPI

object Oreo : AndroidAPI() {
    override val buildcode: BuildCode = BuildCode(Build.VERSION_CODES.O)
}

val AndroidAPI.oreo get() = Oreo

object OreoMR1 : AndroidAPI() {
    override val buildcode: BuildCode = BuildCode(Build.VERSION_CODES.O_MR1)
}

val AndroidAPI.oreoMR1 get() = OreoMR1