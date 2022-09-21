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
 *  you can get a copy from http://www.opensource.org/licenses/lgpl-license.php or write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.framebody

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.NumberFixedLength
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.NumberVariableLength
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v24Frames
import java.nio.ByteBuffer

/**
 * Audio files with variable bit rates are intrinsically difficult to
 * deal with in the case of seeking within the file. The ASPI frame
 * makes seeking easier by providing a list a seek points within the
 * audio file. The seek points are a fractional offset within the audio
 * data, providing a starting point from which to find an appropriate
 * point to start decoding. The presence of an ASPI frame requires the
 * existence of a TLEN frame, indicating the duration of the file in
 * milliseconds. There may only be one 'audio seek point index' frame in
 * a tag.
 *
 * <Header for></Header> 'Seek Point Index', ID: "ASPI">
 * Indexed data start (S)         $xx xx xx xx
 * Indexed data length (L)        $xx xx xx xx
 * Number of index points (N)     $xx xx
 * Bits per index point (b)       $xx
 *
 * Then for every index point the following data is included;
 *
 * Fraction at index (Fi)          $xx (xx)
 *
 * 'Indexed data start' is a byte offset from the beginning of the file.
 * 'Indexed data length' is the byte length of the audio data being
 * indexed. 'Number of index points' is the number of index points, as
 * the name implies. The recommended number is 100. 'Bits per index
 * point' is 8 or 16, depending on the chosen precision. 8 bits works
 * well for short files (less than 5 minutes of audio), while 16 bits is
 * advantageous for long files. 'Fraction at index' is the numerator of
 * the fraction representing a relative position in the data. The
 * denominator is 2 to the power of b.
 *
 * Here are the algorithms to be used in the calculation. The known data
 * must be the offset of the start of the indexed data (S), the offset
 * of the end of the indexed data (E), the number of index points (N),
 * the offset at index i (Oi). We calculate the fraction at index i
 * (Fi).
 *
 * Oi is the offset of the frame whose start is soonest after the point
 * for which the time offset is (i/N * duration).
 *
 * The frame data should be calculated as follows:
 *
 * Fi = Oi/L * 2^b    (rounded down to the nearest integer)
 *
 * Offset calculation should be calculated as follows from data in the
 * frame:
 *
 * Oi = (Fi/2^b)*L    (rounded up to the nearest integer)
 *
 * @author : Paul Taylor
 * @author : Eric Farng
 * @version $Id$
 */
class FrameBodyASPI : AbstractID3v2FrameBody, ID3v24FrameBody {

	override val identifier: String
		get() = ID3v24Frames.FRAME_ID_AUDIO_SEEK_POINT_INDEX

	/**
	 * Creates a new FrameBodyASPI datatype.
	 */
	constructor()

	/**
	 * Creates a new FrameBodyASPI from another FrameBodyASPI
	 *
	 * @param copyObject
	 */
	constructor(copyObject: FrameBodyASPI) : super(copyObject)

	/**
	 * Creates a new FrameBodyASPI datatype.
	 *
	 * @param byteBuffer
	 * @param frameSize
	 * @throws InvalidTagException
	 */
	constructor(byteBuffer: ByteBuffer, frameSize: Int) : super(byteBuffer, frameSize)

	override fun setupObjectList() {
		objectList.add(NumberFixedLength(INDEXED_DATA_START, this, DATA_START_FIELD_SIZE))
		objectList.add(NumberFixedLength(INDEXED_DATA_LENGTH, this, DATA_LENGTH_FIELD_SIZE))
		objectList.add(
			NumberFixedLength(
				NUMBER_OF_INDEX_POINTS,
				this,
				NO_OF_INDEX_POINTS_FIELD_SIZE
			)
		)
		objectList.add(
			NumberFixedLength(
				BITS_PER_INDEX_POINT,
				this,
				BITS_PER_INDEX_POINTS_FIELD_SIZE
			)
		)
		objectList.add(
			NumberVariableLength(
				FRACTION_AT_INDEX,
				this,
				FRACTION_AT_INDEX_MINIMUM_FIELD_SIZE
			)
		)
	}

	companion object {
		private const val DATA_START_FIELD_SIZE = 4
		private const val DATA_LENGTH_FIELD_SIZE = 4
		private const val NO_OF_INDEX_POINTS_FIELD_SIZE = 2
		private const val BITS_PER_INDEX_POINTS_FIELD_SIZE = 1
		private const val FRACTION_AT_INDEX_MINIMUM_FIELD_SIZE = 1
		private const val INDEXED_DATA_START = "IndexedDataStart"
		private const val INDEXED_DATA_LENGTH = "IndexedDataLength"
		private const val NUMBER_OF_INDEX_POINTS = "NumberOfIndexPoints"
		private const val BITS_PER_INDEX_POINT = "BitsPerIndexPoint"
		private const val FRACTION_AT_INDEX = "FractionAtIndex"
	}
}
