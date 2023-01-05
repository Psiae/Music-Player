package com.flammky.androidx.sdk

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

object VersionHelper {

	@JvmStatic
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N_MR1)
	fun hasNMR() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1      // Nougat        7.1   25

	@JvmStatic
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
	fun hasOreo() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O       	 // Oreo          8.0   26

	@JvmStatic
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O_MR1)
	fun hasOMR() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1    	 // Oreo          8.1   27

	@JvmStatic
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.P)
	fun hasPie() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P       	 // Pie           9.0   28

	@JvmStatic
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
	fun hasQ() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q          	 // Q             10.0  29

	@JvmStatic
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
	fun hasR() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R          	 // Red_Velvet    11.0  30

	@JvmStatic
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
	fun hasSnowCone() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S   	 // Snow_cone     12.0  31

	@JvmStatic
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S_V2)
	fun hasSnowConeV2() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2  // Snow_cone_V2  12L 32

	@JvmStatic
	@ChecksSdkIntAtLeast(api = 33)
	fun hasTiramisu() = Build.VERSION.SDK_INT >= 33 						 					 // Tiramisu     	13.0  33

	@JvmStatic
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N)
	fun isNougat() = Build.VERSION.SDK_INT == Build.VERSION_CODES.N     	 // Nougat        7.0   24

	@JvmStatic
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.N_MR1)
	fun isNMR() = Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1    	 // Nougat        7.1   25

	@JvmStatic
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
	fun isOreo() = Build.VERSION.SDK_INT == Build.VERSION_CODES.O       	 // Oreo          8.0   26

	@JvmStatic
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O_MR1)
	fun isOMR() = Build.VERSION.SDK_INT == Build.VERSION_CODES.O_MR1    	 // Oreo        	8.1   27

	@JvmStatic
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.P)
	fun isPie() = Build.VERSION.SDK_INT == Build.VERSION_CODES.P        	 // Pie         	9.0   28

	@JvmStatic
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
	fun isQ() = Build.VERSION.SDK_INT == Build.VERSION_CODES.Q          	 // Q             10.0  29

	@JvmStatic
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
	fun isR() = Build.VERSION.SDK_INT == Build.VERSION_CODES.R          	 	// Red Velvet   11.0  30

	@JvmStatic
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
	fun isSnowCone() = Build.VERSION.SDK_INT == Build.VERSION_CODES.S   	 	// Snow_cone    12.0  31

	@JvmStatic
	@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S_V2)
	fun isSnowConeV2() = Build.VERSION.SDK_INT == Build.VERSION_CODES.S_V2  // Snow_cone_V2 12L   32

	@JvmStatic
	@ChecksSdkIntAtLeast(api = 33)
	fun isTiramisu() = Build.VERSION.SDK_INT == 33                          // Tiramisu 	  13.0  33
}

