/*
 *  MusicTag Copyright (C)2003,2004
 *
 *  This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser
 *  General Public  License as published by the Free Software Foundation; either version 2.1 of the License,
 *  or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License along with this library; if not,
 *  you can getFields a copy from http://www.opensource.org/licenses/lgpl-license.php or write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3

import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.UnableToCreateFileException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.UnableToModifyFileException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.generic.Utils
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp3.MP3File
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.FileSystemMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.DataTypes
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.ID3NumberTotalFields
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.images.Artwork
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference.Languages
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.reference.PictureTypes
import com.flammky.musicplayer.common.media.audio.meta_tag.utils.ShiftData
import com.flammky.musicplayer.core.sdk.AndroidAPI
import com.flammky.musicplayer.core.sdk.AndroidBuildVersion.hasOreo
import java.io.*
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.WritableByteChannel
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.*
import java.util.logging.Level

/**
 * This is the abstract base class for all ID3v2 tags.
 *
 * @author : Paul Taylor
 * @author : Eric Farng
 * @version $Id$
 */
abstract class ID3v2TagBase : AbstractID3Tag, Tag {
	//Start location of this chunk
	//TODO currently only used by ID3 embedded into Wav/Aiff but should be extended to mp3s
	var startLocationInFile: Long? = null
		private set

	//End location of this chunk
	var endLocationInFile: Long? = null
		private set

	/**
	 * Map of all frames for this tag
	 */
	@JvmField
	protected var frameMap: MutableMap<String, MutableList<TagField?>>? = null

	/**
	 * Map of all encrypted frames, these cannot be unencrypted by jaudiotagger
	 */
	@JvmField
	protected var encryptedFrameMap: MutableMap<String, MutableList<TagField?>>? = null

	/**
	 * Return the string which holds the ids of all
	 * duplicate frames.
	 *
	 * @return the string which holds the ids of all duplicate frames.
	 */
	var duplicateFrameId = ""
		protected set

	/**
	 * Returns the number of bytes which come from duplicate frames
	 *
	 * @return the number of bytes which come from duplicate frames
	 */
	var duplicateBytes = 0
		protected set

	/**
	 * Returns the number of bytes which come from empty frames
	 *
	 * @return the number of bytes which come from empty frames
	 */
	var emptyFrameBytes = 0
		protected set

	/**
	 * Returns the tag size as reported by the tag header
	 *
	 * @return the tag size as reported by the tag header
	 */
	var fileReadBytes = 0
		protected set

	/**
	 * Return  byte count of invalid frames
	 *
	 * @return byte count of invalid frames
	 */
	var invalidFrames = 0
		protected set

	/**
	 * Empty Constructor
	 */
	constructor()

	/**
	 * This constructor is used when a tag is created as a duplicate of another
	 * tag of the same type and version.
	 *
	 * @param copyObject
	 */
	protected constructor(copyObject: ID3v2TagBase?)

	/**
	 * Copy primitives apply to all tags
	 *
	 * @param copyObject
	 */
	protected open fun copyPrimitives(copyObject: ID3v2TagBase) {
		logger.config("Copying Primitives")
		//Primitives type variables common to all IDv2 Tags
		duplicateFrameId = copyObject.duplicateFrameId
		duplicateBytes = copyObject.duplicateBytes
		emptyFrameBytes = copyObject.emptyFrameBytes
		fileReadBytes = copyObject.fileReadBytes
		invalidFrames = copyObject.invalidFrames
	}

	/**
	 * Copy frames from another tag,
	 *
	 * @param copyObject
	 */
	//TODO Copy Encrypted frames needs implementing
	protected fun copyFrames(copyObject: ID3v2TagBase) {
		frameMap = LinkedHashMap()
		encryptedFrameMap = LinkedHashMap()

		//Copy Frames that are a valid 2.4 type
		for (id in copyObject.frameMap!!.keys) {
			val fields: List<TagField?> = copyObject.frameMap!![id]!!
			for (o in fields) {
				if (o is AbstractID3v2Frame) {
					addFrame(o as AbstractID3v2Frame?)
				} else if (o is TyerTdatAggregatedFrame) {
					for (next in o.getFrames()) {
						addFrame(next)
					}
				}
			}
		}
	}

	/**
	 * Add the frame converted to the correct version
	 * @param frame
	 */
	protected abstract fun addFrame(frame: AbstractID3v2Frame?)

	/**
	 * Convert the frame to the correct frame(s)
	 *
	 * @param frame
	 * @return
	 * @throws InvalidFrameException
	 */
	@Throws(InvalidFrameException::class)
	protected abstract fun convertFrame(frame: AbstractID3v2Frame?): List<AbstractID3v2Frame?>?

	/**
	 * Return whether tag has frame with this identifier
	 *
	 *
	 * Warning the match is only done against the identifier so if a tag contains a frame with an unsupported body
	 * but happens to have an identifier that is valid for another version of the tag it will return true
	 *
	 * @param identifier frameId to lookup
	 * @return true if tag has frame with this identifier
	 */
	fun hasFrame(identifier: String?): Boolean {
		return frameMap?.containsKey(identifier) ?: false
	}

	/**
	 * Return whether tag has frame with this identifier and a related body. This is required to protect
	 * against circumstances whereby a tag contains a frame with an unsupported body
	 * but happens to have an identifier that is valid for another version of the tag which it has been converted to
	 *
	 *
	 * e.g TDRC is an invalid frame in a v23 tag but if somehow a v23tag has been created by another application
	 * with a TDRC frame we construct an UnsupportedFrameBody to hold it, then this library constructs a
	 * v24 tag, it will contain a frame with id TDRC but it will not have the expected frame body it is not really a
	 * TDRC frame.
	 *
	 * @param identifier frameId to lookup
	 * @return true if tag has frame with this identifier
	 */
	fun hasFrameAndBody(identifier: String?): Boolean {
		if (hasFrame(identifier)) {
			val fields = getFrame(identifier)
			if (fields.size > 0) {
				val frame = fields[0]
				return if (frame is AbstractID3v2Frame) {
					frame.body !is FrameBodyUnsupported
				} else true
			}
		}
		return false
	}

	/**
	 * Return whether tag has frame starting with this identifier
	 *
	 *
	 * Warning the match is only done against the identifier so if a tag contains a frame with an unsupported body
	 * but happens to have an identifier that is valid for another version of the tag it will return true
	 *
	 * @param identifier start of frameId to lookup
	 * @return tag has frame starting with this identifier
	 */
	fun hasFrameOfType(identifier: String?): Boolean {
		val iterator: Iterator<String?> = frameMap!!.keys.iterator()
		var key: String?
		var found = false
		while (iterator.hasNext() && !found) {
			key = iterator.next()
			if (key!!.startsWith(identifier!!)) {
				found = true
			}
		}
		return found
	}

	/**
	 * Return a list containing all the frames with this identifier
	 *
	 *
	 * Warning the match is only done against the identifier so if a tag contains a frame with an unsupported body
	 * but happens to have an identifier that is valid for another version of the tag it will be returned.
	 *
	 * @param identifier is an ID3Frame identifier
	 * @return list of matching frames
	 */
	fun getFrame(identifier: String?): MutableList<TagField?> {
		return frameMap?.get(identifier) ?: ArrayList()
	}

	/**
	 * Return any encrypted frames with this identifier
	 *
	 *
	 *
	 * For single frames return the frame in this tag with given identifier if it exists, if multiple frames
	 * exist with the same identifier it will return a list containing all the frames with this identifier
	 *
	 * @param identifier
	 * @return
	 */
	fun getEncryptedFrame(identifier: String?): List<TagField?> {
		return encryptedFrameMap!![identifier]!!
	}

	/**
	 * Retrieve the first value that exists for this identifier
	 *
	 *
	 * If the value is a String it returns that, otherwise returns a summary of the fields information
	 *
	 * @param identifier
	 * @return
	 */
	override fun getFirst(identifier: String?): String? {
		val frame = getFirstField(identifier) ?: return ""
		return getTextValueForFrame(frame)
	}

	/**
	 * @param frame
	 * @return
	 */
	private fun getTextValueForFrame(frame: AbstractID3v2Frame): String? {
		return frame.body!!.userFriendlyValue
	}

	@Throws(KeyNotFoundException::class)
	override fun getFirstField(genericKey: FieldKey?): TagField? {
		val fields = getFields(genericKey)
		return if (fields!!.size > 0) {
			fields[0]
		} else null
	}

	/**
	 * Retrieve the first tag field that exists for this identifier
	 *
	 * @param identifier
	 * @return tag field or null if doesn't exist
	 */
	override fun getFirstField(identifier: String?): AbstractID3v2Frame? {
		val fields = getFrame(identifier)
		return if (fields == null || fields.isEmpty() || containsAggregatedFrame(fields)) {
			null
		} else fields[0] as AbstractID3v2Frame?
	}

	/**
	 * Add a frame to this tag
	 *
	 * @param frame the frame to add
	 *
	 *
	 *
	 *
	 * Warning if frame(s) already exists for this identifier that they are overwritten
	 */
	//TODO needs to ensure do not addField an invalid frame for this tag
	//TODO what happens if already contains a list with this ID
	fun setFrame(frame: AbstractID3v2Frame) {
		val frames: MutableList<TagField?> = ArrayList()
		frames.add(frame)
		frameMap!![frame.identifier] = frames
	}

