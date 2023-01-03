/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2010 Raphaël Slinckx <raphael@slinckx.net>
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
package com.flammky.musicplayer.common.media.audio.meta_tag.tag

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.images.Artwork
import java.nio.charset.Charset

/**
 * This interface represents the basic data structure for the default
 * audio library functionality.<br></br>
 *
 * Some audio file tagging systems allow to specify multiple values for one type
 * of information. The artist for example. Some songs may be a cooperation of
 * two or more artists. Sometimes a tagging user wants to specify them in the
 * tag without making one long text string.<br></br>
 *
 * The addField() method can be used for this but it is possible the underlying implementation
 * does not support that kind of storing multiple values and will just overwrite the existing value<br></br>
 * <br></br>
 * **Code Examples:**<br></br>
 *
 * <pre>
 * `
 * AudioFile file = AudioFileIO.read(new File("C:\\test.mp3"));
 *
 * Tag tag = file.getTag();
` *
</pre> *
 *
 * @author Raphael Slinckx
 * @author Paul Taylor
 */
interface Tag {
	/**
	 * Create the field based on the generic key and set it in the tag
	 *
	 * @param genericKey
	 * @param value
	 * @throws KeyNotFoundException
	 * @throws FieldDataInvalidException
	 */
	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	fun setField(genericKey: FieldKey?, vararg value: String?)

	/**
	 * Create the field based on the generic key and add it to the tag
	 *
	 * This is handled differently by different formats
	 *
	 * @param genericKey
	 * @param value
	 * @throws KeyNotFoundException
	 * @throws FieldDataInvalidException
	 */
	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	fun addField(genericKey: FieldKey?, vararg value: String?)

	/**
	 * Delete any fields with this key
	 *
	 * @param fieldKey
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	fun deleteField(fieldKey: FieldKey?)

	/**
	 * Delete any fields with this Flac (Vorbis Comment) id
	 *
	 * @param key
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	fun deleteField(key: String?)

	/**
	 * Returns a [list][List] of [TagField] objects whose &quot;[id][TagField.getId]&quot;
	 * is the specified one.<br></br>
	 *
	 *
	 * Can be used to retrieve fields with any identifier, useful if the identifier is not within [FieldKey]
	 *
	 * @param id The field id.
	 * @return A list of [TagField] objects with the given &quot;id&quot;.
	 */
	fun getFields(id: String?): List<TagField?>

	/**
	 * Returns a [list][List] of [TagField] objects whose &quot;[id][TagField.getId]&quot;
	 * is the specified one.<br></br>
	 *
	 * @param id The field id.
	 * @return A list of [TagField] objects with the given &quot;id&quot;.
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	fun getFields(id: FieldKey?): List<TagField?>?

	/**
	 * Iterator over all the fields within the tag, handle multiple fields with the same id
	 *
	 * @return iterator over whole list
	 */
	val fields: Iterator<TagField?>

	/**
	 * Retrieve String value of the first value that exists for this format specific key
	 *
	 *
	 * Can be used to retrieve fields with any identifier, useful if the identifier is not within [FieldKey]
	 *
	 * @param id
	 * @return
	 */
	fun getFirst(id: String?): String?

	/**
	 * Retrieve String value of the first tag field that exists for this generic key
	 *
	 * @param id
	 * @return String value or empty string
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	fun getFirst(id: FieldKey?): String?

	/**
	 * Retrieve all String values that exist for this generic key
	 *
	 * @param id
	 * @return
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	fun getAll(id: FieldKey?): List<String?>?

	/**
	 * Retrieve String value of the nth tag field that exists for this generic key
	 *
	 * @param id
	 * @param n
	 * @return
	 */
	fun getValue(id: FieldKey?, n: Int): String?

	/**
	 * Retrieve the first field that exists for this format specific key
	 *
	 *
	 * Can be used to retrieve fields with any identifier, useful if the identifier is not within [FieldKey]
	 *
	 * @param id audio specific key
	 * @return tag field or null if doesn't exist
	 */
	fun getFirstField(id: String?): TagField?

	/**
	 * @param id
	 * @return the first field that matches this generic key
	 */
	fun getFirstField(id: FieldKey?): TagField?

	/**
	 * Returns `true`, if at least one of the contained
	 * [fields][TagField] is a common field ([TagField.isCommon]).
	 *
	 * @return `true` if a [common][TagField.isCommon]
	 * field is present.
	 */
	fun hasCommonFields(): Boolean

