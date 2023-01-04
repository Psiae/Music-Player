/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Raphaël Slinckx <raphael@slinckx.net>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.AudioFile
import com.flammky.musicplayer.common.media.audio.meta_tag.utils.FileTypeUtil.getMagicExt
import com.flammky.musicplayer.common.media.audio.meta_tag.utils.FileTypeUtil.getMagicFileType
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.channels.ReadableByteChannel
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Contains various frequently used static functions in the different tag formats.
 *
 * @author Raphael Slinckx
 */

/** [org.jaudiotagger.audio.generic.Utils] */
object Utils {
	@JvmField
	var BITS_IN_BYTE_MULTIPLIER = 8

	@JvmField
	var KILOBYTE_MULTIPLIER = 1000
	private val logger = Logger.getLogger("org.jaudiotagger.audio.generic.utils")
	private const val MAX_BASE_TEMP_FILENAME_LENGTH = 20

	/**
	 * Returns the extension of the given file.
	 * The extension is empty if there is no extension
	 * The extension is the string after the last "."
	 *
	 * @param f The file whose extension is requested
	 * @return The extension of the given file
	 */
	@JvmStatic
	fun getExtension(f: File): String {
		val name = f.name.lowercase(Locale.getDefault())
		val i = name.lastIndexOf(".")
		return if (i == -1) {
			""
		} else name.substring(i + 1)
	}

	/**
	 * Returns the extension of the given file based on the file signature.
	 * The extension is empty if the file signature is not recognized.
	 *
	 * @param f The file whose extension is requested
	 * @return The extension of the given file
	 */
	@JvmStatic
	@Throws(IOException::class)
	fun getMagicExtension(f: File): String? {
		val fileType = getMagicFileType(f)
		return getMagicExt(fileType)
	}

	fun getMagicExtension(fd: FileDescriptor): String? {
		val fileType = getMagicFileType(fd)
		return getMagicExt(fileType)
	}

	fun getMagicExtension(fc: FileChannel): String? {
		val fileType = getMagicFileType(fc)
		return getMagicExt(fileType)
	}

	@Throws(IOException::class)
	fun fetchFromChannel(ch: ReadableByteChannel, size: Int): ByteBuffer {
		val buf = ByteBuffer.allocate(size)
		readFromChannel(ch, buf)
		buf.flip()
		return buf
	}

	@Throws(IOException::class)
	fun readFromChannel(channel: ReadableByteChannel, buffer: ByteBuffer): Int {
		val rem = buffer.position()
		while (channel.read(buffer) != -1 && buffer.hasRemaining());
		return buffer.position() - rem
	}

	/**
	 * Computes a number whereby the 1st byte is the least signifcant and the last
	 * byte is the most significant.
	 * So if storing a number which only requires one byte it will be stored in the first
	 * byte.
	 *
	 * @param b The byte array @param start The starting offset in b
	 * (b[offset]). The less significant byte @param end The end index
	 * (included) in b (b[end]). The most significant byte
	 * @return a long number represented by the byte sequence.
	 */
	fun getLongLE(b: ByteBuffer, start: Int, end: Int): Long {
		var number: Long = 0
		for (i in 0 until end - start + 1) {
			number += (b[start + i].toInt() and 0xFF shl i * 8).toLong()
		}
		return number
	}

	fun getLongLE(b: ByteArray, start: Int, end: Int): Long {
		return getLongLE(ByteBuffer.wrap(b), start, end)
	}

	/**
	 * Computes a number whereby the 1st byte is the most significant and the last
	 * byte is the least significant.
	 *
	 * So if storing a number which only requires one byte it will be stored in the last
	 * byte.
	 *
	 * Will fail if end - start >= 8, due to the limitations of the long type.
	 */
	fun getLongBE(b: ByteBuffer, start: Int, end: Int): Long {
		var number: Long = 0
		for (i in 0 until end - start + 1) {
			number += (b[end - i].toInt() and 0xFF).toLong() shl i * 8
		}
		return number
	}

	/**
	 * Computes a number whereby the 1st byte is the least significant and the last
	 * byte is the most significant. This version doesn't take a length,
	 * and it returns an int rather than a long.
	 *
	 * @param b The byte array. Maximum length for valid results is 4 bytes.
	 */
	fun getIntLE(b: ByteArray): Int {
		return getLongLE(ByteBuffer.wrap(b), 0, b.size - 1).toInt()
	}