	protected fun setTagField(id: String, frame: TagField?) {
		val frames: MutableList<TagField?> = ArrayList()
		frames.add(frame)
		frameMap!![id] = frames
	}

	protected abstract val iD3Frames: ID3Frames

	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun setField(genericKey: FieldKey?, vararg values: String?) {
		val tagfield = createField(genericKey, *values)
		setField(tagfield)
	}

	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun addField(genericKey: FieldKey?, vararg value: String?) {
		val tagfield = createField(genericKey, *value)
		addField(tagfield)
	}

	/**
	 * All Number/Count frames  are treated the same (TCK, TPOS, MVNM)
	 *
	 * @param newFrame
	 * @param nextFrame
	 */

	fun mergeNumberTotalFrames(newFrame: AbstractID3v2Frame, nextFrame: AbstractID3v2Frame) {
		val newBody = newFrame.body as AbstractFrameBodyNumberTotal?
		val oldBody = nextFrame.body as AbstractFrameBodyNumberTotal?
		val newNum = newBody?.number
		val newTotal = newBody?.total
		if (newNum != null && newNum > 0) {
			oldBody?.setNumber(newBody.trackNoAsText!!)
		}
		if (newTotal != null && newTotal > 0) {
			oldBody?.setTotal(newBody.trackTotalAsText!!)
		}
		return
	}

	/**
	 * Add frame taking into account existing frames of the same type
	 *
	 * @param newFrame
	 */
	fun mergeDuplicateFrames(newFrame: AbstractID3v2Frame) {
		var frames = frameMap!![newFrame.id]
		if (frames == null) {
			frames = ArrayList()
		}
		val li = frames.listIterator()
		while (li.hasNext()) {
			val tagField = li.next() as? AbstractID3v2Frame
				?: // Skip aggregated frames
				continue
			val nextFrame = tagField
			if (newFrame.body is FrameBodyTXXX) {
				//Value with matching key exists so replace
				if ((newFrame.body as FrameBodyTXXX?)!!.description == (nextFrame.body as FrameBodyTXXX?)!!.description) {
					li.set(newFrame)
					frameMap!![newFrame.id] = frames
					return
				}
			} else if (newFrame.body is FrameBodyWXXX) {
				//Value with matching key exists so replace
				if ((newFrame.body as FrameBodyWXXX?)!!.description == (nextFrame.body as FrameBodyWXXX?)!!.description) {
					li.set(newFrame)
					frameMap!![newFrame.id] = frames
					return
				}
			} else if (newFrame.body is FrameBodyCOMM) {
				if ((newFrame.body as FrameBodyCOMM?)!!.description == (nextFrame.body as FrameBodyCOMM?)!!.description) {
					li.set(newFrame)
					frameMap!![newFrame.id] = frames
					return
				}
			} else if (newFrame.body is FrameBodyUFID) {
				if ((newFrame.body as FrameBodyUFID?)!!.owner == (nextFrame.body as FrameBodyUFID?)!!.owner) {
					li.set(newFrame)
					frameMap!![newFrame.id] = frames
					return
				}
			} else if (newFrame.body is FrameBodyUSLT) {
				if ((newFrame.body as FrameBodyUSLT?)!!.description == (nextFrame.body as FrameBodyUSLT?)!!.description) {
					li.set(newFrame)
					frameMap!![newFrame.id] = frames
					return
				}
			} else if (newFrame.body is FrameBodyPOPM) {
				if ((newFrame.body as FrameBodyPOPM?)!!.emailToUser == (nextFrame.body as FrameBodyPOPM?)!!.emailToUser) {
					li.set(newFrame)
					frameMap!![newFrame.id] = frames
					return
				}
			} else if (newFrame.body is AbstractFrameBodyNumberTotal) {
				mergeNumberTotalFrames(newFrame, nextFrame)
				return
			} else if (newFrame.body is AbstractFrameBodyPairs) {
				val frameBody = newFrame.body as AbstractFrameBodyPairs?
				val existingFrameBody = nextFrame.body as AbstractFrameBodyPairs?
				existingFrameBody!!.addPair(frameBody!!.text)
				return
			}
		}
		if (!iD3Frames.isMultipleAllowed(newFrame.id)) {
			setFrame(newFrame)
		} else {
			//No match found so addField new one
			frames.add(newFrame)
			frameMap!![newFrame.id] = frames
		}
	}

	/**
	 * Add another frame to the map
	 *
	 * @param list
	 * @param frameMap
	 * @param existingFrame
	 * @param frame
	 */
	private fun addNewFrameToMap(
		list: MutableList<TagField?>,
		frameMap: MutableMap<String, MutableList<TagField?>>?,
		existingFrame: AbstractID3v2Frame?,
		frame: AbstractID3v2Frame
	) {
		if (list.size == 0) {
			list.add(existingFrame)
			list.add(frame)
			frameMap!![frame.id] = list
		} else {
			list.add(frame)
		}
	}

	/**
	 * Handles adding of a new field that's shares a frame with other fields, so modifies the existing frame rather
	 * than creating a new frame for these special cases
	 *
	 * @param list
	 * @param frameMap
	 * @param existingFrame
	 * @param frame
	 */
	private fun addNewFrameOrAddField(
		list: MutableList<TagField?>,
		frameMap: MutableMap<String, MutableList<TagField?>>,
		existingFrame: AbstractID3v2Frame?,
		frame: AbstractID3v2Frame
	) {
		val mergedList: MutableList<TagField?> = ArrayList()
		if (existingFrame != null) {
			mergedList.add(existingFrame)
		} else {
			mergedList.addAll(list)
		}
		/**
		 * If the frame is a TXXX frame then we add an extra string to the existing frame
		 * if same description otherwise we create a new frame
		 */
		if (frame.body is FrameBodyTXXX) {
			val frameBody = frame.body as FrameBodyTXXX?
			var match = false
			val i: Iterator<TagField?> = mergedList.listIterator()
			while (i.hasNext()) {
				val existingFrameBody = (i.next() as AbstractID3v2Frame?)!!.body as FrameBodyTXXX?
				if (frameBody!!.description == existingFrameBody!!.description) {
					existingFrameBody.addTextValue(frameBody.text.toString())
					match = true
					break
				}
			}
			if (!match) {
				addNewFrameToMap(list, frameMap, existingFrame, frame)
			}
		} else if (frame.body is FrameBodyWXXX) {
			val frameBody = frame.body as FrameBodyWXXX?
			var match = false
			val i: Iterator<TagField?> = mergedList.listIterator()
			while (i.hasNext()) {
				val existingFrameBody = (i.next() as AbstractID3v2Frame?)!!.body as FrameBodyWXXX?
				if (frameBody!!.description == existingFrameBody!!.description) {
					existingFrameBody.addUrlLink(frameBody.urlLink.toString())
					match = true
					break
				}
			}
			if (!match) {
				addNewFrameToMap(list, frameMap, existingFrame, frame)
			}
		} else if (frame.body is AbstractFrameBodyTextInfo) {
			val frameBody = frame.body as AbstractFrameBodyTextInfo?
			val existingFrameBody = existingFrame!!.body as AbstractFrameBodyTextInfo?
			existingFrameBody!!.addTextValue(frameBody?.text.toString())
		} else if (frame.body is AbstractFrameBodyPairs) {
			val frameBody = frame.body as AbstractFrameBodyPairs?
			val existingFrameBody = existingFrame!!.body as AbstractFrameBodyPairs?
			existingFrameBody!!.addPair(frameBody!!.text)
		} else if (frame.body is AbstractFrameBodyNumberTotal) {
			val frameBody = frame.body as AbstractFrameBodyNumberTotal?
			val existingFrameBody = existingFrame!!.body as AbstractFrameBodyNumberTotal?
			if (frameBody!!.number != null && frameBody.number!! > 0) {
				existingFrameBody!!.setNumber(frameBody.trackNoAsText.toString())
			}
			if (frameBody.total != null && frameBody.total!! > 0) {
				existingFrameBody!!.setTotal(frameBody.trackTotalAsText.toString())
			}
		} else {
			addNewFrameToMap(list, frameMap, existingFrame, frame)
		}
	}

	/**
	 * Set Field
	 *
	 * @param field
	 * @throws FieldDataInvalidException
	 */
	@Throws(FieldDataInvalidException::class)
	override fun setField(field: TagField?) {
		if (field !is AbstractID3v2Frame && field !is AggregatedFrame) {
			throw FieldDataInvalidException(
				"Field $field is not of type AbstractID3v2Frame nor AggregatedFrame"
			)
		}
		if (field is AbstractID3v2Frame) {
			mergeDuplicateFrames(field)
		} else  //TODO not handling multiple aggregated frames of same type
		{
			setTagField(field.id!!, field)
		}
	}

