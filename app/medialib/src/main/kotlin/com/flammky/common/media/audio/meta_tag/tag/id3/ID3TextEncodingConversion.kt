package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagOptionSingleton
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.TextEncoding
import java.util.logging.Logger

/**
 * Functions to encode text according to encodingoptions and ID3 version
 */
object ID3TextEncodingConversion {
	//Logger
	var logger = Logger.getLogger("org.jaudiotagger.tag.id3")

	/**
	 * Check the text encoding is valid for this header type and is appropriate for
	 * user text encoding options.                                             *
	 *
	 * This is called before writing any frames that use text encoding
	 *
	 * @param header       used to identify the ID3tagtype
	 * @param textEncoding currently set
	 * @return valid encoding according to version type and user options
	 */
	@JvmStatic
	fun getTextEncoding(header: AbstractTagFrame?, textEncoding: Byte): Byte {

		//Should not happen, assume v23 and provide a warning
		return if (header == null) {
			logger.warning(
				"Header has not yet been set for this framebody"
			)
			if (TagOptionSingleton.instance.isResetTextEncodingForExistingFrames
			) {
				TagOptionSingleton.instance.getId3v23DefaultTextEncoding()
			} else {
				convertV24textEncodingToV23textEncoding(
					textEncoding
				)
			}
		} else if (header is ID3v24Frame) {
			if (TagOptionSingleton.instance.isResetTextEncodingForExistingFrames
			) {
				//Replace with default
				TagOptionSingleton.instance.getId3v24DefaultTextEncoding()
			} else {
				//All text encodings supported nothing to do
				textEncoding
			}
		} else {
			if (TagOptionSingleton.instance.isResetTextEncodingForExistingFrames
			) {
				//Replace with default
				TagOptionSingleton.instance.getId3v23DefaultTextEncoding()
			} else {
				//If text encoding is an unsupported v24 one we use unicode v23 equivalent
				convertV24textEncodingToV23textEncoding(
					textEncoding
				)
			}
		}
	}

	/**
	 * Sets the text encoding to best Unicode type for the version
	 *
	 * @param header
	 * @return
	 */
	@JvmStatic
	fun getUnicodeTextEncoding(header: AbstractTagFrame?): Byte {
		return if (header == null) {
			logger.warning(
				"Header has not yet been set for this framebody"
			)
			TextEncoding.UTF_16
		} else if (header is ID3v24Frame) {
			TagOptionSingleton.instance.getId3v24UnicodeTextEncoding()
		} else {
			TextEncoding.UTF_16
		}
	}

	/**
	 * Convert v24 text encoding to a valid v23 encoding
	 *
	 * @param textEncoding
	 * @return valid encoding
	 */
	private fun convertV24textEncodingToV23textEncoding(textEncoding: Byte): Byte {
		//Convert to equivalent UTF16 format
		return if (textEncoding == TextEncoding.UTF_16BE) {
			TextEncoding.UTF_16
		} else if (textEncoding == TextEncoding.UTF_8) {
			TextEncoding.ISO_8859_1
		} else {
			textEncoding
		}
	}
}
