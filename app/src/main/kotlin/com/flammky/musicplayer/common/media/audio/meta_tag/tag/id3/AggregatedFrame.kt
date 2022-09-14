package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagField
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagTextField
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.TextEncoding
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset

/**
 * Required when a single generic field maps to multiple ID3 Frames
 */
open class AggregatedFrame : TagTextField {
	//TODO rather than just maintaining insertion order we want to define a preset order

	protected var mFrames: MutableSet<AbstractID3v2Frame> = LinkedHashSet()

	fun addFrame(frame: AbstractID3v2Frame) {
		mFrames.add(frame)
	}

	fun getFrames(): Set<AbstractID3v2Frame> {
		return mFrames
	}
	/**
	 * Returns the content of the underlying frames in order.
	 *
	 * @return Content
	 */
	/**
	 * Sets the content of the field.
	 *
	 * @param content fields content.
	 */
	override var content: String?
		get() {
			val sb = StringBuilder()
			for (next in mFrames) {
				sb.append(next.content)
			}
			return sb.toString()
		}
		set(content) {}
	/**
	 * Returns the current used charset encoding.
	 *
	 * @return Charset encoding.
	 */
	/**
	 * Sets the charset encoding used by the field.
	 *
	 * @param encoding charset.
	 */
	override var encoding: Charset?
		get() {
			val textEncoding = mFrames.iterator().next().body!!.textEncoding
			return TextEncoding.instanceOf.getCharsetForId(textEncoding.toInt())
		}
		set(encoding) {}

	//TODO:needs implementing but not sure if this method is required at all
	override fun copyContent(field: TagField?) {}
	override val id: String
		get() {
			val sb = StringBuilder()
			for (next in mFrames) {
				sb.append(next.id)
			}
			return sb.toString()
		}
	override val isCommon: Boolean
		get() = true
	override val isBinary: Boolean
		get() = false

	override fun isBinary(b: Boolean) {
	}

	override val isEmpty: Boolean
		get() = false

	@get:Throws(UnsupportedEncodingException::class)
	override val rawContent: ByteArray
		get() {
			throw UnsupportedEncodingException()
		}

	override fun toDescriptiveString(): String = content ?: "AggregatedFrameEmpty"
}