	/**
	 * Add new field
	 *
	 *
	 * There is a special handling if adding another text field of the same type, in this case the value will
	 * be appended to the existing field, separated by the null character.
	 *
	 * @param field
	 * @throws FieldDataInvalidException
	 */
	@Throws(FieldDataInvalidException::class)
	override fun addField(field: TagField?) {
		if (field == null) {
			return
		}
		if (field !is AbstractID3v2Frame && field !is AggregatedFrame) {
			throw FieldDataInvalidException(
				"Field $field is not of type AbstractID3v2Frame or AggregatedFrame"
			)
		}
		if (field is AbstractID3v2Frame) {
			val frame = field
			var fields = frameMap!![field.id]

			//No frame of this type
			if (fields == null) {
				fields = ArrayList()
				fields.add(field)
				frameMap!![field.id] = fields
			} else {
				if (fields.size == 1 && fields[0] is AbstractID3v2Frame) {
					addNewFrameOrAddField(fields, frameMap!!, fields[0] as AbstractID3v2Frame?, frame)
				} else {
					addNewFrameOrAddField(fields, frameMap!!, null, frame)
				}
			}
		} else {
			setTagField(field.id!!, field)
		}
	}

	/**
	 * Used for setting multiple frames for a single frame Identifier
	 *
	 *
	 * Warning if frame(s) already exists for this identifier they are overwritten
	 * TODO needs to ensure do not add an invalid frame for this tag
	 *
	 * @param identifier
	 * @param multiFrame
	 */
	fun setFrame(identifier: String, multiFrame: MutableList<TagField?>) {
		logger.finest("Adding " + multiFrame.size + " frames for " + identifier)
		frameMap!![identifier] = multiFrame
	}
	/**
	 * Return the number of frames in this tag of a particular type, multiple frames
	 * of the same time will only be counted once
	 *
	 * @return a count of different frames
	 */
	/*
	public int getFrameCount()
	{
			if (frameMap == null)
			{
					return 0;
			}
			else
			{
					return frameMap.size();
			}
	}
	*/
	/**
	 * Return all frames which start with the identifier, this
	 * can be more than one which is useful if trying to retrieve
	 * similar frames e.g TIT1,TIT2,TIT3 ... and don't know exactly
	 * which ones there are.
	 *
	 *
	 * Warning the match is only done against the identifier so if a tag contains a frame with an unsupported body
	 * but happens to have an identifier that is valid for another version of the tag it will be returned.
	 *
	 * @param identifier
	 * @return an iterator of all the frames starting with a particular identifier
	 */
	fun getFrameOfType(identifier: String?): Iterator<Any> {
		val iterator: Iterator<String?> = frameMap!!.keys.iterator()
		val result: MutableSet<Any> = HashSet()
		var key: String?
		while (iterator.hasNext()) {
			key = iterator.next()
			if (key!!.startsWith(identifier!!)) {
				val o: Any = frameMap!![key]!!
				if (o is List<*>) {
					for (next in o) {
						result.add(next ?: "null")
					}
				} else {
					result.add(o)
				}
			}
		}
		return result.iterator()
	}

	/**
	 * Delete Tag
	 *
	 * @param file to delete the tag from
	 * @throws IOException if problem accessing the file
	 */
	//TODO should clear all data and preferably recover lost space and go upto end of mp3s
	@Throws(IOException::class)
	override fun delete(file: RandomAccessFile?) {
		// this works by just erasing the "ID3" tag at the beginning
		// of the file
		val buffer = ByteArray(FIELD_TAGID_LENGTH)
		//Read into Byte Buffer
		val fc = file!!.channel
		fc.position()
		val byteBuffer = ByteBuffer.allocate(TAG_HEADER_LENGTH)
		fc.read(byteBuffer, 0)
		byteBuffer.flip()
		if (seek(byteBuffer)) {
			file.seek(0L)
			file.write(buffer)
		}
	}

	/**
	 * Is this tag equivalent to another
	 *
	 * @param obj to test for equivalence
	 * @return true if they are equivalent
	 */
	override fun equals(obj: Any?): Boolean {
		if (obj !is ID3v2TagBase) {
			return false
		}
		return frameMap == obj.frameMap && super.equals(obj)
	}

	/**
	 * Return the frames in the order they were added
	 *
	 * @return and iterator of the frmaes/list of multi value frames
	 */
	override fun iterator(): MutableIterator<MutableList<TagField?>?>? {
		return frameMap!!.values.iterator()
	}

	/**
	 * Remove frame(s) with this identifier from tag
	 *
	 * @param identifier frameId to look for
	 */
	open fun removeFrame(identifier: String) {
		logger.config(
			"Removing frame with identifier:$identifier"
		)
		frameMap!!.remove(identifier)
	}

	/**
	 * Remove all frame(s) which have an unsupported body, in other words
	 * remove all frames that are not part of the standard frameSet for
	 * this tag
	 */
	fun removeUnsupportedFrames() {
		val fieldsIterator = iterator()
		while (fieldsIterator!!.hasNext()) {
			val fields = fieldsIterator.next()
			val i = fields!!.iterator()
			while (i.hasNext()) {
				val o = i.next()
				if (o is AbstractID3v2Frame) {
					if (o.body is FrameBodyUnsupported) {
						logger.finest("Removing frame" + o.identifier)
						i.remove()
					}
				}
			}
			if (fields.isEmpty()) {
				fieldsIterator.remove()
			}
		}
	}

	/**
	 * Remove any frames starting with this identifier from tag
	 *
	 * @param identifier start of frameId to look for
	 */
	fun removeFrameOfType(identifier: String) {
		//First fine matching keys
		val result: MutableSet<String?> = HashSet()
		for (key in frameMap!!.keys) {
			if (key.startsWith(identifier)) {
				result.add(key)
			}
		}
		//Then deleteField outside of loop to prevent concurrent modificatioon eception if there are two keys
		//with the same id
		for (match in result) {
			logger.finest("Removing frame with identifier:" + match + "because starts with:" + identifier)
			frameMap!!.remove(match)
		}
	}

	/**
	 * Write tag to file.
	 *
	 * @param file
	 * @param audioStartByte
	 * @return new audioStartByte - different only if the audio content had to be moved
	 * @throws IOException
	 */
	@Throws(IOException::class)
	abstract fun write(file: File?, audioStartByte: Long): Long

	/**
	 * Get file lock for writing too file
	 *
	 *
	 * TODO:this appears to have little effect on Windows Vista
	 *
	 * @param fileChannel
	 * @param filePath
	 * @return lock or null if locking is not supported
	 * @throws IOException                                    if unable to get lock because already locked by another program
	 * @throws OverlappingFileLockException if already locked by another thread in the same VM, we dont catch this
	 * because indicates a programming error
	 */
	@Throws(IOException::class)
	protected fun getFileLockForWriting(fileChannel: FileChannel, filePath: String): FileLock? {
		logger.finest(
			"locking fileChannel for $filePath"
		)
		val fileLock: FileLock?
		fileLock = try {
			fileChannel.tryLock()
		} //Assumes locking is not supported on this platform so just returns null
		catch (exception: IOException) {
			return null
		} //#129 Workaround for https://bugs.openjdk.java.net/browse/JDK-8025619
		catch (error: Error) {
			return null
		}

		//Couldnt getFields lock because file is already locked by another application
		if (fileLock == null) {
			throw IOException(ErrorMessage.GENERAL_WRITE_FAILED_FILE_LOCKED.getMsg(filePath))
		}
		return fileLock
	}

	/**
	 * Write tag to file.
	 *
	 * @param file
	 * @throws IOException TODO should be abstract
	 */
	@Throws(IOException::class)
	override fun write(file: RandomAccessFile?) {
	}

	/**
	 * Write tag to channel.
	 *
	 * @param channel
	 * @throws IOException TODO should be abstract
	 */
	@Throws(IOException::class)
	open fun write(channel: WritableByteChannel?, currentTagSize: Int) {
	}

	/**
	 * Write tag to output stream
	 *
	 * @param outputStream
	 * @throws IOException
	 */
	@Throws(IOException::class)
	fun write(outputStream: OutputStream?) {
		write(Channels.newChannel(outputStream), 0)
	}

	/**
	 * Write tag to output stream
	 *
	 * @param outputStream
	 * @throws IOException
	 */
	@Throws(IOException::class)
	fun write(outputStream: OutputStream?, currentTagSize: Int) {
		write(Channels.newChannel(outputStream), currentTagSize)
	}

	/**
	 * Write paddings byte to the channel
	 *
	 * @param channel
	 * @param padding
	 * @throws IOException
	 */
	@Throws(IOException::class)
	protected fun writePadding(channel: WritableByteChannel, padding: Int) {
		if (padding > 0) {
			channel.write(ByteBuffer.wrap(ByteArray(padding)))
		}
	}

	/**
	 * Does a tag of the correct version exist in this file.
	 *
	 * @param byteBuffer to search through
	 * @return true if tag exists.
	 */
	override fun seek(byteBuffer: ByteBuffer?): Boolean {
		byteBuffer!!.rewind()
		logger.config("ByteBuffer pos:" + byteBuffer.position() + ":limit" + byteBuffer.limit() + ":cap" + byteBuffer.capacity())
		val tagIdentifier = ByteArray(FIELD_TAGID_LENGTH)
		byteBuffer[tagIdentifier, 0, FIELD_TAGID_LENGTH]
		if (!Arrays.equals(tagIdentifier, TAG_ID)) {
			return false
		}
		//Major Version
		return if (byteBuffer.get() != majorVersion) {
			false
		} else byteBuffer.get() == revision
		//Minor Version
	}

