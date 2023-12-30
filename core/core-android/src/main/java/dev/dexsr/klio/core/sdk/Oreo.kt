package com.flammky.musicplayer.core.sdk

import android.os.Build

object Oreo : AndroidAPI() {
    override val buildcode: BuildCode = BuildCode(Build.VERSION_CODES.O)
}

object OreoMR1 : AndroidAPI() {
    override val buildcode: BuildCode = BuildCode(Build.VERSION_CODES.O_MR1)
}