package com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.getIntBE
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.getSizeBEInt32
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.Mp4AtomIdentifier
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * StcoBox ( media (stream) header), holds offsets into the Audio data
 */
class Mp4StcoBox : AbstractMp4Box {

	var dataBuffer
		get() = super.data
		set(value) {
			super.data = value
		}

	/**
	 * The number of offsets
	 *
	 * @return
	 */
	var noOfOffSets = 0
		private set

	/**
	 * The value of the first offset
	 *
	 * @return
	 */
	var firstOffSet = 0
		private set

	/**
	 * Construct box from data and show contents
	 *
	 * @param header header info
	 * @param buffer data of box (doesnt include header data)
	 */
	constructor(header: Mp4BoxHeader?, buffer: ByteBuffer) {
		this.header = header

		//Make a slice of databuffer then we can work with relative or absolute methods safetly

		dataBuffer = buffer.slice()
		val dataBuffer = dataBuffer!!
		dataBuffer.order(ByteOrder.BIG_ENDIAN)
		//Skip the flags
		dataBuffer.position(dataBuffer.position() + VERSION_FLAG_LENGTH + OTHER_FLAG_LENGTH)

		//No of offsets
		noOfOffSets = dataBuffer.int

		//First Offset, useful for sanity checks
		firstOffSet = dataBuffer.int
	}

	fun printTotalOffset() {
		val dataBuffer = dataBuffer!!
		var offset = 0
		dataBuffer.rewind()
		dataBuffer.position(VERSION_FLAG_LENGTH + OTHER_FLAG_LENGTH + NO_OF_OFFSETS_LENGTH)
		for (i in 0 until noOfOffSets - 1) {
			offset += getIntBE(
				dataBuffer,
				dataBuffer.position(),
				dataBuffer.position() + OFFSET_LENGTH - 1
			)
			dataBuffer.position(dataBuffer.position() + OFFSET_LENGTH)
		}
		offset += getIntBE(
			dataBuffer,
			dataBuffer.position(),
			dataBuffer.position() + OFFSET_LENGTH - 1
		)
		println("Print Offset Total:$offset")
	}

	/**
	 * Show All offsets, useful for debugging
	 */
	fun printAllOffsets() {
		val dataBuffer = dataBuffer!!

		println("Print Offsets:start")
		dataBuffer.rewind()
		dataBuffer.position(VERSION_FLAG_LENGTH + OTHER_FLAG_LENGTH + NO_OF_OFFSETS_LENGTH)
		for (i in 0 until noOfOffSets - 1) {
			val offset = dataBuffer.int
			println("offset into audio data is:$offset")
		}
		val offset = dataBuffer.int
		println("offset into audio data is:$offset")
		println("Print Offsets:end")
	}

	fun adjustOffsets(adjustment: Int) {
		val dataBuffer = dataBuffer!!

		//Skip the flags
		dataBuffer.rewind()
		dataBuffer.position(dataBuffer.position() + VERSION_FLAG_LENGTH + OTHER_FLAG_LENGTH + NO_OF_OFFSETS_LENGTH)
		for (i in 0 until noOfOffSets) {
			var offset = dataBuffer.int

			//Calculate new offset and update buffer
			offset = offset + adjustment
			dataBuffer.position(dataBuffer.position() - OFFSET_LENGTH)
			dataBuffer.putInt(offset)
		}
	}

	/**
	 * Construct box from data and adjust offets accordingly
	 *
	 * @param header             header info
	 * @param originalDataBuffer data of box (doesnt include header data)
	 * @param adjustment
	 */
	constructor(header: Mp4BoxHeader?, originalDataBuffer: ByteBuffer, adjustment: Int) {
		this.header = header

		//Make a slice of databuffer then we can work with relative or absolute methods safetly
		dataBuffer = originalDataBuffer.slice()
		val dataBuffer = dataBuffer!!

		//Skip the flags
		dataBuffer.position(dataBuffer.position() + VERSION_FLAG_LENGTH + OTHER_FLAG_LENGTH)

		//No of offsets
		noOfOffSets = getIntBE(
			dataBuffer,
			dataBuffer.position(),
			dataBuffer.position() + NO_OF_OFFSETS_LENGTH - 1
		)
		dataBuffer.position(dataBuffer.position() + NO_OF_OFFSETS_LENGTH)
		for (i in 0 until noOfOffSets) {
			var offset = getIntBE(
				dataBuffer,
				dataBuffer.position(),
				dataBuffer.position() + NO_OF_OFFSETS_LENGTH - 1
			)

			//Calculate new offset and update buffer
			offset = offset + adjustment
			dataBuffer.put(getSizeBEInt32(offset))
		}
	}

