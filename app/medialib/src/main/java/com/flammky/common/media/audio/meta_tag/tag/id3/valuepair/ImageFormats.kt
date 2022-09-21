/**
 * @author : Paul Taylor
 *
 * Version @version:$Id$
 *
 * MusicTag Copyright (C)2003,2004
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public  License as published by the Free Software Foundation; either version 2.1 of the License,
 * or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not,
 * you can get a copy from http://www.opensource.org/licenses/lgpl-license.php or write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * Description:
 * This class maps from v2.2 Image formats (PIC) to v2.3/v2.4 Mimetypes (APIC) and
 * vice versa.
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair

/**
 * Represents common image formats support by ID3 and provides a mapping between the format field supported in ID3v22 and the
 * mimetype field supported by ID3v23/ID3v24.                                                                                                                                    coverImage.getImageData()
 *
 *
 * Note only JPG and PNG are mentioned specifically in the ID3 v22 Spec but it only says 'Image Format is preferably
 * PNG or JPG' , not mandatory. In the jaudiotagger library we also consider GIF as a portable format, and we recognise
 * BMP,PDF and TIFF but do not consider these formats as portable.
 *
 */
//TODO identifying PICT, bit more difficult because in certain formats has an empty 512byte header
object ImageFormats {
	const val V22_JPG_FORMAT = "JPG"
	const val V22_PNG_FORMAT = "PNG"
	const val V22_GIF_FORMAT = "GIF"
	const val V22_BMP_FORMAT = "BMP"
	const val V22_TIF_FORMAT = "TIF"
	const val V22_PDF_FORMAT = "PDF"
	const val V22_PIC_FORMAT = "PIC"
	const val MIME_TYPE_JPEG = "image/jpeg"
	const val MIME_TYPE_PNG = "image/png"
	const val MIME_TYPE_GIF = "image/gif"
	const val MIME_TYPE_BMP = "image/bmp"
	const val MIME_TYPE_TIFF = "image/tiff"
	const val MIME_TYPE_PDF = "image/pdf"
	const val MIME_TYPE_PICT = "image/x-pict"

	/**
	 * Sometimes this is used for jpg instead :or have I made this up
	 */
	const val MIME_TYPE_JPG = "image/jpg"
	private val imageFormatsToMimeType: MutableMap<String, String> = HashMap()
	private val imageMimeTypeToFormat: MutableMap<String, String> = HashMap()

	init {
		imageFormatsToMimeType[V22_JPG_FORMAT] = MIME_TYPE_JPEG
		imageFormatsToMimeType[V22_PNG_FORMAT] = MIME_TYPE_PNG
		imageFormatsToMimeType[V22_GIF_FORMAT] = MIME_TYPE_GIF
		imageFormatsToMimeType[V22_BMP_FORMAT] = MIME_TYPE_BMP
		imageFormatsToMimeType[V22_TIF_FORMAT] = MIME_TYPE_TIFF
		imageFormatsToMimeType[V22_PDF_FORMAT] = MIME_TYPE_PDF
		imageFormatsToMimeType[V22_PIC_FORMAT] = MIME_TYPE_PICT
		var value: String
		for (key in imageFormatsToMimeType.keys) {
			value = imageFormatsToMimeType[key].toString()
			imageMimeTypeToFormat[value] = key
		}

		//The mapping isn't one-one lets add other mimetypes
		imageMimeTypeToFormat[MIME_TYPE_JPG] = V22_JPG_FORMAT
	}

	/**
	 * Get v2.3 mimetype from v2.2 format
	 * @param format
	 * @return
	 */
	@JvmStatic
	fun getMimeTypeForFormat(format: String?): String? {
		return imageFormatsToMimeType[format ?: return null]
	}

	/**
	 * Get v2.2 format from v2.3 mimetype
	 * @param mimeType
	 * @return
	 */
	@JvmStatic
	fun getFormatForMimeType(mimeType: String?): String? {
		return imageMimeTypeToFormat[mimeType ?: return null]
	}

	/**
	 * Is this binary data a png image
	 *
	 * @param data
	 * @return true if binary data matches expected header for a png
	 */
	@JvmStatic
	fun binaryDataIsPngFormat(data: ByteArray): Boolean {
		//Read signature
		return data.size >= 4
			&& 0x89 == (data[0].toInt() and 0xff)
			&& 0x50 == (data[1].toInt() and 0xff)
			&& 0x4E == (data[2].toInt() and 0xff)
			&& 0x47 == (data[3].toInt() and 0xff)
	}

