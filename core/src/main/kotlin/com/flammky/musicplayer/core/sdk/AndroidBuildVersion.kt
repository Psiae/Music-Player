package com.flammky.musicplayer.core.sdk

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

object AndroidBuildVersion {

    // Nougat
    // 7.1
    // 25
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N_MR1)
    @JvmStatic
    fun AndroidAPI.hasNMR() = code >= Nougat_MR1.code

    // Oreo
    // 8.0
    // 26
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
    fun AndroidAPI.hasOreo() = code >= Oreo.code

    // Oreo
    // 8.1
    // 27
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O_MR1)
    fun AndroidAPI.hasOMR() = code >= OreoMR1.code

    // Pie
    // 9.0
    // 28
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.P)
    fun AndroidAPI.hasPie() = code >= Pie.code

    // Q
    // 10.0
    // 29
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
    fun AndroidAPI.hasQ() = code >= Q.code

    // Red_Velvet
    // 11.0
    // 30
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    fun AndroidAPI.hasR() = code >= AndroidR.code

    // Snow_cone
    // 12.0
    // 31
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    fun AndroidAPI.hasSnowCone() = code >= SnowCone.code

    // Snow_cone_V2
    // 12L
    // 32
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S_V2)
    fun AndroidAPI.hasSnowConeV2() = code >= SnowConeV2.code

    // Tiramisu
    // 13.0
    // 33
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    fun AndroidAPI.hasTiramisu() = code >= Tiramisu.code

    // Nougat
    // 7.0
    // 24
    @JvmStatic
    fun AndroidAPI.isNougat() = code == Nougat.code

    // Nougat
    // 7.1
    // 25
    @JvmStatic
    fun AndroidAPI.isNMR() = code == Nougat_MR1.code

    // Oreo
    // 8.0
    // 26
    @JvmStatic
    fun AndroidAPI.isOreo() = code == Oreo.code

    // Oreo
    // 8.1
    // 27
    @JvmStatic
    fun AndroidAPI.isOMR() = code == OreoMR1.code

    // Pie
    // 9.0
    // 28
    @JvmStatic
    fun AndroidAPI.isPie() = code == Pie.code

    // Q
    // 10.0
    // 29
    @JvmStatic
    fun AndroidAPI.isQ() = code == Q.code

    // Red_Velvet
    // 11.0
    // 30
    @JvmStatic
    fun AndroidAPI.isR() = code == AndroidR.code

    // Snow_cone
    // 12.0
    // 31
    @JvmStatic
    fun AndroidAPI.isSnowCone() = code == SnowCone.code

    // Snow_cone_V2
    // 12L
    // 32
    @JvmStatic
    fun AndroidAPI.isSnowConeV2() = code == SnowConeV2.code

    // Tiramisu
    // 13.0
    // 33
    @JvmStatic
    fun AndroidAPI.isTiramisu() = code == Tiramisu.code

    @JvmStatic
    fun AndroidAPI.hasLevel(level: Int) = code.CODE_INT >= level

    @JvmStatic
    fun AndroidAPI.inLevel(from: Int, to: Int) = code.CODE_INT in from..to
}