	/**
	 * This method determines the total tag size taking into account
	 * the preferredSize and the min size required for new tag. For mp3
	 * preferred size is the location of the audio, for other formats
	 * preferred size is the size of the existing tag
	 *
	 * @param tagSize
	 * @param preferredSize
	 * @return
	 */
	protected fun calculateTagSize(tagSize: Int, preferredSize: Int): Int {
		return if (TagOptionSingleton.instance.isId3v2PaddingWillShorten) {
			//We just use required size
			tagSize
		} else {
			//We can fit in the tag so no adjustments required
			if (tagSize <= preferredSize) {
				preferredSize
			} else tagSize + TAG_SIZE_INCREMENT
			//There is not enough room as we need to move the audio file we might
			//as well increase it more than necessary for future changes
		}
	}

	/**
	 * Write the data from the buffer to the file
	 *
	 * @param file
	 * @param headerBuffer
	 * @param bodyByteBuffer
	 * @param padding
	 * @param sizeIncPadding
	 * @param audioStartLocation
	 * @throws IOException
	 */
	@Throws(IOException::class)
	protected fun writeBufferToFile(
		file: File,
		headerBuffer: ByteBuffer?,
		bodyByteBuffer: ByteArray?,
		padding: Int,
		sizeIncPadding: Int,
		audioStartLocation: Long
	) {
		try {
			if (AndroidAPI.hasOreo()) {
				Files.newByteChannel(file.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE)
			} else {
				RandomAccessFile(file, "rw").channel
			}.use { fc ->
					//We need to adjust location of audio file if true
					if (sizeIncPadding > audioStartLocation) {
						fc.position(audioStartLocation)
						ShiftData.shiftDataByOffsetToMakeSpace(
							fc,
							(sizeIncPadding - audioStartLocation).toInt()
						)
					} else if (TagOptionSingleton.instance
							.isId3v2PaddingWillShorten && sizeIncPadding < audioStartLocation
					) {
						fc.position(audioStartLocation)
						ShiftData.shiftDataByOffsetToShrinkSpace(
							fc,
							(audioStartLocation - sizeIncPadding).toInt()
						)
					}
					fc.position(0)
					fc.write(headerBuffer)
					fc.write(ByteBuffer.wrap(bodyByteBuffer))
					fc.write(ByteBuffer.wrap(ByteArray(padding)))
				}
		} catch (ioe: IOException) {
			logger.log(Level.SEVERE, loggingFilename + ioe.message, ioe)
			if (ioe.message == FileSystemMessage.ACCESS_IS_DENIED.msg) {
				logger.severe(ErrorMessage.GENERAL_WRITE_FAILED_TO_OPEN_FILE_FOR_EDITING.getMsg(file.parentFile.path))
				throw UnableToModifyFileException(
					ErrorMessage.GENERAL_WRITE_FAILED_TO_OPEN_FILE_FOR_EDITING.getMsg(
						file.parentFile.path
					)
				)
			} else {
				logger.severe(ErrorMessage.GENERAL_WRITE_FAILED_TO_OPEN_FILE_FOR_EDITING.getMsg(file.parentFile.path))
				throw UnableToCreateFileException(
					ErrorMessage.GENERAL_WRITE_FAILED_TO_OPEN_FILE_FOR_EDITING.getMsg(
						file.parentFile.path
					)
				)
			}
		}
	}

	private fun containsAggregatedFrame(fields: Collection<TagField?>?): Boolean {
		var result = false
		if (fields != null) {
			for (field in fields) {
				if (field is AggregatedFrame) {
					result = true
					break
				}
			}
		}
		return result
	}

	/**
	 * Copy frame into map, whilst accounting for multiple frame of same type which can occur even if there were
	 * not frames of the same type in the original tag
	 *
	 * @param id
	 * @param newFrame
	 */
	protected fun copyFrameIntoMap(id: String, newFrame: AbstractID3v2Frame) {
		var tagFields = frameMap!![newFrame.identifier]
		if (tagFields == null) {
			tagFields = ArrayList()
			tagFields.add(newFrame)
			frameMap!![newFrame.identifier] = tagFields
		} else if (containsAggregatedFrame(tagFields)) {
			logger.severe(
				"Duplicated Aggregate Frame, ignoring:$id"
			)
		} else {
			combineFrames(newFrame, tagFields)
		}
	}

	protected open fun combineFrames(
		newFrame: AbstractID3v2Frame?,
		existing: MutableList<TagField?>
	) {
		existing.add(newFrame)
	}

	/**
	 * Add frame to the frame map
	 *
	 * @param frameId
	 * @param next
	 */
	protected open fun loadFrameIntoMap(frameId: String, next: AbstractID3v2Frame) {
		if (next.body is FrameBodyEncrypted) {
			loadFrameIntoSpecifiedMap(encryptedFrameMap!!, frameId, next)
		} else {
			loadFrameIntoSpecifiedMap(frameMap!!, frameId, next)
		}
	}

	/**
	 * Decides what to with the frame that has just been read from file.
	 * If the frame is an allowable duplicate frame and is a duplicate we add all
	 * frames into an ArrayList and add the ArrayList to the HashMap. if not allowed
	 * to be duplicate we store the number of bytes in the duplicateBytes variable and discard
	 * the frame itself.
	 *
	 * @param frameId
	 * @param next
	 */
	protected open fun loadFrameIntoSpecifiedMap(
		map: MutableMap<String, MutableList<TagField?>>,
		frameId: String,
		next: AbstractID3v2Frame?
	) {
		if (ID3v24Frames.instanceOf.isMultipleAllowed(frameId) ||
			ID3v23Frames.instanceOf.isMultipleAllowed(frameId) ||
			ID3v22Frames.instanceOf.isMultipleAllowed(frameId)
		) {
			//If a frame already exists of this type
			if (map.containsKey(frameId)) {
				val multiValues = map[frameId]!!
				multiValues.add(next)
			} else {
				logger.finer(
					"Adding Multi FrameList(3)$frameId"
				)
				val fields: MutableList<TagField?> = ArrayList()
				fields.add(next)
				map[frameId] = fields
			}
		} else if (map.containsKey(frameId) && !map[frameId]!!.isEmpty()) {
			logger.warning(
				"Ignoring Duplicate Frame:$frameId"
			)
			//If we have multiple duplicate frames in a tag separate them with semicolons
			if (duplicateFrameId.length > 0) {
				duplicateFrameId += ";"
			}
			duplicateFrameId += frameId
			for (tagField in frameMap!![frameId]!!) {
				if (tagField is AbstractID3v2Frame) {
					duplicateBytes += tagField.size
				}
			}
		} else {
			logger.finer(
				"Adding Frame$frameId"
			)
			val fields: MutableList<TagField?> = ArrayList()
			fields.add(next)
			map[frameId] = fields
		}
	}

	/**
	 * Return tag size based upon the sizes of the tags rather than the physical
	 * no of bytes between start of ID3Tag and start of Audio Data.Should be extended
	 * by subclasses to include header.
	 *
	 * @return size of the tag
	 */
	override val size: Int
		get() {
			var size = 0
			val iterator: Iterator<List<TagField?>> = frameMap!!.values.iterator()
			var frame: AbstractID3v2Frame
			while (iterator.hasNext()) {
				val fields = iterator.next()
				if (fields != null) {
					for (field in fields) {
						if (field is AggregatedFrame) {
							for (next in field.getFrames()) {
								size += next.size
							}
						} else if (field is AbstractID3v2Frame) {
							frame = field
							size += frame.size
						}
					}
				}
			}
			return size
		}

	/**
	 * Write all the frames to the byteArrayOutputStream
	 *
	 *
	 *
	 * Currently Write all frames, defaults to the order in which they were loaded, newly
	 * created frames will be at end of tag.
	 *
	 * @return ByteBuffer Contains all the frames written within the tag ready for writing to file
	 * @throws IOException
	 */
	@Throws(IOException::class)
	protected fun writeFramesToBuffer(): ByteArrayOutputStream {
		val bodyBuffer = ByteArrayOutputStream()
		writeFramesToBufferStream(frameMap!!, bodyBuffer)
		writeFramesToBufferStream(encryptedFrameMap!!, bodyBuffer)
		return bodyBuffer
	}

	/**
	 * Write frames in map to bodyBuffer
	 *
	 * @param map
	 * @param bodyBuffer
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun writeFramesToBufferStream(
		map: Map<String, MutableList<TagField?>>,
		bodyBuffer: ByteArrayOutputStream
	) {
		//Sort keys into Preferred Order
		val sortedWriteOrder = TreeSet(
			preferredFrameOrderComparator
		)
		sortedWriteOrder.addAll(map.keys)
		for (id in sortedWriteOrder) {
			val fields: List<TagField?> = map[id]!!
			for (field in fields) {
				if (field is AbstractID3v2Frame) {
					val frame = field
					frame.loggingFilename = loggingFilename
					frame.write(bodyBuffer)
				} else if (field is AggregatedFrame) {
					for (next in field.getFrames()) {
						next.loggingFilename = loggingFilename
						next.write(bodyBuffer)
					}
				}
			}
		}
	}

	/**
	 * @return comparator used to order frames in preferred order for writing to file
	 * so that most important frames are written first.
	 */
	abstract val preferredFrameOrderComparator: Comparator<String>?
	open fun createStructure() {
		createStructureHeader()
		createStructureBody()
	}

