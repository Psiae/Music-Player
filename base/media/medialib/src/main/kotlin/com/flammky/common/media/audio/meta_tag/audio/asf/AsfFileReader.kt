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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.AudioFile
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.SupportedFileFormat
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.AsfHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.AudioStreamChunk
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.io.*
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.io.AsfHeaderReader.Companion.readInfoHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.io.AsfHeaderReader.Companion.readTagHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util.TagConverter.createTagOf
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util.Utils.readGUID
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.InvalidAudioFrameException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.ReadOnlyFileException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.AudioFileReader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.GenericAudioHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagException
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.asf.AsfTag
import java.io.*
import java.nio.channels.FileChannel
import java.util.logging.Logger

/**
 * This reader can read ASF files containing any content (stream type). <br></br>
 *
 * @author Christian Laireiter
 */
class AsfFileReader : AudioFileReader() {
	/**
	 * Determines if the &quot;isVbr&quot; field is set in the extended content
	 * description.<br></br>
	 *
	 * @param header the header to look up.
	 * @return `true` if &quot;isVbr&quot; is present with a
	 * `true` value.
	 */
	private fun determineVariableBitrate(header: AsfHeader?): Boolean {
		assert(header != null)
		var result = false
		val extDesc = header!!.findExtendedContentDescription()
		if (extDesc != null) {
			val descriptors = extDesc.getDescriptorsByName("IsVBR")
			if (descriptors != null && !descriptors.isEmpty()) {
				result = java.lang.Boolean.TRUE.toString() == descriptors[0].getString()
			}
		}
		return result
	}

	/**
	 * Creates a generic audio header instance with provided data from header.
	 *
	 * @param header ASF header which contains the information.
	 * @return generic audio header representation.
	 * @throws CannotReadException If header does not contain mandatory information. (Audio
	 * stream chunk and file header chunk)
	 */
	@Throws(CannotReadException::class)
	private fun getAudioHeader(header: AsfHeader): GenericAudioHeader {
		val info = GenericAudioHeader()
		if (header.fileHeader == null) {
			throw CannotReadException("Invalid ASF/WMA file. File header object not available.")
		}
		if (header.audioStreamChunk == null) {
			throw CannotReadException("Invalid ASF/WMA file. No audio stream contained.")
		}
		info.setBitRate(header.audioStreamChunk!!.kbps)
		info.channelNumber = header.audioStreamChunk!!.channelCount.toInt()
		info.format =
			SupportedFileFormat.WMA.displayName
		info.encodingType = "ASF (audio): " + header.audioStreamChunk!!.codecDescription
		info.isLossless =
			header.audioStreamChunk!!.compressionFormat == AudioStreamChunk.WMA_LOSSLESS
		info.preciseTrackLength = header.fileHeader.preciseDuration.toDouble()
		info.setSamplingRate(header.audioStreamChunk!!.samplingRate.toInt())
		info.isVariableBitRate = determineVariableBitrate(header)
		info.bitsPerSample = header.audioStreamChunk!!.bitsPerSample
		return info
	}

	/**
	 * (overridden)
	 *
	 * @see AudioFileReader.getEncodingInfo
	 */
	@Throws(CannotReadException::class, IOException::class)
	override fun getEncodingInfo(raf: RandomAccessFile): GenericAudioHeader {
		raf.seek(0)
		val info: GenericAudioHeader
		info = try {
			val header = readInfoHeader(raf)
				?: throw CannotReadException("Some values must have been " + "incorrect for interpretation as asf with wma content.")
			getAudioHeader(header)
		} catch (e: Exception) {
			if (e is IOException) {
				throw e
			} else if (e is CannotReadException) {
				throw e
			} else {
				throw CannotReadException("Failed to read. Cause: " + e.message, e)
			}
		}
		return info
	}

	@Throws(CannotReadException::class, IOException::class)
	override fun getEncodingInfo(fc: FileChannel): GenericAudioHeader {
		fc.position(0)
		return try {
			val header = readInfoHeader(fc)
				?: throw CannotReadException("Some values must have been " + "incorrect for interpretation as asf with wma content.")
			getAudioHeader(header)
		} catch (e: Exception) {
			when (e) {
				is IOException, is CannotReadException -> throw e
				else -> throw CannotReadException("Failed to read. Cause: " + e.message, e)
			}
		}
	}



