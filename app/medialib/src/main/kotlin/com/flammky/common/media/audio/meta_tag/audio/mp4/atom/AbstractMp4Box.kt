package com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom

import java.nio.ByteBuffer

/**
 * Abstract mp4 box, contain a header and then rawdata (which may include child boxes)
 */
open class AbstractMp4Box {
	/**
	 * @return the box header
	 */
	var header: Mp4BoxHeader? = null
		protected set

	/**
	 * @return rawdata of this box
	 */
	var data: ByteBuffer? = null
		protected set
}
