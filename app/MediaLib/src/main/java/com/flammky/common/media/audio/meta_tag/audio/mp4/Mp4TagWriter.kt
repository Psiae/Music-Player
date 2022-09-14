/*
 * Entagged Audio Tag library
 * Copyright (c) 2003-2005 Raphaël Slinckx <raphael@slinckx.net>
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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4

import com.flammky.android.core.sdk.VersionHelper
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotWriteException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom.Mp4BoxHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom.Mp4FreeBox
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom.Mp4HdlrBox.Companion.createiTunesStyleHdlrBox
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom.Mp4MetaBox
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom.Mp4MetaBox.Companion.createiTunesStyleMetaBox
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp4.atom.Mp4StcoBox
import com.flammky.musicplayer.common.media.audio.meta_tag.logging.ErrorMessage
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.TagOptionSingleton
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.Mp4Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.mp4.Mp4TagCreator
import com.flammky.musicplayer.common.media.audio.meta_tag.utils.ShiftData.shiftDataByOffsetToMakeSpace
import com.flammky.musicplayer.common.media.audio.meta_tag.utils.tree.DefaultMutableTreeNode
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.logging.Logger
import kotlin.math.abs

/**
 * Writes metadata from mp4, the metadata tags are held under the `ilst` atom as shown below, (note all free atoms are
 * optional).
 *
 *
 * When writing changes the size of all the atoms up to `ilst` has to be recalculated, then if the size of
 * the
 *
 *
 * If the size of the metadata has increased by more than the size of the `free` atom then the size of its parents
 * have to be recalculated. This means `meta`, `udta` and `moov` should be recalculated and the top
 * level `free` atom reduced accordingly.
 *
 *
 * If there is not enough space even if using both of the `free` atoms, then the `mdat` atom has to be
 * shifted down accordingly to make space, and the `stco` atoms have to have their offsets to `mdat`
 * chunks table adjusted accordingly.
 *
 *
 * Exceptions are that the meta/udta/ilst do not currently exist, in which udta/meta/ilst are created. Note it is valid
 * to have meta/ilst without udta but this is less common so we always try to write files according to the Apple/iTunes
 * specification. *
 *
 *
 *
 *
 * <pre>
 * |--- ftyp
 * |--- free
 * |--- moov
 * |......|
 * |......|----- mvdh
 * |......|----- trak (there may be more than one trak atom, e.g. Native Instrument STEM files)
 * |......|.......|
 * |......|.......|-- tkhd
 * |......|.......|-- mdia
 * |......|............|
 * |......|............|-- mdhd
 * |......|............|-- hdlr
 * |......|............|-- minf
 * |......|.................|
 * |......|.................|-- smhd
 * |......|.................|-- dinf
 * |......|.................|-- stbl
 * |......|......................|
 * |......|......................|-- stsd
 * |......|......................|-- stts
 * |......|......................|-- stsc
 * |......|......................|-- stsz
 * |......|......................|-- stco (important! may need to be adjusted.)
 * |......|
 * |......|----- udta
 * |..............|
 * |..............|-- meta
 * |....................|
 * |....................|-- hdlr
 * |....................|-- ilst
 * |....................|.. ..|
 * |....................|.....|---- @nam (Optional for each metadatafield)
 * |....................|.....|.......|-- data
 * |....................|.....|....... ecetera
 * |....................|.....|---- ---- (Optional for reverse dns field)
 * |....................|.............|-- mean
 * |....................|.............|-- name
 * |....................|.............|-- data
 * |....................|................ ecetera
 * |....................|-- free
 * |--- free
 * |--- mdat
</pre> *
 */
