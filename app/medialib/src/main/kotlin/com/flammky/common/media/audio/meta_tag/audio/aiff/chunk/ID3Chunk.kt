package com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff.chunk

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.AudioFile
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.Chunk
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.ChunkHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.aiff.AiffTag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v2TagBase
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v22Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v23Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v24Tag
import java.io.IOException
import java.nio.ByteBuffer
import java.util.logging.Logger

/**
 * Contains the ID3 tags.
 */
class ID3Chunk(
	chunkHeader: ChunkHeader,
	chunkData: ByteBuffer,
	val aiffTag: AiffTag,
	val loggingName: String
) : Chunk(chunkData, chunkHeader) {

	@Throws(IOException::class)
	override fun readChunk(): Boolean {
		AudioFile.logger.config(
			"$loggingName:Reading chunk"
		)
		if (!isId3v2Tag(chunkData)) {
			logger.severe(
				"$loggingName:Invalid ID3 header for ID3 chunk"
			)
			return false
		}
		val version = chunkData.get()
		val id3Tag: ID3v2TagBase
		when (version) {
			ID3v22Tag.MAJOR_VERSION -> {
				id3Tag = ID3v22Tag()
				AudioFile.logger.config(
					"$loggingName:Reading ID3V2.2 tag"
				)
			}
			ID3v23Tag.MAJOR_VERSION -> {
				id3Tag = ID3v23Tag()
				AudioFile.logger.config(
					"$loggingName:Reading ID3V2.3 tag"
				)
			}
			ID3v24Tag.MAJOR_VERSION -> {
				id3Tag = ID3v24Tag()
				AudioFile.logger.config(
					"$loggingName:Reading ID3V2.4 tag"
				)
			}
			else -> return false // bad or unknown version
		}
		aiffTag.iD3Tag = id3Tag
		chunkData.position(0)
		try {
			id3Tag.read(chunkData)
		} catch (e: TagException) {
			AudioFile.logger.severe(
				loggingName + ":Exception reading ID3 tag: " + e.javaClass.name + ": " + e.message
			)
			return false
		}
		return true
	}

	/**
	 * Reads 3 bytes to determine if the tag really looks like ID3 data.
	 */
	@Throws(IOException::class)
	private fun isId3v2Tag(headerData: ByteBuffer): Boolean {
		for (i in 0 until ID3v2TagBase.FIELD_TAGID_LENGTH) {
			if (headerData.get() != ID3v2TagBase.TAG_ID[i]) {
				return false
			}
		}
		return true
	}

	companion object {
		var logger = Logger.getLogger("org.jaudiotagger.audio.aiff.chunk")
	}

}
