package com.flammky.musicplayer.core.sdk

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

object AndroidBuildVersion {

    // Nougat
    // 7.1
    // 25
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N_MR1)
    @JvmStatic
    fun AndroidAPI.hasNMR() = buildcode >= Nougat_MR1.buildcode

    // Oreo
    // 8.0
    // 26
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
    fun AndroidAPI.hasOreo() = buildcode >= Oreo.buildcode

    // Oreo
    // 8.1
    // 27
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O_MR1)
    fun AndroidAPI.hasOMR() = buildcode >= OreoMR1.buildcode

    // Pie
    // 9.0
    // 28
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.P)
    fun AndroidAPI.hasPie() = buildcode >= Pie.buildcode

    // Q
    // 10.0
    // 29
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
    fun AndroidAPI.hasQ() = buildcode >= Q.buildcode

    // Red_Velvet
    // 11.0
    // 30
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    fun AndroidAPI.hasR() = buildcode >= AndroidR.buildcode

    // Snow_cone
    // 12.0
    // 31
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    fun AndroidAPI.hasSnowCone() = buildcode >= SnowCone.buildcode

    // Snow_cone_V2
    // 12L
    // 32
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S_V2)
    fun AndroidAPI.hasSnowConeV2() = buildcode >= SnowConeV2.buildcode

    // Tiramisu
    // 13.0
    // 33
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    fun AndroidAPI.hasTiramisu() = buildcode >= Tiramisu.buildcode

    // Nougat
    // 7.0
    // 24
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N)
    fun AndroidAPI.isNougat() = buildcode == Nougat.buildcode

    // Nougat
    // 7.1
    // 25
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N_MR1)
    fun AndroidAPI.isNMR() = buildcode == Nougat_MR1.buildcode

    // Oreo
    // 8.0
    // 26
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
    fun AndroidAPI.isOreo() = buildcode == Oreo.buildcode

    // Oreo
    // 8.1
    // 27
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O_MR1)
    fun AndroidAPI.isOMR() = buildcode == OreoMR1.buildcode

    // Pie
    // 9.0
    // 28
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.P)
    fun AndroidAPI.isPie() = buildcode == Pie.buildcode

    // Q
    // 10.0
    // 29
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
    fun AndroidAPI.isQ() = buildcode == Q.buildcode

    // Red_Velvet
    // 11.0
    // 30
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    fun AndroidAPI.isR() = buildcode == AndroidR.buildcode

    // Snow_cone
    // 12.0
    // 31
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    fun AndroidAPI.isSnowCone() = buildcode == SnowCone.buildcode

    // Snow_cone_V2
    // 12L
    // 32
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S_V2)
    fun AndroidAPI.isSnowConeV2() = buildcode == SnowConeV2.buildcode

    // Tiramisu
    // 13.0
    // 33
    @JvmStatic
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    fun AndroidAPI.isTiramisu() = buildcode == Tiramisu.buildcode

    @JvmStatic
    @ChecksSdkIntAtLeast(parameter = 1)
    fun AndroidAPI.hasLevel(level: Int) = buildcode.CODE_INT >= level

    @JvmStatic
    @ChecksSdkIntAtLeast(parameter = 1)
    fun AndroidAPI.inLevel(from: Int, to: Int) = inLevel(from .. to)

    @JvmStatic
    @ChecksSdkIntAtLeast(parameter = 1)
    fun AndroidAPI.inLevel(range: IntRange) = buildcode.CODE_INT in range
}