	fun createStructureHeader() {
		MP3File.structureFormatter?.addElement(TYPE_DUPLICATEBYTES, duplicateBytes)
		MP3File.structureFormatter?.addElement(TYPE_DUPLICATEFRAMEID, duplicateFrameId)
		MP3File.structureFormatter?.addElement(TYPE_EMPTYFRAMEBYTES, emptyFrameBytes)
		MP3File.structureFormatter?.addElement(TYPE_FILEREADSIZE, fileReadBytes)
		MP3File.structureFormatter?.addElement(TYPE_INVALIDFRAMES, invalidFrames)
	}

	fun createStructureBody() {
		MP3File.structureFormatter?.openHeadingElement(TYPE_BODY, "")
		var frame: AbstractID3v2Frame
		for (fields in frameMap!!.values) {
			for (o in fields) {
				if (o is AbstractID3v2Frame) {
					frame = o
					frame.createStructure()
				}
			}
		}
		MP3File.structureFormatter?.closeHeadingElement(TYPE_BODY)
	}

	/**
	 * Maps the generic key to the id3 key and return the list of values for this field as strings
	 *
	 * @param genericKey
	 * @return
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	override fun getAll(genericKey: FieldKey?): List<String?>? {
		if (genericKey == null) return null

		//Special case here because the generic key to frameid/subid mapping is identical for trackno versus tracktotal
		//and discno versus disctotal so we have to handle here, also want to ignore index parameter.
		val values: MutableList<String?> = ArrayList()
		val fields = getFields(genericKey)
		return if (ID3NumberTotalFields.isNumber(
				genericKey
			)
		) {
			if (fields != null && fields.isNotEmpty()) {
				val frame =
					fields[0] as AbstractID3v2Frame?
				values.add((frame!!.body as AbstractFrameBodyNumberTotal?)!!.trackNoAsText)
			}
			values
		} else if (ID3NumberTotalFields.isTotal(
				genericKey
			)
		) {
			if (fields != null && fields.isNotEmpty()) {
				val frame =
					fields[0] as AbstractID3v2Frame?
				values.add((frame!!.body as AbstractFrameBodyNumberTotal?)!!.trackTotalAsText)
			}
			values
		} else if (genericKey === FieldKey.RATING) {
			if (fields != null && fields.isNotEmpty()) {
				val frame =
					fields[0] as AbstractID3v2Frame?
				values.add((frame!!.body as FrameBodyPOPM?)!!.rating.toString())
			}
			values
		} else {
			doGetValues(getFrameAndSubIdFromGenericKey(genericKey))
		}
	}

	/**
	 * Retrieve the values that exists for this id3 frame id
	 */
	@Throws(KeyNotFoundException::class)
	override fun getFields(id: String?): MutableList<TagField?> {
		val o = getFrame(id)
		return when (o) {
			null -> {
				ArrayList()
			}
			is List<*> -> {
				// TODO should return copy
				o
			}
			else -> {
				throw RuntimeException("Found entry in frameMap that was not a frame or a list:$o")
			}
		}
	}

	/**
	 * Create Frame of correct ID3 version with the specified id
	 *
	 * @param id
	 * @return
	 */
	abstract fun createFrame(id: String?): AbstractID3v2Frame

	//TODO
	override fun hasCommonFields(): Boolean {
		return true
	}

