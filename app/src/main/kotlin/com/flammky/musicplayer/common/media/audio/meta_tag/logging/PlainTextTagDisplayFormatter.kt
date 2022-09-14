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
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.logging

/*
 * For Formatting metadata contents of a file as simple text
*/
class PlainTextTagDisplayFormatter : AbstractTagDisplayFormatter() {
	var sb = StringBuffer()
	var indent = StringBuffer()
	override fun openHeadingElement(type: String, value: String) {
		addElement(type, value)
		increaseLevel()
	}

	override fun openHeadingElement(type: String, value: Boolean) {
		openHeadingElement(type, value.toString())
	}

	override fun openHeadingElement(type: String, value: Int) {
		openHeadingElement(type, value.toString())
	}

	override fun closeHeadingElement(type: String) {
		decreaseLevel()
	}

	fun increaseLevel() {
		level++
		indent.append("  ")
	}

	fun decreaseLevel() {
		level--
		indent = StringBuffer(indent.substring(0, indent.length - 2))
	}

	override fun addElement(type: String, value: String?) {
		sb.append(indent).append(type).append(":").append(value).append('\n')
	}

	override fun addElement(type: String, value: Int) {
		addElement(type, value.toString())
	}

	override fun addElement(type: String, value: Boolean) {
		addElement(type, value.toString())
	}

	override fun toString(): String {
		return sb.toString()
	}

	companion object {
		private var formatter: PlainTextTagDisplayFormatter? = null
		val instanceOf: AbstractTagDisplayFormatter?
			get() {
				if (formatter == null) {
					formatter = PlainTextTagDisplayFormatter()
				}
				return formatter
			}
	}
}
