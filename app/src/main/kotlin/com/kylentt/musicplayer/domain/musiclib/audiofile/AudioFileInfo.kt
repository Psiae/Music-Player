package com.kylentt.musicplayer.domain.musiclib.audiofile

open class AudioFileInfo protected constructor() {

	open var absolutePath: String = ""
		protected set

	open var dateAdded: Long = -1
		protected set

	open var dateModified: Long = -1
		protected set

	open var fileExtension: String = ""
		protected set

	open var fileName: String = ""
		protected set

	open var fileSize: Int = -1
		protected set

	open var mimeType: String = ""
		protected set

	open var metadata: AudioFileMetadata = AudioFileMetadata.Builder.empty
		protected set

	open class Builder {
		var absolutePath: String = empty.absolutePath
		var dateAdded: Long = empty.dateAdded
		var fileExtension: String = empty.fileExtension
		var fileName: String = empty.fileName
		var fileSize: Int = empty.fileSize
		var dateModified: Long = empty.dateModified
		var mimeType: String = empty.mimeType
		var metadata = empty.metadata

		constructor()

		constructor(audioFileInfo: AudioFileInfo) {
			absolutePath = audioFileInfo.absolutePath
			dateAdded = audioFileInfo.dateAdded
			fileExtension = audioFileInfo.fileExtension
			fileName = audioFileInfo.fileName
			dateModified = audioFileInfo.dateModified
			mimeType = audioFileInfo.mimeType
			metadata = audioFileInfo.metadata
		}

		open fun build(): AudioFileInfo = AudioFileInfo()
			.apply {
				absolutePath = this@Builder.absolutePath
				fileExtension = this@Builder.fileExtension
				fileName = this@Builder.fileName
				dateModified = this@Builder.dateModified
				mimeType = this@Builder.mimeType
				metadata = this@Builder.metadata
			}
	}

	companion object {
		val empty = AudioFileInfo()
	}
}