	/**
	 * Computes a number whereby the 1st byte is the least significant and the last
	 * byte is the most significant. end - start must be no greater than 4.
	 *
	 * @param b The byte array
	 *
	 * @param start The starting offset in b (b[offset]). The less
	 * significant byte
	 *
	 * @param end The end index (included) in b (b[end])
	 *
	 * @return a int number represented by the byte sequence.
	 */
	@JvmStatic
	fun getIntLE(b: ByteArray?, start: Int, end: Int): Int {
		return getLongLE(ByteBuffer.wrap(b), start, end).toInt()
	}

	/**
	 * Computes a number whereby the 1st byte is the most significant and the last
	 * byte is the least significant.
	 *
	 * @param b The ByteBuffer
	 *
	 * @param start The starting offset in b. The less
	 * significant byte
	 *
	 * @param end The end index (included) in b
	 *
	 * @return an int number represented by the byte sequence.
	 */
	@JvmStatic
	fun getIntBE(b: ByteBuffer, start: Int, end: Int): Int {
		return getLongBE(b, start, end).toInt()
	}

	/**
	 * Computes a number whereby the 1st byte is the most significant and the last
	 * byte is the least significant.
	 *
	 * @param b The ByteBuffer
	 *
	 * @param start The starting offset in b. The less
	 * significant byte
	 *
	 * @param end The end index (included) in b
	 *
	 * @return a short number represented by the byte sequence.
	 */
	@JvmStatic
	fun getShortBE(b: ByteBuffer, start: Int, end: Int): Short {
		return getIntBE(b, start, end).toShort()
	}

	/**
	 * Convert int to byte representation - Big Endian (as used by mp4).
	 *
	 * @param size
	 * @return byte representation
	 */
	@JvmStatic
	fun getSizeBEInt32(size: Int): ByteArray {
		val b = ByteArray(4)
		b[0] = (size shr 24 and 0xFF).toByte()
		b[1] = (size shr 16 and 0xFF).toByte()
		b[2] = (size shr 8 and 0xFF).toByte()
		b[3] = (size and 0xFF).toByte()
		return b
	}

	/**
	 * Convert short to byte representation - Big Endian (as used by mp4).
	 *
	 * @param size  number to convert
	 * @return byte representation
	 */
	@JvmStatic
	fun getSizeBEInt16(size: Short): ByteArray {
		val b = ByteArray(2)
		b[0] = (size.toInt() shr 8 and 0xFF).toByte()
		b[1] = (size.toInt() and 0xFF).toByte()
		return b
	}

	/**
	 * Convert int to byte representation - Little Endian (as used by ogg vorbis).
	 *
	 * @param size   number to convert
	 * @return byte representation
	 */
	@JvmStatic
	fun getSizeLEInt32(size: Int): ByteArray {
		val b = ByteArray(4)
		b[0] = (size and 0xff).toByte()
		b[1] = ((size ushr 8).toLong() and 0xffL).toByte()
		b[2] = ((size ushr 16).toLong() and 0xffL).toByte()
		b[3] = ((size ushr 24).toLong() and 0xffL).toByte()
		return b
	}

	/**
	 * Convert a byte array to a Pascal string. The first byte is the byte count,
	 * followed by that many active characters.
	 *
	 * @param bb
	 * @return
	 * @throws IOException
	 */
	@Throws(IOException::class)
	fun readPascalString(bb: ByteBuffer): String {
		val len = u(bb.get()) //Read as unsigned value
		val buf = ByteArray(len)
		bb[buf]
		return String(buf, 0, len, StandardCharsets.ISO_8859_1)
	}

	/**
	 * Reads bytes from a ByteBuffer as if they were encoded in the specified CharSet.
	 *
	 * @param buffer
	 * @param offset offset from current position
	 * @param length size of data to process
	 * @param encoding
	 * @return
	 */
	fun getString(buffer: ByteBuffer, offset: Int, length: Int, encoding: Charset?): String {
		val b = ByteArray(length)
		buffer.position(buffer.position() + offset)
		buffer[b]
		return String(b, 0, length, encoding!!)
	}

	/**
	 * Reads bytes from a ByteBuffer as if they were encoded in the specified CharSet.
	 *
	 * @param buffer
	 * @param encoding
	 * @return
	 */
	fun getString(buffer: ByteBuffer, encoding: Charset?): String {
		val b = ByteArray(buffer.remaining())
		buffer[b]
		return String(b, 0, b.size, encoding!!)
	}

