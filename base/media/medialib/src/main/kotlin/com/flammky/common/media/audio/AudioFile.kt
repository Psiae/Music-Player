package com.flammky.common.media.audio

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.AudioFileIO
import timber.log.Timber
import java.io.File
import java.io.FileDescriptor

@Deprecated("TO BE REMOVED")
class AudioFile private constructor() {
	private var mContext: Context? = null
	private var mFile: File? = null
	private var mFileDescriptor: FileDescriptor? = null
	private var mUri: Uri? = null
	private var _AF: com.flammky.musicplayer.common.media.audio.meta_tag.audio.AudioFile? = null
	private var _ex: Exception? = null

	val imageData: ByteArray?
		get() = _AF?.tag?.firstArtwork?.binaryData

	val file: File?
		get() = mFile

	val ex: Exception?
		get() = _ex

	class Builder {
		private var _context: Context? = null
		private var _file: File? = null
		private var _fileDescriptor: ParcelFileDescriptor? = null
		private var _uri: Uri? = null
		private var _ex: Exception? = null

		private constructor()

		private constructor(context: Context) {
			_context = context
		}

		constructor(context: Context, uri: Uri) : this(context) {
			val type = context.contentResolver.getType(uri)
			Timber.d("creating AudioFile for uri=$uri, type=$type")
			val fd: ParcelFileDescriptor? = try {
				when {
					uri.scheme == ContentResolver.SCHEME_CONTENT || uri.scheme == ContentResolver.SCHEME_FILE -> context.contentResolver.openFileDescriptor(uri, "r")!!
					else -> TODO("Uri not yet supported")
				}
			} catch (ex: Exception) {
				Timber.d("AudioFileBuilder ex: $ex")
				_ex = ex
				null
			}
			_fileDescriptor = fd
			_uri = uri
		}

		fun build(): AudioFile = AudioFile().apply {
			mContext = _context
			mFile = _file
			mFileDescriptor = _fileDescriptor?.fileDescriptor
			mUri = _uri

			if (this@Builder._ex == null) {
				try {
					when {
						mFileDescriptor != null -> _AF = AudioFileIO.readMagic(mFileDescriptor!!)
					}
				} catch (ex: Exception) {
					println("audioFile readMagic fail: ${ex.toString()}")
					this@Builder._ex = ex
				}
			}
			_ex = this@Builder._ex
			_fileDescriptor?.close()
		}
	}


	companion object {
	}
}
