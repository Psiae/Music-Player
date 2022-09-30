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
package com.flammky.musicplayer.common.media.audio.meta_tag.audio.flac

import com.flammky.android.core.sdk.VersionHelper
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotReadException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.CannotWriteException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.exceptions.NoWritePermissionsException
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.flac.metadatablock.*
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.flac.metadatablock.MetadataBlockHeader.Companion.readHeader
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.Tag
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.flac.FlacTag
import com.flammky.musicplayer.common.media.audio.meta_tag.utils.ShiftData.shiftDataByOffsetToMakeSpace
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.AccessDeniedException
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Write Flac Tag
 *
 * For best compatibility with other applications we write blocks (where they exist) in the following order:
 *
 * STREAM
 * VORBIS_COMMENT
 * PICTURE
 * SEEK
 * CUESHEET
 * APPLICATION
 * PADDING
 */
class FlacTagWriter {
	private val tc = FlacTagCreator()

	/**
	 *
	 * Remove VORBIS_COMMENT or PICTURE blocks from file
	 *
	 * @param tag
	 * @param file
	 * @throws IOException
	 * @throws CannotWriteException
	 */
	@Throws(CannotWriteException::class)
	fun delete(tag: Tag?, file: Path) {
		//This will save the file without any Comment or PictureData blocks
		val emptyTag = FlacTag(null, ArrayList())
		write(emptyTag, file)
	}

	/**
	 * Makes writing tag a bit simpler
	 */
	private class MetadataBlockInfo {
		private val blocks: MutableList<MetadataBlock> = ArrayList()
		var streamInfoBlock: MetadataBlock? = null
		val metadataBlockPadding: MutableList<MetadataBlock> = ArrayList(1)
		val metadataBlockApplication: MutableList<MetadataBlock> = ArrayList(1)
		val metadataBlockSeekTable: MutableList<MetadataBlock> = ArrayList(1)
		val metadataBlockCueSheet: MutableList<MetadataBlock> = ArrayList(1)
		val listOfNonMetadataBlocks: List<MetadataBlock>
			get() {
				for (next in metadataBlockSeekTable) {
					blocks.add(next)
				}
				for (next in metadataBlockCueSheet) {
					blocks.add(next)
				}
				for (next in metadataBlockApplication) {
					blocks.add(next)
				}
				return blocks
			}

		/**
		 * Count of non-metadata blocks
		 *
		 * Doesnt include STREAM or PADDING
		 *
		 * @param blockInfo
		 * @return
		 */
		fun getOtherBlockCount(blockInfo: MetadataBlockInfo): Int {
			var count = blockInfo.metadataBlockApplication.size
			count += blockInfo.metadataBlockSeekTable.size
			count += blockInfo.metadataBlockCueSheet.size
			return count
		}

		/**
		 * @return space currently available for writing all Flac metadatablocks except for STREAM which is fixed size
		 */
		fun computeAvailableRoom(): Int {
			var length = 0
			for (aMetadataBlockApplication in metadataBlockApplication) {
				length += aMetadataBlockApplication.length
			}
			for (aMetadataBlockSeekTable in metadataBlockSeekTable) {
				length += aMetadataBlockSeekTable.length
			}
			for (aMetadataBlockCueSheet in metadataBlockCueSheet) {
				length += aMetadataBlockCueSheet.length
			}

			//Note when reading metadata has been put into padding as well for purposes of write
			for (aMetadataBlockPadding in metadataBlockPadding) {
				length += aMetadataBlockPadding.length
			}
			return length
		}

		/**
		 * @return space required to write the metadata blocks that are part of Flac but are not part of tagdata
		 * in the normal sense.
		 */
		fun computeNeededRoom(): Int {
			var length = 0
			for (aMetadataBlockApplication in metadataBlockApplication) {
				length += aMetadataBlockApplication.length
			}
			for (aMetadataBlockSeekTable in metadataBlockSeekTable) {
				length += aMetadataBlockSeekTable.length
			}
			for (aMetadataBlockCueSheet in metadataBlockCueSheet) {
				length += aMetadataBlockCueSheet.length
			}
			return length
		}
	}

