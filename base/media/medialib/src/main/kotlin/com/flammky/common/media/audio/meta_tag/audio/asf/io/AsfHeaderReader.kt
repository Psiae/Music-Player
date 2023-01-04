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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.io

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.AsfHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.GUID
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util.Utils
import java.io.*
import java.math.BigInteger
import java.nio.channels.Channels
import java.nio.channels.FileChannel

/**
 * This *class * reads an ASF header out of an input stream an creates an
 * [AsfHeader] object if successful. <br></br>
 * For now only ASF ver 1.0 is supported, because ver 2.0 seems not to be used
 * anywhere. <br></br>
 * ASF headers contains other chunks. As of this other readers of current
 * **package ** are called from within.
 *
 * @author Christian Laireiter
 */
class AsfHeaderReader
/**
 * Creates an instance of this reader.
 *
 * @param toRegister    The chunk readers to utilize.
 * @param readChunkOnce if `true`, each chunk type (identified by chunk
 * GUID) will handled only once, if a reader is available, other
 * chunks will be discarded.
 */
	(toRegister: List<Class<out ChunkReader?>>, readChunkOnce: Boolean) :
	ChunkContainerReader<AsfHeader>(toRegister, readChunkOnce) {
	/**
	 * {@inheritDoc}
	 */
	override fun canFail(): Boolean {
		return false
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(IOException::class)
	override fun createContainer(
		streamPosition: Long,
		chunkLength: BigInteger,
		stream: InputStream
	): AsfHeader {
		val chunkCount = Utils.readUINT32(stream)
		/*
		 * 2 reserved bytes. first should be equal to 0x01 and second 0x02. ASF
		 * specification suggests to not read the content if second byte is not
		 * 0x02.
		 */if (stream.read() != 1) {
			throw IOException("No ASF") //$NON-NLS-1$
		}
		if (stream.read() != 2) {
			throw IOException("No ASF") //$NON-NLS-1$
		}
		/*
		 * Creating the resulting object
		 */return AsfHeader(streamPosition, chunkLength, chunkCount)
	}

	/**
	 * {@inheritDoc}
	 */
	override val applyingIds: Array<GUID>
		get() = APPLYING.clone()

	/**
	 * Sets the [AsfExtHeaderReader], which is to be used, when an header
	 * extension object is found.
	 *
	 * @param extReader header extension object reader.
	 */
	fun setExtendedHeaderReader(extReader: AsfExtHeaderReader) {
		for (curr in extReader.applyingIds) {
			readerMap[curr] = extReader
		}
	}

	companion object {
		/**
		 * The GUID this reader [applies to][.getApplyingIds]
		 */
		private val APPLYING = arrayOf(GUID.GUID_HEADER)

		/**
		 * ASF reader configured to extract all information.
		 */
		private val FULL_READER: AsfHeaderReader

		/**
		 * ASF reader configured to just extract information about audio streams.<br></br>
		 * If the ASF file only contains one audio stream it works fine.<br></br>
		 */
		private val INFO_READER: AsfHeaderReader

		/**
		 * ASF reader configured to just extract metadata information.<br></br>
		 */
		private val TAG_READER: AsfHeaderReader

		init {
			val readers: MutableList<Class<out ChunkReader>> = ArrayList()
			readers.add(FileHeaderReader::class.java)
			readers.add(StreamChunkReader::class.java)
			INFO_READER = AsfHeaderReader(readers, true)
			readers.clear()
			readers.add(ContentDescriptionReader::class.java)
			readers.add(ContentBrandingReader::class.java)
			readers.add(LanguageListReader::class.java)
			readers.add(MetadataReader::class.java)
			/**
			 * Create the header extension object readers with just content
			 * description reader, extended content description reader, language
			 * list reader and both metadata object readers.
			 */
			val extReader = AsfExtHeaderReader(readers, true)
			val extReader2 = AsfExtHeaderReader(readers, true)
			TAG_READER = AsfHeaderReader(readers, true)
			TAG_READER.setExtendedHeaderReader(extReader)
			readers.add(FileHeaderReader::class.java)
			readers.add(StreamChunkReader::class.java)
			readers.add(EncodingChunkReader::class.java)
			readers.add(EncryptionChunkReader::class.java)
			readers.add(StreamBitratePropertiesReader::class.java)
			FULL_READER = AsfHeaderReader(readers, false)
			FULL_READER.setExtendedHeaderReader(extReader2)
		}

		/**
		 * Creates a Stream that will read from the specified
		 * [RandomAccessFile];<br></br>
		 *
		 * @param raf data source to read from.
		 * @return a stream which accesses the source.
		 */
		private fun createStream(raf: RandomAccessFile): InputStream {
			return FullRequestInputStream(BufferedInputStream(RandomAccessFileInputstream(raf)))
		}

		/**
		 * This method extracts the full ASF-Header from the given file.<br></br>
		 * If no header could be extracted `null` is returned. <br></br>
		 *
		 * @param file the ASF file to read.<br></br>
		 * @return AsfHeader-Wrapper, or `null` if no supported ASF
		 * header was found.
		 * @throws IOException on I/O Errors.
		 */
		@Throws(IOException::class)
		fun readHeader(file: File?): AsfHeader? {
			val stream: InputStream = FileInputStream(file)
			val result = FULL_READER.read(Utils.readGUID(stream), stream, 0)
			stream.close()
			return result
		}

		/**
		 * This method tries to extract a full ASF-header out of the given stream. <br></br>
		 * If no header could be extracted `null` is returned. <br></br>
		 *
		 * @param file File which contains the ASF header.
		 * @return AsfHeader-Wrapper, or `null` if no supported ASF
		 * header was found.
		 * @throws IOException Read errors
		 */
		@Throws(IOException::class)
		fun readHeader(file: RandomAccessFile): AsfHeader? {
			val stream = createStream(file)
			return FULL_READER.read(Utils.readGUID(stream), stream, 0)
		}

		/**
		 * This method tries to extract an ASF-header out of the given stream, which
		 * only contains information about the audio stream.<br></br>
		 * If no header could be extracted `null` is returned. <br></br>
		 *
		 * @param file File which contains the ASF header.
		 * @return AsfHeader-Wrapper, or `null` if no supported ASF
		 * header was found.
		 * @throws IOException Read errors
		 */
		@JvmStatic
		@Throws(IOException::class)
		fun readInfoHeader(file: RandomAccessFile): AsfHeader? {
			val stream = createStream(file)
			return INFO_READER.read(Utils.readGUID(stream), stream, 0)
		}

		fun readInfoHeader(fc: FileChannel): AsfHeader? {
			val stream = FullRequestInputStream(BufferedInputStream(Channels.newInputStream(fc)))
			return INFO_READER.read(Utils.readGUID(stream), stream, 0)
		}

		/**
		 * This method tries to extract an ASF-header out of the given stream, which
		 * only contains metadata.<br></br>
		 * If no header could be extracted `null` is returned. <br></br>
		 *
		 * @param file File which contains the ASF header.
		 * @return AsfHeader-Wrapper, or `null` if no supported ASF
		 * header was found.
		 * @throws IOException Read errors
		 */
		@JvmStatic
		@Throws(IOException::class)
		fun readTagHeader(file: RandomAccessFile): AsfHeader? {
			val stream = createStream(file)
			return TAG_READER.read(Utils.readGUID(stream), stream, 0)
		}

		fun readTagHeader(fc: FileChannel): AsfHeader? {
			val stream = FullRequestInputStream(BufferedInputStream(Channels.newInputStream(fc)))
			return TAG_READER.read(Utils.readGUID(stream), stream, 0)
		}
	}
}
