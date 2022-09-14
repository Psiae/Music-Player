package com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/**
 * Ftyp (File Type) is the first atom, can be used to help identify the mp4 container type
 */
class Mp4FtypBox(header: Mp4BoxHeader?, dataBuffer: ByteBuffer) : AbstractMp4Box() {
	var majorBrand: String? = null
		private set
	var majorBrandVersion = 0
		private set
	private val compatibleBrands: MutableList<String> = ArrayList()

	/**
	 * @param header     header info
	 * @param dataBuffer data of box (doesnt include header data)
	 */
	init {
		this.header = header
		this.data = dataBuffer
		this.data!!.order(ByteOrder.BIG_ENDIAN)
	}

	@Throws(CannotReadException::class)
	fun processData() {
		val dataBuffer = data!!
		val decoder = Charset.forName("ISO-8859-1").newDecoder()
		try {
			majorBrand = decoder.decode(dataBuffer.slice().limit(MAJOR_BRAND_LENGTH) as ByteBuffer)
				.toString()
		} catch (cee: CharacterCodingException) {
			//Ignore
		}
		dataBuffer.position(dataBuffer.position() + MAJOR_BRAND_LENGTH)
		majorBrandVersion = dataBuffer.int
		while (dataBuffer.position() < dataBuffer.limit() && dataBuffer.limit() - dataBuffer.position() >= COMPATIBLE_BRAND_LENGTH) {
			decoder.onMalformedInput(CodingErrorAction.REPORT)
			decoder.onMalformedInput(CodingErrorAction.REPORT)
			try {
				val brand =
					decoder.decode(dataBuffer.slice().limit(COMPATIBLE_BRAND_LENGTH) as ByteBuffer)
						.toString()
				//Sometimes just extra groups of four nulls
				if (brand != "\u0000\u0000\u0000\u0000") {
					compatibleBrands.add(brand)
				}
			} catch (cee: CharacterCodingException) {
				//Ignore
			}
			dataBuffer.position(dataBuffer.position() + COMPATIBLE_BRAND_LENGTH)
		}
	}

	override fun toString(): String {
		var info = "Major Brand:" + majorBrand + "Version:" + majorBrandVersion
		if (compatibleBrands.size > 0) {
			info += "Compatible:"
			for (brand in compatibleBrands) {
				info += brand
				info += ","
			}
			return info.substring(0, info.length - 1)
		}
		return info
	}

	fun getCompatibleBrands(): List<String> {
		return compatibleBrands
	}

	/**
	 * Major brand, helps identify whats contained in the file, used by major and compatible brands
	 * but this is not an exhaustive list, so for that reason we don't force the values read from the file
	 * to tie in with this enum.
	 */
	enum class Brand
	/**
	 * @param id          it is stored as in file
	 * @param description human readable description
	 */(//SOmetimes used by protected mutli track audio
		val id: String, val description: String
	) {
		ISO14496_1_BASE_MEDIA("isom", "ISO 14496-1"), ISO14496_12_BASE_MEDIA(
			"iso2",
			"ISO 14496-12"
		),
		ISO14496_1_VERSION_1("mp41", "ISO 14496-1"), ISO14496_1_VERSION_2(
			"mp42",
			"ISO 14496-2:Multi track with BIFS scenes"
		),
		QUICKTIME_MOVIE("qt  ", "Original Quicktime"), JVT_AVC(
			"avc1",
			"JVT"
		),
		THREEG_MOBILE_MP4("MPA ", "3G Mobile"), APPLE_AAC_AUDIO(
			"M4P ",
			"Apple Audio"
		),
		AES_ENCRYPTED_AUDIO("M4B ", "Apple encrypted Audio"), APPLE_AUDIO(
			"mp71",
			"Apple Audio"
		),
		ISO14496_12_MPEG7_METADATA("mp71", "MAIN_SYNTHESIS"), APPLE_AUDIO_ONLY("M4A ", "M4A Audio");

	}

	companion object {
		private const val MAJOR_BRAND_LENGTH = 4
		private const val COMPATIBLE_BRAND_LENGTH = 4 //Can be multiple of these
	}
}
