package com.kylentt.mediaplayer.helper

import android.os.Build

object VersionHelper {
  fun hasNMR() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1    // Nougat       7.1     25
  fun hasOreo() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O       // Oreo         8.0     26
  fun hasOMR() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1    // Oreo         8.1     27
  fun hasPie() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P        // Pie          9.0     28
  fun hasQ() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q          // Q            10      29
  fun hasR() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R          // Red Velvet   11      30
  fun hasSnowCone() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S   // Snow Cone    12      31

  fun isNougat() = Build.VERSION.SDK_INT == Build.VERSION_CODES.N     // Nougat       7.0     24
  fun isNMR() = Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1    // Nougat       7.1     25
  fun isOreo() = Build.VERSION.SDK_INT == Build.VERSION_CODES.O       // Oreo         8.0     26
  fun isOMR() = Build.VERSION.SDK_INT == Build.VERSION_CODES.O_MR1    // Oreo         8.1     27
  fun isPie() = Build.VERSION.SDK_INT == Build.VERSION_CODES.P        // Pie          9.0     28
  fun isQ() = Build.VERSION.SDK_INT == Build.VERSION_CODES.Q          // Q            10      29
  fun isR() = Build.VERSION.SDK_INT == Build.VERSION_CODES.R          // Red Velvet   11      30
  fun isSnowCone() = Build.VERSION.SDK_INT == Build.VERSION_CODES.S   // Snow Cone    12      31
}
