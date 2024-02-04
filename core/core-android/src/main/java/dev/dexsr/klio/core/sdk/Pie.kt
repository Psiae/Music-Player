package com.flammky.musicplayer.core.sdk

import android.os.Build
import dev.dexsr.klio.core.sdk.AndroidAPI

object Pie : AndroidAPI() {
    override val buildcode: BuildCode = BuildCode(Build.VERSION_CODES.P)
}

val AndroidAPI.pie get() = Pie