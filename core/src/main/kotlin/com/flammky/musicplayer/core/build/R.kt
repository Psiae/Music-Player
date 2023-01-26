package com.flammky.musicplayer.core.build

import android.os.Build

object R : AndroidAPI() {
    override val code: BuildCode = BuildCode(Build.VERSION_CODES.R)
}