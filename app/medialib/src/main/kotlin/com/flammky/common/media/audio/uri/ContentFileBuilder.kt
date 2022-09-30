package com.flammky.musicplayer.common.media.audio.uri

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.flammky.musicplayer.common.exception.ReleasedException
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class ContentFileBuilder : AndroidFileBuilder {
	private var mBufferSize: Int = 8192
	private var mContext: Context? = null
	private var mUri: Uri? = null

	@Throws(IllegalArgumentException::class)
	constructor(context: Context, contentUri: Uri, bufferSize: Int) {
		require(contentUri.scheme == ContentResolver.SCHEME_CONTENT)
		mContext = context
		mUri = contentUri
		mBufferSize = bufferSize
	}

	@Throws(IllegalArgumentException::class)
	constructor(context: Context, contentUri: Uri) : this(context, contentUri, 8192)

	@Throws(ReleasedException::class)
	override fun build(): File {
		val context = mContext
			?: throw ReleasedException("ContentFileBuilder is already released")
		val uri = mUri
			?: throw ReleasedException("ContentFileBuilder is already released")

		val file = File.createTempFile("contentFileBuilder", null)
		val inputStream = context.contentResolver.openInputStream(uri)

		inputStream?.use { iStream ->
			writeToFile(iStream, file, ByteArray(mBufferSize))
		}

		return file
	}

	override fun buildToDir(dir: File): File {
		val context = mContext
			?: throw ReleasedException("ContentFileBuilder is already released")
		val uri = mUri
			?: throw ReleasedException("ContentFileBuilder is already released")

		val file = File.createTempFile("ContentFileBuilder", null, dir)
		val inputStream = context.contentResolver.openInputStream(uri)

		inputStream?.use { iStream ->
			writeToFile(iStream, file, ByteArray(mBufferSize))
		}

		return file
	}

	override fun release() {
		mContext = null
	}


	private fun writeToFile(iStream: InputStream, target: File, buffer: ByteArray) {
		FileOutputStream(target).use { oStream ->
			while (true) {
				val byteCount = iStream.read(buffer)
				if (byteCount < 0) break
				oStream.write(buffer, 0, byteCount)
			}
			oStream.flush()
		}
	}
}
