package com.flammky.musicplayer.domain.musiclib.player.exoplayer

import com.flammky.common.kotlin.comparable.clamp

object PlayerHelper {
	private const val minVol = PlayerConstants.MIN_VOLUME
	private const val maxVol = PlayerConstants.MAX_VOLUME

	/**
	 * @param [vol] the requested Volume as [Float]
	 * @return proper Volume according to [androidx.media3.session.MediaController.setVolume] range
	 */

	fun fixVolumeToRange(vol: Float): Float = vol.clamp(minVol, maxVol)
}
