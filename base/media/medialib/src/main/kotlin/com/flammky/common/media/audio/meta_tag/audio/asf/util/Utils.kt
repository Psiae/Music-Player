/*
 * Entagged Audio Tag library
 * Copyright (c) 2004-2005 Christian Laireiter <liree@web.de>
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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.AsfHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.GUID
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.nio.charset.Charset
import java.util.*

/**
 * Some static Methods which are used in several Classes. <br></br>
 *
 * @author Christian Laireiter
 */
object Utils {
	const val DIFF_BETWEEN_ASF_DATE_AND_JAVA_DATE = 11644470000000L

	/**
	 * Stores the default line separator of the current underlying system.
	 */
	val LINE_SEPARATOR = System.getProperty("line.separator") //$NON-NLS-1$

	/**
	 *
	 */
	private const val MAXIMUM_STRING_LENGTH_ALLOWED = 32766

	/**
	 * This method checks given string will not exceed limit in bytes[] when
	 * converted UTF-16LE encoding (2 bytes per character) and checks whether
	 * the length doesn't exceed 65535 bytes. <br></br>
	 *
	 * @param value The string to check.
	 * @throws IllegalArgumentException If byte representation takes more than 65535 bytes.
	 */
	@Throws(IllegalArgumentException::class)
	fun checkStringLengthNullSafe(value: String?) {
		if (value != null) {
			require(value.length <= MAXIMUM_STRING_LENGTH_ALLOWED) {
				ErrorMessage.WMA_LENGTH_OF_STRING_IS_TOO_LARGE.getMsg(
					value.length * 2
				)
			}
		}
	}

	/**
	 * @param value String to check for null
	 * @return true unless string is too long
	 */
	fun isStringLengthValidNullSafe(value: String?): Boolean {
		if (value != null) {
			if (value.length > MAXIMUM_STRING_LENGTH_ALLOWED) {
				return false
			}
		}
		return true
	}

	/**
	 * effectively copies a specified amount of bytes from one stream to
	 * another.
	 *
	 * @param source stream to read from
	 * @param dest   stream to write to
	 * @param amount amount of bytes to copy
	 * @throws IOException on I/O errors, and if the source stream depletes before all
	 * bytes have been copied.
	 */
	@Throws(IOException::class)
	fun copy(source: InputStream, dest: OutputStream, amount: Long) {
		val buf = ByteArray(8192)
		var copied: Long = 0
		while (copied < amount) {
			var toRead = 8192
			if (amount - copied < 8192) {
				toRead = (amount - copied).toInt()
			}
			val read = source.read(buf, 0, toRead)
			if (read == -1) {
				throw IOException(
					"Inputstream has to continue for another " + (amount - copied) + " bytes."
				)
			}
			dest.write(buf, 0, read)
			copied += read.toLong()
		}
	}

	/**
	 * Copies all of the source to the destination.<br></br>
	 *
	 * @param source source to read from
	 * @param dest   stream to write to
	 * @throws IOException on I/O errors.
	 */
	@Throws(IOException::class)
	fun flush(source: InputStream, dest: OutputStream) {
		val buf = ByteArray(8192)
		var read: Int
		while (source.read(buf).also { read = it } != -1) {
			dest.write(buf, 0, read)
		}
	}

	/**
	 * This method will create a byte[] at the size of `byteCount`
	 * and insert the bytes of `value` (starting from lowset byte)
	 * into it. <br></br>
	 * You can easily create a Word (16-bit), DWORD (32-bit), QWORD (64 bit) out
	 * of the value, ignoring the original type of value, since java
	 * automatically performs transformations. <br></br>
	 * **Warning: ** This method works with unsigned numbers only.
	 *
	 * @param value     The value to be written into the result.
	 * @param byteCount The number of bytes the array has got.
	 * @return A byte[] with the size of `byteCount` containing the
	 * lower byte values of `value`.
	 */
	fun getBytes(value: Long, byteCount: Int): ByteArray {
		val result = ByteArray(byteCount)
		for (i in result.indices) {
			result[i] = (value ushr i * 8 and 0xFFL).toByte()
		}
		return result
	}

