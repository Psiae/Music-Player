package com.flammky.musicplayer.core.sdk

import android.os.Build
import dev.dexsr.klio.core.sdk.AndroidAPI

object AndroidR : AndroidAPI() {
    override val buildcode: BuildCode = BuildCode(Build.VERSION_CODES.R)
}

val AndroidAPI.R: AndroidR
    get() = AndroidR