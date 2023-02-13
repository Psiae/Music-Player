package com.flammky.musicplayer.core.sdk

import android.os.Build

object SnowCone : AndroidAPI() {
    override val code: BuildCode = BuildCode(Build.VERSION_CODES.S)
}

object SnowConeV2 : AndroidAPI() {
    override val code: BuildCode = BuildCode(Build.VERSION_CODES.S_V2)
}