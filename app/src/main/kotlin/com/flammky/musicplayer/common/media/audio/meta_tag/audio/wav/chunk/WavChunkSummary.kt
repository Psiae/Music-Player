package com.flammky.musicplayer.common.media.audio.meta_tag.audio.wav.chunk

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.iff.ChunkSummary
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.wav.WavChunkType
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.wav.WavTag
import java.util.logging.Logger

/**
 * AIFF Specific methods for ChunkSummarys
 */
object WavChunkSummary {
	// Logger Object
	var logger = Logger.getLogger("org.jaudiotagger.audio.wav.chunk")

	/**
	 * Get start location in file of first metadata chunk (could be LIST or ID3)
	 *
	 * @param tag
	 * @return
	 */
	fun getStartLocationOfFirstMetadataChunk(tag: WavTag): Long {
		//Work out the location of the first metadata tag (could be id3 or LIST tag)
		return if (tag.metadataChunkSummaryList.size > 0) {
			tag.metadataChunkSummaryList[0].fileStartLocation
		} else -1
	}

	/**
	 * Checks that there are only metadata tags after the currently selected metadata because this means its safe to truncate
	 * the remainder of the file.
	 *
	 * @param tag
	 * @return
	 */
	@JvmStatic
	fun isOnlyMetadataTagsAfterStartingMetadataTag(tag: WavTag): Boolean {
		val startLocationOfMetadatTag = getStartLocationOfFirstMetadataChunk(tag)
		if (startLocationOfMetadatTag == -1L) {
			logger.severe("Unable to find any metadata tags !")
			return false
		}
		var firstMetadataTag = false
		for (cs in tag.getChunkSummaryList()) {
			if (firstMetadataTag) {
				if (cs.chunkId != WavChunkType.ID3.code &&
					cs.chunkId != WavChunkType.ID3_UPPERCASE.code &&
					cs.chunkId != WavChunkType.LIST.code &&
					cs.chunkId != WavChunkType.INFO.code
				) {
					return false
				}
			} else {
				if (cs.fileStartLocation == startLocationOfMetadatTag) {
					//Found starting point
					firstMetadataTag = true
				}
			}
		}

		//Should always be true but this is to protect against something gone wrong
		return firstMetadataTag == true
	}

	/**
	 * Get chunk before the first metadata tag
	 *
	 * @param tag
	 * @return
	 */
	@JvmStatic
	fun getChunkBeforeFirstMetadataTag(tag: WavTag): ChunkSummary? {
		val startLocationOfMetadatTag = getStartLocationOfFirstMetadataChunk(tag)
		for (i in tag.getChunkSummaryList().indices) {
			val cs = tag.getChunkSummaryList()[i]
			if (cs.fileStartLocation == startLocationOfMetadatTag) {
				return tag.getChunkSummaryList()[i - 1]
			}
		}
		return null
	}
}