	/**
	 * Convenience method to convert the given string into a byte sequence which
	 * has the format of the charset given.
	 *
	 * @param source  string to convert.
	 * @param charset charset to apply
	 * @return the source's binary representation according to the charset.
	 */
	fun getBytes(source: String?, charset: Charset?): ByteArray {
		assert(charset != null)
		assert(source != null)
		val encoded = charset!!.encode(source)
		val result = ByteArray(encoded.limit())
		encoded.rewind()
		encoded[result]
		return result
	}
	/**
	 * Since date values in ASF files are given in 100 ns steps since first
	 * january of 1601 a little conversion must be done. <br></br>
	 * This method converts a date given in described manner to a calendar.
	 *
	 * @param fileTime
	 * Time in 100ns since 1 jan 1601
	 * @return Calendar holding the date representation.
	 */
	/* Old method that ran very slowely and doesnt logical correct, how does dividing something
		at 10-4 by 10,000 convert it to 10 -3
	public static GregorianCalendar getDateOf(final BigInteger fileTime) {
			final GregorianCalendar result = new GregorianCalendar(1601, 0, 1);
			// lose anything beyond milliseconds, because calendar can't handle
			// less value
			BigInteger time = fileTime.divide(new BigInteger("10000")); //$NON-NLS-1$
			final BigInteger maxInt = new BigInteger(String
							.valueOf(Integer.MAX_VALUE));
			while (time.compareTo(maxInt) > 0) {
					result.add(Calendar.MILLISECOND, Integer.MAX_VALUE);
					time = time.subtract(maxInt);
			}
			result.add(Calendar.MILLISECOND, time.intValue());
			return result;
	}
	*/
	/**
	 * Date values in ASF files are given in 100 ns (10 exp -4) steps since first
	 *
	 * @param fileTime Time in 100ns since 1 jan 1601
	 * @return Calendar holding the date representation.
	 */
	fun getDateOf(fileTime: BigInteger): GregorianCalendar {
		val result = GregorianCalendar()

		// Divide by 10 to convert from -4 to -3 (millisecs)
		val time = fileTime.divide(BigInteger("10"))
		// Construct Date taking into the diff between 1601 and 1970
		val date = Date(time.toLong() - DIFF_BETWEEN_ASF_DATE_AND_JAVA_DATE)
		result.time = date
		return result
	}

	/**
	 * Tests if the given string is `null` or just contains
	 * whitespace characters.
	 *
	 * @param toTest String to test.
	 * @return see description.
	 */
	@JvmStatic
	fun isBlank(toTest: String?): Boolean {
		if (toTest == null) {
			return true
		}
		for (i in 0 until toTest.length) {
			if (!Character.isWhitespace(toTest[i])) {
				return false
			}
		}
		return true
	}

	/**
	 * Reads 8 bytes from stream and interprets them as a UINT64 which is
	 * returned as [BigInteger].<br></br>
	 *
	 * @param stream stream to readm from.
	 * @return a BigInteger which represents the read 8 bytes value.
	 * @throws IOException if problem reading bytes
	 */
	@Throws(IOException::class)
	fun readBig64(stream: InputStream): BigInteger {
		val bytes = ByteArray(8)
		val oa = ByteArray(8)
		val read = stream.read(bytes)
		if (read != 8) {
			// 8 bytes mandatory.
			throw EOFException()
		}
		for (i in bytes.indices) {
			oa[7 - i] = bytes[i]
		}
		return BigInteger(oa)
	}

