package com.flammky.android.medialib.providers.metadata

import android.net.Uri
import javax.annotation.concurrent.Immutable
import kotlin.time.Duration

@Immutable
open class FileMetadata protected constructor(
	@JvmField
	val absolutePath: String?,
	@JvmField
	val fileName: String?,
	val dateAdded: Duration?,
	val lastModified: Duration?,
	@JvmField
	val size: Long?,
) {

	class Builder {
		var absolutePath: String? = null
			private set
		var fileName: String? = null
			private set
		var dateAdded: Duration? = null
			private set
		var lastModified: Duration? = null
			private set
		var size: Long? = null
			private set

		fun setAbsolutePath(absolutePath: String?) = apply { this.absolutePath = absolutePath }
		fun setFileName(fileName: String?) = apply { this.fileName = fileName }
		fun setDateAdded(dateAdded: Duration?) = apply { this.dateAdded = dateAdded }
		fun setLastModified(lastModified: Duration?) = apply { this.lastModified = lastModified }
		fun setSize(size: Long?) = apply { this.size = size }
		fun build(): FileMetadata = FileMetadata(
			absolutePath = absolutePath,
			fileName = fileName,
			dateAdded = dateAdded,
			lastModified = lastModified,
			size = size
		)
	}

	companion object {
		fun build(apply: Builder.() -> Unit): FileMetadata = Builder().apply(apply).build()
	}
}

@Immutable
open class VirtualFileMetadata(
	@JvmField
	val uri: Uri,
	@JvmField
	val scheme: String?,
	absolutePath: String?,
	fileName: String?,
	dateAdded: Duration?,
	lastModified: Duration?,
	size: Long?
) : FileMetadata(absolutePath, fileName, dateAdded, lastModified, size) {

	class Builder {
		var uri: Uri = Uri.EMPTY
			private set
		var scheme: String? = null
			private set
		var absolutePath: String? = null
			private set
		var fileName: String? = null
			private set
		var dateAdded: Duration? = null
			private set
		var lastModified: Duration? = null
			private set
		var size: Long? = null
			private set

		fun setUri(uri: Uri?) = apply { this.uri = uri ?: Uri.EMPTY }
		fun setScheme(scheme: String?) = apply { this.scheme = scheme }
		fun setAbsolutePath(absolutePath: String?) = apply { this.absolutePath = absolutePath }
		fun setFileName(fileName: String?) = apply { this.fileName = fileName }
		fun setDateAdded(dateAdded: Duration?) = apply { this.dateAdded = dateAdded }
		fun setLastModified(lastModified: Duration?) = apply { this.lastModified = lastModified }
		fun setSize(size: Long?) = apply { this.size = size }
		fun build(): VirtualFileMetadata = VirtualFileMetadata(
			uri = uri,
			scheme = scheme,
			absolutePath = absolutePath,
			fileName = fileName,
			dateAdded = dateAdded,
			lastModified = lastModified,
			size = size
		)
	}

	companion object {
		inline val FileMetadata.isVirtual
			get() = this is VirtualFileMetadata

		fun build(apply: Builder.() -> Unit): VirtualFileMetadata = Builder().apply(apply).build()
	}
}
