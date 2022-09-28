package com.flammky.android.medialib.common.audio

/**
 * Audio attributes specifications
 *
 * @see android.media.AudioAttributes
 */
class AudioAttributes private constructor() {


	sealed interface ContentType {

	}



	class Builder() {

		fun build(): AudioAttributes = AudioAttributes()
	}

	companion object {

		val DEFAULT = build {}

		@JvmStatic
		fun build(apply: Builder.() -> Unit): AudioAttributes {
			return Builder().apply(apply).build()
		}
	}
}
