package com.flammky.musicplayer.core.build

import android.os.Build

object Q : AndroidAPI() {
    override val code: BuildCode = BuildCode(Build.VERSION_CODES.Q)
}