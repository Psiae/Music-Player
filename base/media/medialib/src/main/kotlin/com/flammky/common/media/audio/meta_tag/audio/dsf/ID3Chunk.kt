package com.flammky.musicplayer.common.media.audio.meta_tag.audio.dsf

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import java.nio.ByteBuffer
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Created by Paul on 28/01/2016.
 */
class ID3Chunk(val dataBuffer: ByteBuffer) {

	companion object {
		var logger = Logger.getLogger("org.jaudiotagger.audio.generic.ID3Chunk")
		fun readChunk(dataBuffer: ByteBuffer): ID3Chunk? {
			val type = Utils.readThreeBytesAsChars(dataBuffer)
			if (DsfChunkType.ID3.code == type) {
				return ID3Chunk(dataBuffer)
			}
			logger.log(Level.WARNING, "Invalid type:$type where expected ID3 tag")
			return null
		}
	}
}