	/**
	 * @param tag
	 * @param file
	 * @throws CannotWriteException
	 * @throws IOException
	 */
	@Throws(CannotWriteException::class)
	fun write(tag: Tag, file: Path) {
		if (!VersionHelper.hasOreo()) TODO("Require API <= 26")


		logger.config(
			"$file Writing tag"
		)
		try {
			FileChannel.open(file, StandardOpenOption.WRITE, StandardOpenOption.READ).use { fc ->
				val blockInfo = MetadataBlockInfo()

				//Read existing data
				val flacStream = FlacStreamReader(fc, "$file ")
				try {
					flacStream.findStream()
				} catch (cre: CannotReadException) {
					throw CannotWriteException(cre.message)
				}
				var isLastBlock = false
				while (!isLastBlock) {
					try {
						val mbh = readHeader(fc)
						if (mbh.blockType != null) {
							when (mbh.blockType) {
								BlockType.STREAMINFO -> {
									blockInfo.streamInfoBlock =
										MetadataBlock(mbh, MetadataBlockDataStreamInfo(mbh, fc))
								}
								BlockType.VORBIS_COMMENT, BlockType.PADDING, BlockType.PICTURE -> {

									//All these will be replaced by the new metadata so we just treat as padding in order
									//to determine how much space is already allocated in the file
									fc.position(fc.position() + mbh.dataLength)
									val mbd: MetadataBlockData =
										MetadataBlockDataPadding(mbh.dataLength)
									blockInfo.metadataBlockPadding.add(MetadataBlock(mbh, mbd))
								}
								BlockType.APPLICATION -> {
									val mbd: MetadataBlockData =
										MetadataBlockDataApplication(mbh, fc)
									blockInfo.metadataBlockApplication.add(MetadataBlock(mbh, mbd))
								}
								BlockType.SEEKTABLE -> {
									val mbd: MetadataBlockData = MetadataBlockDataSeekTable(mbh, fc)
									blockInfo.metadataBlockSeekTable.add(MetadataBlock(mbh, mbd))
								}
								BlockType.CUESHEET -> {
									val mbd: MetadataBlockData = MetadataBlockDataCueSheet(mbh, fc)
									blockInfo.metadataBlockCueSheet.add(MetadataBlock(mbh, mbd))
								}
								else -> {

									//TODO What are the consequences of doing this ?
									fc.position(fc.position() + mbh.dataLength)
								}
							}
						}
						isLastBlock = mbh.isLastBlock
					} catch (cre: CannotReadException) {
						throw CannotWriteException(cre.message)
					}
				}

				//Number of bytes in the existing file available before audio data
				val availableRoom = blockInfo.computeAvailableRoom()

				//Minimum Size of the New tag data without padding
				val newTagSize = tc.convertMetadata(tag).limit()

				//Other blocks required size
				val otherBlocksRequiredSize = blockInfo.computeNeededRoom()

				//Number of bytes required for new tagdata and other metadata blocks
				val neededRoom = newTagSize + otherBlocksRequiredSize

				//Go to start of Flac within file
				fc.position(flacStream.startOfFlacInFile.toLong())

				//There is enough room to fit the tag without moving the audio just need to
				//adjust padding accordingly need to allow space for padding header if padding required
				logger.config(
					"$file:Writing tag available bytes:$availableRoom:needed bytes:$neededRoom"
				)
				if (availableRoom == neededRoom || availableRoom > neededRoom + MetadataBlockHeader.HEADER_LENGTH) {
					logger.config(
						"$file:Room to Rewrite"
					)
					writeAllNonAudioData(tag, fc, blockInfo, flacStream, availableRoom - neededRoom)
				} else {
					logger.config(file.toString() + ":Audio must be shifted " + "NewTagSize:" + newTagSize + ":AvailableRoom:" + availableRoom + ":MinimumAdditionalRoomRequired:" + (neededRoom - availableRoom))
					//As we are having to move both anyway may as well put in the default padding
					insertUsingChunks(
						file,
						tag,
						fc,
						blockInfo,
						flacStream,
						neededRoom + FlacTagCreator.Companion.DEFAULT_PADDING,
						availableRoom
					)
				}
			}
		} catch (ade: AccessDeniedException) {
			logger.log(Level.SEVERE, ade.message, ade)
			throw NoWritePermissionsException(file.toString() + ":" + ade.message)
		} catch (ioe: IOException) {
			logger.log(Level.SEVERE, ioe.message, ioe)
			throw CannotWriteException(file.toString() + ":" + ioe.message)
		}
	}