	/**
	 * Creates a tag instance with provided data from header.
	 *
	 * @param header ASF header which contains the information.
	 * @return generic audio header representation.
	 */
	private fun getTag(header: AsfHeader): AsfTag {
		return createTagOf(header)
	}

	/**
	 * (overridden)
	 *
	 * @see AudioFileReader.getTag
	 */
	@Throws(CannotReadException::class, IOException::class)
	override fun getTag(raf: RandomAccessFile): AsfTag {
		raf.seek(0)
		val tag: AsfTag
		tag = try {
			val header = readTagHeader(raf)
				?: throw CannotReadException("Some values must have been " + "incorrect for interpretation as asf with wma content.")
			createTagOf(header)
		} catch (e: Exception) {
			logger.severe(e.message)
			if (e is IOException) {
				throw e
			} else if (e is CannotReadException) {
				throw e
			} else {
				throw CannotReadException("Failed to read. Cause: " + e.message)
			}
		}
		return tag
	}

	override fun getTag(fc: FileChannel): Tag {
		fc.position(0)
		return try {
			val header = readTagHeader(fc)
				?: throw CannotReadException("Some values must have been " + "incorrect for interpretation as asf with wma content.")
			createTagOf(header)
		} catch (e: Exception) {
			when (e) {
				is IOException, is CannotReadException -> {
					throw e
				}
				else -> {
					throw CannotReadException("Failed to read. Cause: " + e.message)
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(
		CannotReadException::class,
		IOException::class,
		TagException::class,
		ReadOnlyFileException::class,
		InvalidAudioFrameException::class
	)
	override fun read(f: File): AudioFile {
		if (!f.canRead()) {
			if (!f.exists()) {
				logger.severe("Unable to find:" + f.path)
				throw FileNotFoundException(ErrorMessage.UNABLE_TO_FIND_FILE.getMsg(f.path))
			} else {
				throw CannotReadException(
					ErrorMessage.GENERAL_READ_FAILED_DO_NOT_HAVE_PERMISSION_TO_READ_FILE.getMsg(
						f.absolutePath
					)
				)
			}
		}
		var stream: InputStream? = null
		return try {
			stream = FullRequestInputStream(BufferedInputStream(FileInputStream(f)))
			val header = HEADER_READER.read(readGUID(stream), stream, 0)
				?: throw CannotReadException(ErrorMessage.ASF_HEADER_MISSING.getMsg(f.absolutePath))
			if (header.fileHeader == null) {
				throw CannotReadException(ErrorMessage.ASF_FILE_HEADER_MISSING.getMsg(f.absolutePath))
			}

			// Just log a warning because file seems to play okay
			if (header.fileHeader.fileSize.toLong() != f.length()) {
				logger.warning(
					ErrorMessage.ASF_FILE_HEADER_SIZE_DOES_NOT_MATCH_FILE_SIZE.getMsg(
						f.absolutePath,
						header.fileHeader.fileSize.toLong(),
						f.length()
					)
				)
			}
			AudioFile(f, getAudioHeader(header), getTag(header))
		} catch (e: CannotReadException) {
			throw e
		} catch (e: Exception) {
			throw CannotReadException(
				"\"$f\" :$e", e
			)
		} finally {
			try {
				stream?.close()
			} catch (ex: Exception) {
				LOGGER.severe(
					"\"$f\" :$ex"
				)
			}
		}
	}

	companion object {
		/**
		 * Logger instance
		 */
		private val LOGGER = Logger.getLogger("org.jaudiotagger.audio.asf")

		/**
		 * This reader will be configured to read tag and audio header information.<br></br>
		 */
		private val HEADER_READER: AsfHeaderReader

		init {
			val readers: MutableList<Class<out ChunkReader?>> = ArrayList()
			readers.addAll(
				listOf(
					ContentDescriptionReader::class.java,
					ContentBrandingReader::class.java,
					MetadataReader::class.java,
					LanguageListReader::class.java
				)
			)

			// Create the header extension object reader with just content
			// description reader as well
			// as extended content description reader.
			val extReader = AsfExtHeaderReader(readers, true)

			readers.addAll(
				listOf(
					FileHeaderReader::class.java,
					StreamChunkReader::class.java
				)
			)

			HEADER_READER = AsfHeaderReader(readers, true)
			HEADER_READER.setExtendedHeaderReader(extReader)
		}
	}
}
