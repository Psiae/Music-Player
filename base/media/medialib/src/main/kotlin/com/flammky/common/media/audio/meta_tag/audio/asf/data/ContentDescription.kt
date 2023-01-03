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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.util.Utils
import java.io.IOException
import java.io.OutputStream
import java.math.BigInteger
import java.util.*

/**
 * This class represents the data of a chunk which contains title, author,
 * copyright, description and the rating of the file. <br></br>
 * It is optional within ASF files. But if, exists only once.
 *
 * @author Christian Laireiter
 */
class ContentDescription constructor(pos: Long = 0, chunkLen: BigInteger = BigInteger.ZERO) :
	MetadataContainer(ContainerType.CONTENT_DESCRIPTION, pos, chunkLen) {
	/**
	 * @return Returns the author.
	 */
	/**
	 * @param fileAuthor The author to set.
	 * @throws IllegalArgumentException If "UTF-16LE"-byte-representation would take more than 65535
	 * bytes.
	 */
	@set:Throws(IllegalArgumentException::class)
	var author: String?
		get() = getValueFor(KEY_AUTHOR)
		set(fileAuthor) {
			setStringValue(KEY_AUTHOR, fileAuthor)
		}
	/**
	 * @return Returns the comment.
	 */
	/**
	 * @param tagComment The comment to set.
	 * @throws IllegalArgumentException If "UTF-16LE"-byte-representation would take more than 65535
	 * bytes.
	 */
	@set:Throws(IllegalArgumentException::class)
	var comment: String?
		get() = getValueFor(KEY_DESCRIPTION)
		set(tagComment) {
			setStringValue(KEY_DESCRIPTION, tagComment)
		}

	/**
	 * @return Returns the copyRight.
	 */
	val copyRight: String?
		get() = getValueFor(KEY_COPYRIGHT)

	override val currentAsfChunkSize: Long
		get() {
			var result: Long = 44 // GUID + UINT64 for size + 5 times string length
			// (each
			// 2 bytes) + 5 times zero term char (2 bytes each).
			result += (author!!.length * 2).toLong() // UTF-16LE
			result += (comment!!.length * 2).toLong()
			result += (rating!!.length * 2).toLong()
			result += (title!!.length * 2).toLong()
			result += ((copyRight?.length ?: 0) * 2).toLong()
			return result
		}

	/**
	 * @return returns the rating.
	 */
	/**
	 * @param ratingText The rating to be set.
	 * @throws IllegalArgumentException If "UTF-16LE"-byte-representation would take more than 65535
	 * bytes.
	 */
	@set:Throws(IllegalArgumentException::class)
	var rating: String?
		get() = getValueFor(KEY_RATING)
		set(ratingText) {
			setStringValue(KEY_RATING, ratingText)
		}
	/**
	 * @return Returns the title.
	 */
	/**
	 * @param songTitle The title to set.
	 * @throws IllegalArgumentException If "UTF-16LE"-byte-representation would take more than 65535
	 * bytes.
	 */
	@set:Throws(IllegalArgumentException::class)
	var title: String?
		get() = getValueFor(KEY_TITLE)
		set(songTitle) {
			setStringValue(KEY_TITLE, songTitle)
		}

	/**
	 * {@inheritDoc}
	 */
	override fun isAddSupported(descriptor: MetadataDescriptor): Boolean {
		return ALLOWED.contains(descriptor.name) && super.isAddSupported(descriptor)
	}

	/**
	 * {@inheritDoc}
	 */
	override fun prettyPrint(prefix: String): String {
		val result = StringBuilder(super.prettyPrint(prefix))
		result.append(prefix).append("  |->Title      : ").append(title)
			.append(Utils.LINE_SEPARATOR)
		result.append(prefix).append("  |->Author     : ").append(author)
			.append(Utils.LINE_SEPARATOR)
		result.append(prefix).append("  |->Copyright  : ").append(copyRight)
			.append(Utils.LINE_SEPARATOR)
		result.append(prefix).append("  |->Description: ").append(comment)
			.append(Utils.LINE_SEPARATOR)
		result.append(prefix).append("  |->Rating     :").append(rating)
			.append(Utils.LINE_SEPARATOR)
		return result.toString()
	}

	/**
	 * @param cpright The copyRight to set.
	 * @throws IllegalArgumentException If "UTF-16LE"-byte-representation would take more than 65535
	 * bytes.
	 */
	@Throws(IllegalArgumentException::class)
	fun setCopyright(cpright: String?) {
		setStringValue(KEY_COPYRIGHT, cpright)
	}

	/**
	 * {@inheritDoc}
	 */
	@Throws(IOException::class)
	override fun writeInto(out: OutputStream): Long {
		val chunkSize = currentAsfChunkSize
		out.write(guid.bytes)
		Utils.writeUINT64(
			currentAsfChunkSize, out
		)
		// write the sizes of the string representations plus 2 bytes zero term
		// character
		Utils.writeUINT16(
			title!!.length * 2 + 2, out
		)
		Utils.writeUINT16(
			author!!.length * 2 + 2, out
		)
		Utils.writeUINT16(
			copyRight!!.length * 2 + 2, out
		)
		Utils.writeUINT16(
			comment!!.length * 2 + 2, out
		)
		Utils.writeUINT16(
			rating!!.length * 2 + 2, out
		)
		// write the Strings
		out.write(
			Utils.getBytes(
				title, AsfHeader.ASF_CHARSET
			)
		)
		out.write(AsfHeader.ZERO_TERM)
		out.write(
			Utils.getBytes(
				author, AsfHeader.ASF_CHARSET
			)
		)
		out.write(AsfHeader.ZERO_TERM)
		out.write(
			Utils.getBytes(
				copyRight, AsfHeader.ASF_CHARSET
			)
		)
		out.write(AsfHeader.ZERO_TERM)
		out.write(
			Utils.getBytes(
				comment, AsfHeader.ASF_CHARSET
			)
		)
		out.write(AsfHeader.ZERO_TERM)
		out.write(
			Utils.getBytes(
				rating, AsfHeader.ASF_CHARSET
			)
		)
		out.write(AsfHeader.ZERO_TERM)
		return chunkSize
	}

	companion object {
		/**
		 * Stores the only allowed keys of this metadata container.
		 */
		val ALLOWED: Set<String>

		/**
		 * Field key for author.
		 */
		const val KEY_AUTHOR = "AUTHOR"

		/**
		 * Field key for copyright.
		 */
		const val KEY_COPYRIGHT = "COPYRIGHT"

		/**
		 * Field key for description.
		 */
		const val KEY_DESCRIPTION = "DESCRIPTION"

		/**
		 * Field key for rating.
		 */
		const val KEY_RATING = "RATING"

		/**
		 * Field key for title.
		 */
		const val KEY_TITLE = "TITLE"

		init {
			ALLOWED = HashSet(
				Arrays.asList(
					KEY_AUTHOR,
					KEY_COPYRIGHT,
					KEY_DESCRIPTION,
					KEY_RATING,
					KEY_TITLE
				)
			)
		}
	}
}
