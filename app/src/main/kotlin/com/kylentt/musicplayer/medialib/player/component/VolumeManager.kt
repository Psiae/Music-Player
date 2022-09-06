package com.kylentt.musicplayer.medialib.player.component

abstract class VolumeManager {
	abstract var internalVolume: Float
	abstract var deviceVolume: Int

	abstract var deviceMuted: Boolean
}
