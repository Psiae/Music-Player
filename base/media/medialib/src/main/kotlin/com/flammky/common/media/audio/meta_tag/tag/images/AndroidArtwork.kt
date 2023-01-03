package com.flammky.musicplayer.common.media.audio.meta_tag.tag.images

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.flac.metadatablock.MetadataBlockDataPicture
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.ImageFormats.getMimeTypeForBinarySignature
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference.PictureTypes
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

/**
 * Represents artwork in a format independent way
 */
class AndroidArtwork : Artwork {

	override var binaryData: ByteArray? = null
	override var mimeType: String = ""
	override var description: String = ""
	override var isLinked = false
	override var imageUrl = ""
	override var pictureType = -1
	override var width: Int = 0
	override var height: Int = 0

	/**
	 * Should be called when you wish to prime the artwork for saving
	 *
	 * @return
	 */
	override fun setImageFromData(): Boolean {
		throw UnsupportedOperationException()
	}

	@get:Throws(IOException::class)
	override val image: Any
		get() {
			throw UnsupportedOperationException()
		}

	/**
	 * Create Artwork from File
	 *
	 * @param file
	 * @throws IOException
	 */
	@Throws(IOException::class)
	override fun setFromFile(file: File) {
		val imageFile = RandomAccessFile(file, "r")
		val imagedata = ByteArray(imageFile.length().toInt())
		imageFile.read(imagedata)
		imageFile.close()
		binaryData = imagedata
		mimeType = getMimeTypeForBinarySignature(imagedata) ?: ""
		description = ""
		pictureType = PictureTypes.DEFAULT_ID
	}

	/**
	 * Create Linked Artwork from URL
	 *
	 * @param url
	 * @throws IOException
	 */
	@Throws(IOException::class)
	fun setLinkedFromURL(url: String) {
		isLinked = true
		imageUrl = url
	}

	/**
	 * Populate Artwork from MetadataBlockDataPicture as used by Flac and VorbisComment
	 *
	 * @param coverArt
	 */
	override fun setFromMetadataBlockDataPicture(coverArt: MetadataBlockDataPicture) {
		mimeType = coverArt.mimeType
		description = coverArt.description
		pictureType = coverArt.pictureType
		if (coverArt.isImageUrl) {
			isLinked = coverArt.isImageUrl
			imageUrl = coverArt.getImageUrl()
		} else {
			binaryData = coverArt.imageData
		}
		width = coverArt.width
		height = coverArt.height
	}

	companion object {

		/**
		 * Create Artwork from File
		 *
		 * @param file
		 * @return
		 * @throws IOException
		 */
		@Throws(IOException::class)
		fun createArtworkFromFile(file: File): AndroidArtwork {
			val artwork = AndroidArtwork()
			artwork.setFromFile(file)
			return artwork
		}

		/**
		 *
		 * @param url
		 * @return
		 * @throws IOException
		 */
		@Throws(IOException::class)
		fun createLinkedArtworkFromURL(url: String): AndroidArtwork {
			val artwork = AndroidArtwork()
			artwork.setLinkedFromURL(url)
			return artwork
		}

		/**
		 * Create artwork from Flac block
		 *
		 * @param coverArt
		 * @return
		 */
		fun createArtworkFromMetadataBlockDataPicture(coverArt: MetadataBlockDataPicture): AndroidArtwork {
			val artwork = AndroidArtwork()
			artwork.setFromMetadataBlockDataPicture(coverArt)
			return artwork
		}
	}

}