	/**
	 * Read a 32-bit big-endian unsigned integer using a DataInput.
	 *
	 * Reads 4 bytes but returns as long
	 */
	@JvmStatic
	@Throws(IOException::class)
	fun readUint32(di: DataInput): Long {
		val buf8 = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
		di.readFully(buf8, 4, 4)
		return ByteBuffer.wrap(buf8).long
	}

	fun readUint32(fc: FileChannel): Long {
		val buf8 = ByteBuffer.allocate(8)
		fc.read(buf8)
		buf8.position(4)
		return buf8.long
	}

	/**
	 * Read a 16-bit big-endian unsigned integer.
	 *
	 * Reads 2 bytes but returns as an integer
	 */
	@JvmStatic
	@Throws(IOException::class)
	fun readUint16(di: DataInput): Int {
		val buf = byteArrayOf(0x00, 0x00, 0x00, 0x00)
		di.readFully(buf, 2, 2)
		return ByteBuffer.wrap(buf).int
	}

	/**
	 * Read a string of a specified number of ASCII bytes.
	 */
	@JvmStatic
	@Throws(IOException::class)
	fun readString(di: DataInput, charsToRead: Int): String {
		val buf = ByteArray(charsToRead)
		di.readFully(buf)
		return String(buf, StandardCharsets.US_ASCII)
	}

	fun readString(channel: FileChannel, charsToRead: Int): String {
		val buf = ByteBuffer.allocate(charsToRead)
		channel.read(buf)
		return String(buf.array(), StandardCharsets.US_ASCII)
	}

	fun readString(buf: ByteBuffer, charsToRead: Int): String {
		return String(buf.array(), StandardCharsets.US_ASCII)
	}

	fun skip(buffer: ByteBuffer, count: Int): Int {
		val toSkip = buffer.remaining().coerceAtMost(count)
		buffer.position(buffer.position() + toSkip)
		return toSkip
	}

	/**
	 * Get a base for temp file, this should be long enough so that it easy to work out later what file the temp file
	 * was created for if it is left lying round, but not ridiculously long as this can cause problems with max filename
	 * limits and is not very useful.
	 *
	 * @param file
	 * @return
	 */
	fun getBaseFilenameForTempFile(file: File): String {
		val filename = getMinBaseFilenameAllowedForTempFile(file)
		return if (filename.length <= MAX_BASE_TEMP_FILENAME_LENGTH) {
			filename
		} else filename.substring(
			0,
			MAX_BASE_TEMP_FILENAME_LENGTH
		)
	}

	/**
	 * @param file
	 * @return filename with audioformat separator stripped of,
	 * lengthened to ensure not too small for valid tempfile
	 * creation.
	 */
	fun getMinBaseFilenameAllowedForTempFile(file: File): String {
		val s = AudioFile.getBaseFilename(file)
		if (s.length >= 3) return s

		return when (s.length) {
			1 -> s + "000" // may this was meant to be 0 ?
			1 -> s + "00"
			2 -> s + "0"
			else -> return s
		}
	}

	/**
	 * Rename file, and if normal rename fails, try copy and delete instead.
	 *
	 * @param fromFile
	 * @param toFile
	 * @return
	 */
	fun rename(fromFile: File?, toFile: File): Boolean {
		logger.log(
			Level.CONFIG,
			"Renaming From:" + fromFile!!.absolutePath + " to " + toFile.absolutePath
		)
		if (toFile.exists()) {
			logger.log(Level.SEVERE, "Destination File:$toFile already exists")
			return false
		}

		//Rename File, could fail because being  used or because trying to rename over filesystems
		val result = fromFile.renameTo(toFile)
		return if (!result) {
			// Might be trying to rename over filesystem, so try copy and delete instead
			if (copy(
					fromFile,
					toFile
				)
			) {
				//If copy works but deletion of original file fails then it is because the file is being used
				//so we need to delete the file we have just created
				val deleteResult = fromFile.delete()
				if (!deleteResult) {
					logger.log(
						Level.SEVERE,
						"Unable to delete File:$fromFile"
					)
					toFile.delete()
					return false
				}
				true
			} else {
				false
			}
		} else true
	}

