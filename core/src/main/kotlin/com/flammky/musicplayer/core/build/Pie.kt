package com.flammky.musicplayer.core.build

import android.os.Build

object Pie : AndroidAPI() {
    override val code: BuildCode = BuildCode(Build.VERSION_CODES.P)
}