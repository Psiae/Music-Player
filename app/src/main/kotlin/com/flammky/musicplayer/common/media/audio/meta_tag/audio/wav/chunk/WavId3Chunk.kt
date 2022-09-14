package com.flammky.musicplayer.common.media.audio.meta_tag.audio.wav.chunk

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.AudioFile
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.Chunk
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.ChunkHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v2TagBase
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v22Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v23Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v24Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.wav.WavTag
import java.io.IOException
import java.nio.ByteBuffer
import java.util.logging.Logger

/**
 * Contains the ID3 tags.
 */
class WavId3Chunk
/**
 * Constructor.
 * @param chunkData  The content of this chunk
 * @param chunkHeader        The header for this chunk
 * @param tag        The WavTag into which information is stored
 * @param loggingName
 */(
	chunkData: ByteBuffer?,
	chunkHeader: ChunkHeader?,
	private val wavTag: WavTag,
	private val loggingName: String
) : Chunk(
	chunkData!!, chunkHeader!!
) {
	@Throws(IOException::class)
	override fun readChunk(): Boolean {
		if (!isId3v2Tag(chunkData)) {
			logger.severe("Invalid ID3 header for ID3 chunk")
			return false
		}
		val version = chunkData.get().toInt()
		val id3Tag: ID3v2TagBase
		when (version) {
			ID3v22Tag.MAJOR_VERSION.toInt() -> {
				id3Tag = ID3v22Tag()
				id3Tag.loggingFilename = loggingName
			}
			ID3v23Tag.MAJOR_VERSION.toInt() -> {
				id3Tag = ID3v23Tag()
				id3Tag.loggingFilename = loggingName
			}
			ID3v24Tag.MAJOR_VERSION.toInt() -> {
				id3Tag = ID3v24Tag()
				id3Tag.loggingFilename = loggingName
			}
			else -> return false // bad or unknown version
		}
		id3Tag.setStartLocationInFile(chunkHeader.startLocationInFile + ChunkHeader.CHUNK_HEADER_SIZE)
		id3Tag.setEndLocationInFile(chunkHeader.startLocationInFile + ChunkHeader.CHUNK_HEADER_SIZE + chunkHeader.size)
		wavTag.isExistingId3Tag = true
		wavTag.iD3Tag = id3Tag
		chunkData.position(0)
		try {
			id3Tag.read(chunkData)
		} catch (e: TagException) {
			AudioFile.logger.info("Exception reading ID3 tag: " + e.javaClass.name + ": " + e.message)
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
		var logger = Logger.getLogger("org.jaudiotagger.audio.wav.chunk")
	}
}
