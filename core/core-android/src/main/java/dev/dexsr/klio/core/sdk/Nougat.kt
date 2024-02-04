package com.flammky.musicplayer.core.sdk

import android.os.Build
import dev.dexsr.klio.core.sdk.AndroidAPI

object Nougat : AndroidAPI() {
    override val buildcode: BuildCode = BuildCode(Build.VERSION_CODES.N)
}

val AndroidAPI.nougat get() = Nougat

object Nougat_MR1 : AndroidAPI() {
    override val buildcode: BuildCode = BuildCode(Build.VERSION_CODES.N_MR1)
}

val AndroidAPI.nougatMR1 get() = Nougat_MR1