	/**
	 * Does this tag contain a field with the specified key
	 *
	 * @param key The field id to look for.
	 * @return true if has field , false if does not or if no mapping for key exists
	 */
	override fun hasField(key: FieldKey?): Boolean {
		requireNotNull(key) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }
		return try {
			getFirstField(key) != null
		} catch (knfe: KeyNotFoundException) {
			logger.log(Level.SEVERE, knfe.message, knfe)
			false
		}
	}

	/**
	 * Does this tag contain a field with the specified id
	 *
	 * @see Tag.hasField
	 */
	override fun hasField(id: String?): Boolean {
		return hasFrame(id)
	}

	/**
	 * Is this tag empty
	 *
	 * @see Tag.isEmpty
	 */
	override val isEmpty: Boolean
		get() = frameMap?.isEmpty() ?: true //Iterator of each different frameId in this tag

	/**
	 * @return iterator of all fields, multiple values for the same Id (e.g multiple TXXX frames) count as separate
	 * fields
	 */
	override val fields: Iterator<TagField?>
		get() {
			//Iterator of each different frameId in this tag
			val allTagFields: MutableList<TagField?> = ArrayList()
			for (value in frameMap!!.values) {
				allTagFields.addAll(value)
			}
			return allTagFields.iterator()
		}//this is thrown when no more elements//Done this way because it.hasNext() incorrectly counts empty list
	//whereas it.next() works correctly
	/**
	 * Count number of frames/fields in this tag
	 *
	 * @return
	 */
	override val fieldCount: Int
		get() {
			val it = fields
			var count = 0

			//Done this way because it.hasNext() incorrectly counts empty list
			//whereas it.next() works correctly
			try {
				while (true) {
					it.next()
					count++
				}
			} catch (nse: NoSuchElementException) {
				//this is thrown when no more elements
			}
			return count
		}//this is thrown when no more elements//Done this way because it.hasNext() incorrectly counts empty list
	//whereas it.next() works correctly
	/**
	 * Return count of fields, this considers a text frame with two null separated values as two fields, if you want
	 * a count of frames @see getFrameCount
	 *
	 * @return count of fields
	 */
	override val fieldCountIncludingSubValues: Int
		get() {
			val it = fields
			var count = 0

			//Done this way because it.hasNext() incorrectly counts empty list
			//whereas it.next() works correctly
			try {
				while (true) {
					val next = it.next()
					if (next is AbstractID3v2Frame) {
						val frame = next
						if (frame.body is AbstractFrameBodyTextInfo && frame.body !is FrameBodyTXXX) {
							val frameBody = frame.body as AbstractFrameBodyTextInfo?
							count += frameBody!!.numberOfValues
							continue
						}
					}
					count++
				}
			} catch (nse: NoSuchElementException) {
				//this is thrown when no more elements
			}
			return count
		}

	//TODO is this a special field?
	@Throws(FieldDataInvalidException::class)
	override fun setEncoding(enc: Charset?): Boolean {
		throw UnsupportedOperationException("Not Implemented Yet")
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

	/**
	 * Retrieve the value that exists for this generic key and this index
	 *
	 *
	 * Have to do some special mapping for certain generic keys because they share frame
	 * with another generic key.
	 *
	 * @param genericKey
	 * @return
	 */
	@Throws(KeyNotFoundException::class)
	override fun getValue(genericKey: FieldKey?, n: Int): String? {
		if (genericKey == null) {
			throw KeyNotFoundException()
		}

		//Special case here because the generic key to frameid/subid mapping is identical for trackno versus tracktotal
		//and discno versus disctotal so we have to handle here, also want to ignore index parameter.
		if (ID3NumberTotalFields.isNumber(genericKey) || ID3NumberTotalFields.isTotal(genericKey)) {
			val fields = getFields(genericKey)
			if (fields != null && fields.size > 0) {
				//Should only be one frame so ignore index value, and we ignore multiple values within the frame
				//it would make no sense if it existed.
				val frame = fields[0] as AbstractID3v2Frame?
				if (ID3NumberTotalFields.isNumber(genericKey)) {
					return (frame!!.body as AbstractFrameBodyNumberTotal?)!!.trackNoAsText
				} else if (ID3NumberTotalFields.isTotal(genericKey)) {
					return (frame!!.body as AbstractFrameBodyNumberTotal?)!!.trackTotalAsText
				}
			} else {
				return ""
			}
		} else if (genericKey === FieldKey.RATING) {
			val fields = getFields(genericKey)
			return if (fields != null && fields.size > n) {
				val frame =
					fields[n] as AbstractID3v2Frame?
				(frame!!.body as FrameBodyPOPM?)!!.rating.toString()
			} else {
				""
			}
		}
		val frameAndSubId = getFrameAndSubIdFromGenericKey(genericKey)
		return doGetValueAtIndex(frameAndSubId, n)
	}

	/**
	 * Create a new field
	 *
	 * Only MUSICIAN field make use of Varargs values field
	 *
	 * @param genericKey is the generic key
	 * @param values
	 * @return
	 * @throws KeyNotFoundException
	 * @throws FieldDataInvalidException
	 */
	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun createField(genericKey: FieldKey?, vararg values: String?): TagField? {
		if (genericKey == null) {
			throw KeyNotFoundException()
		}
		require(!(values == null || values[0] == null)) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }
		val value = values[0]
		val formatKey = getFrameAndSubIdFromGenericKey(genericKey)

		//FrameAndSubId does not contain enough info for these fields to be able to work out what to update
		//that is why we need the extra processing here instead of doCreateTagField()
		return if (ID3NumberTotalFields.isNumber(genericKey)) {
			val frame = createFrame(
				formatKey.frameId
			)
			val framebody = frame.body as AbstractFrameBodyNumberTotal?
			framebody!!.setNumber(value.toString())
			frame
		} else if (ID3NumberTotalFields.isTotal(genericKey)) {
			val frame = createFrame(
				formatKey.frameId
			)
			val framebody = frame.body as AbstractFrameBodyNumberTotal?
			framebody!!.setTotal(value.toString())
			frame
		} else {
			doCreateTagField(formatKey, *values.filterNotNull().toTypedArray())
		}
	}

	/**
	 * Create Frame for Id3 Key
	 *
	 *
	 * Only textual data supported at the moment, should only be used with frames that
	 * support a simple string argument.
	 *
	 * @param formatKey
	 * @param values
	 * @return
	 * @throws KeyNotFoundException
	 * @throws FieldDataInvalidException
	 */
	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	protected fun doCreateTagField(formatKey: FrameAndSubId, vararg values: String): TagField {
		val value = values[0]
		val frame = createFrame(
			formatKey.frameId
		)
		if (frame.body is FrameBodyUFID) {
			(frame.body as FrameBodyUFID?)!!.owner = formatKey.getSubId()
			try {
				(frame.body as FrameBodyUFID?)!!.uniqueIdentifier =
					value.toByteArray(charset("ISO-8859-1"))
			} catch (uee: UnsupportedEncodingException) {
				//This will never happen because we are using a charset supported on all platforms
				//but just in case
				throw RuntimeException("When encoding UFID charset ISO-8859-1 was deemed unsupported")
			}
		} else if (frame.body is FrameBodyTXXX) {
			(frame.body as FrameBodyTXXX?)!!.description = formatKey.getSubId()
			(frame.body as FrameBodyTXXX?)!!.text = value
		} else if (frame.body is FrameBodyWXXX) {
			(frame.body as FrameBodyWXXX?)!!.description = formatKey.getSubId()
			(frame.body as FrameBodyWXXX?)!!.urlLink = value
		} else if (frame.body is FrameBodyCOMM) {
			//Set description if set
			if (formatKey.getSubId() != null) {
				(frame.body as FrameBodyCOMM?)!!.description = formatKey.getSubId()
				//Special Handling for Media Monkey Compatability
				if ((frame.body as FrameBodyCOMM?)!!.isMediaMonkeyFrame) {
					(frame.body as FrameBodyCOMM?)!!.language = Languages.MEDIA_MONKEY_ID
				}
			}
			(frame.body as FrameBodyCOMM?)!!.text = value
		} else if (frame.body is FrameBodyUSLT) {
			(frame.body as FrameBodyUSLT?)!!.description = ""
			(frame.body as FrameBodyUSLT?)!!.lyric = value
		} else if (frame.body is FrameBodyWOAR) {
			(frame.body as FrameBodyWOAR?)!!.urlLink = value
		} else if (frame.body is AbstractFrameBodyTextInfo) {
			(frame.body as AbstractFrameBodyTextInfo?)!!.text = value
		} else if (frame.body is FrameBodyPOPM) {
			(frame.body as FrameBodyPOPM?)!!.parseString(value)
		} else if (frame.body is FrameBodyIPLS) {
			if (formatKey.getSubId() != null) {
				(frame.body as FrameBodyIPLS?)!!.addPair(formatKey.getSubId(), value)
			} else {
				if (values.size >= 2) {
					(frame.body as FrameBodyIPLS?)!!.addPair(
						values[0], values[1]
					)
				} else {
					(frame.body as FrameBodyIPLS?)!!.addPair(
						values[0]
					)
				}
			}
		} else if (frame.body is FrameBodyTIPL) {
			if (formatKey.getSubId() != null) {
				(frame.body as FrameBodyTIPL?)!!.addPair(formatKey.getSubId(), value)
			} else if (values.size >= 2) {
				(frame.body as FrameBodyTIPL?)!!.addPair(
					values[0], values[1]
				)
			} else {
				(frame.body as FrameBodyTIPL?)!!.addPair(
					values[0]
				)
			}
		} else if (frame.body is FrameBodyTMCL) {
			if (values.size >= 2) {
				(frame.body as FrameBodyTMCL?)!!.addPair(
					values[0], values[1]
				)
			} else {
				(frame.body as FrameBodyTMCL?)!!.addPair(
					values[0]
				)
			}
		} else if (frame.body is FrameBodyAPIC || frame.body is FrameBodyPIC) {
			throw UnsupportedOperationException(ErrorMessage.ARTWORK_CANNOT_BE_CREATED_WITH_THIS_METHOD.msg)
		} else {
			throw FieldDataInvalidException("Field with key of:" + formatKey.frameId + ":does not accept cannot parse data:" + value)
		}
		return frame
	}

	/**
	 * Create a list of values for this (sub)frame
	 *
	 *
	 * This method  does all the complex stuff of splitting multiple values in one frame into separate values.
	 *
	 * @param formatKey
	 * @return
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	protected fun doGetValues(formatKey: FrameAndSubId): List<String?> {
		val values: MutableList<String?> = ArrayList()
		if (formatKey.getSubId() != null) {
			//Get list of frames that this uses
			val list = getFields(
				formatKey.frameId
			)
			val li = list.listIterator()
			while (li.hasNext()) {
				val next = (li.next() as AbstractID3v2Frame?)!!.body
				if (next is FrameBodyTXXX) {
					if (next.description == formatKey.getSubId()) {
						values.addAll(next.values)
					}
				} else if (next is FrameBodyWXXX) {
					if (next.description == formatKey.getSubId()) {
						values.addAll(next.urlLinks)
					}
				} else if (next is FrameBodyCOMM) {
					if (next.description == formatKey.getSubId()) {
						values.addAll(next.values)
					}
				} else if (next is FrameBodyUFID) {
					if (next.owner == formatKey.getSubId()) {
						if (next.uniqueIdentifier != null) {
							values.add(String(next.uniqueIdentifier!!))
						}
					}
				} else if (next is AbstractFrameBodyPairs) {
					for (entry in next.pairing.getMapping()) {
						if (entry.key == formatKey.getSubId()) {
							if (entry.value != null) {
								values.add(entry.value)
							}
						}
					}
				} else {
					logger.severe(loggingFilename + ":Need to implement getFields(FieldKey genericKey) for:" + formatKey + next!!.javaClass)
				}
			}
		} else if (formatKey.getGenericKey() != null && formatKey.getGenericKey() === FieldKey.INVOLVEDPEOPLE) {
			val list = getFields(
				formatKey.frameId
			)
			val li = list.listIterator()
			while (li.hasNext()) {
				val next = (li.next() as AbstractID3v2Frame?)!!.body
				if (next is AbstractFrameBodyPairs) {
					for (entry in next.pairing.getMapping()) {
						//if(!StandardIPLSKey.isKey(entry.getKey()))
						if (true) {
							if (entry.value!!.isNotEmpty()) {
								if (entry.key!!.isNotEmpty()) {
									values.add(entry.pairValue)
								} else {
									values.add(entry.value)
								}
							}
						}
					}
				}
			}
		} else {
			val list = getFields(
				formatKey.frameId
			)
			for (next in list) {
				val frame = next as AbstractID3v2Frame?
				if (frame != null) {
					if (frame.body is AbstractFrameBodyTextInfo) {
						val fb = frame.body as AbstractFrameBodyTextInfo?
						values.addAll(fb!!.values)
					} else {
						values.add(getTextValueForFrame(frame))
					}
				}
			}
		}
		return values
	}

	/**
	 * Get the value at the index, we massage the values so that the index as used in the generic interface rather
	 * than simply taking the frame index. For example if two composers have been added then then they can be retrieved
	 * individually using index=0, index=1 despite the fact that both internally will be stored in a single TCOM frame.
	 *
	 * @param formatKey
	 * @param index     the index specified by the user
	 * @return
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	protected fun doGetValueAtIndex(formatKey: FrameAndSubId, index: Int): String? {
		val values = doGetValues(formatKey)
		return if (values.size > index) {
			values[index]
		} else ""
	}

	/**
	 * Create a link to artwork, this is not recommended because the link may be broken if the mp3 or image
	 * file is moved
	 *
	 * @param url specifies the link, it could be a local file or could be a full url
	 * @return
	 */
	fun createLinkedArtworkField(url: String): TagField {
		val frame = createFrame(
			getFrameAndSubIdFromGenericKey(FieldKey.COVER_ART).frameId
		)
		if (frame.body is FrameBodyAPIC) {
			val body = frame.body as FrameBodyAPIC?
			body!!.setObjectValue(
				DataTypes.OBJ_PICTURE_DATA,
				url.toByteArray(StandardCharsets.ISO_8859_1)
			)
			body.setObjectValue(DataTypes.OBJ_PICTURE_TYPE, PictureTypes.DEFAULT_ID)
			body.setObjectValue(DataTypes.OBJ_MIME_TYPE, FrameBodyAPIC.IMAGE_IS_URL)
			body.setObjectValue(DataTypes.OBJ_DESCRIPTION, "")
		} else if (frame.body is FrameBodyPIC) {
			val body = frame.body as FrameBodyPIC?
			body!!.setObjectValue(
				DataTypes.OBJ_PICTURE_DATA,
				url.toByteArray(StandardCharsets.ISO_8859_1)
			)
			body.setObjectValue(DataTypes.OBJ_PICTURE_TYPE, PictureTypes.DEFAULT_ID)
			body.setObjectValue(DataTypes.OBJ_IMAGE_FORMAT, FrameBodyAPIC.IMAGE_IS_URL)
			body.setObjectValue(DataTypes.OBJ_DESCRIPTION, "")
		}
		return frame
	}

	/**
	 * Some frames are used to store a number/total value, we have to consider both values when requested to delete a
	 * key relating to one of them
	 *
	 * @param formatKey
	 * @param numberFieldKey
	 * @param totalFieldKey
	 * @param deleteNumberFieldKey
	 */
	private fun deleteNumberTotalFrame(
		formatKey: FrameAndSubId,
		numberFieldKey: FieldKey,
		totalFieldKey: FieldKey,
		deleteNumberFieldKey: Boolean
	) {
		if (deleteNumberFieldKey) {
			val total = this.getFirst(totalFieldKey)
			if (total!!.length == 0) {
				doDeleteTagField(formatKey)
				return
			} else {
				val fields = getFrame(
					formatKey.frameId
				)
				if (fields.size > 0) {
					val frame = fields[0] as AbstractID3v2Frame?
					val frameBody = frame!!.body as AbstractFrameBodyNumberTotal?
					if (frameBody!!.total == 0) {
						doDeleteTagField(formatKey)
						return
					}
					frameBody.number = 0
					return
				}
			}
		} else {
			val number = this.getFirst(numberFieldKey)
			if (number!!.length == 0) {
				doDeleteTagField(formatKey)
				return
			} else {
				for (field in getFrame(
					formatKey.frameId
				)) {
					if (field is AbstractID3v2Frame) {
						val frameBody = field.body as AbstractFrameBodyNumberTotal?
						if (frameBody!!.number == 0) {
							doDeleteTagField(formatKey)
						}
						frameBody.total = 0
					}
				}
			}
		}
	}

	/**
	 * Delete fields with this generic key
	 *
	 * If generic key maps to multiple frames then do special processing here rather doDeleteField()
	 *
	 * @param fieldKey
	 */
	@Throws(KeyNotFoundException::class)
	override fun deleteField(fieldKey: FieldKey?) {
		val formatKey = getFrameAndSubIdFromGenericKey(fieldKey)
		if (fieldKey == null) {
			throw KeyNotFoundException()
		}
		when (fieldKey) {
			FieldKey.TRACK -> deleteNumberTotalFrame(
				formatKey,
				FieldKey.TRACK,
				FieldKey.TRACK_TOTAL,
				true
			)
			FieldKey.TRACK_TOTAL -> deleteNumberTotalFrame(
				formatKey,
				FieldKey.TRACK,
				FieldKey.TRACK_TOTAL,
				false
			)
			FieldKey.DISC_NO -> deleteNumberTotalFrame(
				formatKey,
				FieldKey.DISC_NO,
				FieldKey.DISC_TOTAL,
				true
			)
			FieldKey.DISC_TOTAL -> deleteNumberTotalFrame(
				formatKey,
				FieldKey.DISC_NO,
				FieldKey.DISC_TOTAL,
				false
			)
			FieldKey.MOVEMENT_NO -> deleteNumberTotalFrame(
				formatKey,
				FieldKey.MOVEMENT_NO,
				FieldKey.MOVEMENT_TOTAL,
				true
			)
			FieldKey.MOVEMENT_TOTAL -> deleteNumberTotalFrame(
				formatKey,
				FieldKey.MOVEMENT_NO,
				FieldKey.MOVEMENT_TOTAL,
				false
			)
			else -> doDeleteTagField(formatKey)
		}
	}

	/**
	 * Internal delete method, for deleting/modifying an individual ID3 frame
	 *
	 * @param formatKey
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	protected fun doDeleteTagField(formatKey: FrameAndSubId) {
		if (formatKey.getSubId() != null) {
			//Get list of frames that this uses
			val list = getFields(formatKey.frameId)
			val li = list.listIterator()
			while (li.hasNext()) {
				val next = (li.next() as AbstractID3v2Frame?)!!.body
				if (next is FrameBodyTXXX) {
					if (next.description == formatKey.getSubId()) {
						if (list.size == 1) {
							removeFrame(formatKey.frameId!!)
						} else {
							li.remove()
						}
					}
				} else if (next is FrameBodyCOMM) {
					if (next.description == formatKey.getSubId()) {
						if (list.size == 1) {
							removeFrame(formatKey.frameId!!)
						} else {
							li.remove()
						}
					}
				} else if (next is FrameBodyWXXX) {
					if (next.description == formatKey.getSubId()) {
						if (list.size == 1) {
							removeFrame(formatKey.frameId!!)
						} else {
							li.remove()
						}
					}
				} else if (next is FrameBodyUFID) {
					if (next.owner == formatKey.getSubId()) {
						if (list.size == 1) {
							removeFrame(formatKey.frameId!!)
						} else {
							li.remove()
						}
					}
				} else {
					throw RuntimeException("Need to implement getFields(FieldKey genericKey) for:" + next!!.javaClass)
				}
			}
		} else if (formatKey.getSubId() == null) {
			removeFrame(formatKey.frameId!!)
		}
	}

	protected abstract fun getFrameAndSubIdFromGenericKey(genericKey: FieldKey?): FrameAndSubId

	/**
	 * Get field(s) for this generic key
	 *
	 *
	 * This will return the number of underlying frames of this type, for example if you have added two TCOM field
	 * values these will be stored within a single frame so only one field will be returned not two. This can be
	 * confusing because getValues() would return two values.
	 *
	 * @param genericKey
	 * @return
	 * @throws KeyNotFoundException
	 */
	@Throws(KeyNotFoundException::class)
	override fun getFields(genericKey: FieldKey?): List<TagField?>? {
		requireNotNull(genericKey) { ErrorMessage.GENERAL_INVALID_NULL_ARGUMENT.msg }
		val formatKey = getFrameAndSubIdFromGenericKey(genericKey)

		//Get list of frames that this uses, as we are going to remove entries we don't want take a copy
		val list = getFields(
			formatKey.frameId
		)
		val filteredList: MutableList<TagField?> = ArrayList()
		val subFieldId = formatKey.getSubId()
		return if (subFieldId != null) {
			for (tagfield in list) {
				val next = (tagfield as AbstractID3v2Frame?)!!.body
				if (next is FrameBodyTXXX) {
					if (next.description == formatKey.getSubId()) {
						filteredList.add(tagfield)
					}
				} else if (next is FrameBodyWXXX) {
					if (next.description == formatKey.getSubId()) {
						filteredList.add(tagfield)
					}
				} else if (next is FrameBodyCOMM) {
					if (next.description == formatKey.getSubId()) {
						filteredList.add(tagfield)
					}
				} else if (next is FrameBodyUFID) {
					if (next.owner == formatKey.getSubId()) {
						filteredList.add(tagfield)
					}
				} else if (next is FrameBodyIPLS) {
					for (entry in next.pairing.getMapping()) {
						if (entry.key == formatKey.getSubId()) {
							filteredList.add(tagfield)
						}
					}
				} else if (next is FrameBodyTIPL) {
					for (entry in next.pairing.getMapping()) {
						if (entry.key == formatKey.getSubId()) {
							filteredList.add(tagfield)
						}
					}
				} else {
					logger.severe("Need to implement getFields(FieldKey genericKey) for:" + formatKey + next!!.javaClass)
				}
			}
			filteredList
		} else if (ID3NumberTotalFields.isNumber(genericKey)) {
			for (tagfield in list) {
				val next = (tagfield as AbstractID3v2Frame?)!!.body
				if (next is AbstractFrameBodyNumberTotal) {
					if (next.number != null) {
						filteredList.add(tagfield)
					}
				}
			}
			filteredList
		} else if (ID3NumberTotalFields.isTotal(genericKey)) {
			for (tagfield in list) {
				val next = (tagfield as AbstractID3v2Frame?)!!.body
				if (next is AbstractFrameBodyNumberTotal) {
					if (next.total != null) {
						filteredList.add(tagfield)
					}
				}
			}
			filteredList
		} else {
			list
		}
	}

	/**
	 * This class had to be created to minimize the duplicate code in concrete subclasses
	 * of this class. It is required in some cases when using the fieldKey enums because enums
	 * cannot be sub classed. We want to use enums instead of regular classes because they are
	 * much easier for end users to  to use.
	 */
	inner class FrameAndSubId(
		private val genericKey: FieldKey?,
		val frameId: String?,
		private val subId: String?
	) {
		fun getGenericKey(): FieldKey? {
			return genericKey
		}

		fun getSubId(): String? {
			return subId
		}

		override fun toString(): String {
			return String.format("%s:%s:%s", genericKey?.name, frameId, subId)
		}
	}

	override val firstArtwork: Artwork?
		get() = artworkList?.firstOrNull()

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
	 * Create field and then set within tag itself
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

	override fun toString(): String {
		val out = StringBuilder()
		out.append("Tag content:\n")
		val it = fields
		while (it.hasNext()) {
			val field = it.next()
			out.append("\t")
			out.append(field!!.id)
			out.append(":")
			out.append(field.toDescriptiveString())
			out.append("\n")
		}
		return out.toString()
	}

	@Throws(KeyNotFoundException::class, FieldDataInvalidException::class)
	override fun createCompilationField(value: Boolean): TagField? {
		return if (value) {
			createField(
				FieldKey.IS_COMPILATION,
				"1"
			)
		} else {
			createField(
				FieldKey.IS_COMPILATION,
				"0"
			)
		}
	}

	fun setStartLocationInFile(startLocationInFile: Long) {
		this.startLocationInFile = startLocationInFile
	}

	fun setEndLocationInFile(endLocationInFile: Long) {
		this.endLocationInFile = endLocationInFile
	}

	companion object {
		@JvmStatic
		protected val TYPE_HEADER = "header"

		@JvmStatic
		protected val TYPE_BODY = "body"

		//Tag ID as held in file
		@JvmField
		val TAG_ID = byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte())
		const val TAGID = "ID3"

		//The tag header is the same for ID3v2 versions
		const val TAG_HEADER_LENGTH = 10
		const val FIELD_TAGID_LENGTH = 3
		const val FIELD_TAG_MAJOR_VERSION_LENGTH = 1
		const val FIELD_TAG_MINOR_VERSION_LENGTH = 1
		const val FIELD_TAG_FLAG_LENGTH = 1
		const val FIELD_TAG_SIZE_LENGTH = 4
		const val FIELD_TAGID_POS = 0
		const val FIELD_TAG_MAJOR_VERSION_POS = 3
		const val FIELD_TAG_MINOR_VERSION_POS = 4
		const val FIELD_TAG_FLAG_POS = 5
		const val FIELD_TAG_SIZE_POS = 6
		protected const val TAG_SIZE_INCREMENT = 100

		/**
		 * Holds the ids of invalid duplicate frames
		 */
		protected const val TYPE_DUPLICATEFRAMEID = "duplicateFrameId"

		/**
		 * Holds count the number of bytes used up by invalid duplicate frames
		 */
		protected const val TYPE_DUPLICATEBYTES = "duplicateBytes"

		/**
		 * Holds count the number bytes used up by empty frames
		 */
		protected const val TYPE_EMPTYFRAMEBYTES = "emptyFrameBytes"

		/**
		 * Holds the size of the tag as reported by the tag header
		 */
		protected const val TYPE_FILEREADSIZE = "fileReadSize"

		/**
		 * Holds count of invalid frames, (frames that could not be read)
		 */
		protected const val TYPE_INVALIDFRAMES = "invalidFrames"

		/**
		 * True if files has a ID3v2 header
		 *
		 * @param raf
		 * @return
		 * @throws IOException
		 */
		@Throws(IOException::class)
		private fun isID3V2Header(raf: RandomAccessFile): Boolean {
			val start = raf.filePointer
			val tagIdentifier = ByteArray(FIELD_TAGID_LENGTH)
			raf.read(tagIdentifier)
			raf.seek(start)
			return Arrays.equals(
				tagIdentifier,
				TAG_ID
			)
		}

		@Throws(IOException::class)
		private fun isID3V2Header(fc: FileChannel): Boolean {
			val start = fc.position()
			val headerBuffer = Utils.readFileDataIntoBufferBE(fc, FIELD_TAGID_LENGTH)
			fc.position(start)
			val s = Utils.readThreeBytesAsChars(headerBuffer)
			return s == TAGID
		}

		/**
		 * Determines if file contain an id3 tag and if so positions the file pointer just after the end
		 * of the tag.
		 *
		 *
		 * This method is used by non mp3s (such as .ogg and .flac) to determine if they contain an id3 tag
		 *
		 * @param raf
		 * @return
		 * @throws IOException
		 */
		@Throws(IOException::class)
		fun isId3Tag(raf: RandomAccessFile): Boolean {
			if (!isID3V2Header(raf)) {
				return false
			}
			//So we have a tag
			val tagHeader = ByteArray(FIELD_TAG_SIZE_LENGTH)
			raf.seek(raf.filePointer + FIELD_TAGID_LENGTH + FIELD_TAG_MAJOR_VERSION_LENGTH + FIELD_TAG_MINOR_VERSION_LENGTH + FIELD_TAG_FLAG_LENGTH)
			raf.read(tagHeader)
			val bb = ByteBuffer.wrap(tagHeader)
			val size = ID3SyncSafeInteger.bufferToValue(bb)
			raf.seek((size + TAG_HEADER_LENGTH).toLong())
			return true
		}

		/**
		 * Is ID3 tag
		 *
		 * @param fc
		 * @return
		 * @throws IOException
		 */
		@JvmStatic
		@Throws(IOException::class)
		fun isId3Tag(fc: FileChannel): Boolean {
			if (!isID3V2Header(fc)) {
				return false
			}
			//So we have a tag
			val bb = ByteBuffer.allocateDirect(FIELD_TAG_SIZE_LENGTH)
			fc.position(fc.position() + FIELD_TAGID_LENGTH + FIELD_TAG_MAJOR_VERSION_LENGTH + FIELD_TAG_MINOR_VERSION_LENGTH + FIELD_TAG_FLAG_LENGTH)
			fc.read(bb)
			bb.flip()
			val size = ID3SyncSafeInteger.bufferToValue(bb)
			fc.position((size + TAG_HEADER_LENGTH).toLong())
			return true
		}

		/**
		 * Checks to see if the file contains an ID3tag and if so return its size as reported in
		 * the tag header  and return the size of the tag (including header), if no such tag exists return
		 * zero.
		 *
		 * @param fileDescriptor
		 * @return the end of the tag in the file or zero if no tag exists.
		 * @throws IOException
		 */
		@Throws(IOException::class)
		fun getV2TagSizeIfExists(fileDescriptor: FileDescriptor): Long {
			val bb: ByteBuffer?
			var fis: FileInputStream? = null
			var fc: FileChannel? = null

			try {
				fis = FileInputStream(fileDescriptor)
				fc = fis.channel
				bb = ByteBuffer.allocate(TAG_HEADER_LENGTH)

				fc.read(bb)
				bb.flip()

				if (bb.limit() < TAG_HEADER_LENGTH) return 0
			} finally {
				fis?.close()
				fc?.close()
			}

			if (bb == null) return 0

			val tagIdentifier = ByteArray(FIELD_TAGID_LENGTH)
			bb[tagIdentifier, 0, FIELD_TAGID_LENGTH]
			if (!tagIdentifier.contentEquals(TAG_ID)) return 0

			val majorVersion = bb.get()
			if (majorVersion != ID3v22Tag.MAJOR_VERSION
				&& majorVersion != ID3v23Tag.MAJOR_VERSION
				&& majorVersion != ID3v24Tag.MAJOR_VERSION
			) {
				return 0
			}

			//Skip Minor Version
			bb.get()

			//Skip Flags
			bb.get()

			//Get size as recorded in frame header
			var frameSize = ID3SyncSafeInteger.bufferToValue(bb)

			//addField header size to frame size
			frameSize += TAG_HEADER_LENGTH
			return frameSize.toLong()
		}

		/**
		 * Checks to see if the file contains an ID3tag and if so return its size as reported in
		 * the tag header  and return the size of the tag (including header), if no such tag exists return
		 * zero.
		 *
		 * @param file
		 * @return the end of the tag in the file or zero if no tag exists.
		 * @throws IOException
		 */
		@JvmStatic
		@Throws(IOException::class)
		fun getV2TagSizeIfExists(file: File): Long {
			var fis: FileInputStream? = null
			var fc: FileChannel? = null
			var bb: ByteBuffer? = null
			try {
				//Files
				fis = FileInputStream(file)
				fc = fis.channel

				//Read possible Tag header  Byte Buffer
				bb = ByteBuffer.allocate(TAG_HEADER_LENGTH)
				fc.read(bb)
				bb.flip()
				if (bb.limit() < TAG_HEADER_LENGTH) {
					return 0
				}
			} finally {
				fc?.close()
				fis?.close()
			}

			//ID3 identifier
			val tagIdentifier = ByteArray(FIELD_TAGID_LENGTH)
			bb!![tagIdentifier, 0, FIELD_TAGID_LENGTH]
			if (!Arrays.equals(tagIdentifier, TAG_ID)) {
				return 0
			}

			//Is it valid Major Version
			val majorVersion = bb.get()
			if (majorVersion != ID3v22Tag.MAJOR_VERSION && majorVersion != ID3v23Tag.MAJOR_VERSION && majorVersion != ID3v24Tag.MAJOR_VERSION) {
				return 0
			}

			//Skip Minor Version
			bb.get()

			//Skip Flags
			bb.get()

			//Get size as recorded in frame header
			var frameSize = ID3SyncSafeInteger.bufferToValue(bb)

			//addField header size to frame size
			frameSize += TAG_HEADER_LENGTH
			return frameSize.toLong()
		}
	}
}
