package com.flammky.common.media.audio

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.flammky.android.app.AppDelegate
import com.flammky.musicplayer.common.media.audio.meta_tag.audio.mp3.MP3File
import com.flammky.musicplayer.common.media.audio.uri.AndroidFileBuilder
import com.flammky.musicplayer.common.media.audio.uri.ContentFileBuilder
import timber.log.Timber
import java.io.File
import java.io.FileDescriptor

interface IDK
class IDKImpl : IDK

fun idk() {
	Log.d("", "")
}

class AudioFile private constructor() {
	private var mContext: Context? = null
	private var mFile: File? = null
	private var mFileDescriptor: FileDescriptor? = null
	private var mUri: Uri? = null
	private var mp3File: MP3File? = null

	val imageData: ByteArray?
		get() = mp3File?.let {
			it.iD3v2Tag?.firstArtwork?.binaryData ?: it.iD3v1Tag?.firstArtwork?.binaryData
		}

	val file: File?
		get() = mFile

	class Builder {
		private var _context: Context? = null
		private var _file: File? = null
		private var _fileDescriptor: ParcelFileDescriptor? = null
		private var _uri: Uri? = null

		private constructor()

		private constructor(context: Context) {
			_context = context
		}

		constructor(context: Context, file: File) : this(context) {
			_file = file
		}

		constructor(context: Context, uri: Uri, dir: File?) : this(context) {
			val builder: AndroidFileBuilder = when {
				uri.scheme == ContentResolver.SCHEME_CONTENT -> ContentFileBuilder(context, uri)
				else -> TODO("Uri not yet supported")
			}

			builder.run {
				val nDir = dir ?: AppDelegate.cacheManager.startupCacheDir
				_file = buildToDir(nDir)
				release()
			}
		}

		constructor(context: Context, uri: Uri) : this(context) {
			val fd: ParcelFileDescriptor? = try {
				when {
					uri.scheme == ContentResolver.SCHEME_CONTENT || uri.scheme == ContentResolver.SCHEME_FILE -> context.contentResolver.openFileDescriptor(uri, "r")!!
					else -> TODO("Uri not yet supported")
				}
			} catch (ex: Exception) {
				Timber.d("AudioFileBuilder ex: $ex")
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

			try {
				when {
					mFile != null -> mp3File = MP3File(mFile!!)
					mFileDescriptor != null -> mp3File = MP3File(mFileDescriptor!!, mUri!!)
				}
			} catch (_: Exception) {}

			_fileDescriptor?.close()
		}
	}


	companion object {
		@Suppress("SpellCheckingInspection")
		private val CACHE_DIR_NAME = File("flammky.media.audiofile")

		fun fromContext(context: Context, file: File) = Builder(context, file)
		fun fromContext(context: Context, uri: Uri, dir: File?) = Builder(context, uri, dir)
	}
}
