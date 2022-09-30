package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v23Frames
import java.nio.ByteBuffer

/**
 * Album Sort name, this is what MusicBrainz uses in ID3v23 because TSOA not supported.
 *
 * However iTunes uses TSOA even in ID3v23, so we have two possible options
 */
class FrameBodyXSOA : AbstractFrameBodyTextInfo, ID3v23FrameBody {

	override val identifier: String
		get() = ID3v23Frames.FRAME_ID_V3_ALBUM_SORT_ORDER_MUSICBRAINZ

	/**
	 * Creates a new FrameBodyTSOT datatype.
	 */
	constructor()
	constructor(body: FrameBodyXSOA) : super(body)

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
