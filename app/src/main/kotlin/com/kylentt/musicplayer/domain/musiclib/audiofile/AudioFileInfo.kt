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

	open var parentFileName: String = ""
		protected set

	open var mimeType: String = ""
		protected set

	open var metadata: AudioFileMetadata = AudioFileMetadata.Builder.empty
		protected set

	open class Builder {
		var absolutePath: String = empty.absolutePath
		var dateAdded: Long = empty.dateAdded
		var dateModified: Long = empty.dateModified
		var fileExtension: String = empty.fileExtension
		var fileName: String = empty.fileName
		var fileSize: Int = empty.fileSize
		var mimeType: String = empty.mimeType
		var metadata = empty.metadata
		var parentFileName: String = empty.parentFileName

		constructor()

		constructor(audioFileInfo: AudioFileInfo) {
			absolutePath = audioFileInfo.absolutePath
			dateAdded = audioFileInfo.dateAdded
			dateModified = audioFileInfo.dateModified
			fileExtension = audioFileInfo.fileExtension
			fileName = audioFileInfo.fileName
			fileSize = audioFileInfo.fileSize
			mimeType = audioFileInfo.mimeType
			metadata = audioFileInfo.metadata
			parentFileName = audioFileInfo.parentFileName
		}

		open fun build(): AudioFileInfo = AudioFileInfo()
			.apply {
				absolutePath = this@Builder.absolutePath
				dateAdded = this@Builder.dateAdded
				dateModified = this@Builder.dateModified
				fileExtension = this@Builder.fileExtension
				fileName = this@Builder.fileName
				fileSize = this@Builder.fileSize
				mimeType = this@Builder.mimeType
				metadata = this@Builder.metadata
				parentFileName = this@Builder.parentFileName
			}
	}

	companion object {
		val empty = AudioFileInfo()
	}
}
