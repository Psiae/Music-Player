package com.kylentt.musicplayer.core.sdk

import android.os.Build

object VersionHelper {

  @JvmStatic fun hasNMR() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1    // Nougat       7.1     25
  @JvmStatic fun hasOreo() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O       // Oreo         8.0     26
  @JvmStatic fun hasOMR() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1    // Oreo         8.1     27
  @JvmStatic fun hasPie() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P        // Pie          9.0     28
  @JvmStatic fun hasQ() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q          // Q            10      29
  @JvmStatic fun hasR() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R          // Red Velvet   11      30
  @JvmStatic fun hasSnowCone() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S   // Snow Cone    12      31

  @JvmStatic fun isNougat() = Build.VERSION.SDK_INT == Build.VERSION_CODES.N     // Nougat       7.0     24
  @JvmStatic fun isNMR() = Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1    // Nougat       7.1     25
  @JvmStatic fun isOreo() = Build.VERSION.SDK_INT == Build.VERSION_CODES.O       // Oreo         8.0     26
  @JvmStatic fun isOMR() = Build.VERSION.SDK_INT == Build.VERSION_CODES.O_MR1    // Oreo         8.1     27
  @JvmStatic fun isPie() = Build.VERSION.SDK_INT == Build.VERSION_CODES.P        // Pie          9.0     28
  @JvmStatic fun isQ() = Build.VERSION.SDK_INT == Build.VERSION_CODES.Q          // Q            10      29
  @JvmStatic fun isR() = Build.VERSION.SDK_INT == Build.VERSION_CODES.R          // Red Velvet   11      30
  @JvmStatic fun isSnowCone() = Build.VERSION.SDK_INT == Build.VERSION_CODES.S   // Snow Cone    12      31

}
