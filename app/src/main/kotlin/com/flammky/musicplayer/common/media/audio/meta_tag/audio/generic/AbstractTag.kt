/*
 * jaudiotagger library
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.images.Artwork
import java.nio.charset.Charset

/**
 * This class is the default implementation for
 * [Tag] and introduces some more useful
 * functionality to be implemented.<br></br>
 *
 * @author Raphaël Slinckx
 */
abstract class AbstractTag : Tag {
	/**
	 * Stores the amount of [TagField] with [TagField.isCommon]
	 * `true`.
	 */
	protected var commonNumber = 0

	/**
	 * This map stores the [ids][TagField.getId] of the stored
	 * fields to the [fields][TagField] themselves. Because a linked hashMap is used the order
	 * that they are added in is preserved, the only exception to this rule is when two fields of the same id
	 * exist, both will be returned according to when the first item was added to the file. <br></br>
	 */
	protected var fieldMap: MutableMap<String?, MutableList<TagField?>> = LinkedHashMap()

	/**
	 * Add field
	 *
	 * @see Tag.addField
	 */
	override fun addField(field: TagField?) {
		if (field == null) {
			return
		}
		var list = fieldMap[field.id]

		// There was no previous item
		if (list == null) {
			list = ArrayList()
			list.add(field)
			fieldMap[field.id] = list
			if (field.isCommon) {
				commonNumber++
			}
		} else {
			// We append to existing list
			list.add(field)
		}
	}

	/**
	 * Get list of fields within this tag with the specified id
	 *
	 * @see Tag.getFields
	 */
	override fun getFields(id: String?): List<TagField?> {
		return fieldMap[id] ?: ArrayList()
	}

	@Throws(KeyNotFoundException::class)
	fun getAll(id: String?): List<String> {
		val fields: MutableList<String> = ArrayList()
		val tagFields = getFields(id)
		for (tagField in tagFields) {
			fields.add(tagField.toString())
		}
		return fields
	}

	/**
	 *
	 * @param id
	 * @param index
	 * @return
	 */
	fun getItem(id: String?, index: Int): String {
		val l = getFields(id)
		return if (l.size > index) l[index].toString() else ""
	}

	/**
	 * Retrieve the first value that exists for this generic key
	 *
	 * @param genericKey
	 * @return
	 */
	@Throws(KeyNotFoundException::class)
	override fun getFirst(genericKey: FieldKey?): String? {
		return getValue(genericKey, 0)
	}

	override fun getFirst(id: String?): String? {
		val l = getFields(id)
		return if (l.isNotEmpty()) l[0].toString() else ""
	}

	override fun getFirstField(id: String?): TagField? {
		val l = getFields(id)
		return if (l.isNotEmpty()) l[0] else null
	}

	val all: List<TagField?>
		get() {
			val fieldList: MutableList<TagField?> = ArrayList()
			for (listOfFields in fieldMap.values) {
				for (next in listOfFields) {
					fieldList.add(next)
				}
			}
			return fieldList
		}

	override val fields: MutableIterator<TagField>
		get() {
			val it: Iterator<Map.Entry<String?, MutableList<TagField?>>> = fieldMap.entries.iterator()
			return object : MutableIterator<TagField> {
				private var fieldsIt: MutableIterator<TagField?>? = null
				private fun changeIt() {
					if (!it.hasNext()) {
						return
					}
					val (_, l) = it.next()
					fieldsIt = l.iterator()
				}

				override fun hasNext(): Boolean {
					if (fieldsIt == null) {
						changeIt()
					}
					return it.hasNext() || fieldsIt != null && fieldsIt?.hasNext() ?: false
				}

				override fun next(): TagField {
					if (!fieldsIt!!.hasNext()) {
						changeIt()
					}
					return fieldsIt!!.next()!!
				}

				override fun remove() {
					fieldsIt!!.remove()
				}
			}
		}

	/**
	 * Return field count
	 *
	 * TODO:There must be a more efficient way to do this.
	 *
	 * @return field count
	 */
	override val fieldCount: Int
		get() {
			val it = fields
			var count = 0
			while (it.hasNext()) {
				count++
				it.next()
			}
			return count
		}
	override val fieldCountIncludingSubValues: Int
		get() = fieldCount

	/**
	 * Does this tag contain any comon fields
	 *
	 * @see Tag.hasCommonFields
	 */
	override fun hasCommonFields(): Boolean {
		return commonNumber != 0
	}

