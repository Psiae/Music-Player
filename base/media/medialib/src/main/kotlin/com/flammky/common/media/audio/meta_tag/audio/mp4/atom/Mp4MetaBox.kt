package com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.Mp4AtomIdentifier
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import java.nio.ByteBuffer

/**
 * This MP4 MetaBox is the parent of metadata, it usually contains four bytes of data
 * that needs to be processed before we can examine the children. But I also have a file that contains
 * meta (and no udta) that does not have this children data.
 */
class Mp4MetaBox(header: Mp4BoxHeader?, dataBuffer: ByteBuffer) : AbstractMp4Box() {
	/**
	 * @param header     header info
	 * @param dataBuffer data of box (doesn't include header data)
	 */
	init {
		this.header = header
		this.data = dataBuffer
	}

	@Throws(CannotReadException::class)
	fun processData() {
		//4-skip the meta flags and check they are the meta flags
		val b = ByteArray(FLAGS_LENGTH)
		data!![b]
		if (b[0].toInt() != 0) {
			throw CannotReadException(ErrorMessage.MP4_FILE_META_ATOM_CHILD_DATA_NOT_NULL.msg)
		}
	}

	companion object {
		const val FLAGS_LENGTH = 4

		/**
		 * Create an iTunes style Meta box
		 *
		 *
		 * Useful when writing to mp4 that previously didn't contain an mp4 meta atom
		 *
		 * @param childrenSize
		 * @return
		 */
		@JvmStatic
		fun createiTunesStyleMetaBox(childrenSize: Int): Mp4MetaBox {
			val metaHeader =
				Mp4BoxHeader(
					Mp4AtomIdentifier.META.fieldName
				)
			metaHeader.length = (Mp4BoxHeader.Companion.HEADER_LENGTH + FLAGS_LENGTH + childrenSize)
			val metaData =
				ByteBuffer.allocate(FLAGS_LENGTH)
			return Mp4MetaBox(metaHeader, metaData)
		}
	}
}
