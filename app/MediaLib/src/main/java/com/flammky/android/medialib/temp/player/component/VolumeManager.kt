package com.flammky.android.medialib.temp.player.component

abstract class VolumeManager {
	abstract var internalVolume: Float
	abstract var deviceVolume: Int

	abstract var deviceMuted: Boolean
}
