/**
 * @author : Paul Taylor
 *
 * Version @version:$Id$
 * Date :${DATE}
 *
 * Jaikoz Copyright Copyright (C) 2003 -2005 JThink Ltd
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions

/**
 * Thrown if portion of file thought to be an AudioFrame is found to not be.
 */
class InvalidAudioFrameException(message: String?) : Exception(message) {
	companion object {
		/**
		 *
		 */
		private const val serialVersionUID = 7213597113547233971L
	}
}
