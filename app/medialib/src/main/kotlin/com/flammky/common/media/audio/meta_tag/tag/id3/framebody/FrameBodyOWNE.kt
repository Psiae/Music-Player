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

import com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype.*
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3TextEncodingConversion.getTextEncoding
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3TextEncodingConversion.getUnicodeTextEncoding
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.ID3v24Frames
import com.flammky.musicplayer.common.media.audio.meta_tag.tag.id3.valuepair.TextEncoding
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Ownership frame.
 *
 *
 * The ownership frame might be used as a reminder of a made transaction
 * or, if signed, as proof. Note that the "USER" and "TOWN" frames are
 * good to use in conjunction with this one. The frame begins, after the
 * frame ID, size and encoding fields, with a 'price payed' field. The
 * first three characters of this field contains the currency used for
 * the transaction, encoded according to ISO-4217 alphabetic
 * currency code. Concatenated to this is the actual price payed, as a
 * numerical string using "." as the decimal separator. Next is an 8
 * character date string (YYYYMMDD) followed by a string with the name
 * of the seller as the last field in the frame. There may only be one
 * "OWNE" frame in a tag.
 *
 * <table border=0 width="70%">
 * <tr><td>&lt;Header for 'Ownership frame', ID: "OWNE"&gt;</td></tr>
 * <tr><td>Text encoding  </td><td>$xx                     </td></tr>
 * <tr><td>Price payed    </td><td>&lt;text string&gt; $00 </td></tr>
 * <tr><td>Date of purch. </td><td>&lt;text string&gt;     </td></tr>
 * <tr><td>Seller</td><td>&lt;text string according to encoding&gt;</td></tr>
</table> *
 *
 *
 * For more details, please refer to the ID3 specifications:
 *
 *  * [ID3 v2.3.0 Spec](http://www.id3.org/id3v2.3.0.txt)
 *
 *
 * @author : Paul Taylor
 * @author : Eric Farng
 * @version $Id$
 */
class FrameBodyOWNE : AbstractID3v2FrameBody, ID3v24FrameBody, ID3v23FrameBody {

	override val identifier: String?
		get() = ID3v24Frames.FRAME_ID_OWNERSHIP

	/**
	 * Creates a new FrameBodyOWNE datatype.
	 */
	constructor() {
		//        this.setObject("Text Encoding", new Byte((byte) 0));
		//        this.setObject("Price Paid", "");
		//        this.setObject("Date Of Purchase", "");
		//        this.setObject("Seller", "");
	}

	constructor(body: FrameBodyOWNE) : super(body)

	/**
	 * Creates a new FrameBodyOWNE datatype.
	 *
	 * @param textEncoding
	 * @param pricePaid
	 * @param dateOfPurchase
	 * @param seller
	 */
	constructor(textEncoding: Byte, pricePaid: String?, dateOfPurchase: String?, seller: String?) {
		setObjectValue(DataTypes.OBJ_TEXT_ENCODING, textEncoding)
		setObjectValue(DataTypes.OBJ_PRICE_PAID, pricePaid)
		setObjectValue(DataTypes.OBJ_PURCHASE_DATE, dateOfPurchase)
		setObjectValue(DataTypes.OBJ_SELLER_NAME, seller)
	}

	/**
	 * Creates a new FrameBodyOWNE datatype.
	 *
	 * @param byteBuffer
	 * @param frameSize
	 * @throws InvalidTagException if unable to create framebody from buffer
	 */
	constructor(byteBuffer: ByteBuffer, frameSize: Int) : super(byteBuffer, frameSize)

	/**
	 * If the seller name cannot be encoded using current encoder, change the encoder
	 */
	override fun write(tagBuffer: ByteArrayOutputStream) {
		//Ensure valid for type
		textEncoding = getTextEncoding(header, textEncoding)

		//Ensure valid for data
		if (!(getObject(DataTypes.OBJ_SELLER_NAME) as AbstractString?)!!.canBeEncoded()) {
			textEncoding = getUnicodeTextEncoding(header)
		}
		super.write(tagBuffer)
	}

	/**
	 *
	 */
	override fun setupObjectList() {
		objectList.add(
			NumberHashMap(
				DataTypes.OBJ_TEXT_ENCODING,
				this,
				TextEncoding.TEXT_ENCODING_FIELD_SIZE
			)
		)
		objectList.add(StringNullTerminated(DataTypes.OBJ_PRICE_PAID, this))
		objectList.add(StringDate(DataTypes.OBJ_PURCHASE_DATE, this))
		objectList.add(TextEncodedStringSizeTerminated(DataTypes.OBJ_SELLER_NAME, this))
	}
}
