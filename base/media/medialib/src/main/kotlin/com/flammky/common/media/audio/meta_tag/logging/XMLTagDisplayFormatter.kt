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

import java.text.CharacterIterator
import java.text.StringCharacterIterator

/*
 * For Formatting the metadata contents of a file in an XML format
 *
 * This could provide the basis of a representation of a files metadata, which can then be manipulated to
* to create technical reports.
*/
class XMLTagDisplayFormatter : AbstractTagDisplayFormatter() {
	var sb = StringBuffer()
	override fun openHeadingElement(type: String, value: String) {
		if (value.isEmpty()) {
			sb.append(xmlOpen(type))
		} else {
			sb.append(xmlOpenHeading(type, replaceXMLCharacters(value)))
		}
	}

	override fun openHeadingElement(type: String, value: Boolean) {
		openHeadingElement(type, value.toString())
	}

	override fun openHeadingElement(type: String, value: Int) {
		openHeadingElement(type, value.toString())
	}

	override fun closeHeadingElement(type: String) {
		sb.append(xmlClose(type))
	}

	override fun addElement(type: String, value: String?) {
		sb.append(xmlFullTag(type, replaceXMLCharacters(value)))
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
		protected const val xmlOpenStart = "<"
		protected const val xmlOpenEnd = ">"
		protected const val xmlCloseStart = "</"
		protected const val xmlCloseEnd = ">"
		protected const val xmlSingleTagClose = " />"
		protected const val xmlCDataTagOpen = "<![CDATA["
		protected const val xmlCDataTagClose = "]]>"

		/**
		 * Return xml open tag round a string e.g <tag>
		 * @param xmlName
		 * @return
		</tag> */
		fun xmlOpen(xmlName: String): String {
			return xmlOpenStart + xmlName + xmlOpenEnd
		}

		fun xmlOpenHeading(name: String, data: String): String {
			return xmlOpen(
				"$name id=\"$data\""
			)
		}

		/**
		 * Return CDATA tag around xml data e.g <![CDATA[xmlData]]>
		 * We also need to deal with special chars
		 * @param xmlData
		 * @return
		 */
		fun xmlCData(xmlData: String): String {
			var tempChar: Char
			val replacedString = StringBuffer()
			for (i in 0 until xmlData.length) {
				tempChar = xmlData[i]
				if (Character.isLetterOrDigit(tempChar) || Character.isSpaceChar(tempChar)) {
					replacedString.append(tempChar)
				} else {
					replacedString.append("&#x")
						.append(Integer.toString(Character.codePointAt(xmlData, i), 16))
				}
			}
			return xmlCDataTagOpen + replacedString + xmlCDataTagClose
		}

		/**
		 * Return xml close tag around a string e.g
		 * @param xmlName
		 * @return
		 */
		fun xmlClose(xmlName: String): String {
			return xmlCloseStart + xmlName + xmlCloseEnd
		}

		fun xmlSingleTag(data: String): String {
			return xmlOpenStart + data + xmlSingleTagClose
		}

		fun xmlFullTag(xmlName: String, data: String): String {
			return xmlOpen(xmlName) + xmlCData(data) + xmlClose(xmlName)
		}

		/**
		 * Replace any special xml characters with the appropiate escape sequences
		 * required to be done for the actual element names
		 * @param xmlData
		 * @return
		 */
		fun replaceXMLCharacters(xmlData: String?): String {
			val sb = StringBuffer()
			val sCI = StringCharacterIterator(xmlData)
			var c = sCI.first()
			while (c != CharacterIterator.DONE) {
				when (c) {
					'&' -> sb.append("&amp;")
					'<' -> sb.append("&lt;")
					'>' -> sb.append("&gt;")
					'"' -> sb.append("&quot;")
					'\'' -> sb.append("&apos;")
					else -> sb.append(c)
				}
				c = sCI.next()
			}
			return sb.toString()
		}
	}
}
