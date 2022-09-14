package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody

/**
 * Represents a frameBody for a frame identifier that is not defined for the tag version but was valid for a for an
 * earlier tag version.
 * The body consists  of an array of bytes representing all the bytes in the body.
 */
class FrameBodyDeprecated : AbstractID3v2FrameBody, ID3v24FrameBody, ID3v23FrameBody {

	/**
	 * Return the original frameBody that was used to construct the DeprecatedFrameBody
	 *
	 * @return the original frameBody
	 */
	/** The original frameBody is held so can be retrieved
	 * when converting a DeprecatedFrameBody back to a normal framebody
	 */
	var originalFrameBody: AbstractID3v2FrameBody? = null
		private set

	/**
	 * Creates a new FrameBodyDeprecated wrapper around the frameBody
	 * @param frameBody
	 */
	constructor(frameBody: AbstractID3v2FrameBody?) {
		originalFrameBody = frameBody
	}

	/**
	 * Copy constructor
	 *
	 * @param copyObject a copy is made of this
	 */
	constructor(copyObject: FrameBodyDeprecated) : super(copyObject)

	/**
	 * Return the frame identifier
	 *
	 * @return the identifier
	 */
	override val identifier: String
		get() = originalFrameBody?.identifier.toString()

	/**
	 * Delgate size to size of original frameBody, if frameBody already exist will take this value from the frame header
	 * but it is always recalculated before writing any changes back to disk.
	 *
	 * @return size in bytes of this frame body
	 */
	override var size: Int
		get() = originalFrameBody!!.size
		set(size) {
			super.size = size
		}

	/**
	 * @param obj
	 * @return whether obj is equivalent to this object
	 */
	override fun equals(obj: Any?): Boolean {
		if (obj !is FrameBodyDeprecated) {
			return false
		}
		return identifier == obj.identifier && super.equals(obj)
	}

	/**
	 * Because the contents of this frame are an array of bytes and could be large we just
	 * return the identifier.
	 *
	 * @return a string representation of this frame
	 */
	override fun toString(): String {
		return identifier
	}

	/**
	 * Setup the Object List.
	 *
	 * This is handled by the wrapped class
	 */
	override fun setupObjectList() {}

	//TODO When is this null, it seems it can be but Im not sure why
	override val briefDescription: String
		get() =//TODO When is this null, it seems it can be but Im not sure why
			if (originalFrameBody != null) {
				originalFrameBody!!.briefDescription
			} else ""
}