	companion object {
		const val VERSION_FLAG_POS = 0
		const val OTHER_FLAG_POS = 1
		const val NO_OF_OFFSETS_POS = 4
		const val VERSION_FLAG_LENGTH = 1
		const val OTHER_FLAG_LENGTH = 3
		const val NO_OF_OFFSETS_LENGTH = 4
		const val OFFSET_LENGTH = 4

		@Throws(
			IOException::class,
			CannotReadException::class
		)
		fun getStco(raf: RandomAccessFile): Mp4StcoBox {
			val fc = raf.channel
			val moovHeader: Mp4BoxHeader =
				Mp4BoxHeader.Companion.seekWithinLevel(
					fc,
					Mp4AtomIdentifier.MOOV.fieldName
				)
					?: throw CannotReadException(
						"This file does not appear to be an audio file"
					)
			val moovBuffer =
				ByteBuffer.allocate(moovHeader.length - Mp4BoxHeader.HEADER_LENGTH)
			fc.read(moovBuffer)
			moovBuffer.rewind()

			//Level 2-Searching for "mvhd" somewhere within "moov", we make a slice after finding header
			//so all getFields() methods will be relative to mvdh positions
			var boxHeader: Mp4BoxHeader? =
				Mp4BoxHeader.Companion.seekWithinLevel(
					moovBuffer,
					Mp4AtomIdentifier.MVHD.fieldName
				)
					?: throw CannotReadException(
						"This file does not appear to be an audio file"
					)
			val mvhdBuffer = moovBuffer.slice()
			mvhdBuffer.position(mvhdBuffer.position() + (boxHeader?.dataLength ?: 0))

			//Level 2-Searching for "trak" within "moov"
			boxHeader =
				Mp4BoxHeader.Companion.seekWithinLevel(mvhdBuffer, Mp4AtomIdentifier.TRAK.fieldName)
			if (boxHeader == null) {
				throw CannotReadException(
					"This file does not appear to be an audio file"
				)
			}
			//Level 3-Searching for "mdia" within "trak"
			boxHeader =
				Mp4BoxHeader.Companion.seekWithinLevel(
					mvhdBuffer,
					Mp4AtomIdentifier.MDIA.fieldName
				)
			if (boxHeader == null) {
				throw CannotReadException(
					"This file does not appear to be an audio file"
				)
			}

			//Level 4-Searching for "mdhd" within "mdia"
			boxHeader =
				Mp4BoxHeader.Companion.seekWithinLevel(
					mvhdBuffer,
					Mp4AtomIdentifier.MDHD.fieldName
				)
			if (boxHeader == null) {
				throw CannotReadException(
					"This file does not appear to be an audio file"
				)
			}

			//Level 4-Searching for "minf" within "mdia"
			mvhdBuffer.position(mvhdBuffer.position() + boxHeader.dataLength)
			boxHeader =
				Mp4BoxHeader.Companion.seekWithinLevel(
					mvhdBuffer,
					Mp4AtomIdentifier.MINF.fieldName
				)
			if (boxHeader == null) {
				throw CannotReadException(
					"This file does not appear to be an audio file"
				)
			}

			//Level 5-Searching for "smhd" within "minf"
			//Only an audio track would have a smhd frame
			boxHeader =
				Mp4BoxHeader.Companion.seekWithinLevel(
					mvhdBuffer,
					Mp4AtomIdentifier.SMHD.fieldName
				)
			if (boxHeader == null) {
				throw CannotReadException(
					"This file does not appear to be an audio file"
				)
			}
			mvhdBuffer.position(mvhdBuffer.position() + boxHeader.dataLength)

			//Level 5-Searching for "stbl within "minf"
			boxHeader =
				Mp4BoxHeader.Companion.seekWithinLevel(
					mvhdBuffer,
					Mp4AtomIdentifier.STBL.fieldName
				)
			if (boxHeader == null) {
				throw CannotReadException(
					"This file does not appear to be an audio file"
				)
			}

			//Level 6-Searching for "stco within "stbl"
			boxHeader =
				Mp4BoxHeader.Companion.seekWithinLevel(
					mvhdBuffer,
					Mp4AtomIdentifier.STCO.fieldName
				)
			if (boxHeader == null) {
				throw CannotReadException(
					"This file does not appear to be an audio file"
				)
			}
			return Mp4StcoBox(boxHeader, mvhdBuffer)
		}

		@Throws(IOException::class, CannotReadException::class)
		fun debugShowStcoInfo(raf: RandomAccessFile) {
			val stco = getStco(raf)
			stco.printAllOffsets()
		}
	}
}