	/**
	 * Is this binary data a jpg image
	 *
	 * @param data
	 * @return true if binary data matches expected header for a jpg
	 *
	 * Some details http://www.obrador.com/essentialjpeg/headerinfo.htm
	 */
	@JvmStatic
	fun binaryDataIsJpgFormat(data: ByteArray): Boolean {
		//Read signature
		return data.size >= 4
			&& 0xff == (data[0].toInt() and 0xff)
			&& 0xd8 == (data[1].toInt() and 0xff)
			&& 0xff == (data[2].toInt() and 0xff)
			&& 0xdb == (data[3].toInt() and 0xff)

		//Can be Can be FF D8 FF DB (samsung) , FF D8 FF E0 (standard) or FF D8 FF E1 or some other formats
		//see http://www.garykessler.net/library/file_sigs.html
		//FF D8 is SOI Marker, FFE0 or FFE1 is JFIF Marker
	}

	/**
	 * Is this binary data a gif image
	 *
	 * @param data
	 * @return true if binary data matches expected header for a gif
	 */
	@JvmStatic
	fun binaryDataIsGifFormat(data: ByteArray): Boolean {
		return data.size >= 3
			&& 0x47 == (data[0].toInt() and 0xff) // G
			&& 0x49 == (data[1].toInt() and 0xff)  // I
			&& 0x46 == (data[2].toInt() and 0xff) // F
		//Read signature
	}

	/**
	 *
	 * Is this binary data a bmp image
	 *
	 * @param data
	 * @return true if binary data matches expected header for a bmp
	 */
	@JvmStatic
	fun binaryDataIsBmpFormat(data: ByteArray): Boolean {
		//Read signature
		return data.size >= 2
			&& 0x42 == (data[0].toInt() and 0xff) // B
			&& 0x4d == (data[1].toInt() and 0xff) // M
	}

	/**
	 * Is this binary data a pdf image
	 *
	 * Details at http://en.wikipedia.org/wiki/Magic_number_%28programming%29
	 *
	 * @param data
	 * @return true if binary data matches expected header for a pdf
	 */
	fun binaryDataIsPdfFormat(data: ByteArray): Boolean {
		//Read signature
		return data.size >= 4
			&& 0x25 == (data[0].toInt() and 0xff) // %
			&& 0x50 == (data[1].toInt() and 0xff) // P
			&& 0x44 == (data[2].toInt() and 0xff) // D
			&& 0x46 == (data[0].toInt() and 0xff) // F
	}

	/**
	 * is this binary data a tiff image
	 *
	 * Details at http://en.wikipedia.org/wiki/Magic_number_%28programming%29
	 * @param data
	 * @return true if binary data matches expected header for a tiff
	 */

	/** [org.jaudiotagger.tag.id3.valuepair.ImageFormats] */
	fun binaryDataIsTiffFormat(data: ByteArray): Boolean {
		//Read signature Intel
		return data.size >= 4
			&&
			((0x49 == (data[0].toInt() and 0xff)
				&& 0x49 == (data[1].toInt() and 0xff)
				&& 0x2a == (data[2].toInt() and 0xff)
				&& 0x00 == (data[3].toInt() and 0xff))
				||
				(0x4d == (data[0].toInt() and 0xff)
					&& 0x4d == (data[1].toInt() and 0xff)
					&& 0x00 == (data[2].toInt() and 0xff)
					&& 0x2a == (data[3].toInt() and 0xff)))
	}

	/**
	 *
	 * @param data
	 * @return true if the image format is a portable format recognised across operating systems
	 */
	fun isPortableFormat(data: ByteArray): Boolean {
		return binaryDataIsPngFormat(data)
			|| binaryDataIsJpgFormat(data)
			|| binaryDataIsGifFormat(data)
	}

	/**
	 *
	 * @param data
	 * @return correct mimetype for the image data represented by this byte data
	 */
	@JvmStatic
	fun getMimeTypeForBinarySignature(data: ByteArray): String? {
		return if (binaryDataIsPngFormat(data)) {
			MIME_TYPE_PNG
		} else if (binaryDataIsJpgFormat(
				data
			)
		) {
			MIME_TYPE_JPEG
		} else if (binaryDataIsGifFormat(
				data
			)
		) {
			MIME_TYPE_GIF
		} else if (binaryDataIsBmpFormat(
				data
			)
		) {
			MIME_TYPE_BMP
		} else if (binaryDataIsPdfFormat(
				data
			)
		) {
			MIME_TYPE_PDF
		} else if (binaryDataIsTiffFormat(
				data
			)
		) {
			MIME_TYPE_TIFF
		} else {
			null
		}
	}
}
