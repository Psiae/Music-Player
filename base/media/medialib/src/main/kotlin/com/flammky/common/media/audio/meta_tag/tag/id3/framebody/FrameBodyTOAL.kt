package com.flammky.common.media.audio.meta_tag.tag.id3.framebody

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v24Frames
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody.AbstractFrameBodyTextInfo
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody.ID3v23FrameBody
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody.ID3v24FrameBody
import java.nio.ByteBuffer

/**
 * Original album/movie/show title Text information frame.
 *
 * The 'Original album/movie/show title' frame is intended for the title of the original recording (or source of sound), if for example the music
 * in the file should be a cover of a previously released song.
 *
 *
 * For more details, please refer to the ID3 specifications:
 *
 *  * [ID3 v2.3.0 Spec](http://www.id3.org/id3v2.3.0.txt)
 *
 *
 * @author : Paul Taylor
 * @author : Eric Farng
 * @version $Id$
 */
class FrameBodyTOAL : AbstractFrameBodyTextInfo, ID3v23FrameBody, ID3v24FrameBody {


	/**
	 * Creates a new FrameBodyTOAL datatype.
	 */
	constructor()
	constructor(body: FrameBodyTOAL) : super(body)

	/**
	 * Creates a new FrameBodyTOAL datatype.
	 *
	 * @param textEncoding
	 * @param text
	 */
	constructor(textEncoding: Byte, text: String?) : super(textEncoding, text)

	/**
	 * Creates a new FrameBodyTOAL datatype.
	 *
	 * @param byteBuffer
	 * @param frameSize
	 * @throws InvalidTagException
	 */
	constructor(byteBuffer: ByteBuffer, frameSize: Int) : super(byteBuffer, frameSize)


	override val identifier: String?
		get() = ID3v24Frames.FRAME_ID_ORIG_TITLE
}
