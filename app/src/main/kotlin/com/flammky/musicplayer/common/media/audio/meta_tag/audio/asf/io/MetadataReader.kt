package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.io

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.*
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util.Utils
import java.io.IOException
import java.io.InputStream

/**
 * Reads an interprets &quot;Metadata Object&quot;, &quot;Metadata Library
 * Object&quot; and &quot;Extended Content Description&quot; of ASF files.<br></br>
 *
 * @author Christian Laireiter
 */
class MetadataReader : ChunkReader {
	/**
	 * {@inheritDoc}
	 */
	override fun canFail(): Boolean {
		return false
	}

	/**
	 * {@inheritDoc}
	 */
	override val applyingIds: Array<GUID>
		get() = APPLYING.clone()

	/**
	 * {@inheritDoc}
	 */
	@Throws(IOException::class)
	override fun read(guid: GUID?, stream: InputStream, streamPosition: Long): Chunk? {
		guid ?: return null

		val chunkLen = Utils.readBig64(stream)
		val result = MetadataContainer(guid, streamPosition, chunkLen)
		// isExtDesc will be set to true, if a extended content description
		// chunk is read
		// otherwise it is a metadata object, there are only slight differences
		val isExtDesc = result.containerType === ContainerType.EXTENDED_CONTENT
		val recordCount = Utils.readUINT16(stream)
		for (i in 0 until recordCount) {
			var languageIndex = 0
			var streamNumber = 0
			if (!isExtDesc) {
				/*
				 * Metadata objects have a language index and a stream number
				 */
				languageIndex = Utils.readUINT16(stream)
				assert(languageIndex >= 0 && languageIndex < MetadataDescriptor.MAX_LANG_INDEX)
				assert(result.containerType === ContainerType.METADATA_LIBRARY_OBJECT || languageIndex == 0)
				streamNumber = Utils.readUINT16(stream)
				assert(streamNumber >= 0 && streamNumber <= MetadataDescriptor.MAX_STREAM_NUMBER)
			}
			val nameLen = Utils.readUINT16(stream)
			var recordName: String? = null
			if (isExtDesc) {
				recordName = Utils.readFixedSizeUTF16Str(stream, nameLen)
			}
			val dataType = Utils.readUINT16(stream)
			assert(dataType >= 0 && dataType <= 6)
			val dataLen = if (isExtDesc) Utils.readUINT16(stream)
				.toLong() else Utils.readUINT32(stream)
			assert(dataLen >= 0)
			assert(result.containerType === ContainerType.METADATA_LIBRARY_OBJECT || dataLen <= MetadataDescriptor.DWORD_MAXVALUE)
			if (!isExtDesc) {
				recordName = Utils.readFixedSizeUTF16Str(stream, nameLen)
			}
			val descriptor = MetadataDescriptor(
				result.containerType, recordName!!, dataType, streamNumber, languageIndex
			)
			when (dataType) {
				MetadataDescriptor.TYPE_STRING -> descriptor.setStringValue(
					Utils.readFixedSizeUTF16Str(
						stream,
						dataLen.toInt()
					)
				)
				MetadataDescriptor.TYPE_BINARY -> descriptor.setBinaryValue(
					Utils.readBinary(
						stream,
						dataLen
					)
				)
				MetadataDescriptor.TYPE_BOOLEAN -> {
					assert(isExtDesc && dataLen == 4L || !isExtDesc && dataLen == 2L)
					descriptor.setBooleanValue(readBoolean(stream, dataLen.toInt()))
				}
				MetadataDescriptor.TYPE_DWORD -> {
					assert(dataLen == 4L)
					descriptor.setDWordValue(Utils.readUINT32(stream))
				}
				MetadataDescriptor.TYPE_WORD -> {
					assert(dataLen == 2L)
					descriptor.setWordValue(Utils.readUINT16(stream))
				}
				MetadataDescriptor.TYPE_QWORD -> {
					assert(dataLen == 8L)
					descriptor.setQWordValue(Utils.readUINT64(stream))
				}
				MetadataDescriptor.TYPE_GUID -> {
					assert(dataLen == GUID.GUID_LENGTH.toLong())
					descriptor.setGUIDValue(Utils.readGUID(stream))
				}
				else ->                     // Unknown, hopefully the convention for the size of the
					// value
					// is given, so we could read it binary
					descriptor.setStringValue(
						"Invalid datatype: " + String(
							Utils.readBinary(
								stream,
								dataLen
							)
						)
					)
			}
			result.addDescriptor(descriptor)
		}
		return result
	}

	/**
	 * Reads the given number of bytes and checks the last byte, if it's equal to
	 * one or zero (true / false).<br></br>
	 *
	 * @param stream stream to read from
	 * @param length number of bytes to read
	 * @return `true` or `false`.
	 * @throws IOException on I/O Errors
	 */
	@Throws(IOException::class)
	private fun readBoolean(stream: InputStream, length: Int): Boolean {
		val tmp = ByteArray(length)
		stream.read(tmp)
		return tmp[length - 1].toInt() == 1
	}

	companion object {
		/**
		 * The GUID this reader [applies to][.getApplyingIds]
		 */
		private val APPLYING = arrayOf(
			ContainerType.EXTENDED_CONTENT.containerGUID,
			ContainerType.METADATA_OBJECT.containerGUID,
			ContainerType.METADATA_LIBRARY_OBJECT.containerGUID
		)
	}
}
