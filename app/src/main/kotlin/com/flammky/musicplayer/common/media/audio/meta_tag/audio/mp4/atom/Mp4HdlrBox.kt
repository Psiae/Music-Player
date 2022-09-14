package com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.Mp4AtomIdentifier
import java.nio.ByteBuffer
import java.nio.charset.Charset

/**
 * HdlrBox ( Handler box),
 *
 * Describes the type of metadata in the following ilst or minf atom
 */
class Mp4HdlrBox(header: Mp4BoxHeader?, dataBuffer: ByteBuffer) : AbstractMp4Box() {
	var handlerType // 4 bytes;
		: String? = null
		private set
	var mediaDataType: MediaDataType? = null
		private set

	/**
	 * DataBuffer must start from from the start of the body
	 *
	 * @param header     header info
	 * @param dataBuffer data of box (doesnt include header data)
	 */
	init {
		this.header = header
		this.data = dataBuffer
	}

	@Throws(CannotReadException::class)
	fun processData() {
		//Skip other flags
		val dataBuffer = this.data!!
		dataBuffer.position(dataBuffer.position() + VERSION_FLAG_LENGTH + OTHER_FLAG_LENGTH + RESERVED_FLAG_LENGTH)
		val decoder = Charset.forName("ISO-8859-1").newDecoder()
		try {
			handlerType =
				decoder.decode(dataBuffer.slice().limit(HANDLER_LENGTH) as ByteBuffer).toString()
		} catch (cee: CharacterCodingException) {
			//Ignore
		}

		//To getFields human readable name
		mediaDataType = mediaDataTypeMap!![handlerType]
	}

	override fun toString(): String {
		return "handlerType:" + handlerType + ":human readable:" + mediaDataType!!.description
	}

	enum class MediaDataType(val id: String, val description: String) {
		ODSM(
			"odsm",
			"ObjectDescriptorStream - defined in ISO/IEC JTC1/SC29/WG11 - CODING OF MOVING PICTURES AND AUDIO"
		),
		CRSM(
			"crsm",
			"ClockReferenceStream - defined in ISO/IEC JTC1/SC29/WG11 - CODING OF MOVING PICTURES AND AUDIO"
		),
		SDSM(
			"sdsm",
			"SceneDescriptionStream - defined in ISO/IEC JTC1/SC29/WG11 - CODING OF MOVING PICTURES AND AUDIO"
		),
		M7SM(
			"m7sm",
			"MPEG7Stream - defined in ISO/IEC JTC1/SC29/WG11 - CODING OF MOVING PICTURES AND AUDIO"
		),
		OCSM(
			"ocsm",
			"ObjectContentInfoStream - defined in ISO/IEC JTC1/SC29/WG11 - CODING OF MOVING PICTURES AND AUDIO"
		),
		IPSM(
			"ipsm",
			"IPMP Stream - defined in ISO/IEC JTC1/SC29/WG11 - CODING OF MOVING PICTURES AND AUDIO"
		),
		MJSM(
			"mjsm",
			"MPEG-J Stream - defined in ISO/IEC JTC1/SC29/WG11 - CODING OF MOVING PICTURES AND AUDIO"
		),
		MDIR("mdir", "Apple Meta Data iTunes Reader"), MP7B(
			"mp7b",
			"MPEG-7 binary XML"
		),
		MP7T("mp7t", "MPEG-7 XML"), VIDE("vide", "Video Track"), SOUN(
			"soun",
			"Sound Track"
		),
		HINT("hint", "Hint Track"), APPL("appl", "Apple specific"), META(
			"meta",
			"Timed Metadata track - defined in ISO/IEC JTC1/SC29/WG11 - CODING OF MOVING PICTURES AND AUDIO"
		);

	}

	companion object {
		const val VERSION_FLAG_LENGTH = 1
		const val OTHER_FLAG_LENGTH = 3
		const val RESERVED_FLAG_LENGTH = 4
		const val HANDLER_LENGTH = 4
		const val RESERVED1_LENGTH = 4
		const val RESERVED2_LENGTH = 4
		const val RESERVED3_LENGTH = 4
		const val NAME_LENGTH = 2
		const val HANDLER_POS = VERSION_FLAG_LENGTH + OTHER_FLAG_LENGTH + RESERVED_FLAG_LENGTH
		const val RESERVED1_POS = HANDLER_POS + HANDLER_LENGTH

		//Size used by iTunes, but other application could use different size because name field is variable
		const val ITUNES_META_HDLR_DAT_LENGTH = VERSION_FLAG_LENGTH +
			OTHER_FLAG_LENGTH +
			RESERVED_FLAG_LENGTH +
			HANDLER_LENGTH +
			RESERVED1_LENGTH +
			RESERVED2_LENGTH +
			RESERVED3_LENGTH +
			NAME_LENGTH
		private var mediaDataTypeMap: MutableMap<String?, MediaDataType>? = null

		init {
			//Create maps to speed up lookup from raw value to enum
			mediaDataTypeMap = HashMap()
			val mediaDataTypeMap = mediaDataTypeMap!!
			for (next in MediaDataType.values()) {
				mediaDataTypeMap[next.id] = next
			}
		}

		/**
		 * Create an iTunes style Hdlr box for use within Meta box
		 *
		 *
		 * Useful when writing to mp4 that previously didn't contain an mp4 meta atom
		 *
		 *
		 * Doesnt write the child data but uses it to set the header length, only sets the atoms immediate
		 * data
		 *
		 * @return
		 */
		@JvmStatic
		fun createiTunesStyleHdlrBox(): Mp4HdlrBox {
			val hdlrHeader =
				Mp4BoxHeader(
					Mp4AtomIdentifier.HDLR.fieldName
				)
			hdlrHeader.length = Mp4BoxHeader.Companion.HEADER_LENGTH + ITUNES_META_HDLR_DAT_LENGTH
			val hdlrData =
				ByteBuffer.allocate(ITUNES_META_HDLR_DAT_LENGTH)
			hdlrData.put(
				HANDLER_POS,
				0x6d.toByte()
			) //mdir
			hdlrData.put(
				HANDLER_POS + 1,
				0x64.toByte()
			)
			hdlrData.put(
				HANDLER_POS + 2,
				0x69.toByte()
			)
			hdlrData.put(
				HANDLER_POS + 3,
				0x72.toByte()
			)
			hdlrData.put(
				RESERVED1_POS,
				0x61.toByte()
			) //appl
			hdlrData.put(
				RESERVED1_POS + 1,
				0x70.toByte()
			)
			hdlrData.put(
				RESERVED1_POS + 2,
				0x70.toByte()
			)
			hdlrData.put(
				RESERVED1_POS + 3,
				0x6c.toByte()
			)
			hdlrData.rewind()
			return Mp4HdlrBox(hdlrHeader, hdlrData)
		}
	}
}
