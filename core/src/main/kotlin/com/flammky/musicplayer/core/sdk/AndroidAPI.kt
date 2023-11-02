package com.flammky.musicplayer.core.sdk

import android.os.Build

/**
 * @see AndroidBuildVersion
 */
abstract class AndroidAPI {

    abstract val buildcode: BuildCode

    companion object : AndroidAPI() {

        private val current: AndroidAPI = when(val sdk = Build.VERSION.SDK_INT) {
            Tiramisu.buildcode.CODE_INT -> Tiramisu
            SnowConeV2.buildcode.CODE_INT -> SnowConeV2
            SnowCone.buildcode.CODE_INT -> SnowCone
            AndroidR.buildcode.CODE_INT -> AndroidR
            Q.buildcode.CODE_INT -> Q
            Pie.buildcode.CODE_INT -> Pie
            Oreo.buildcode.CODE_INT -> Oreo
            Nougat.buildcode.CODE_INT -> Nougat
            else -> error("Unsupported Android API version: $sdk")
        }

        override val buildcode: BuildCode = current.buildcode
    }
}