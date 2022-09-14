package com.flammky.musicplayer.common.media.audio.meta_tag.audio.asf.data

import java.math.BigInteger

/**
 * This class represents the ASF extended header object (chunk).<br></br>
 * Like [AsfHeader] it contains multiple other ASF objects (chunks).<br></br>
 *
 * @author Christian Laireiter
 */
class AsfExtendedHeader
/**
 * Creates an instance.<br></br>
 *
 * @param pos    Position within the stream.<br></br>
 * @param length the length of the extended header object.
 */
	(pos: Long, length: BigInteger?) : ChunkContainer(GUID.GUID_HEADER_EXTENSION, pos, length) {
	/**
	 * @return Returns the contentDescription.
	 */
	val contentDescription: ContentDescription
		get() = getFirst(
			GUID.GUID_CONTENTDESCRIPTION,
			ContentDescription::class.java
		) as ContentDescription

	/**
	 * @return Returns the tagHeader.
	 */
	val extendedContentDescription: MetadataContainer
		get() = getFirst(
			GUID.GUID_EXTENDED_CONTENT_DESCRIPTION,
			MetadataContainer::class.java
		) as MetadataContainer

	/**
	 * Returns a language list object if present.
	 *
	 * @return a language list object.
	 */
	val languageList: LanguageList
		get() = getFirst(GUID.GUID_LANGUAGE_LIST, LanguageList::class.java) as LanguageList

	/**
	 * Returns a metadata library object if present.
	 *
	 * @return metadata library objet
	 */
	val metadataLibraryObject: MetadataContainer
		get() = getFirst(
			GUID.GUID_METADATA_LIBRARY,
			MetadataContainer::class.java
		) as MetadataContainer

	/**
	 * Returns a metadata object if present.
	 *
	 * @return metadata object
	 */
	val metadataObject: MetadataContainer
		get() = getFirst(GUID.GUID_METADATA, MetadataContainer::class.java) as MetadataContainer
}
