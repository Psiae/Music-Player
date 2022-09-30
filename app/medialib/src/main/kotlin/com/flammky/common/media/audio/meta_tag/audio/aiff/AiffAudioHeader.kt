package com.flammky.musicplayer.common.media.audio.meta_tag.audio.aiff

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.GenericAudioHeader
import java.util.*

/**
 * Non-"tag" metadata from the AIFF file. In general, read-only.
 */
class AiffAudioHeader : GenericAudioHeader() {
	enum class Endian {
		BIG_ENDIAN, LITTLE_ENDIAN
	}
	/**
	 * Return the file type (AIFF or AIFC)
	 */
	/**
	 * Set the file type (AIFF or AIFC)
	 */
	var fileType: AiffType? = null
	/**
	 * Return the timestamp of the file.
	 */
	/**
	 * Set the timestamp.
	 */
	var timestamp: Date? = null
	/**
	 * Return endian status (big or little)
	 */
	/**
	 * Set endian status (big or little)
	 */
	var endian: Endian
	/**
	 * Return the name. May be null.
	 */
	/**
	 * Set the name
	 */
	//    private String audioEncoding;
	var name: String? = null
	/**
	 * Return the author
	 */
	/**
	 * Set the author
	 */
	var author: String? = null
	/**
	 * Return the copyright. May be null.
	 */
	/**
	 * Set the copyright
	 */
	var copyright: String? = null
	private val applicationIdentifiers: MutableList<String>
	private val comments: MutableList<String>
	private val annotations: MutableList<String>

	init {
		applicationIdentifiers = ArrayList()
		comments = ArrayList()
		annotations = ArrayList()
		endian = Endian.BIG_ENDIAN
	}

	/**
	 * Return list of all application identifiers
	 */
	fun getApplicationIdentifiers(): List<String> {
		return applicationIdentifiers
	}

	/**
	 * Add an application identifier. There can be any number of these.
	 */
	fun addApplicationIdentifier(id: String) {
		applicationIdentifiers.add(id)
	}

	/**
	 * Return list of all annotations
	 */
	fun getAnnotations(): List<String> {
		return annotations
	}

	/**
	 * Add an annotation. There can be any number of these.
	 */
	fun addAnnotation(a: String) {
		annotations.add(a)
	}

	/**
	 * Return list of all comments
	 */
	fun getComments(): List<String> {
		return comments
	}

	/**
	 * Add a comment. There can be any number of these.
	 */
	fun addComment(c: String) {
		comments.add(c)
	}

	override fun toString(): String {
		val sb = StringBuilder("\n")
		if (name != null && !name!!.isEmpty()) {
			sb.append("\tName:$name\n")
		}
		if (author != null && !author!!.isEmpty()) {
			sb.append("\tAuthor:$author\n")
		}
		if (copyright != null && !copyright!!.isEmpty()) {
			sb.append("\tCopyright:$copyright\n")
		}
		if (comments.size > 0) {
			sb.append("Comments:\n")
			for (next in comments) {
				sb.append(
					"""	$next
"""
				)
			}
		}
		if (applicationIdentifiers.size > 0) {
			sb.append("ApplicationIds:\n")
			for (next in applicationIdentifiers) {
				sb.append(
					"""	$next
"""
				)
			}
		}
		if (annotations.size > 0) {
			sb.append("Annotations:\n")
			for (next in annotations) {
				sb.append(
					"""	$next
"""
				)
			}
		}
		return super.toString() + sb.toString()
	}
}
