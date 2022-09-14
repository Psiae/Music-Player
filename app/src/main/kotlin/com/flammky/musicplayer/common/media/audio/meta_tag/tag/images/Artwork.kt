package com.flammky.musicplayer.common.media.audio.meta_tag.tag.images

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.flac.metadatablock.MetadataBlockDataPicture
import java.io.File
import java.io.IOException

/**
 * Represents artwork in a format independent  way
 */
interface Artwork {
	var binaryData: ByteArray?
	var mimeType: String
	var description: String
	var height: Int
	var width: Int

	/**
	 * Should be called when you wish to prime the artwork for saving
	 *
	 * @return
	 */
	fun setImageFromData(): Boolean

	@get:Throws(IOException::class)
	val image: Any
	var isLinked: Boolean
	var imageUrl: String
	var pictureType: Int

	/**
	 * Create Artwork from File
	 *
	 * @param file
	 * @throws IOException
	 */
	@Throws(IOException::class)
	fun setFromFile(file: File)

	/**
	 * Populate Artwork from MetadataBlockDataPicture as used by Flac and VorbisComment
	 *
	 * @param coverArt
	 */
	fun setFromMetadataBlockDataPicture(coverArt: MetadataBlockDataPicture)
}
