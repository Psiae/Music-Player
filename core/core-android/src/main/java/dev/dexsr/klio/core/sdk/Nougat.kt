package com.flammky.musicplayer.core.sdk

import android.os.Build

object Nougat : AndroidAPI() {
    override val buildcode: BuildCode = BuildCode(Build.VERSION_CODES.N)
}

object Nougat_MR1 : AndroidAPI() {
    override val buildcode: BuildCode = BuildCode(Build.VERSION_CODES.N_MR1)
}