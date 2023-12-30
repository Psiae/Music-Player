package com.flammky.musicplayer.core.sdk

import android.os.Build

object AndroidR : AndroidAPI() {
    override val buildcode: BuildCode = BuildCode(Build.VERSION_CODES.R)
}