	/**
	 * Reads `size` bytes from the stream.<br></br>
	 *
	 * @param stream stream to read from.
	 * @param size   amount of bytes to read.
	 * @return the read bytes.
	 * @throws IOException on I/O errors.
	 */
	@Throws(IOException::class)
	fun readBinary(stream: InputStream, size: Long): ByteArray {
		val result = ByteArray(size.toInt())
		stream.read(result)
		return result
	}

	/**
	 * This method reads a UTF-16 String, which length is given on the number of
	 * characters it consists of. <br></br>
	 * The stream must be at the number of characters. This number contains the
	 * terminating zero character (UINT16).
	 *
	 * @param stream Input source
	 * @return String
	 * @throws IOException read errors
	 */
	@Throws(IOException::class)
	fun readCharacterSizedString(stream: InputStream): String {
		val result = StringBuilder()
		val strLen = readUINT16(stream)
		var character = stream.read()
		character = character or (stream.read() shl 8)
		do {
			if (character != 0) {
				result.append(character.toChar())
				character = stream.read()
				character = character or (stream.read() shl 8)
			}
		} while (character != 0 || result.length + 1 > strLen)
		check(strLen == result.length + 1) {
			"Invalid Data for current interpretation" //$NON-NLS-1$
		}
		return result.toString()
	}

	/**
	 * This method reads a UTF-16 encoded String. <br></br>
	 * For the use this method the number of bytes used by current string must
	 * be known. <br></br>
	 * The ASF specification recommends that those strings end with a
	 * terminating zero. However it also says that it is not always the case.
	 *
	 * @param stream Input source
	 * @param strLen Number of bytes the String may take.
	 * @return read String.
	 * @throws IOException read errors.
	 */
	@Throws(IOException::class)
	fun readFixedSizeUTF16Str(stream: InputStream, strLen: Int): String {
		var strBytes = ByteArray(strLen)
		val read = stream.read(strBytes)
		if (read == strBytes.size) {
			if (strBytes.size >= 2) {
				/*
				 * Zero termination is recommended but optional. So check and
				 * if, remove.
				 */
				if (strBytes[strBytes.size - 1].toInt() == 0 && strBytes[strBytes.size - 2].toInt() == 0) {
					val copy = ByteArray(strBytes.size - 2)
					System.arraycopy(strBytes, 0, copy, 0, strBytes.size - 2)
					strBytes = copy
				}
			}
			return String(strBytes, Charset.forName("UTF-16LE"))
		}
		throw IllegalStateException("Couldn't read the necessary amount of bytes.")
	}

	/**
	 * This Method reads a GUID (which is a 16 byte long sequence) from the
	 * given `raf` and creates a wrapper. <br></br>
	 * **Warning **: <br></br>
	 * There is no way of telling if a byte sequence is a guid or not. The next
	 * 16 bytes will be interpreted as a guid, whether it is or not.
	 *
	 * @param stream Input source.
	 * @return A class wrapping the guid.
	 * @throws IOException happens when the file ends before guid could be extracted.
	 */
	@JvmStatic
	@Throws(IOException::class)
	fun readGUID(stream: InputStream?): GUID {
		requireNotNull(stream) {
			"Argument must not be null" //$NON-NLS-1$
		}
		val binaryGuid = IntArray(GUID.GUID_LENGTH)
		for (i in binaryGuid.indices) {
			binaryGuid[i] = stream.read()
		}
		return GUID(binaryGuid)
	}

	/**
	 * Reads 2 bytes from stream and interprets them as UINT16.<br></br>
	 *
	 * @param stream stream to read from.
	 * @return UINT16 value
	 * @throws IOException on I/O Errors.
	 */
	@Throws(IOException::class)
	fun readUINT16(stream: InputStream): Int {
		var result = stream.read()
		result = result or (stream.read() shl 8)
		return result
	}

