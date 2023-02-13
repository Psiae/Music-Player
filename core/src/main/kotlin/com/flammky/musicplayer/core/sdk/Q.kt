package com.flammky.musicplayer.core.sdk

import android.os.Build

object Q : AndroidAPI() {
    override val code: BuildCode = BuildCode(Build.VERSION_CODES.Q)
}