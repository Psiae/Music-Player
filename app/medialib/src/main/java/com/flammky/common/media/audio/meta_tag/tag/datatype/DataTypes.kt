/**
 * @author : Paul Taylor
 * @author : Eric Farng
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
 *
 * Description:
 * Object Types,all types used by the various frame bodies and associated objects are defined here
 * this works better than putting them with their associated bodies because bodies dont all fall
 * the neccessary hierachy, and values are also required in some Objects (which were previously
 * defined seperately).
 *
 * Warning:Values should not be seperated by space as this will break XML display of tag.
 */
package com.flammky.musicplayer.common.media.audio.meta_tag.tag.datatype

object DataTypes {
	/**
	 * Represents a text encoding, now only IDv2Frames not Lyrics3 tags use
	 * text encoding objects but both use Object Strings and these check
	 * for a text encoding. The method below returns a default if one not set.
	 */
	const val OBJ_TEXT_ENCODING = "TextEncoding"

	//Reference to datatype holding the main textual data
	const val OBJ_TEXT = "Text"

	//Reference to datatype holding non textual textual data
	const val OBJ_DATA = "Data"

	//Reference to datatype holding a description of the textual data
	const val OBJ_DESCRIPTION = "Description"

	//Reference to datatype holding reference to owner of frame.
	const val OBJ_OWNER = "Owner"

	//Reference to datatype holding a number
	const val OBJ_NUMBER = "Number"

	//Reference to timestamps
	const val OBJ_DATETIME = "DateTime"

	/**
	 *
	 */
	const val OBJ_GENRE = "Genre"

	/**
	 *
	 */
	const val OBJ_ID3V2_FRAME_DESCRIPTION = "ID3v2FrameDescription"

	//ETCO Frame
	const val OBJ_TYPE_OF_EVENT = "TypeOfEvent"
	const val OBJ_TIMED_EVENT = "TimedEvent"
	const val OBJ_TIMED_EVENT_LIST = "TimedEventList"

	//SYTC Frame
	const val OBJ_SYNCHRONISED_TEMPO_DATA = "SynchronisedTempoData"
	const val OBJ_SYNCHRONISED_TEMPO = "SynchronisedTempo"
	const val OBJ_SYNCHRONISED_TEMPO_LIST = "SynchronisedTempoList"

	/**
	 *
	 */
	const val OBJ_TIME_STAMP_FORMAT = "TimeStampFormat"

	/**
	 *
	 */
	const val OBJ_TYPE_OF_CHANNEL = "TypeOfChannel"

	/**
	 *
	 */
	const val OBJ_RECIEVED_AS = "RecievedAs"

	//APIC Frame
	const val OBJ_PICTURE_TYPE = "PictureType"
	const val OBJ_PICTURE_DATA = "PictureData"
	const val OBJ_MIME_TYPE = "MIMEType"
	const val OBJ_IMAGE_FORMAT = "ImageType"

	//AENC Frame
	const val OBJ_PREVIEW_START = "PreviewStart"
	const val OBJ_PREVIEW_LENGTH = "PreviewLength"
	const val OBJ_ENCRYPTION_INFO = "EncryptionInfo"

	//COMR Frame
	const val OBJ_PRICE_STRING = "PriceString"
	const val OBJ_VALID_UNTIL = "ValidUntil"
	const val OBJ_CONTACT_URL = "ContactURL"
	const val OBJ_SELLER_NAME = "SellerName"
	const val OBJ_SELLER_LOGO = "SellerLogo"

	//CRM Frame
	const val OBJ_ENCRYPTED_DATABLOCK = "EncryptedDataBlock"

	//ENCR Frame
	const val OBJ_METHOD_SYMBOL = "MethodSymbol"

	//EQU2 Frame
	const val OBJ_FREQUENCY = "Frequency"
	const val OBJ_VOLUME_ADJUSTMENT = "Volume Adjustment"
	const val OBJ_INTERPOLATION_METHOD = "InterpolationMethod"
	const val OBJ_FILENAME = "Filename"

	//GRID Frame
	const val OBJ_GROUP_SYMBOL = "GroupSymbol"
	const val OBJ_GROUP_DATA = "GroupData"

	//LINK Frame
	const val OBJ_URL = "URL"
	const val OBJ_ID = "ID"

	//OWNE Frame
	const val OBJ_PRICE_PAID = "PricePaid"
	const val OBJ_PURCHASE_DATE = "PurchaseDate"

	//POPM Frame
	const val OBJ_EMAIL = "Email"
	const val OBJ_RATING = "Rating"
	const val OBJ_COUNTER = "Counter"

	//POSS Frame
	const val OBJ_POSITION = "Position"

	//RBUF Frame
	const val OBJ_BUFFER_SIZE = "BufferSize"
	const val OBJ_EMBED_FLAG = "EmbedFlag"
	const val OBJ_OFFSET = "Offset"

	//RVRB Frame
	const val OBJ_REVERB_LEFT = "ReverbLeft"
	const val OBJ_REVERB_RIGHT = "ReverbRight"
	const val OBJ_REVERB_BOUNCE_LEFT = "ReverbBounceLeft"
	const val OBJ_REVERB_BOUNCE_RIGHT = "ReverbBounceRight"
	const val OBJ_REVERB_FEEDBACK_LEFT_TO_LEFT = "ReverbFeedbackLeftToLeft"
	const val OBJ_REVERB_FEEDBACK_LEFT_TO_RIGHT = "ReverbFeedbackLeftToRight"
	const val OBJ_REVERB_FEEDBACK_RIGHT_TO_RIGHT = "ReverbFeedbackRightToRight"
	const val OBJ_REVERB_FEEDBACK_RIGHT_TO_LEFT = "ReverbFeedbackRightToLeft"
	const val OBJ_PREMIX_LEFT_TO_RIGHT = "PremixLeftToRight"
	const val OBJ_PREMIX_RIGHT_TO_LEFT = "PremixRightToLeft"

	//SIGN Frame
	const val OBJ_SIGNATURE = "Signature"

	//SYLT Frame
	const val OBJ_CONTENT_TYPE = "contentType"

	//ULST Frame
	const val OBJ_LANGUAGE = "Language"
	const val OBJ_LYRICS = "Lyrics"
	const val OBJ_URLLINK = "URLLink"

	//CHAP Frame
	const val OBJ_ELEMENT_ID = "ElementID"
	const val OBJ_START_TIME = "StartTime"
	const val OBJ_END_TIME = "EndTime"
	const val OBJ_START_OFFSET = "StartOffset"
	const val OBJ_END_OFFSET = "EndOffset" //CTOC Frame
}
