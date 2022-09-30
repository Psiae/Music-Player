package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.DataTypes
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v24Frames
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.TextEncoding
import java.nio.ByteBuffer

/**
 * Is part of a Compilation (iTunes frame)
 *
 *
 * determines whether or not track is part of compilation
 *
 * @author : Paul Taylor
 */
class FrameBodyTCMP : AbstractFrameBodyTextInfo, ID3v24FrameBody, ID3v23FrameBody {

	override val identifier: String?
		get() = ID3v24Frames.FRAME_ID_IS_COMPILATION

	/**
	 * Creates a new FrameBodyTCMP datatype, with compilation enabled
	 *
	 * This is the preferred constructor to use because TCMP frames should not exist
	 * unless they are set to true
	 */
	constructor() {
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, TextEncoding.ISO_8859_1)
		setObjectValue(DataTypes.OBJ_TEXT, IS_COMPILATION)
	}

	constructor(body: FrameBodyTCMP) : super(body)

	/**
	 * Creates a new FrameBodyTCMP datatype.
	 *
	 * @param textEncoding
	 * @param text
	 */
	constructor(textEncoding: Byte, text: String?) : super(textEncoding, text)

	val isCompilation: Boolean
		get() = text == IS_COMPILATION

	/**
	 * Creates a new FrameBodyTIT1 datatype.
	 *
	 * @param byteBuffer
	 * @param frameSize
	 * @throws InvalidTagException
	 */
	constructor(byteBuffer: ByteBuffer, frameSize: Int) : super(byteBuffer, frameSize)

	companion object {
		//TODO does iTunes have to have null terminator?
		var IS_COMPILATION = "1\u0000"
	}
}