	/**Add Padding Block
	 *
	 * @param paddingSize
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	@Throws(UnsupportedEncodingException::class)
	fun addPaddingBlock(paddingSize: Int): ByteBuffer {
		//Padding
		logger.config(
			"padding:$paddingSize"
		)
		val buf = ByteBuffer.allocate(paddingSize)
		if (paddingSize > 0) {
			val paddingDataSize = paddingSize - MetadataBlockHeader.HEADER_LENGTH
			val paddingHeader = MetadataBlockHeader(true, BlockType.PADDING, paddingDataSize)
			val padding = MetadataBlockDataPadding(paddingDataSize)
			buf.put(paddingHeader.bytes)
			buf.put(padding.bytes)
			buf.rewind()
		}
		return buf
	}

	/**
	 * Write all blocks except audio
	 *
	 * @param tag
	 * @param fc
	 * @param blockInfo
	 * @param flacStream
	 * @param padding
	 *
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun writeAllNonAudioData(
		tag: Tag,
		fc: FileChannel,
		blockInfo: MetadataBlockInfo,
		flacStream: FlacStreamReader,
		padding: Int
	) {
		//Jump over Id3 (if exists) and flac header
		fc.position((flacStream.startOfFlacInFile + FlacStreamReader.Companion.FLAC_STREAM_IDENTIFIER_LENGTH).toLong())

		//Write Stream Block
		writeStreamBlock(fc, blockInfo)

		//Write tag (vorbiscomment, picture)
		fc.write(
			tc.convertMetadata(
				tag,
				padding > 0 || blockInfo.getOtherBlockCount(blockInfo) > 0
			)
		)

		//Write other non metadata blocks
		val blocks: List<MetadataBlock> = blockInfo.listOfNonMetadataBlocks
		if (blocks.size > 1) {
			for (i in 0 until blocks.size - 1) {
				fc.write(ByteBuffer.wrap(blocks[i].header.bytesWithoutIsLastBlockFlag))
				fc.write(blocks[i].data.bytes)
			}
		}

		//Write last non-metadata block
		if (blocks.size > 0) {
			if (padding > 0) {
				fc.write(ByteBuffer.wrap(blocks[blocks.size - 1].header.bytesWithoutIsLastBlockFlag))
			} else {
				fc.write(ByteBuffer.wrap(blocks[blocks.size - 1].header.bytesWithLastBlockFlag))
			}
			fc.write(blocks[blocks.size - 1].data.bytes)
		}

		//Write padding
		if (padding > 0) {
			fc.write(addPaddingBlock(padding))
		}
	}

	/**
	 * Insert metadata into space that is not large enough
	 *
	 * We do this by reading/writing chunks of data allowing it to work on low memory systems
	 *
	 * Chunk size defined by TagOptionSingleton.getInstance().getWriteChunkSize()
	 *
	 * @param tag
	 * @param fc
	 * @param blockInfo
	 * @param flacStream
	 * @param neededRoom
	 * @param availableRoom
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 */
	@Throws(IOException::class, UnsupportedEncodingException::class)
	private fun insertUsingChunks(
		file: Path,
		tag: Tag,
		fc: FileChannel,
		blockInfo: MetadataBlockInfo,
		flacStream: FlacStreamReader,
		neededRoom: Int,
		availableRoom: Int
	) {
		//Find end of metadata blocks (start of Audio), i.e start of Flac + 4 bytes for 'fLaC', 4 bytes for streaminfo header and
		//34 bytes for streaminfo and then size of all the other existing blocks
		val audioStart: Long = (flacStream.startOfFlacInFile
			+ FlacStreamReader.Companion.FLAC_STREAM_IDENTIFIER_LENGTH
			+ MetadataBlockHeader.HEADER_LENGTH
			+ MetadataBlockDataStreamInfo.STREAM_INFO_DATA_LENGTH
			+ availableRoom).toLong()

		//Extra Space Required for larger metadata block
		val extraSpaceRequired = neededRoom - availableRoom
		logger.config(
			"$file Audio needs shifting:$extraSpaceRequired"
		)
		fc.position(audioStart)
		shiftDataByOffsetToMakeSpace(fc, extraSpaceRequired)

		//Jump over Id3 (if exists) and Flac Header
		fc.position((flacStream.startOfFlacInFile + FlacStreamReader.Companion.FLAC_STREAM_IDENTIFIER_LENGTH).toLong())
		writeAllNonAudioData(
			tag,
			fc,
			blockInfo,
			flacStream,
			FlacTagCreator.Companion.DEFAULT_PADDING
		)
	}

	@Throws(IOException::class)
	private fun writeStreamBlock(fc: FileChannel, blockInfo: MetadataBlockInfo) {
		//Write StreamInfo, we always write this first even if wasn't first in original spec
		fc.write(ByteBuffer.wrap(blockInfo.streamInfoBlock!!.header.bytesWithoutIsLastBlockFlag))
		fc.write(blockInfo.streamInfoBlock!!.data.bytes)
	}

	companion object {
		// Logger Object
		var logger = Logger.getLogger("org.jaudiotagger.audio.flac")
	}
}
