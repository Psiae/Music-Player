package com.flammky.musicplayer.common.media.audio.meta_tag.audio.real

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.readString
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils.readUint32
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

class RealChunk(val id: String, val size: Int, val bytes: ByteArray) {

	val dataInputStream: DataInputStream
		get() = DataInputStream(ByteArrayInputStream(bytes))
	val isCONT: Boolean
		get() = CONT == id
	val isPROP: Boolean
		get() = PROP == id

	override fun toString(): String {
		return id + "\t" + size
	}

	companion object {
		protected const val RMF = ".RMF"
		protected const val PROP = "PROP"
		protected const val MDPR = "MDPR"
		protected const val CONT = "CONT"
		protected const val DATA = "DATA"
		protected const val INDX = "INDX"

		@Throws(CannotReadException::class, IOException::class)
		fun readChunk(raf: RandomAccessFile?): RealChunk {
			val id = readString(
				raf!!, 4
			)
			val size = readUint32(
				raf
			).toInt()
			if (size < 8) {
				throw CannotReadException(
					"Corrupt file: RealAudio chunk length at position "
						+ (raf.filePointer - 4)
						+ " cannot be less than 8"
				)
			}
			if (size > raf.length() - raf.filePointer + 8) {
				throw CannotReadException(
					"Corrupt file: RealAudio chunk length of " + size
						+ " at position " + (raf.filePointer - 4)
						+ " extends beyond the end of the file"
				)
			}
			val bytes = ByteArray(size - 8)
			raf.readFully(bytes)
			return RealChunk(id, size, bytes)
		}

		fun readChunk(fc: FileChannel): RealChunk {
			val id = readString(fc, 4)
			val size = readUint32(fc).toInt()
			if (size < 8) {
				throw CannotReadException(
					"Corrupt file: RealAudio chunk length at position "
						+ (fc.position() - 4)
						+ " cannot be less than 8"
				)
			}
			if (size > fc.size() - fc.position() + 8) {
				throw CannotReadException(
					"Corrupt file: RealAudio chunk length of " + size
						+ " at position " + (fc.position() - 4)
						+ " extends beyond the end of the file"
				)
			}
			val bytes = ByteArray(size - 8)
			fc.read(ByteBuffer.wrap(bytes))
			return RealChunk(id, size, bytes)
		}
	}
}