class Mp4TagWriter(  //For logging
	private val loggingName: String
) {
	private val tc = Mp4TagCreator()

	/**
	 * Replace the `ilst` metadata.
	 *
	 *
	 * Because it is the same size as the original data nothing else has to be modified.
	 *
	 * @param fc
	 * @param newIlstData
	 * @throws CannotWriteException
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun writeMetadataSameSize(
		fc: SeekableByteChannel,
		ilstHeader: Mp4BoxHeader?,
		newIlstData: ByteBuffer
	) {
		logger.config("Writing:Option 1:Same Size")
		fc.position(ilstHeader!!.filePos)
		fc.write(newIlstData)
	}

	/**
	 * When the size of the metadata has changed and it can't be compensated for by `free` atom
	 * we have to adjust the size of the size field up to the moovheader level for the `udta` atom and
	 * its child `meta` atom.
	 *
	 * @param moovHeader
	 * @param moovBuffer
	 * @param sizeAdjustment can be negative or positive     *
	 * @param udtaHeader
	 * @param metaHeader
	 * @return
	 * @throws IOException
	 */
	private fun adjustSizeOfMoovHeader(
		moovHeader: Mp4BoxHeader,
		moovBuffer: ByteBuffer?,
		sizeAdjustment: Int,
		udtaHeader: Mp4BoxHeader?,
		metaHeader: Mp4BoxHeader?
	) {


		//Adjust moov header size, adjusts the underlying buffer
		moovHeader.length = (moovHeader.length + sizeAdjustment)

		//Edit the fields in moovBuffer (note moovbuffer doesnt include header)
		if (udtaHeader != null) {
			//Write the updated udta atom header to moov buffer
			udtaHeader.length = (udtaHeader.length + sizeAdjustment)
			moovBuffer!!.position((udtaHeader.filePos - moovHeader.filePos - Mp4BoxHeader.HEADER_LENGTH).toInt())
			moovBuffer.put(udtaHeader.headerData!!)
		}
		if (metaHeader != null) {
			//Write the updated udta atom header to moov buffer
			metaHeader.length = (metaHeader.length + sizeAdjustment)
			moovBuffer!!.position((metaHeader.filePos - moovHeader.filePos - Mp4BoxHeader.HEADER_LENGTH).toInt())
			moovBuffer.put(metaHeader.headerData!!)
		}
	}

	/**
	 * Existing metadata larger than new metadata, so we can usually replace metadata and add/modify free atom.
	 *
	 * @param fc
	 * @param moovHeader
	 * @param udtaHeader
	 * @param metaHeader
	 * @param ilstHeader
	 * @param mdatHeader
	 * @param neroTagsHeader
	 * @param moovBuffer
	 * @param newIlstData
	 * @param stcos
	 * @param sizeOfExistingMetaLevelFreeAtom
	 * @throws IOException
	 * @throws CannotWriteException
	 */
	@Throws(IOException::class)
	private fun writeOldMetadataLargerThanNewMetadata(
		fc: SeekableByteChannel,
		moovHeader: Mp4BoxHeader?,
		udtaHeader: Mp4BoxHeader?,
		metaHeader: Mp4BoxHeader?,
		ilstHeader: Mp4BoxHeader?,
		mdatHeader: Mp4BoxHeader,
		neroTagsHeader: Mp4BoxHeader?,
		moovBuffer: ByteBuffer?,
		newIlstData: ByteBuffer,
		stcos: List<Mp4StcoBox?>?,
		sizeOfExistingMetaLevelFreeAtom: Int
	) {
		logger.config("Writing:Option 1:Smaller Size")
		val ilstPositionRelativeToAfterMoovHeader =
			(ilstHeader!!.filePos - (moovHeader!!.filePos + Mp4BoxHeader.HEADER_LENGTH)).toInt()
		//Create an amended freeBaos atom and write it if it previously existed as a free atom immediately
		//after ilst as a child of meta
		val sizeOfNewIlstAtom = newIlstData.limit()
		if (sizeOfExistingMetaLevelFreeAtom > 0) {
			logger.config("Writing:Option 2:Smaller Size have free atom:" + ilstHeader.length + ":" + sizeOfNewIlstAtom)
			fc.position(ilstHeader.filePos)
			fc.write(newIlstData)

			//Write the modified free atom that comes after ilst
			//New ilst + new free should put at same position as Old Ilst + old free so nothing else to do
			val newFreeSize =
				sizeOfExistingMetaLevelFreeAtom + (ilstHeader.length - sizeOfNewIlstAtom)
			val newFreeBox = Mp4FreeBox(newFreeSize - Mp4BoxHeader.HEADER_LENGTH)
			fc.write(newFreeBox.header!!.headerData)
			fc.write(newFreeBox.data)
		} else {
			//We need to create a new one, so dont have to adjust all the headers but only works if the size
			//of tags has decreased by more 8 characters so there is enough room for the free boxes header we take
			//into account size of new header in calculating size of box
			val newFreeSize =
				ilstHeader.length - sizeOfNewIlstAtom - Mp4BoxHeader.HEADER_LENGTH
			if (newFreeSize > 0) {
				logger.config("Writing:Option 3:Smaller Size can create free atom")
				fc.position(ilstHeader.filePos)
				fc.write(newIlstData)

				//Create new free box
				//New ilst + new free should put at same postion as Old Ilst
				val newFreeBox = Mp4FreeBox(newFreeSize)
				fc.write(newFreeBox.header!!.headerData)
				fc.write(newFreeBox.data)
			} else {
				logger.config("Writing:Option 4:Smaller Size <=8 cannot create free atoms")

				//This is where Moov atom currently ends (need for later)
				val endOfOriginalMoovAtom = moovHeader.fileEndPos

				//Size of new metadata will be this amount smaller
				val sizeReducedBy = ilstHeader.length - sizeOfNewIlstAtom

				//Edit stcos atoms within moov header, we need to adjust offsets by the amount mdat is going to be shifted
				//unless mdat is at start of file
				if (mdatHeader.filePos > moovHeader.filePos) {
					for (stoc in stcos!!) {
						stoc!!.adjustOffsets(-sizeReducedBy)
					}
				}

				//Edit and rewrite the moov, udta and meta header in moov buffer
				adjustSizeOfMoovHeader(
					moovHeader,
					moovBuffer!!,
					-sizeReducedBy,
					udtaHeader,
					metaHeader
				)

				//Write modified MoovHeader
				fc.position(moovHeader.filePos)
				fc.write(moovHeader.headerData)

				//Write modified MoovBuffer upto start of ilst data
				moovBuffer.rewind()
				moovBuffer.limit(ilstPositionRelativeToAfterMoovHeader)
				fc.write(moovBuffer)

				//Write new ilst data
				fc.write(newIlstData)

				//Write rest of moov after the old ilst data, as we may have adjusted stcos atoms that occur after ilst
				moovBuffer.limit(moovBuffer.capacity())
				moovBuffer.position(ilstPositionRelativeToAfterMoovHeader + ilstHeader.length)
				fc.write(moovBuffer)

				//Delete the previous sizeReducedBy bytes from endOfOriginalMovAtom
				shiftData(fc, endOfOriginalMoovAtom, abs(sizeReducedBy))
			}
		}
	}

	/**
	 * Delete deleteSize from startDeleteFrom, shifting down the data that comes after
	 *
	 * @param fc
	 * @param startDeleteFrom
	 * @param deleteSize
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun shiftData(fc: SeekableByteChannel, startDeleteFrom: Long, deleteSize: Int) {
		//Position for reading after the tag
		fc.position(startDeleteFrom)
		val buffer =
			ByteBuffer.allocate(TagOptionSingleton.instance.writeChunkSize.toInt())
		while (fc.read(buffer) >= 0 || buffer.position() != 0) {
			buffer.flip()
			val readPosition = fc.position()
			fc.position(readPosition - deleteSize - buffer.limit())
			fc.write(buffer)
			fc.position(readPosition)
			buffer.compact()
		}
		//Truncate the file after the last chunk
		val newLength = fc.size() - deleteSize
		logger.config(
			"$loggingName-------------Setting new length to:$newLength"
		)
		fc.truncate(newLength)
	}

	/**
	 * We can fit the metadata in under the meta item just by using some of the padding available in the `free`
	 * atom under the `meta` atom
	 *
	 * @param fc
	 * @param sizeOfExistingMetaLevelFreeAtom
	 * @param newIlstData
	 * @param additionalSpaceRequiredForMetadata
	 * @throws IOException
	 * @throws CannotWriteException
	 */
	@Throws(IOException::class, CannotWriteException::class)
	private fun writeNewMetadataLargerButCanUseFreeAtom(
		fc: SeekableByteChannel,
		ilstHeader: Mp4BoxHeader?,
		sizeOfExistingMetaLevelFreeAtom: Int,
		newIlstData: ByteBuffer,
		additionalSpaceRequiredForMetadata: Int
	) {
		//Shrink existing free atom size
		val newFreeSize = sizeOfExistingMetaLevelFreeAtom - additionalSpaceRequiredForMetadata
		logger.config("Writing:Option 5;Larger Size can use meta free atom need extra:" + newFreeSize + "bytes")
		fc.position(ilstHeader!!.filePos)
		fc.write(newIlstData)

		//Create an amended smaller freeBaos atom and write it to file
		val newFreeBox = Mp4FreeBox(newFreeSize - Mp4BoxHeader.HEADER_LENGTH)
		fc.write(newFreeBox.header!!.headerData)
		fc.write(newFreeBox.data)
	}

	/**
	 * Write tag to file.
	 *
	 * @param tag     tag data
	 * @param file     current file
	 * @throws CannotWriteException
	 * @throws IOException
	 */
	@Throws(CannotWriteException::class)
	fun write(tag: Tag?, file: Path?) {
		if (!VersionHelper.hasOreo()) TODO("Implement API < 26")

		logger.config("Started writing tag data")
		try {
			Files.newByteChannel(file, StandardOpenOption.READ, StandardOpenOption.WRITE)
				.use { fc ->
					var sizeOfExistingIlstAtom = 0
					val sizeRequiredByNewIlstAtom: Int
					val positionOfNewIlstAtomRelativeToMoovAtom: Int
					val positionOfStartOfIlstAtomInMoovBuffer: Int
					val sizeOfExistingMetaLevelFreeAtom: Int
					var positionOfTopLevelFreeAtom: Int
					var sizeOfExistingTopLevelFreeAtom: Int
					//Found top level free atom that comes after moov and before mdat, (also true if no free atom ?)
					var topLevelFreeAtomComesBeforeMdatAtomAndAfterMetadata: Boolean
					val topLevelFreeHeader: Mp4BoxHeader?
					val atomTree: Mp4AtomTree

					//Build AtomTree based on existing metadata
					atomTree = try {
						Mp4AtomTree(fc, false)
					} catch (cre: CannotReadException) {
						throw CannotWriteException(cre.message)
					}
					val mdatHeader = atomTree.getBoxHeader(atomTree.mdatNode)
						?: throw CannotWriteException(ErrorMessage.MP4_CHANGES_TO_FILE_FAILED_CANNOT_FIND_AUDIO.msg)
					//Unable to find audio so no chance of saving any changes

					//Go through every field constructing the data that will appear starting from ilst box
					val newIlstData = tc.convertMetadata(tag)
					newIlstData.rewind()
					sizeRequiredByNewIlstAtom = newIlstData.limit()

					//Moov Box header
					val moovHeader = atomTree.getBoxHeader(atomTree.moovNode)
					val stcos = atomTree.getStcos()
					val ilstHeader = atomTree.getBoxHeader(atomTree.ilstNode)
					val udtaHeader = atomTree.getBoxHeader(atomTree.udtaNode)
					val metaHeader = atomTree.getBoxHeader(atomTree.metaNode)
					val hdlrMetaHeader = atomTree.getBoxHeader(atomTree.hdlrWithinMetaNode)
					val neroTagsHeader = atomTree.getBoxHeader(atomTree.tagsNode)
					val trakHeader =
						atomTree.getBoxHeader(atomTree.getTrakNodes()[atomTree.getTrakNodes().size - 1])
					val moovBuffer = atomTree.moovBuffer


					//Work out if we/what kind of metadata hierarchy we currently have in the file
					//Udta
					if (udtaHeader != null) {
						//Meta
						if (metaHeader != null) {
							//ilst - record where ilst is,and where it ends
							if (ilstHeader != null) {
								sizeOfExistingIlstAtom = ilstHeader.length

								//Relative means relative to moov buffer after moov header
								positionOfStartOfIlstAtomInMoovBuffer = ilstHeader.filePos.toInt()
								positionOfNewIlstAtomRelativeToMoovAtom =
									(positionOfStartOfIlstAtomInMoovBuffer - (moovHeader!!.filePos + Mp4BoxHeader.HEADER_LENGTH)).toInt()
							} else {
								//Place ilst immediately after existing hdlr atom
								if (hdlrMetaHeader != null) {
									positionOfStartOfIlstAtomInMoovBuffer =
										hdlrMetaHeader.fileEndPos.toInt()
									positionOfNewIlstAtomRelativeToMoovAtom =
										(positionOfStartOfIlstAtomInMoovBuffer - (moovHeader!!.filePos + Mp4BoxHeader.HEADER_LENGTH)).toInt()
								} else {
									positionOfStartOfIlstAtomInMoovBuffer =
										metaHeader.filePos.toInt() + Mp4BoxHeader.HEADER_LENGTH + Mp4MetaBox.FLAGS_LENGTH
									positionOfNewIlstAtomRelativeToMoovAtom =
										(positionOfStartOfIlstAtomInMoovBuffer - (moovHeader!!.filePos + Mp4BoxHeader.HEADER_LENGTH)).toInt()
								}
							}
						} else {
							//There no ilst or meta header so we set to position where it would be if it existed
							positionOfNewIlstAtomRelativeToMoovAtom =
								moovHeader!!.length - Mp4BoxHeader.HEADER_LENGTH
							positionOfStartOfIlstAtomInMoovBuffer = moovHeader.fileEndPos.toInt()
						}
					} else {
						//Create new structure just after the end of the last trak atom, as that means
						// all modifications to trak atoms and its children (stco atoms) are *explicitly* written
						// as part of the moov atom (and not just bulk copied via writeDataAfterIlst())
						if (metaHeader != null) {
							positionOfStartOfIlstAtomInMoovBuffer = trakHeader!!.fileEndPos.toInt()
							positionOfNewIlstAtomRelativeToMoovAtom =
								(positionOfStartOfIlstAtomInMoovBuffer - (moovHeader!!.filePos + Mp4BoxHeader.HEADER_LENGTH)).toInt()
						} else {
							//There no udta,ilst or meta header so we set to position where it would be if it existed
							positionOfStartOfIlstAtomInMoovBuffer = moovHeader!!.fileEndPos.toInt()
							positionOfNewIlstAtomRelativeToMoovAtom =
								moovHeader.length - Mp4BoxHeader.HEADER_LENGTH
						}
					}

					//Find size of Level-4 Free atom (if any) immediately after ilst atom
					sizeOfExistingMetaLevelFreeAtom = getMetaLevelFreeAtomSize(atomTree)


					//Level-1 free atom
					positionOfTopLevelFreeAtom = 0
					sizeOfExistingTopLevelFreeAtom = 0
					topLevelFreeAtomComesBeforeMdatAtomAndAfterMetadata = true
					for (freeNode in atomTree.getFreeNodes()) {
						val parentNode = freeNode.parent as? DefaultMutableTreeNode<*>
						if (parentNode?.isRoot() == true) {
							topLevelFreeHeader = freeNode.userObject as Mp4BoxHeader?
							sizeOfExistingTopLevelFreeAtom = topLevelFreeHeader!!.length
							positionOfTopLevelFreeAtom = topLevelFreeHeader.filePos.toInt()
							break
						}
					}
					if (sizeOfExistingTopLevelFreeAtom > 0) {
						if (positionOfTopLevelFreeAtom > mdatHeader.filePos) {
							topLevelFreeAtomComesBeforeMdatAtomAndAfterMetadata = false
						} else if (positionOfTopLevelFreeAtom < moovHeader.filePos) {
							topLevelFreeAtomComesBeforeMdatAtomAndAfterMetadata = false
						}
					} else {
						positionOfTopLevelFreeAtom = mdatHeader.filePos.toInt()
					}
					logger.config("Read header successfully ready for writing")
					//The easiest option since no difference in the size of the metadata so all we have to do is
					//replace the ilst atom (and children)
					if (sizeOfExistingIlstAtom == sizeRequiredByNewIlstAtom) {
						writeMetadataSameSize(fc, ilstHeader, newIlstData)
					} else if (sizeOfExistingIlstAtom > sizeRequiredByNewIlstAtom) {
						writeOldMetadataLargerThanNewMetadata(
							fc,
							moovHeader,
							udtaHeader,
							metaHeader,
							ilstHeader,
							mdatHeader,
							neroTagsHeader,
							moovBuffer,
							newIlstData,
							stcos,
							sizeOfExistingMetaLevelFreeAtom
						)
					} else {
						//We have enough space in existing meta level free atom
						val additionalSpaceRequiredForMetadata =
							sizeRequiredByNewIlstAtom - sizeOfExistingIlstAtom
						if (additionalSpaceRequiredForMetadata <= sizeOfExistingMetaLevelFreeAtom - Mp4BoxHeader.HEADER_LENGTH) {
							writeNewMetadataLargerButCanUseFreeAtom(
								fc,
								ilstHeader,
								sizeOfExistingMetaLevelFreeAtom,
								newIlstData,
								additionalSpaceRequiredForMetadata
							)
						} else {
							val additionalMetaSizeThatWontFitWithinMetaAtom =
								additionalSpaceRequiredForMetadata - sizeOfExistingMetaLevelFreeAtom

							//Go up to position of start of Moov Header
							fc.position(moovHeader.filePos)

							//No existing Metadata
							if (udtaHeader == null) {
								writeNoExistingUdtaAtom(
									fc,
									newIlstData,
									moovHeader,
									moovBuffer,
									mdatHeader,
									stcos,
									sizeOfExistingTopLevelFreeAtom,
									topLevelFreeAtomComesBeforeMdatAtomAndAfterMetadata,
									neroTagsHeader
								)
							} else if (metaHeader == null) {
								writeNoExistingMetaAtom(
									udtaHeader,
									fc,
									newIlstData,
									moovHeader,
									moovBuffer,
									mdatHeader,
									stcos,
									sizeOfExistingTopLevelFreeAtom,
									topLevelFreeAtomComesBeforeMdatAtomAndAfterMetadata,
									neroTagsHeader,
									positionOfStartOfIlstAtomInMoovBuffer,
									sizeOfExistingIlstAtom,
									positionOfTopLevelFreeAtom,
									additionalMetaSizeThatWontFitWithinMetaAtom
								)
							} else {
								writeHaveExistingMetadata(
									udtaHeader,
									metaHeader,
									fc,
									positionOfNewIlstAtomRelativeToMoovAtom,
									moovHeader,
									moovBuffer,
									mdatHeader,
									stcos,
									sizeOfExistingTopLevelFreeAtom,
									topLevelFreeAtomComesBeforeMdatAtomAndAfterMetadata,
									newIlstData,
									neroTagsHeader,
									sizeOfExistingIlstAtom
								)
							}
						}
					}
					//Ensure we have written correctly, reject if not
					checkFileWrittenCorrectly(mdatHeader, fc, stcos)
				}
		} catch (ioe: IOException) {
			throw CannotWriteException(file.toString() + ":" + ioe.message)
		}
	}

	/**
	 * Replace tags atom (and children) by a `free` atom.
	 *
	 * @param fc
	 * @param tagsHeader
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun convertandWriteTagsAtomToFreeAtom(
		fc: SeekableByteChannel,
		tagsHeader: Mp4BoxHeader
	) {
		val freeBox = Mp4FreeBox(tagsHeader.dataLength)
		fc.write(freeBox.header!!.headerData)
		fc.write(freeBox.data)
	}

	/**
	 * Determine the size of the `free` atom immediately after `ilst` atom at the same level (if any),
	 * we can use this if `ilst` needs to grow or shrink because of more less metadata.
	 *
	 * @param atomTree
	 * @return
	 */
	private fun getMetaLevelFreeAtomSize(atomTree: Mp4AtomTree): Int {
		var oldMetaLevelFreeAtomSize: Int //Level 4 - Free
		oldMetaLevelFreeAtomSize = 0
		for (freeNode in atomTree.getFreeNodes()) {
			val parentNode = freeNode.parent as DefaultMutableTreeNode<*>?
			val brotherNode = freeNode.previousSibling
			if (parentNode?.isRoot() == false) {
				val parentHeader = parentNode.userObject as Mp4BoxHeader?
				val freeHeader = freeNode.userObject as Mp4BoxHeader?

				//We are only interested in free atoms at this level if they come after the ilst node
				if (brotherNode != null) {
					val brotherHeader = brotherNode.userObject as Mp4BoxHeader?
					if (parentHeader!!.id == Mp4AtomIdentifier.META.fieldName && brotherHeader!!.id == Mp4AtomIdentifier.ILST.fieldName) {
						oldMetaLevelFreeAtomSize = freeHeader!!.length
						break
					}
				}
			}
		}
		return oldMetaLevelFreeAtomSize
	}

	/**
	 * Check file written correctly.
	 *
	 * @param mdatHeader
	 * @param fc
	 * @param stcos
	 * @throws CannotWriteException
	 * @throws IOException
	 */
	@Throws(CannotWriteException::class, IOException::class)
	private fun checkFileWrittenCorrectly(
		mdatHeader: Mp4BoxHeader,
		fc: SeekableByteChannel,
		stcos: List<Mp4StcoBox?>?
	) {
		logger.config("Checking file has been written correctly")
		try {
			//Create a tree from the new file
			val newAtomTree: Mp4AtomTree
			newAtomTree = Mp4AtomTree(fc, false)

			//Check we still have audio data file, and check length
			val newMdatHeader = newAtomTree.getBoxHeader(newAtomTree.mdatNode)
				?: throw CannotWriteException(ErrorMessage.MP4_CHANGES_TO_FILE_FAILED_NO_DATA.msg)
			if (newMdatHeader.length != mdatHeader.length) {
				throw CannotWriteException(ErrorMessage.MP4_CHANGES_TO_FILE_FAILED_DATA_CORRUPT.msg)
			}

			//Should always have udta atom after writing to file
			val newUdtaHeader = newAtomTree.getBoxHeader(newAtomTree.udtaNode)
				?: throw CannotWriteException(ErrorMessage.MP4_CHANGES_TO_FILE_FAILED_NO_TAG_DATA.msg)

			//Should always have meta atom after writing to file
			val newMetaHeader = newAtomTree.getBoxHeader(newAtomTree.metaNode)
				?: throw CannotWriteException(ErrorMessage.MP4_CHANGES_TO_FILE_FAILED_NO_TAG_DATA.msg)

			// Check that we at the very least have the same number of chunk offsets
			val newStcos = newAtomTree.getStcos()
			if (newStcos.size != stcos!!.size) {
				// at the very least, we have to have the same number of 'stco' atoms
				throw CannotWriteException(
					ErrorMessage.MP4_CHANGES_TO_FILE_FAILED_INCORRECT_NUMBER_OF_TRACKS.getMsg(
						stcos.size, newStcos.size
					)
				)
			}
			//Check offsets are correct, may not match exactly in original file so just want to make
			//sure that the discrepancy if any is preserved

			// compare the first new stco offset with mdat,
			// and ensure that all following ones have a constant shift
			var shift = 0
			for (i in newStcos.indices) {
				val newStco = newStcos[i]
				val stco = stcos[i]
				logger.finer("stco:Original First Offset" + stco!!.firstOffSet)
				logger.finer("stco:Original Diff" + (stco.firstOffSet - mdatHeader.filePos).toInt())
				logger.finer("stco:Original Mdat Pos" + mdatHeader.filePos)
				logger.finer("stco:New First Offset" + newStco.firstOffSet)
				logger.finer("stco:New Diff" + (newStco.firstOffSet - newMdatHeader.filePos).toInt())
				logger.finer("stco:New Mdat Pos" + newMdatHeader.filePos)
				if (i == 0) {
					val diff = (stco.firstOffSet - mdatHeader.filePos).toInt()
					if (newStco.firstOffSet - newMdatHeader.filePos != diff.toLong()) {
						val discrepancy =
							(newStco.firstOffSet - newMdatHeader.filePos - diff).toInt()
						throw CannotWriteException(
							ErrorMessage.MP4_CHANGES_TO_FILE_FAILED_INCORRECT_OFFSETS.getMsg(
								discrepancy
							)
						)
					}
					shift = stco.firstOffSet - newStco.firstOffSet
				} else {
					if (shift != stco.firstOffSet - newStco.firstOffSet) {
						throw CannotWriteException(
							ErrorMessage.MP4_CHANGES_TO_FILE_FAILED_INCORRECT_OFFSETS.getMsg(
								shift
							)
						)
					}
				}
			}
		} catch (e: Exception) {
			if (e is CannotWriteException) {
				throw e
			} else {
				e.printStackTrace()
				throw CannotWriteException(ErrorMessage.MP4_CHANGES_TO_FILE_FAILED.msg + ":" + e.message)
			}
		} finally {
			//Close references to new file
			fc.close()
		}
		logger.config("File has been written correctly")
	}

	/**
	 * Delete the tag.
	 *
	 *
	 *
	 * This is achieved by writing an empty `ilst` atom.
	 *
	 * @param file
	 * @throws IOException
	 */
	@Throws(CannotWriteException::class)
	fun delete(tag: Tag?, file: Path?) {
		var tag = tag
		tag = Mp4Tag()
		write(tag, file)
	}

	/**
	 * Use when we need to write metadata and there is no existing `udta` atom so we keepp the existing moov data
	 * but have to ajdjust the moov header lengths and then create the complete udta/metadata structure and add to the
	 * end.
	 *
	 * If we can fit the new metadata into top level free atom we just shrink that accordingly
	 *
	 * If we cant then we leave it alone and just shift all the data down aftet the moov (i.e top level free and mdat)
	 *
	 * @param fc
	 * @param newIlstData
	 * @param moovHeader
	 * @param moovBuffer
	 * @param mdatHeader
	 * @param stcos
	 * @param sizeOfExistingTopLevelFreeAtom
	 * @param topLevelFreeAtomComesBeforeMdatAtomAndAfterMetadata
	 * @throws IOException
	 * @throws CannotWriteException
	 */
	@Throws(IOException::class)
	private fun writeNoExistingUdtaAtom(
		fc: SeekableByteChannel,
		newIlstData: ByteBuffer,
		moovHeader: Mp4BoxHeader?,
		moovBuffer: ByteBuffer?,
		mdatHeader: Mp4BoxHeader,
		stcos: List<Mp4StcoBox?>?,
		sizeOfExistingTopLevelFreeAtom: Int,
		topLevelFreeAtomComesBeforeMdatAtomAndAfterMetadata: Boolean,
		neroTagsHeader: Mp4BoxHeader?
	) {
		val endOfOriginalMoovAtom = moovHeader!!.fileEndPos
		val hdlrBox = createiTunesStyleHdlrBox()
		val metaBox = createiTunesStyleMetaBox(
			hdlrBox.header!!.length + newIlstData.limit()
		)
		val udtaHeader = Mp4BoxHeader(Mp4AtomIdentifier.UDTA.fieldName)
		udtaHeader.length = (Mp4BoxHeader.HEADER_LENGTH + metaBox.header!!.length)

		//If we can fit in top level free atom we dont have to move mdat data
		val isMdatDataMoved = adjustStcosIfNoSuitableTopLevelAtom(
			sizeOfExistingTopLevelFreeAtom,
			topLevelFreeAtomComesBeforeMdatAtomAndAfterMetadata,
			udtaHeader.length,
			stcos,
			moovHeader,
			mdatHeader
		)

		//Edit the Moov header to length and rewrite to account for new udta atom
		moovHeader.length = (moovHeader.length + udtaHeader.length)

		//Position to start of Moov Header in File
		fc.position(moovHeader.filePos)

		//Write the new Moov Header
		fc.write(moovHeader.headerData)

		//Write the Existing Moov Data
		moovBuffer!!.rewind()
		fc.write(moovBuffer)

		//TODO what about nero tag ?
		if (!isMdatDataMoved) {
			logger.severe("Writing:Option 5.1;No udta atom")

			//Now Write new atoms required for holding metadata under udta/meta/hdlr
			fc.write(udtaHeader.headerData)
			fc.write(metaBox.header!!.headerData)
			fc.write(metaBox.data)
			fc.write(hdlrBox.header!!.headerData)
			fc.write(hdlrBox.data)

			//Write new ilst data
			fc.write(newIlstData)

			//Shrink the free atom accordingly to accommodate the extra data
			adjustTopLevelFreeAtom(fc, sizeOfExistingTopLevelFreeAtom, udtaHeader.length)
		} else {
			logger.severe("Writing:Option 5.2;No udta atom, not enough free space")

			//Position after MoovBuffer in file
			fc.position(endOfOriginalMoovAtom)
			shiftDataByOffsetToMakeSpace(fc, udtaHeader.length)

			//Go back to position just after MoovBuffer in file
			fc.position(endOfOriginalMoovAtom)

			//Now Write new atoms required for holding metadata under udta/meta/hdlr
			fc.write(udtaHeader.headerData)
			fc.write(metaBox.header!!.headerData)
			fc.write(metaBox.data)
			fc.write(hdlrBox.header!!.headerData)
			fc.write(hdlrBox.data)

			//Write new ilst data
			fc.write(newIlstData)
		}
	}

	/**
	 * Use when we need to write metadata, we have a `udta` atom but there is no existing meta atom so we
	 * have to create the complete metadata structure.
	 *
	 * @param fc
	 * @param newIlstData
	 * @param moovHeader
	 * @param moovBuffer
	 * @param mdatHeader
	 * @param stcos
	 * @param sizeOfExistingTopLevelFreeAtom
	 * @param topLevelFreeAtomComesBeforeMdatAtomAndAfterMetadata
	 * @throws IOException
	 * @throws CannotWriteException
	 */
	@Throws(IOException::class)
	private fun writeNoExistingMetaAtom(
		udtaHeader: Mp4BoxHeader,
		fc: SeekableByteChannel,
		newIlstData: ByteBuffer,
		moovHeader: Mp4BoxHeader?,
		moovBuffer: ByteBuffer?,
		mdatHeader: Mp4BoxHeader,
		stcos: List<Mp4StcoBox?>?,
		sizeOfExistingTopLevelFreeAtom: Int,
		topLevelFreeAtomComesBeforeMdatAtomAndAfterMetadata: Boolean,
		neroTagsHeader: Mp4BoxHeader?,
		positionOfStartOfIlstAtomInMoovBuffer: Int,
		existingSizeOfIlstData: Int,
		topLevelFreeSize: Int,
		additionalMetaSizeThatWontFitWithinMetaAtom: Int
	) {
		var udtaHeader = udtaHeader
		val newIlstDataSize = newIlstData.limit()
		val existingMoovHeaderDataLength = moovHeader!!.dataLength
		val endOfOriginalMoovAtom = moovHeader.fileEndPos

		//Udta didnt have a meta atom but it may have some other data we want to preserve (I think)
		val existingUdtaLength = udtaHeader.length
		val existingUdtaDataLength = udtaHeader.dataLength
		val hdlrBox = createiTunesStyleHdlrBox()
		val metaBox = createiTunesStyleMetaBox(
			hdlrBox.header!!.length + newIlstDataSize
		)
		udtaHeader = Mp4BoxHeader(Mp4AtomIdentifier.UDTA.fieldName)
		udtaHeader.length =
			(Mp4BoxHeader.HEADER_LENGTH + metaBox.header!!.length + existingUdtaDataLength)
		val increaseInSizeOfUdtaAtom = udtaHeader.dataLength - existingUdtaDataLength
		val isMdatDataMoved = adjustStcosIfNoSuitableTopLevelAtom(
			sizeOfExistingTopLevelFreeAtom,
			topLevelFreeAtomComesBeforeMdatAtomAndAfterMetadata,
			increaseInSizeOfUdtaAtom,
			stcos,
			moovHeader,
			mdatHeader
		)

		//Edit and rewrite the Moov header upto start of Udta
		moovHeader.length = (moovHeader.length + increaseInSizeOfUdtaAtom)

		//Position to start of Moov Header in File
		fc.position(moovHeader.filePos)

		//Write the new Moov Header
		fc.write(moovHeader.headerData)

		//Write Moov data upto start of existing udta
		moovBuffer!!.rewind()
		moovBuffer.limit(existingMoovHeaderDataLength - existingUdtaLength)
		fc.write(moovBuffer)

		//Write new atoms required for holding metadata in iTunes format
		fc.write(udtaHeader.headerData)

		//Write any atoms if they previously existed within udta atom
		if (moovBuffer.position() + Mp4BoxHeader.HEADER_LENGTH < moovBuffer.capacity()) {
			moovBuffer.limit(moovBuffer.capacity())
			moovBuffer.position(moovBuffer.position() + Mp4BoxHeader.HEADER_LENGTH)
			fc.write(moovBuffer)
		}
		if (!isMdatDataMoved) {
			logger.severe("Writing:Option 6.1;No meta atom")
			//Write our newly constructed meta/hdlr headers (required for ilst)
			fc.write(metaBox.header!!.headerData)
			fc.write(metaBox.data)
			fc.write(hdlrBox.header!!.headerData)
			fc.write(hdlrBox.data)

			//Write new ilst data
			fc.write(newIlstData)
			writeRestOfMoovHeaderAfterNewIlistAndAmendedTopLevelFreeAtom(
				fc,
				positionOfStartOfIlstAtomInMoovBuffer,
				moovHeader,
				moovBuffer,
				additionalMetaSizeThatWontFitWithinMetaAtom,
				topLevelFreeSize,
				neroTagsHeader,
				existingSizeOfIlstData
			)
		} else {
			logger.severe("Writing:Option 6.2;No meta atom, not enough free space")

			//Position after MoovBuffer in file
			fc.position(endOfOriginalMoovAtom)

			//Shift the existing data after Moov Atom by the size of the new meta atom (includes ilst under it)
			shiftDataByOffsetToMakeSpace(fc, metaBox.header!!.length)

			//Now Write new ilst data, continuing from the end of the original Moov atom
			fc.position(endOfOriginalMoovAtom)

			//Write our newly constructed meta/hdlr headers (required for ilst)
			fc.write(metaBox.header!!.headerData)
			fc.write(metaBox.data)
			fc.write(hdlrBox.header!!.headerData)
			fc.write(hdlrBox.data)

			//Write te actual ilst data
			fc.write(newIlstData)
		}
	}

	/**
	 * We have existing structure but we need more space then we have available.
	 *
	 * @param udtaHeader
	 * @param fc
	 * @param positionOfStartOfIlstAtomInMoovBuffer
	 * @param moovHeader
	 * @param moovBuffer
	 * @param mdatHeader
	 * @param stcos
	 * @param topLevelFreeSize
	 * @param topLevelFreeAtomComesBeforeMdatAtomAndAfterMetadata
	 * @throws IOException
	 * @throws CannotWriteException
	 */
	@Throws(IOException::class)
	private fun writeHaveExistingMetadata(
		udtaHeader: Mp4BoxHeader,
		metaHeader: Mp4BoxHeader,
		fc: SeekableByteChannel,
		positionOfStartOfIlstAtomInMoovBuffer: Int,
		moovHeader: Mp4BoxHeader?,
		moovBuffer: ByteBuffer?,
		mdatHeader: Mp4BoxHeader,
		stcos: List<Mp4StcoBox?>?,
		topLevelFreeSize: Int,
		topLevelFreeAtomComesBeforeMdatAtomAndAfterMetadata: Boolean,
		newIlstData: ByteBuffer,
		neroTagsHeader: Mp4BoxHeader?,
		existingSizeOfIlstData: Int
	) {
		val endOfOriginalMoovAtom = moovHeader!!.fileEndPos
		val sizeRequiredByNewIlstAtom = newIlstData.limit()

		//Since we know we cant fit the data into the meta/free atom we dont try to use it, instead we leave it available for future smaller data additions
		//So we just decide if we can fit the extra data into any available toplevel free atom
		val additionalMetaSizeThatWontFitWithinMetaAtom =
			sizeRequiredByNewIlstAtom - existingSizeOfIlstData
		val isMdatDataMoved = adjustStcosIfNoSuitableTopLevelAtom(
			topLevelFreeSize,
			topLevelFreeAtomComesBeforeMdatAtomAndAfterMetadata,
			additionalMetaSizeThatWontFitWithinMetaAtom,
			stcos,
			moovHeader,
			mdatHeader
		)

		//Edit and rewrite the Moov header inc udta and meta headers)
		adjustSizeOfMoovHeader(
			moovHeader,
			moovBuffer,
			additionalMetaSizeThatWontFitWithinMetaAtom,
			udtaHeader,
			metaHeader
		)

		//Position to start of Moov Header in File
		fc.position(moovHeader.filePos)

		//Write MoovHeader (with new larger size)
		fc.write(moovHeader.headerData)

		//Now write from updated Moov buffer up until location of start of ilst atom
		//(Moov buffer contains all of Moov except Mov header)
		moovBuffer!!.rewind()
		moovBuffer.limit(positionOfStartOfIlstAtomInMoovBuffer)
		fc.write(moovBuffer)

		//If the top level free large enough to provide the extra space required then we didnt have to move the mdat
		//data we just write the new ilst data, rest of moov buffer and amended size top level free atom
		if (!isMdatDataMoved) {
			logger.severe("Writing:Option 7.1, Increased Data")

			//Write new ilst data
			fc.write(newIlstData)
			writeRestOfMoovHeaderAfterNewIlistAndAmendedTopLevelFreeAtom(
				fc,
				positionOfStartOfIlstAtomInMoovBuffer,
				moovHeader,
				moovBuffer,
				additionalMetaSizeThatWontFitWithinMetaAtom,
				topLevelFreeSize,
				neroTagsHeader,
				existingSizeOfIlstData
			)
		} else {
			logger.severe("Writing:Option 7.2 Increased Data, not enough free space")

			//Position after MoovBuffer in file
			fc.position(endOfOriginalMoovAtom)

			//Shift the existing data after Moov Atom by the increased size of ilst data
			shiftDataByOffsetToMakeSpace(fc, additionalMetaSizeThatWontFitWithinMetaAtom)

			//Now Write new ilst data, starting at the same location as the oldiLst atom
			fc.position(moovHeader.filePos + Mp4BoxHeader.HEADER_LENGTH + positionOfStartOfIlstAtomInMoovBuffer)
			fc.write(newIlstData)

			//Now Write any data that existed in MoovHeader after the old ilst atom (if any)
			moovBuffer.limit(moovBuffer.capacity())
			moovBuffer.position(positionOfStartOfIlstAtomInMoovBuffer + existingSizeOfIlstData)
			if (moovBuffer.position() < moovBuffer.capacity()) {
				fc.write(moovBuffer)
			}
		}
	}

	@Throws(IOException::class)
	private fun writeRestOfMoovHeaderAfterNewIlistAndAmendedTopLevelFreeAtom(
		fc: SeekableByteChannel,
		positionOfStartOfIlstAtomInMoovBuffer: Int,
		moovHeader: Mp4BoxHeader?,
		moovBuffer: ByteBuffer?,
		additionalMetaSizeThatWontFitWithinMetaAtom: Int,
		topLevelFreeSize: Int,
		neroTagsHeader: Mp4BoxHeader?,
		existingSizeOfIlstData: Int
	) {
		//Write the remainder of any data in the moov buffer thats comes after existing ilst/metadata level free atoms
		//but we replace any neroTags atoms with free atoms as these cause problems
		if (neroTagsHeader != null) {
			moovBuffer!!.limit(moovBuffer.capacity())
			moovBuffer.position(positionOfStartOfIlstAtomInMoovBuffer + existingSizeOfIlstData)
			writeFromEndOfIlstToNeroTagsAndMakeNeroFree(moovHeader, moovBuffer, fc, neroTagsHeader)

			//Shrink the top level free atom to accomodate the extra data
			adjustTopLevelFreeAtom(
				fc,
				topLevelFreeSize,
				additionalMetaSizeThatWontFitWithinMetaAtom
			)
		} else {
			//Write the remaining children under moov that come after ilst atom
			moovBuffer!!.limit(moovBuffer.capacity())
			moovBuffer.position(positionOfStartOfIlstAtomInMoovBuffer + existingSizeOfIlstData)
			if (moovBuffer.position() < moovBuffer.capacity()) {
				fc.write(moovBuffer)
			}

			//Shrink the top level free atom to accommodate the extra data
			adjustTopLevelFreeAtom(
				fc,
				topLevelFreeSize,
				additionalMetaSizeThatWontFitWithinMetaAtom
			)
		}
	}

	/**
	 * If any data between existing `ilst` atom and `tags` atom write it to new file, then convertMetadata
	 * `tags` atom to a `free` atom.
	 *
	 * @param fc
	 * @param neroTagsHeader
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun writeFromEndOfIlstToNeroTagsAndMakeNeroFree(
		moovHeader: Mp4BoxHeader?,
		moovBuffer: ByteBuffer?,
		fc: SeekableByteChannel,
		neroTagsHeader: Mp4BoxHeader
	) {
		//Write from after ilst (already in position) upto start of tags atom
		//And write from there to the start of the (nero) tags atom
		moovBuffer!!.limit((neroTagsHeader.filePos - (moovHeader!!.filePos + Mp4BoxHeader.HEADER_LENGTH)).toInt())
		fc.write(moovBuffer)

		//Now write a free atom to replace the nero atom
		convertandWriteTagsAtomToFreeAtom(fc, neroTagsHeader)
	}

	/**
	 * We adjust `free` top level atom, allowing us to not need to move `mdat` atom.
	 *
	 * @param fc
	 * @param sizeOfExistingTopLevelAtom
	 * @param additionalMetaSizeThatWontFitWithinMetaAtom
	 * @throws IOException
	 * @throws CannotWriteException
	 */
	@Throws(IOException::class)
	private fun adjustTopLevelFreeAtom(
		fc: SeekableByteChannel,
		sizeOfExistingTopLevelAtom: Int,
		additionalMetaSizeThatWontFitWithinMetaAtom: Int
	) {
		//If the shift is less than the space available in this second free atom data size we just
		//shrink the free atom accordingly
		if (sizeOfExistingTopLevelAtom - Mp4BoxHeader.HEADER_LENGTH >= additionalMetaSizeThatWontFitWithinMetaAtom) {
			logger.config("Writing:Option 6;Larger Size can use top free atom")
			val freeBox = Mp4FreeBox(
				sizeOfExistingTopLevelAtom - Mp4BoxHeader.HEADER_LENGTH - additionalMetaSizeThatWontFitWithinMetaAtom
			)
			fc.write(freeBox.header!!.headerData)
			fc.write(freeBox.data)
		} else if (sizeOfExistingTopLevelAtom == additionalMetaSizeThatWontFitWithinMetaAtom) {
			logger.config("Writing:Option 7;Larger Size uses top free atom including header")
		} else {
			//MDAT comes before MOOV, nothing to do because data has already been written
		}
	}

	/**
	 * May need to rewrite the `stco` offsets, if the location of `mdat` (audio) header is going to move.
	 *
	 * @param topLevelFreeSize
	 * @param topLevelFreeAtomComesBeforeMdatAtomAndAfterMetadata
	 * @param additionalSizeRequired
	 * @param stcos
	 * @param moovHeader
	 * @param mdatHeader
	 *
	 * @return `true`, if offsets were adjusted because unable to fit in new
	 * metadata without shifting `mdat` header further down
	 */
	private fun adjustStcosIfNoSuitableTopLevelAtom(
		topLevelFreeSize: Int,
		topLevelFreeAtomComesBeforeMdatAtomAndAfterMetadata: Boolean,
		additionalSizeRequired: Int,
		stcos: List<Mp4StcoBox?>?,
		moovHeader: Mp4BoxHeader?,
		mdatHeader: Mp4BoxHeader
	): Boolean {
		//We don't bother using the top level free atom because not big enough anyway, we need to adjust offsets
		//by the amount mdat is going to be shifted as long as mdat is after moov
		if (mdatHeader.filePos > moovHeader!!.filePos) {
			//Edit stco atoms within moov header, if the free atom comes after mdat OR
			//(there is not enough space in the top level free atom
			//or special case (of not matching exactly the free atom plus header so could remove free atom completely)
			/** [org.jaudiotagger.audio.mp4.Mp4TagWriter] */
			if (!topLevelFreeAtomComesBeforeMdatAtomAndAfterMetadata
				|| (topLevelFreeSize - Mp4BoxHeader.HEADER_LENGTH < additionalSizeRequired && topLevelFreeSize != additionalSizeRequired)
			) {
				for (stoc in stcos!!) {
					stoc!!.adjustOffsets(additionalSizeRequired)
				}
				return true
			}
		}
		return false
	}

	companion object {
		// Logger Object
		var logger = Logger.getLogger("org.jaudiotagger.tag.mp4")
	}
}
