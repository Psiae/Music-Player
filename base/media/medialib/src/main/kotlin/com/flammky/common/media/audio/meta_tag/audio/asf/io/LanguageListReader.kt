package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.io

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.Chunk
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.GUID
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.LanguageList
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util.Utils
import java.io.IOException
import java.io.InputStream

/**
 * Reads and interprets the &quot;Language List Object&quot; of ASF files.<br></br>
 *
 * @author Christian Laireiter
 */
class LanguageListReader : ChunkReader {
	/**
	 * {@inheritDoc}
	 */
	override fun canFail(): Boolean {
		return false
	}

	/**
	 * {@inheritDoc}
	 */
	override val applyingIds: Array<GUID>
		get() = APPLYING.clone()

	/**
	 * {@inheritDoc}
	 */
	@Throws(IOException::class)
	override fun read(guid: GUID?, stream: InputStream, streamPosition: Long): Chunk? {
		assert(GUID.GUID_LANGUAGE_LIST.equals(guid))
		val chunkLen = Utils.readBig64(stream)
		val readUINT16 = Utils.readUINT16(stream)
		val result = LanguageList(streamPosition, chunkLen)
		for (i in 0 until readUINT16) {
			val langIdLen = stream.read() and 0xFF
			val langId = Utils.readFixedSizeUTF16Str(stream, langIdLen)
			assert(langId.length == langIdLen / 2 - 1 || langId.length == langIdLen / 2)
			result.addLanguage(langId)
		}
		return result
	}

	companion object {
		/**
		 * The GUID this reader [applies to][.getApplyingIds]
		 */
		private val APPLYING = arrayOf(GUID.GUID_LANGUAGE_LIST)
	}
}
