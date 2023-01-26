package com.flammky.musicplayer.core.build

import android.os.Build

/**
 * @see AndroidBuildVersion
 */
abstract class AndroidAPI {

    abstract val code: BuildCode

    companion object : AndroidAPI() {

        private val current: AndroidAPI = when(val sdk = Build.VERSION.SDK_INT) {
            Tiramisu.code.CODE_INT -> Tiramisu
            SnowConeV2.code.CODE_INT -> SnowConeV2
            SnowCone.code.CODE_INT -> SnowCone
            R.code.CODE_INT -> R
            Q.code.CODE_INT -> Q
            Pie.code.CODE_INT -> Pie
            Oreo.code.CODE_INT -> Oreo
            Nougat.code.CODE_INT -> Nougat
            else -> error("Unsupported Android API version: $sdk")
        }

        override val code: BuildCode = current.code
    }
}