	/**
	 * Determines whether the tag has at least one field with the specified field key.
	 *
	 * @param fieldKey
	 * @return
	 */
	fun hasField(fieldKey: FieldKey?): Boolean

	/**
	 * Determines whether the tag has at least one field with the specified
	 * &quot;id&quot;.
	 *
	 * @param id The field id to look for.
	 * @return `true` if tag contains a [TagField] with the
	 * given [id][TagField.getId].
	 */
	fun hasField(id: String?): Boolean

	/**
	 * Determines whether the tag has no fields specified.<br></br>
	 *
	 * @return `true` if tag contains no field.
	 */
	val isEmpty: Boolean

	//TODO, do we need this
	override fun toString(): String

	/**
	 * Return the number of fields
	 *
	 *
	 * Fields with the same identifiers are counted separately
	 *
	 * i.e two TITLE fields in a Vorbis Comment file would count as two
	 *
	 * @return total number of fields
	 */
	val fieldCount: Int

	/**
	 * Return the number of fields taking multiple value fields into consideration
	 *
	 * Fields that actually contain multiple values are counted seperately
	 *
	 * i.e. a TCON frame in ID3v24 frame containing multiple genres would add to count for each genre.
	 *
	 * @return total number of fields taking multiple value fields into consideration
	 */
	val fieldCountIncludingSubValues: Int

	//TODO is this a special field?
	@Throws(FieldDataInvalidException::class)
	fun setEncoding(enc: Charset?): Boolean

	/**
	 * @return a list of all artwork in this file using the format independent Artwork class
	 */
	val artworkList: List<Artwork>?

	/**
	 * @return first artwork or null if none exist
	 */
	val firstArtwork: Artwork?

	/**
	 * Delete any instance of tag fields used to store artwork
	 *
	 *
	 * We need this additional deleteField method because in some formats artwork can be stored
	 * in multiple fields
	 *
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	fun deleteArtworkField()

	/**
	 * Create artwork field based on the data in artwork
	 *
	 * @param artwork
	 * @return suitable tagfield for this format that represents the artwork data
	 * @throws FieldDataInvalidException
	 */
	@Throws(FieldDataInvalidException::class)
	fun createField(artwork: Artwork?): TagField?

	/**
	 * Create artwork field based on the data in artwork and then set it in the tag itself
	 *
	 *
	 * @param artwork
	 * @throws FieldDataInvalidException
	 */
	@Throws(FieldDataInvalidException::class)
	fun setField(artwork: Artwork?)

	/**
	 * Create artwork field based on the data in artwork and then add it to the tag itself
	 *
	 *
	 * @param artwork
	 * @throws FieldDataInvalidException
	 */
	@Throws(FieldDataInvalidException::class)
	fun addField(artwork: Artwork?)

	/**
	 * Sets a field in the structure, used internally by the library<br></br>
	 *
	 *
	 * @param field The field to add.
	 * @throws FieldDataInvalidException
	 */
	@Throws(FieldDataInvalidException::class)
	fun setField(field: TagField?)

	/**
	 * Adds a field to the structure, used internally by the library<br></br>
	 *
	 *
	 * @param field The field to add.
	 * @throws FieldDataInvalidException
	 */
	@Throws(FieldDataInvalidException::class)
	fun addField(field: TagField?)

	/**
	 * Create a new field based on generic key, used internally by the library
	 *
	 *
	 * Only textual data supported at the moment. The genericKey will be mapped
	 * to the correct implementation key and return a TagField.
	 *
	 * Usually the value field should only be one value, but certain fields may require more than one value
	 * currently the only field to require this is the MUSICIAN field, it should contain instrument and then
	 * performer name
	 *
	 * @param genericKey is the generic key
	 * @param value      to store
	 * @return
	 * @throws KeyNotFoundException
	 * @throws FieldDataInvalidException
	 */
	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	fun createField(genericKey: FieldKey?, vararg value: String?): TagField?

	/**
	 * Creates isCompilation field
	 *
	 * It is useful to have this method because it handles ensuring that the correct value to represent a boolean
	 * is stored in the underlying field format.
	 *
	 * @param value
	 * @return
	 * @throws KeyNotFoundException
	 * @throws FieldDataInvalidException
	 */
	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	fun createCompilationField(value: Boolean): TagField?
}
