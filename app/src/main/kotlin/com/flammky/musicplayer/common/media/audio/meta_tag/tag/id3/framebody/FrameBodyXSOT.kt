package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v23Frames
import java.nio.ByteBuffer

/**
 * Title Sort name, this is what MusicBrainz uses in ID3v23 because TSOT not supported.
 *
 * However iTunes uses TSOT even in ID3v23, so we have two possible options
 */
class FrameBodyXSOT : AbstractFrameBodyTextInfo, ID3v23FrameBody {


	override val identifier: String
		get() = ID3v23Frames.FRAME_ID_V3_TITLE_SORT_ORDER_MUSICBRAINZ

	/**
	 * Creates a new FrameBodyTSOT datatype.
	 */
	constructor()
	constructor(body: FrameBodyXSOT) : super(body)

	/**
	 * Creates a new FrameBodyTSOT datatype.
	 *
	 * @param textEncoding
	 * @param text
	 */
	constructor(textEncoding: Byte, text: String?) : super(textEncoding, text)

	/**
	 * Creates a new FrameBodyTSOT datatype.
	 *
	 * @param byteBuffer
	 * @param frameSize
	 * @throws InvalidTagException
	 */
	constructor(byteBuffer: ByteBuffer, frameSize: Int) : super(byteBuffer, frameSize)
}
