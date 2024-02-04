package com.flammky.musicplayer.core.sdk

import android.os.Build
import dev.dexsr.klio.core.sdk.AndroidAPI

object Q : AndroidAPI() {
    override val buildcode: BuildCode = BuildCode(Build.VERSION_CODES.Q)
}

val AndroidAPI.q get() = Q