	/**
	 * Reads 4 bytes from stream and interprets them as UINT32.<br></br>
	 *
	 * @param stream stream to read from.
	 * @return UINT32 value
	 * @throws IOException on I/O Errors.
	 */
	@Throws(IOException::class)
	fun readUINT32(stream: InputStream): Long {
		var result: Long = 0
		var i = 0
		while (i <= 24) {

			// Warning, always cast to long here. Otherwise it will be
			// shifted as int, which may produce a negative value, which will
			// then be extended to long and assign the long variable a negative
			// value.
			result = result or (stream.read().toLong() shl i)
			i += 8
		}
		return result
	}

	/**
	 * Reads long as little endian.
	 *
	 * @param stream Data source
	 * @return long value
	 * @throws IOException read error, or eof is reached before long is completed
	 */
	@Throws(IOException::class)
	fun readUINT64(stream: InputStream): Long {
		var result: Long = 0
		var i = 0
		while (i <= 56) {

			// Warning, always cast to long here. Otherwise it will be
			// shifted as int, which may produce a negative value, which will
			// then be extended to long and assign the long variable a negative
			// value.
			result = result or (stream.read().toLong() shl i)
			i += 8
		}
		return result
	}

	/**
	 * This method reads a UTF-16 encoded String, beginning with a 16-bit value
	 * representing the number of bytes needed. The String is terminated with as
	 * 16-bit ZERO. <br></br>
	 *
	 * @param stream Input source
	 * @return read String.
	 * @throws IOException read errors.
	 */
	@Throws(IOException::class)
	fun readUTF16LEStr(stream: InputStream): String {
		val strLen = readUINT16(stream)
		var buf = ByteArray(strLen)
		val read = stream.read(buf)
		if (read == strLen || strLen == 0 && read == -1) {
			/*
			 * Check on zero termination
			 */
			if (buf.size >= 2) {
				if (buf[buf.size - 1].toInt() == 0 && buf[buf.size - 2].toInt() == 0) {
					val copy = ByteArray(buf.size - 2)
					System.arraycopy(buf, 0, copy, 0, buf.size - 2)
					buf = copy
				}
			}
			return String(buf, AsfHeader.ASF_CHARSET)
		}
		throw IllegalStateException("Invalid Data for current interpretation") //$NON-NLS-1$
	}

	/**
	 * Writes the given value as UINT16 into the stream.
	 *
	 * @param number value to write.
	 * @param out    stream to write into.
	 * @throws IOException On I/O errors
	 */
	@Throws(IOException::class)
	fun writeUINT16(number: Int, out: OutputStream) {
		require(number >= 0) {
			"positive value expected." //$NON-NLS-1$
		}
		val toWrite = ByteArray(2)
		var i = 0
		while (i <= 8) {
			toWrite[i / 8] = (number shr i and 0xFF).toByte()
			i += 8
		}
		out.write(toWrite)
	}

	/**
	 * Writes the given value as UINT32 into the stream.
	 *
	 * @param number value to write.
	 * @param out    stream to write into.
	 * @throws IOException On I/O errors
	 */
	@Throws(IOException::class)
	fun writeUINT32(number: Long, out: OutputStream) {
		require(number >= 0) {
			"positive value expected." //$NON-NLS-1$
		}
		val toWrite = ByteArray(4)
		var i = 0
		while (i <= 24) {
			toWrite[i / 8] = (number shr i and 0xFFL).toByte()
			i += 8
		}
		out.write(toWrite)
	}

	/**
	 * Writes the given value as UINT64 into the stream.
	 *
	 * @param number value to write.
	 * @param out    stream to write into.
	 * @throws IOException On I/O errors
	 */
	@Throws(IOException::class)
	fun writeUINT64(number: Long, out: OutputStream) {
		require(number >= 0) {
			"positive value expected." //$NON-NLS-1$
		}
		val toWrite = ByteArray(8)
		var i = 0
		while (i <= 56) {
			toWrite[i / 8] = (number shr i and 0xFFL).toByte()
			i += 8
		}
		out.write(toWrite)
	}
}
