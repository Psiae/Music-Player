package com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype

/**
 * A pair of values
 *
 * USed by TIPL, TMCL and IPLS frames that store pairs of values
 */
class Pair(key: String?, value: String?) {
	var key: String? = null
	var value: String? = null

	init {
		this.key = key
		this.value = value
	}

	val pairValue: String
		get() = key + '\u0000' + value
}