	/**
	 * Copy a File.
	 *
	 * ToDo refactor AbstractTestCase to use this method as it contains an exact duplicate.
	 *
	 * @param fromFile The existing File
	 * @param toFile   The new File
	 * @return `true` if and only if the renaming succeeded;
	 * `false` otherwise
	 */
	fun copy(fromFile: File?, toFile: File?): Boolean {
		return try {
			copyThrowsOnException(fromFile, toFile)
			true
		} catch (e: IOException) {
			e.printStackTrace()
			false
		}
	}

	/**
	 * Reads 4 bytes and concatenates them into a String.
	 * This pattern is used for ID's of various kinds.
	 *
	 * @param bytes
	 * @return
	 * @throws IOException
	 */
	@JvmStatic
	fun readFourBytesAsChars(bytes: ByteBuffer): String {
		val b = ByteArray(4)
		bytes[b]
		return String(b, StandardCharsets.ISO_8859_1)
	}

	/**
	 * Reads 3 bytes and concatenates them into a String.
	 * This pattern is used for ID's of various kinds.
	 *
	 * @param bytes
	 * @return
	 */
	@JvmStatic
	fun readThreeBytesAsChars(bytes: ByteBuffer): String {
		val b = ByteArray(3)
		bytes[b]
		return String(b, StandardCharsets.ISO_8859_1)
	}

	/**
	 * Used to convert (signed integer) to an long as if signed integer was unsigned hence allowing
	 * it to represent full range of integral values.
	 *
	 * @param n
	 * @return
	 */
	fun u(n: Int): Long {
		return n.toLong() and 0xffffffffL
	}

	/**
	 * Used to convert (signed short) to an integer as if signed short was unsigned hence allowing
	 * it to represent values 0 -> 65536 rather than -32786 -> 32786
	 * @param n
	 * @return
	 */
	fun u(n: Short): Int {
		return n.toInt() and 0xffff
	}

	/**
	 * Used to convert (signed byte) to an integer as if signed byte was unsigned hence allowing
	 * it to represent values 0 -> 255 rather than -128 -> 127.
	 *
	 * @param n
	 * @return
	 */
	@JvmStatic
	fun u(n: Byte): Int {
		return n.toInt() and 0xff
	}

	/**
	 *
	 * @param fc
	 * @param size
	 * @return
	 * @throws IOException
	 */
	@JvmStatic
	@Throws(IOException::class)
	fun readFileDataIntoBufferLE(fc: FileChannel, size: Int): ByteBuffer {
		val tagBuffer = ByteBuffer.allocateDirect(size)
		fc.read(tagBuffer)
		tagBuffer.position(0)
		tagBuffer.order(ByteOrder.LITTLE_ENDIAN)
		return tagBuffer
	}

	/**
	 *
	 * @param fc
	 * @param size
	 * @return
	 * @throws IOException
	 */
	@Throws(IOException::class)
	fun readFileDataIntoBufferBE(fc: FileChannel, size: Int): ByteBuffer {
		val tagBuffer = ByteBuffer.allocateDirect(size)
		fc.read(tagBuffer)
		tagBuffer.position(0)
		tagBuffer.order(ByteOrder.BIG_ENDIAN)
		return tagBuffer
	}

	/**
	 * Copy src file to dst file. FileChannels are used to maximize performance.
	 *
	 * @param source source File
	 * @param destination destination File which will be created or truncated, before copying, if it already exists
	 *
	 * @throws IOException if any error occurS
	 */
	@JvmStatic
	@Throws(IOException::class)
	fun copyThrowsOnException(source: File?, destination: File?) {
		// Must be done in a loop as there's no guarantee that a request smaller than request count will complete in one invocation.
		// Setting the transfer size more than about 1MB is pretty pointless because there is no asymptotic benefit. What you're trying
		// to achieve with larger transfer sizes is fewer context switches, and every time you double the transfer size you halve the
		// context switch cost. Pretty soon it vanishes into the noise.
		FileInputStream(source).use { inStream ->
			FileOutputStream(destination).use { outStream ->
				val inChannel = inStream.channel
				val outChannel = outStream.channel
				val size = inChannel.size()
				var position: Long = 0
				while (position < size) {
					position += inChannel.transferTo(position, 1024L * 1024L, outChannel)
				}
			}
		}
	}

	/**
	 *
	 * @param length
	 * @return true if length is an odd number
	 */
	@JvmStatic
	fun isOddLength(length: Long): Boolean {
		return length and 1L != 0L
	}
}
