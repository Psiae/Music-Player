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

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.Chunk
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.EncryptionChunk
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data.GUID
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util.Utils
import java.io.IOException
import java.io.InputStream

/**
 * This class reads the chunk containing encoding data <br></br>
 * **Warning:**<br></br>
 * Implementation is not completed. More analysis of this chunk is needed.
 *
 * @author Christian Laireiter
 **** */
internal class EncryptionChunkReader
/**
 * Should not be used for now.
 */
protected constructor() : ChunkReader {
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
	override fun read(guid: GUID?, stream: InputStream, chunkStart: Long): Chunk? {
		val result: EncryptionChunk
		val chunkLen = Utils.readBig64(stream)
		result = EncryptionChunk(chunkLen)

		// Can't be interpreted
		/*
		 * Object ID GUID 128 Object Size QWORD 64 Secret Data Length DWORD 32
		 * Secret Data INTEGER varies Protection Type Length DWORD 32 Protection
		 * Type char varies Key ID Length DWORD 32 Key ID char varies License
		 * URL Length DWORD 32 License URL char varies * Read the
		 */
		val secretData: ByteArray
		val protectionType: ByteArray
		val keyID: ByteArray
		val licenseURL: ByteArray

		// Secret Data length
		var fieldLength: Int
		fieldLength = Utils.readUINT32(stream).toInt()
		// Secret Data
		secretData = ByteArray(fieldLength + 1)
		stream.read(secretData, 0, fieldLength)
		secretData[fieldLength] = 0

		// Protection type Length
		fieldLength = 0
		fieldLength = Utils.readUINT32(stream).toInt()
		// Protection Data Length
		protectionType = ByteArray(fieldLength + 1)
		stream.read(protectionType, 0, fieldLength)
		protectionType[fieldLength] = 0

		// Key ID length
		fieldLength = 0
		fieldLength = Utils.readUINT32(stream).toInt()
		// Key ID
		keyID = ByteArray(fieldLength + 1)
		stream.read(keyID, 0, fieldLength)
		keyID[fieldLength] = 0

		// License URL length
		fieldLength = 0
		fieldLength = Utils.readUINT32(stream).toInt()
		// License URL
		licenseURL = ByteArray(fieldLength + 1)
		stream.read(licenseURL, 0, fieldLength)
		licenseURL[fieldLength] = 0
		result.secretData = String(secretData)
		result.protectionType = String(protectionType)
		result.keyID = String(keyID)
		result.licenseURL = String(licenseURL)
		result.position = chunkStart
		return result
	}

	companion object {
		/**
		 * The GUID this reader [applies to][.getApplyingIds]
		 */
		private val APPLYING = arrayOf(GUID.GUID_CONTENT_ENCRYPTION)
	}
}
