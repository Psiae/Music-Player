package com.flammky.musicplayer.common.media.audio.meta_tag.audio.real

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.SupportedFileFormat
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.AudioFileReader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.GenericAudioHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.readString
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.readUint16
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.readUint32
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.FieldDataInvalidException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.FieldKey
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.Tag
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.FileChannel

/**
 * Real Media File Format: Major Chunks: .RMF PROP MDPR CONT DATA INDX
 */
class RealFileReader : AudioFileReader() {

	@Throws(CannotReadException::class, IOException::class)
	override fun getEncodingInfo(raf: RandomAccessFile): GenericAudioHeader {
		val info = GenericAudioHeader()
		val prop = findPropChunk(raf)
		val dis = prop.dataInputStream
		val objVersion = readUint16(
			dis
		)
		if (objVersion == 0) {
			val maxBitRate = readUint32(
				dis
			) / 1000
			val avgBitRate = readUint32(
				dis
			) / 1000
			val maxPacketSize = readUint32(
				dis
			)
			val avgPacketSize = readUint32(
				dis
			)
			val packetCnt = readUint32(
				dis
			)
			val duration = readUint32(
				dis
			).toInt() / 1000
			val preroll = readUint32(
				dis
			)
			val indexOffset = readUint32(
				dis
			)
			val dataOffset = readUint32(
				dis
			)
			val numStreams = readUint16(
				dis
			)
			val flags = readUint16(
				dis
			)
			info.setBitRate(avgBitRate.toInt())
			info.preciseTrackLength = (duration.toDouble())
			info.isVariableBitRate = maxBitRate != avgBitRate
			info.format = (SupportedFileFormat.RA.displayName)
		}
		return info
	}

	override fun getEncodingInfo(fc: FileChannel): GenericAudioHeader {
		val info = GenericAudioHeader()
		val prop = RealChunk.Companion.readChunk(fc)
		val dis = prop.dataInputStream
		val objVersion = readUint16(dis)
		if (objVersion == 0) {
			val maxBitRate = readUint32(
				dis
			) / 1000
			val avgBitRate = readUint32(
				dis
			) / 1000
			val maxPacketSize = readUint32(
				dis
			)
			val avgPacketSize = readUint32(
				dis
			)
			val packetCnt = readUint32(
				dis
			)
			val duration = readUint32(
				dis
			).toInt() / 1000
			val preroll = readUint32(
				dis
			)
			val indexOffset = readUint32(
				dis
			)
			val dataOffset = readUint32(
				dis
			)
			val numStreams = readUint16(
				dis
			)
			val flags = readUint16(
				dis
			)
			info.setBitRate(avgBitRate.toInt())
			info.preciseTrackLength = (duration.toDouble())
			info.isVariableBitRate = maxBitRate != avgBitRate
			info.format = (SupportedFileFormat.RA.displayName)
		}
		return info
	}

	@Throws(
		IOException::class,
		CannotReadException::class
	)
	private fun findPropChunk(raf: RandomAccessFile?): RealChunk {
		val rmf: RealChunk =
			RealChunk.Companion.readChunk(
				raf
			)
		return RealChunk.Companion.readChunk(raf)
	}

	@Throws(IOException::class, CannotReadException::class)
	private fun findContChunk(raf: RandomAccessFile?): RealChunk {
		val rmf: RealChunk = RealChunk.Companion.readChunk(raf)
		val prop: RealChunk = RealChunk.Companion.readChunk(raf)
		var rv: RealChunk = RealChunk.Companion.readChunk(raf)
		while (!rv.isCONT) rv = RealChunk.Companion.readChunk(raf)
		return rv
	}

	@Throws(CannotReadException::class, IOException::class)
	override fun getTag(raf: RandomAccessFile): Tag {
		val cont = findContChunk(raf)
		val dis = cont.dataInputStream
		val title = readString(
			dis, readUint16(
				dis
			)
		)
		val author = readString(
			dis, readUint16(
				dis
			)
		)
		val copyright = readString(
			dis, readUint16(
				dis
			)
		)
		val comment = readString(
			dis, readUint16(
				dis
			)
		)
		val rv = RealTag()
		// NOTE: frequently these fields are off-by-one, thus the crazy
		// logic below...
		try {
			rv.addField(FieldKey.TITLE, title.ifEmpty { author })
			rv.addField(FieldKey.ARTIST, if (title.isEmpty()) copyright else author)
			rv.addField(FieldKey.COMMENT, comment)
		} catch (fdie: FieldDataInvalidException) {
			throw RuntimeException(fdie)
		}
		return rv
	}

	override fun getTag(fc: FileChannel): Tag {
		val cont = run {
			val rmf: RealChunk = RealChunk.Companion.readChunk(fc)
			val prop: RealChunk = RealChunk.Companion.readChunk(fc)
			var rv: RealChunk = RealChunk.Companion.readChunk(fc)
			while (!rv.isCONT) rv = RealChunk.Companion.readChunk(fc)
			rv
		}
		val dis = cont.dataInputStream
		val title = readString(
			dis, readUint16(
				dis
			)
		)
		val author = readString(
			dis, readUint16(
				dis
			)
		)
		val copyright = readString(
			dis, readUint16(
				dis
			)
		)
		val comment = readString(
			dis, readUint16(
				dis
			)
		)
		val rv = RealTag()
		// NOTE: frequently these fields are off-by-one, thus the crazy
		// logic below...
		try {
			rv.addField(FieldKey.TITLE, title.ifEmpty { author })
			rv.addField(FieldKey.ARTIST, if (title.isEmpty()) copyright else author)
			rv.addField(FieldKey.COMMENT, comment)
		} catch (fdie: FieldDataInvalidException) {
			throw RuntimeException(fdie)
		}
		return rv
	}
}