	/**
	 * Does this tag contain a field with the specified id
	 *
	 * @see Tag.hasField
	 */
	override fun hasField(id: String?): Boolean {
		return getFields(id).isNotEmpty()
	}

	override fun hasField(fieldKey: FieldKey?): Boolean {
		return hasField(fieldKey!!.name)
	}

	/**
	 * Determines whether the given charset encoding may be used for the
	 * represented tagging system.
	 *
	 * @param enc charset encoding.
	 * @return `true` if the given encoding can be used.
	 */
	protected abstract fun isAllowedEncoding(enc: Charset?): Boolean

	/**
	 * Is this tag empty
	 *
	 * @see Tag.isEmpty
	 */
	override val isEmpty: Boolean
		get() = fieldMap.isEmpty()

	/**
	 * Create new field and set it in the tag
	 *
	 * @param genericKey
	 * @param value
	 * @throws KeyNotFoundException
	 * @throws FieldDataInvalidException
	 */
	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun setField(genericKey: FieldKey?, vararg value: String?) {
		val tagfield = createField(genericKey, *value)
		setField(tagfield)
	}

	/**
	 * Create new field and add it to the tag
	 *
	 * @param genericKey
	 * @param value
	 * @throws KeyNotFoundException
	 * @throws FieldDataInvalidException
	 */
	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun addField(genericKey: FieldKey?, vararg value: String?) {
		val tagfield = createField(genericKey, *value)
		addField(tagfield)
	}

	/**
	 * Set field
	 *
	 * Changed:Just because field is empty it doesn't mean it should be deleted. That should be the choice
	 * of the developer. (Or does this break things)
	 *
	 * @see Tag.setField
	 */
	override fun setField(field: TagField?) {
		if (field == null) {
			return
		}

		// If there is already an existing field with same id
		// and both are TextFields, we replace the first element
		var list = fieldMap[field.id]
		if (list != null) {
			list[0] = field
			return
		}

		// Else we put the new field in the fields.
		list = ArrayList()
		list.add(field)
		fieldMap[field.id] = list
		if (field.isCommon) {
			commonNumber++
		}
	}

	/**
	 * Set or add encoding
	 *
	 * @see Tag.setEncoding
	 */
	override fun setEncoding(enc: Charset?): Boolean {
		if (!isAllowedEncoding(enc)) {
			return false
		}
		val it = fields
		while (it.hasNext()) {
			val field = it.next()
			if (field is TagTextField) {
				field.encoding = enc
			}
		}
		return true
	}

	/**
	 * (overridden)
	 *
	 * @see Object.toString
	 */
	override fun toString(): String {
		val out = StringBuffer()
		out.append("Tag content:\n")
		val it = fields
		while (it.hasNext()) {
			val field = it.next()
			out.append("\t")
			out.append(field.id)
			out.append(":")
			out.append(field.toDescriptiveString())
			out.append("\n")
		}
		return out.toString().substring(0, out.length - 1)
	}

	/**
	 *
	 * @param genericKey
	 * @param value
	 * @return
	 * @throws KeyNotFoundException
	 * @throws FieldDataInvalidException
	 */
	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	abstract override fun createField(genericKey: FieldKey?, vararg value: String?): TagField?

	/**
	 *
	 * @param id
	 * @return
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	abstract override fun getFirstField(id: FieldKey?): TagField?

	/**
	 *
	 * @param fieldKey
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	abstract override fun deleteField(fieldKey: FieldKey?)

	/**
	 * Delete all occurrences of field with this id.
	 *
	 * @param key
	 */
	override fun deleteField(key: String?) {
		fieldMap.remove(key)
	}

	override val firstArtwork: Artwork?
		get() {
			val artwork: List<Artwork>? = artworkList
			return if (!artwork.isNullOrEmpty()) {
				artwork[0]
			} else null
		}

	/**
	 * Create field and then set within tag itself
	 *
	 * @param artwork
	 * @throws FieldDataInvalidException
	 */
	@Throws(FieldDataInvalidException::class)
	override fun setField(artwork: Artwork?) {
		this.setField(createField(artwork))
	}

	/**
	 * Create field and then add within tag itself
	 *
	 * @param artwork
	 * @throws FieldDataInvalidException
	 */
	@Throws(FieldDataInvalidException::class)
	override fun addField(artwork: Artwork?) {
		this.addField(createField(artwork))
	}

	/**
	 * Delete all instance of artwork Field
	 *
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	override fun deleteArtworkField() {
		this.deleteField(FieldKey.COVER_ART)
	}
}
