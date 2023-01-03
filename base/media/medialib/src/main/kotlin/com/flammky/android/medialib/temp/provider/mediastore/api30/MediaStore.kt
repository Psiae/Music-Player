package com.flammky.android.medialib.temp.provider.mediastore.api30

import android.content.ContentResolver
import android.media.ExifInterface
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Downloads
import androidx.annotation.RequiresApi
import com.flammky.android.core.sdk.VersionHelper
import com.flammky.android.medialib.temp.provider.mediastore.base.BaseColumns
import com.flammky.common.kotlin.time.annotation.DurationValue
import com.flammky.android.medialib.temp.annotation.StorageDataUnit
import com.flammky.android.medialib.temp.annotation.StorageDataValue
import java.io.File
import java.util.concurrent.TimeUnit


/**
 * [GoogleSource](https://android.googlesource.com/platform/packages/providers/MediaProvider/+/refs/heads/android11-dev/apex/framework/java/android/provider/MediaStore.java)
 */

@RequiresApi(Build.VERSION_CODES.R)
internal object MediaStore30 {

	init {
		check(VersionHelper.hasR()) {
			val n = if (VersionHelper.isQ()) "MediaStore29" else "MediaStore28"
			"Invalid Usage of API 30 on API ${Build.VERSION.SDK_INT}, use $n instead"
		}
	}

	abstract class MediaColumns : BaseColumns() {

		/**
		 * Absolute filesystem path to the media item on disk.
		 *
		 *
		 * On Android 11, you can use this value when you access an existing
		 * file using direct file paths. That's because this value has a valid
		 * file path. However, don't assume that the file is always available.
		 * Be prepared to handle any file-based I/O errors that could occur.
		 *
		 *
		 * Don't use this value when you create or update a media file, even
		 * if you're on Android 11 and are using direct file paths. Instead,
		 * use the values of the [.DISPLAY_NAME] and
		 * [.RELATIVE_PATH] columns.
		 *
		 *
		 * Note that apps may not have filesystem permissions to directly access
		 * this path. Instead of trying to open this path directly, apps should
		 * use [ContentResolver.openFileDescriptor] to gain
		 * access.
		 *
		 * + "_data"
		 * + type: String
		 */
		@Deprecated(
			message =
			"""
			Apps may not have filesystem permissions to directly access this path.
			Instead of trying to open this path directly,
			apps should use {@link ContentResolver#openFileDescriptor(Uri, String)} to gain access.
			""",
			level = DeprecationLevel.WARNING
		)
		val DATA = MediaStore.MediaColumns.DATA

		/**
		 * Indexed value of [java.io.File.length] extracted from this media
		 * item.
		 *
		 * + "_size"
		 * + type: Long
		 * + ReadOnly
		 */
		@StorageDataValue(StorageDataUnit.BYTE)
		val SIZE = MediaStore.MediaColumns.SIZE

		/**
		 * The display name of the media item.
		 *
		 *
		 * For example, an item stored at
		 * `/storage/0000-0000/DCIM/Vacation/IMG1024.JPG` would have a
		 * display name of `IMG1024.JPG`.
		 *
		 * + "_display_name"
		 * + type: STRING
		 */
		val DISPLAY_NAME = MediaStore.MediaColumns.DISPLAY_NAME

		/**
		 * The time the media item was first added.
		 *
		 * + "date_added"
		 * + type: Long
		 * + ReadOnly
		 */
		@DurationValue(TimeUnit.SECONDS)
		val DATE_ADDED = MediaStore.MediaColumns.DATE_ADDED

		/**
		 * Indexed value of [File.lastModified] extracted from this
		 * media item.
		 *
		 * + "date_modified"
		 * + type: Long
		 * + ReadOnly
		 */
		@DurationValue(TimeUnit.SECONDS)
		val DATE_MODIFIED = MediaStore.MediaColumns.DATE_MODIFIED

		/**
		 * Indexed value of [MediaMetadataRetriever.METADATA_KEY_DATE] or
		 * [ExifInterface.TAG_DATETIME_ORIGINAL] extracted from this media
		 * item.
		 *
		 *
		 * Note that images must define both
		 * [ExifInterface.TAG_DATETIME_ORIGINAL] and
		 * `ExifInterface#TAG_OFFSET_TIME_ORIGINAL` to reliably determine
		 * this value in relation to the epoch.
		 *
		 * // METADATA_KEY_DATE is DATE_TAKEN
		 *
		 * + "datetaken"
		 * + type: Long
		 * + ReadOnly
		 */
		@DurationValue(TimeUnit.MILLISECONDS)
		val DATE_TAKEN = MediaStore.MediaColumns.DATE_TAKEN

		/**
		 * The MIME type of the media item.
		 *
		 *
		 * This is typically defined based on the file extension of the media
		 * item. However, it may be the value of the `format` attribute
		 * defined by the *Dublin Core Media Initiative* standard,
		 * extracted from any XMP metadata contained within this media item.
		 *
		 *
		 * Note: the `format` attribute may be ignored if the top-level
		 * MIME type disagrees with the file extension. For example, it's
		 * reasonable for an `image/jpeg` file to declare a `format`
		 * of `image/vnd.google.panorama360+jpg`, but declaring a
		 * `format` of `audio/ogg` would be ignored.
		 *
		 *
		 * This is a read-only column that is automatically computed.
		 *
		 * // METADATA_KEY_MIMETYPE is MIME_TYPE
		 *
		 * + "mime_type"
		 * + type: String
		 */
		val MIME_TYPE = MediaStore.MediaColumns.MIME_TYPE

		/**
		 * Flag indicating if a media item is DRM protected.
		 *
		 * + "is_drm"
		 * + type: Int
		 */
		val IS_DRM = MediaStore.MediaColumns.IS_DRM

		/**
		 * Flag indicating if a media item is pending, and still being inserted
		 * by its owner. While this flag is set, only the owner of the item can
		 * open the underlying file; requests from other apps will be rejected.
		 *
		 *
		 * Pending items are retained either until they are published by setting
		 * the field to `0`, or until they expire as defined by
		 * [.DATE_EXPIRES].
		 *
		 * + "is_pending"
		 * + type: Int
		 *
		 * @see MediaStore.QUERY_ARG_MATCH_PENDING
		 */
		val IS_PENDING = MediaStore.MediaColumns.IS_PENDING

		/**
		 * Flag indicating if a media item is trashed.
		 *
		 *
		 * Trashed items are retained until they expire as defined by
		 * [.DATE_EXPIRES].
		 *
		 * + "is_trashed"
		 * + type: Int
		 *
		 * @see MediaColumns.IS_TRASHED
		 *
		 * @see MediaStore.QUERY_ARG_MATCH_TRASHED
		 *
		 * @see MediaStore.createTrashRequest
		 */
		val IS_TRASHED = MediaStore.MediaColumns.IS_TRASHED

		/**
		 * The time the media item should be considered expired. Typically only
		 * meaningful in the context of [.IS_PENDING] or
		 * [.IS_TRASHED].
		 *
		 *
		 * The value stored in this column is automatically calculated when
		 * [.IS_PENDING] or [.IS_TRASHED] is changed. The default
		 * pending expiration is typically 7 days, and the default trashed
		 * expiration is typically 30 days.
		 *
		 *
		 * Expired media items are automatically deleted once their expiration
		 * time has passed, typically during during the next device idle period.
		 *
		 * + "date_expires"
		 * + type: Long
		 * + ReadOnly
		 */
		@DurationValue(TimeUnit.SECONDS)
		val DATE_EXPIRES = MediaStore.MediaColumns.DATE_EXPIRES

		/**
		 * Indexed value of
		 * [MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH],
		 * [MediaMetadataRetriever.METADATA_KEY_IMAGE_WIDTH] or
		 * [ExifInterface.TAG_IMAGE_WIDTH] extracted from this media item.
		 *
		 * + "width"
		 * + type: Int
		 * + ReadOnly
		 */
		val WIDTH = MediaStore.MediaColumns.WIDTH

		/**
		 * Indexed value of
		 * [MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT],
		 * [MediaMetadataRetriever.METADATA_KEY_IMAGE_HEIGHT] or
		 * [ExifInterface.TAG_IMAGE_LENGTH] extracted from this media
		 * item.
		 *
		 * + "height"
		 * + type: Int
		 * + ReadOnly
		 */
		val HEIGHT = MediaStore.MediaColumns.HEIGHT

		/**
		 * Calculated value that combines [.WIDTH] and [.HEIGHT]
		 * into a user-presentable string.
		 *
		 * +  "resolution"
		 * + type: String
		 * + ReadOnly
		 */
		val RESOLUTION = MediaStore.MediaColumns.RESOLUTION

		/**
		 * Package name that contributed this media. The value may be
		 * `NULL` if ownership cannot be reliably determined.
		 *
		 * + "owner_package_name"
		 * + type: String
		 * + ReadOnly
		 */
		val OWNER_PACKAGE_NAME = MediaStore.MediaColumns.OWNER_PACKAGE_NAME

		/**
		 * Volume name of the specific storage device where this media item is
		 * persisted. The value is typically one of the volume names returned
		 * from [MediaStore.getExternalVolumeNames].
		 *
		 *
		 * This is a read-only column that is automatically computed.
		 *
		 * + "volume_name"
		 * + type: String
		 * + ReadOnly
		 */
		val VOLUME_NAME = MediaStore.MediaColumns.VOLUME_NAME

		/**
		 * Relative path of this media item within the storage device where it
		 * is persisted. For example, an item stored at
		 * `/storage/0000-0000/DCIM/Vacation/IMG1024.JPG` would have a
		 * path of `DCIM/Vacation/`.
		 *
		 *
		 * This value should only be used for organizational purposes, and you
		 * should not attempt to construct or access a raw filesystem path using
		 * this value. If you need to open a media item, use an API like
		 * [ContentResolver.openFileDescriptor].
		 *
		 *
		 * When this value is set to `NULL` during an
		 * [ContentResolver.insert] operation, the newly created item will
		 * be placed in a relevant default location based on the type of media
		 * being inserted. For example, a `image/jpeg` item will be placed
		 * under [Environment.DIRECTORY_PICTURES].
		 *
		 *
		 * You can modify this column during an [ContentResolver.update]
		 * call, which will move the underlying file on disk.
		 *
		 *
		 * In both cases above, content must be placed under a top-level
		 * directory that is relevant to the media type. For example, attempting
		 * to place a `audio/mpeg` file under
		 * [Environment.DIRECTORY_PICTURES] will be rejected.
		 *
		 * + "relative_path"
		 * + type: String
		 */
		val RELATIVE_PATH = MediaStore.MediaColumns.RELATIVE_PATH

		/**
		 * The primary bucket ID of this media item. This can be useful to
		 * present the user a first-level clustering of related media items.
		 * This is a read-only column that is automatically computed.
		 *
		 * + "bucket_id"
		 * + type: Int
		 * + ReadOnly
		 */
		val BUCKET_ID = MediaStore.MediaColumns.BUCKET_ID

		/**
		 * The primary bucket display name of this media item. This can be
		 * useful to present the user a first-level clustering of related
		 * media items. This is a read-only column that is automatically
		 * computed.
		 *
		 * + "bucket_display_name"
		 * + type: String
		 * + ReadOnly
		 */
		val BUCKET_DISPLAY_NAME = MediaStore.MediaColumns.BUCKET_DISPLAY_NAME

		/**
		 * The group ID of this media item. This can be useful to present
		 * the user a grouping of related media items, such a burst of
		 * images, or a `JPG` and `DNG` version of the same
		 * image.
		 *
		 *
		 * This is a read-only column that is automatically computed based
		 * on the first portion of the filename. For example,
		 * `IMG1024.BURST001.JPG` and `IMG1024.BURST002.JPG`
		 * will have the same [.GROUP_ID] because the first portion of
		 * their filenames is identical.
		 *
		 * + "group_id"
		 * + type: Int
		 * + ReadOnly
		 * + Removed
		 *
		 * @removed
		 */
		@Deprecated("")
		protected val GROUP_ID = "group_id"

		/**
		 * The "document ID" GUID as defined by the *XMP Media
		 * Management* standard, extracted from any XMP metadata contained
		 * within this media item. The value is `null` when no metadata
		 * was found.
		 *
		 *
		 * Each "document ID" is created once for each new resource. Different
		 * renditions of that resource are expected to have different IDs.
		 *
		 * + "document_id"
		 * + type: String
		 * + ReadOnly
		 */
		val DOCUMENT_ID = MediaStore.MediaColumns.DOCUMENT_ID

		/**
		 * The "instance ID" GUID as defined by the *XMP Media
		 * Management* standard, extracted from any XMP metadata contained
		 * within this media item. The value is `null` when no metadata
		 * was found.
		 *
		 *
		 * This "instance ID" changes with each save operation of a specific
		 * "document ID".
		 *
		 * + "instance_id"
		 * + type: String
		 * + ReadOnly
		 */
		val INSTANCE_ID = MediaStore.MediaColumns.INSTANCE_ID

		/**
		 * The "original document ID" GUID as defined by the *XMP Media
		 * Management* standard, extracted from any XMP metadata contained
		 * within this media item.
		 *
		 *
		 * This "original document ID" links a resource to its original source.
		 * For example, when you save a PSD document as a JPEG, then convert the
		 * JPEG to GIF format, the "original document ID" of both the JPEG and
		 * GIF files is the "document ID" of the original PSD file.
		 *
		 * + "original_document_id"
		 * + type: String
		 * + ReadOnly
		 */
		val ORIGINAL_DOCUMENT_ID = MediaStore.MediaColumns.ORIGINAL_DOCUMENT_ID

		/**
		 * Indexed value of
		 * [MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION],
		 * [MediaMetadataRetriever.METADATA_KEY_IMAGE_ROTATION], or
		 * [ExifInterface.TAG_ORIENTATION] extracted from this media item.
		 *
		 *
		 * For consistency the indexed value is expressed in degrees, such as 0,
		 * 90, 180, or 270.
		 *
		 * + "orientation"
		 * + type: Int
		 * + ReadOnly
		 */
		val ORIENTATION = MediaStore.MediaColumns.ORIENTATION

		/**
		 * Flag indicating if the media item has been marked as being a
		 * "favorite" by the user.
		 *
		 * + "is_favorite"
		 * + type: Int
		 *
		 * @see MediaColumns.IS_FAVORITE
		 *
		 * @see MediaStore.QUERY_ARG_MATCH_FAVORITE
		 *
		 * @see MediaStore.createFavoriteRequest
		 */
		val IS_FAVORITE = MediaStore.MediaColumns.IS_FAVORITE

		/**
		 * Flag indicating if the media item has been marked as being part of
		 * the [Downloads] collection.
		 *
		 * + "is_download"
		 * + type: Int
		 * + ReadOnly
		 */
		val IS_DOWNLOAD = MediaStore.MediaColumns.IS_DOWNLOAD

		/**
		 * Generation number at which metadata for this media item was first
		 * inserted. This is useful for apps that are attempting to quickly
		 * identify exactly which media items have been added since a previous
		 * point in time. Generation numbers are monotonically increasing over
		 * time, and can be safely arithmetically compared.
		 *
		 *
		 * Detecting media additions using generation numbers is more robust
		 * than using [.DATE_ADDED], since those values may change in
		 * unexpected ways when apps use [File.setLastModified] or
		 * when the system clock is set incorrectly.
		 *
		 *
		 * Note that before comparing these detailed generation values, you
		 * should first confirm that the overall version hasn't changed by
		 * checking [MediaStore.getVersion], since that
		 * indicates when a more radical change has occurred. If the overall
		 * version changes, you should assume that generation numbers have been
		 * reset and perform a full synchronization pass.
		 *
		 * + "generation_added"
		 * + type: Int
		 * + ReadOnly
		 *
		 * @see MediaStore.getGeneration
		 */
		val GENERATION_ADDED = MediaStore.MediaColumns.GENERATION_ADDED

		/**
		 * Generation number at which metadata for this media item was last
		 * changed. This is useful for apps that are attempting to quickly
		 * identify exactly which media items have changed since a previous
		 * point in time. Generation numbers are monotonically increasing over
		 * time, and can be safely arithmetically compared.
		 *
		 *
		 * Detecting media changes using generation numbers is more robust than
		 * using [.DATE_MODIFIED], since those values may change in
		 * unexpected ways when apps use [File.setLastModified] or
		 * when the system clock is set incorrectly.
		 *
		 *
		 * Note that before comparing these detailed generation values, you
		 * should first confirm that the overall version hasn't changed by
		 * checking [MediaStore.getVersion], since that
		 * indicates when a more radical change has occurred. If the overall
		 * version changes, you should assume that generation numbers have been
		 * reset and perform a full synchronization pass.
		 *
		 * + "generation_modified"
		 * + type: Int
		 * + ReadOnly
		 *
		 * @see MediaStore.getGeneration
		 */
		val GENERATION_MODIFIED = MediaStore.MediaColumns.GENERATION_MODIFIED

		/**
		 * Indexed XMP metadata extracted from this media item.
		 *
		 *
		 * The structure of this metadata is defined by the
		 * [*XMP Media Management* standard](https://en.wikipedia.org/wiki/Extensible_Metadata_Platform), published as ISO 16684-1:2012.
		 *
		 *
		 * This metadata is typically extracted from a
		 * [ExifInterface.TAG_XMP] contained inside an image file or from
		 * a `XMP_` box contained inside an ISO/IEC base media file format
		 * (MPEG-4 Part 12).
		 *
		 *
		 * Note that any location details are redacted from this metadata for
		 * privacy reasons.
		 *
		 * + "xmp"
		 * + type: BLOB
		 * + ReadOnly
		 */
		val XMP = MediaStore.MediaColumns.XMP
		// =======================================
		// ==== MediaMetadataRetriever values ====
		// =======================================
		// =======================================
		// ==== MediaMetadataRetriever values ====
		// =======================================
		/**
		 * Indexed value of
		 * [MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER] extracted
		 * from this media item.
		 *
		 * + "cd_track_number"
		 * + type: String
		 * + ReadOnly
		 */
		val CD_TRACK_NUMBER = MediaStore.MediaColumns.CD_TRACK_NUMBER

		/**
		 * Indexed value of [MediaMetadataRetriever.METADATA_KEY_ALBUM]
		 * extracted from this media item.
		 *
		 * + "album"
		 * + type: String
		 * + ReadOnly
		 */
		val ALBUM = MediaStore.MediaColumns.ALBUM

		/**
		 * Indexed value of [MediaMetadataRetriever.METADATA_KEY_ARTIST]
		 * or [ExifInterface.TAG_ARTIST] extracted from this media item.
		 *
		 * + "artist"
		 * + type: String
		 * + ReadOnly
		 */
		val ARTIST = MediaStore.MediaColumns.ARTIST

		/**
		 * Indexed value of [MediaMetadataRetriever.METADATA_KEY_AUTHOR]
		 * extracted from this media item.
		 *
		 * + "author"
		 * + type: String
		 * + ReadOnly
		 */
		val AUTHOR = MediaStore.MediaColumns.AUTHOR

		/**
		 * Indexed value of [MediaMetadataRetriever.METADATA_KEY_COMPOSER]
		 * extracted from this media item.
		 *
		 * + "composer"
		 * + type: String
		 * + ReadOnly
		 */
		val COMPOSER = MediaStore.MediaColumns.COMPOSER

		/**
		 * Indexed value of [MediaMetadataRetriever.METADATA_KEY_GENRE]
		 * extracted from this media item.
		 *
		 * + "genre"
		 * + type: String
		 * + ReadOnly
		 */
		val GENRE = MediaStore.MediaColumns.GENRE

		/**
		 * Indexed value of [MediaMetadataRetriever.METADATA_KEY_TITLE]
		 * extracted from this media item.
		 *
		 * + "title"
		 * + type: String
		 * + ReadOnly
		 */
		val TITLE = MediaStore.MediaColumns.TITLE

		/**
		 * Indexed value of [MediaMetadataRetriever.METADATA_KEY_YEAR]
		 * extracted from this media item.
		 *
		 * + "year"
		 * + type: Int
		 * + ReadOnly
		 */
		val YEAR = MediaStore.MediaColumns.YEAR

		/**
		 * Indexed value of [MediaMetadataRetriever.METADATA_KEY_DURATION]
		 * extracted from this media item.
		 *
		 * + "duration"
		 * + type: Long
		 * + ReadOnly
		 */
		@DurationValue(TimeUnit.MILLISECONDS)
		val DURATION = MediaStore.MediaColumns.DURATION

		/**
		 * Indexed value of [MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS]
		 * extracted from this media item.
		 *
		 * + "num_tracks"
		 * + type: Int
		 * + ReadOnly
		 */
		val NUM_TRACKS = MediaStore.MediaColumns.NUM_TRACKS

		/**
		 * Indexed value of [MediaMetadataRetriever.METADATA_KEY_WRITER]
		 * extracted from this media item.
		 *
		 * + "writer"
		 * + type: String
		 * + ReadOnly
		 */
		val WRITER = MediaStore.MediaColumns.WRITER

		/**
		 * Indexed value of [MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST]
		 * extracted from this media item.
		 *
		 * + "album_artist"
		 * + type: String
		 * + ReadOnly
		 */
		val ALBUM_ARTIST = MediaStore.MediaColumns.ALBUM_ARTIST

		/**
		 * Indexed value of [MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER]
		 * extracted from this media item.
		 *
		 * + "disc_number"
		 * + type: String
		 * + ReadOnly
		 */
		val DISC_NUMBER = MediaStore.MediaColumns.DISC_NUMBER

		/**
		 * Indexed value of [MediaMetadataRetriever.METADATA_KEY_COMPILATION]
		 * extracted from this media item.
		 *
		 * + "compilation"
		 * + type: String
		 * + ReadOnly
		 */
		val COMPILATION = MediaStore.MediaColumns.COMPILATION

		// HAS_AUDIO is ignored
		// HAS_VIDEO is ignored
		// VIDEO_WIDTH is WIDTH
		// VIDEO_HEIGHT is HEIGHT
		// HAS_AUDIO is ignored
		// HAS_VIDEO is ignored
		// VIDEO_WIDTH is WIDTH
		// VIDEO_HEIGHT is HEIGHT

		/**
		 * Indexed value of [MediaMetadataRetriever.METADATA_KEY_BITRATE]
		 * extracted from this media item.
		 *
		 * + "bitrate"
		 * + type: Int
		 * + ReadOnly
		 */
		val BITRATE = MediaStore.MediaColumns.BITRATE

		// TIMED_TEXT_LANGUAGES is ignored
		// IS_DRM is ignored
		// LOCATION is LATITUDE and LONGITUDE
		// VIDEO_ROTATION is ORIENTATION
		// TIMED_TEXT_LANGUAGES is ignored
		// IS_DRM is ignored
		// LOCATION is LATITUDE and LONGITUDE
		// VIDEO_ROTATION is ORIENTATION

		/**
		 * Indexed value of
		 * [MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE]
		 * extracted from this media item.
		 *
		 * + "capture_framerate"
		 * + type: Float
		 * + ReadOnly
		 */
		val CAPTURE_FRAMERATE = MediaStore.MediaColumns.CAPTURE_FRAMERATE

		// HAS_IMAGE is ignored
		// IMAGE_COUNT is ignored
		// IMAGE_PRIMARY is ignored
		// IMAGE_WIDTH is WIDTH
		// IMAGE_HEIGHT is HEIGHT
		// IMAGE_ROTATION is ORIENTATION
		// VIDEO_FRAME_COUNT is ignored
		// EXIF_OFFSET is ignored
		// EXIF_LENGTH is ignored
		// COLOR_STANDARD is ignored
		// COLOR_TRANSFER is ignored
		// COLOR_RANGE is ignored
		// SAMPLERATE is ignored
		// BITS_PER_SAMPLE is ignored

		companion object : MediaColumns() {
			// convenient reference
		}

	}

	object Files {

		private val EXTERNAL_CONTENT_URI: Uri? =
			MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

		/**
		 * File metadata columns.
		 */

		abstract class FileColumns : MediaColumns() {
			// Promoted to MediaColumns:
			/*
				TITLE
				DATE_TAKEN
				ORIENTATION
				BUCKET_ID
				BUCKET_DISPLAY_NAME
				GROUP_ID
			*/

			/**
			 * The MTP storage ID of the file
			 *
			 * + "storage_id"
			 * + type: Int
			 *
			 * @hide
			 */
			@Deprecated("")
			protected val STORAGE_ID = "storage_id"

			/**
			 * The MTP format code of the file
			 *
			 * + "format"
			 * + type: Int
			 * + ReadOnly
			 *
			 * @hide
			 */
			protected val FORMAT = "format"

			/**
			 * The index of the parent directory of the file
			 *
			 * + "parent"
			 * + type: Int
			 * + ReadOnly
			 */
			val PARENT = MediaStore.Files.FileColumns.PARENT

			/**
			 * The media type (audio, video, image, document, playlist or subtitle)
			 * of the file, or 0 for not a media file
			 *
			 * + "media_type"
			 * + type: Int
			 */
			val MEDIA_TYPE = MediaStore.Files.FileColumns.MEDIA_TYPE

			companion object : FileColumns() {

				// convenient reference

				/**
				 * Constant for the [.MEDIA_TYPE] column indicating that file
				 * is not an audio, image, video, document, playlist, or subtitles file.
				 *
				 * + 0
				 */
				const val MEDIA_TYPE_NONE = MediaStore.Files.FileColumns.MEDIA_TYPE_NONE

				/**
				 * Constant for the [.MEDIA_TYPE] column indicating that file
				 * is an image file.
				 *
				 * + 1
				 */
				const val MEDIA_TYPE_IMAGE = MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE

				/**
				 * Constant for the [.MEDIA_TYPE] column indicating that file
				 * is an audio file.
				 *
				 * + 2
				 */
				const val MEDIA_TYPE_AUDIO = MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO

				/**
				 * Constant for the [.MEDIA_TYPE] column indicating that file
				 * is a video file.
				 *
				 * + 3
				 */
				const val MEDIA_TYPE_VIDEO = MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO

				/**
				 * Constant for the [.MEDIA_TYPE] column indicating that file
				 * is a playlist file.
				 *
				 * + 4
				 */
				const val MEDIA_TYPE_PLAYLIST = MediaStore.Files.FileColumns.MEDIA_TYPE_PLAYLIST

				/**
				 * Constant for the [.MEDIA_TYPE] column indicating that file
				 * is a subtitles or lyrics file.
				 *
				 * + 5
				 */
				const val MEDIA_TYPE_SUBTITLE = MediaStore.Files.FileColumns.MEDIA_TYPE_SUBTITLE

				/**
				 * Constant for the [.MEDIA_TYPE] column indicating that file is a document file.
				 *
				 * + 6
				 */
				const val MEDIA_TYPE_DOCUMENT = MediaStore.Files.FileColumns.MEDIA_TYPE_DOCUMENT
			}

		}
	}

	object Download {

		val CONTENT_TYPE = MediaStore.Downloads.CONTENT_TYPE
		val EXTERNAL_CONTENT_URI = MediaStore.Downloads.EXTERNAL_CONTENT_URI
		val INTERNAL_CONTENT_URI = MediaStore.Downloads.INTERNAL_CONTENT_URI

		/**
		 * Download metadata columns.
		 */

		abstract class DownloadColumns : MediaColumns() {

			/**
			 * Uri indicating where the item has been downloaded from.
			 *
			 * + "download_uri"
			 * + type: String
			 */
			val DOWNLOAD_URI = MediaStore.DownloadColumns.DOWNLOAD_URI

			/**
			 * Uri indicating HTTP referer of [.DOWNLOAD_URI].
			 *
			 * + "referer_uri"
			 * + type: String
			 */
			val REFERER_URI = MediaStore.DownloadColumns.REFERER_URI

			/**
			 * The description of the download.
			 *
			 * + "description"
			 * + type: String
			 *
			 * @removed
			 */
			@Deprecated("")
			protected val DESCRIPTION = "description"

			companion object : DownloadColumns() {
				// convenient reference
			}

		}
	}

	/**
	 * Collection of all media with MIME type of
	 * ```
	 * image/`*`
	 * ```
	 */
	object Images {

		val CONTENT_TYPE = MediaStore.Images.Media.CONTENT_TYPE
		val DEFAULT_SORT_ORDER = MediaStore.Images.Media.DEFAULT_SORT_ORDER
		val EXTERNAL_CONTENT_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
		val INTERNAL_CONTENT_URI = MediaStore.Images.Media.INTERNAL_CONTENT_URI

		/**
		 * Image metadata columns.
		 */

		abstract class ImageColumns : MediaColumns() {

			/**
			 * The picasa id of the image
			 *
			 * + "picasa_id"
			 * + type: String
			 */
			@Deprecated(
				message =
				"""
					this value was only relevant for images hosted on Picasa,
					which are no longer supported.
				""",
				level = DeprecationLevel.WARNING
			)
			val PICASA_ID = MediaStore.Images.ImageColumns.PICASA_ID

			/**
			 * Whether the video should be published as public or private
			 *
			 * + "isprivate"
			 * + type: Int
			 */
			val IS_PRIVATE = MediaStore.Images.ImageColumns.IS_PRIVATE

			/**
			 * The latitude where the image was captured.
			 *
			 * + "latitude"
			 * + type: Float
			 * + ReadOnly
			 */
			@Deprecated(
				message =
				"""
					location details are no longer indexed for privacy reasons,
					and this value is now always null.
          You can still manually obtain location metadata using ExifInterface#getLatLong(float[]).
					""",
				level = DeprecationLevel.WARNING
			)
			val LATITUDE = MediaStore.Images.ImageColumns.LATITUDE

			/**
			 * The longitude where the image was captured.
			 *
			 * + "longitude"
			 * + type: Float
			 * + ReadOnly
			 */
			@Deprecated(
				message = """
					location details are no longer indexed for privacy reasons,
					and this value is now always null.
           You can still manually obtain location metadata using ExifInterface#getLatLong(float[]).
					 """,
				level = DeprecationLevel.WARNING
			)
			val LONGITUDE = MediaStore.Images.ImageColumns.LONGITUDE

			/**
			 * The mini thumb id.
			 *
			 * + "mini_thumb_magic"
			 * + type: Int
			 */
			@Deprecated(
				message =
				"""
					all thumbnails should be obtained via MediaStore.Images.Thumbnails#getThumbnail,
					as this value is no longer supported.
					""",
				level = DeprecationLevel.WARNING
			)
			val MINI_THUMB_MAGIC = MediaStore.Images.ImageColumns.MINI_THUMB_MAGIC

			/**
			 * Indexed value of [ExifInterface.TAG_IMAGE_DESCRIPTION]
			 * extracted from this media item.
			 *
			 * + "description"
			 * + type: String
			 * + ReadOnly
			 */
			val DESCRIPTION = MediaStore.Images.ImageColumns.DESCRIPTION

			/**
			 * Indexed value of [ExifInterface.TAG_EXPOSURE_TIME]
			 * extracted from this media item.
			 *
			 * + "exposure_time"
			 * + type: String
			 * + ReadOnly
			 */
			val EXPOSURE_TIME = MediaStore.Images.ImageColumns.EXPOSURE_TIME

			/**
			 * Indexed value of [ExifInterface.TAG_F_NUMBER]
			 * extracted from this media item.
			 *
			 * + "f_number"
			 * + type: String
			 * + ReadOnly
			 */
			val F_NUMBER = MediaStore.Images.ImageColumns.F_NUMBER

			/**
			 * Indexed value of [ExifInterface.TAG_ISO_SPEED_RATINGS]
			 * extracted from this media item.
			 *
			 * + "iso"
			 * + type: String
			 * + ReadOnly
			 */
			val ISO = MediaStore.Images.ImageColumns.ISO

			/**
			 * Indexed value of [ExifInterface.TAG_SCENE_CAPTURE_TYPE]
			 * extracted from this media item.
			 *
			 * + "scene_capture_type"
			 * + type: String
			 * + ReadOnly
			 */
			val SCENE_CAPTURE_TYPE = MediaStore.Images.ImageColumns.SCENE_CAPTURE_TYPE

			companion object : ImageColumns() {
				// convenient reference
			}

		}
	}


	/**
	 * Collection of all media with MIME type of
	 * ```
	 * audio/`*`
	 * ```
	 */
	object Audio {

		val CONTENT_TYPE = MediaStore.Audio.Media.CONTENT_TYPE
		val DEFAULT_SORT_ORDER = MediaStore.Audio.Media.DEFAULT_SORT_ORDER
		val EXTERNAL_CONTENT_URI = MediaStore.Audio.Media.INTERNAL_CONTENT_URI
		val INTERNAL_CONTENT_URI = MediaStore.Audio.Media.INTERNAL_CONTENT_URI

		/**
		 * Audio metadata columns.
		 */
		abstract class AudioColumns : MediaColumns() {
			// Promoted to MediaColumns
			/*
				DURATION
				ARTIST
				COMPOSER
				ALBUM
			*/

			/**
			 * A non human readable key calculated from the TITLE, used for
			 * searching, sorting and grouping
			 *
			 * + "title_key"
			 * + type: String
			 * + ReadOnly
			 *
			 * @see MediaStore.Audio.keyFor
			 */
			@Deprecated(
				message =
				"""
					These keys are generated using java.util.Locale#ROOT, which means they don't reflect
					locale-specific sorting preferences. To apply locale-specific sorting preferences, use
					ContentResolver#QUERY_ARG_SQL_SORT_ORDER with COLLATE LOCALIZED,or
					ContentResolver#QUERY_ARG_SORT_LOCALE.
				""",
				level = DeprecationLevel.WARNING
			)

			val TITLE_KEY = MediaStore.Audio.AudioColumns.TITLE_KEY

			/**
			 * The position within the audio item at which playback should be
			 * resumed.
			 *
			 * + "bookmark"
			 * + type: Long
			 */
			@DurationValue(TimeUnit.MILLISECONDS)
			val BOOKMARK = MediaStore.Audio.AudioColumns.BOOKMARK

			/**
			 * The id of the artist who created the audio file, if any
			 *
			 * + "artist_id"
			 * + type: Int
			 * + ReadOnly
			 */
			val ARTIST_ID = MediaStore.Audio.AudioColumns.ARTIST_ID


			/**
			 * A non human readable key calculated from the ARTIST, used for
			 * searching, sorting and grouping
			 *
			 * + "artist_key"
			 * + type: String
			 * + ReadOnly
			 *
			 * @see MediaStore.Audio.keyFor
			 */
			@Deprecated(
				message =
				"""
					These keys are generated using java.util.Locale#ROOT, which means they don't reflect
					locale-specific sorting preferences. To apply locale-specific sorting preferences, use
          ContentResolver#QUERY_ARG_SQL_SORT_ORDER with COLLATE LOCALIZED, or
					ContentResolver#QUERY_ARG_SORT_LOCALE.
				""",
				level = DeprecationLevel.WARNING
			)
			val ARTIST_KEY = MediaStore.Audio.AudioColumns.ARTIST_KEY

			/**
			 * The id of the album the audio file is from, if any
			 *
			 * + "album_id"
			 * + type: Int
			 * + ReadOnly
			 */
			val ALBUM_ID = MediaStore.Audio.AudioColumns.ALBUM_ID

			/**
			 * A non human readable key calculated from the ALBUM, used for
			 * searching, sorting and grouping
			 *
			 * + "album_key"
			 * + type: String
			 * + ReadOnly
			 *
			 * @see MediaStore.Audio.keyFor
			 */
			@Deprecated(
				message =
				"""
					These keys are generated using java.util.Locale#ROOT, which means they don't reflect
					locale-specific sorting preferences. To apply locale-specific sorting preferences, use
          ContentResolver#QUERY_ARG_SQL_SORT_ORDER with COLLATE LOCALIZED, or
					ContentResolver#QUERY_ARG_SORT_LOCALE.
				""",
				level = DeprecationLevel.WARNING
			)
			val ALBUM_KEY = MediaStore.Audio.AlbumColumns.ALBUM_KEY

			/**
			 * The track number of this song on the album, if any.
			 * This number encodes both the track number and the
			 * disc number. For multi-disc sets, this number will
			 * be 1xxx for tracks on the first disc, 2xxx for tracks
			 * on the second disc, etc.
			 *
			 * + "track"
			 * + type: Int
			 * + ReadOnly
			 *
			 */
			val TRACK = MediaStore.Audio.AudioColumns.TRACK

			/**
			 * Non-zero if the audio file is music
			 *
			 * + "is_music"
			 * + type: Int
			 * + ReadOnly
			 */
			val IS_MUSIC = MediaStore.Audio.AudioColumns.IS_MUSIC

			/**
			 * Non-zero if the audio file is a podcast
			 *
			 * + "is_podcast"
			 * + type: Int
			 * + ReadOnly
			 */
			val IS_PODCAST = MediaStore.Audio.AudioColumns.IS_PODCAST

			/**
			 * Non-zero if the audio file may be a ringtone
			 *
			 * + "is_ringtone"
			 * + type: Int
			 * + ReadOnly
			 */
			val IS_RINGTONE = MediaStore.Audio.AudioColumns.IS_RINGTONE

			/**
			 * Non-zero if the audio file may be an alarm
			 *
			 * + "is_alarm"
			 * + type: Int
			 * + ReadOnly
			 */
			val IS_ALARM = MediaStore.Audio.AudioColumns.IS_ALARM

			/**
			 * Non-zero if the audio file may be a notification sound
			 *
			 * + "is_notification"
			 * + type: Int
			 * + ReadOnly
			 */
			val IS_NOTIFICATION = MediaStore.Audio.AudioColumns.IS_NOTIFICATION

			/**
			 * Non-zero if the audio file is an audiobook
			 *
			 * + "is_audiobook"
			 * + type: Int
			 * + ReadOnly
			 */
			val IS_AUDIOBOOK = MediaStore.Audio.AudioColumns.IS_AUDIOBOOK

			/**
			 * The id of the genre the audio file is from, if any
			 *
			 * + "genre_id"
			 * + type: Int
			 * + ReadOnly
			 */
			val GENRE_ID = MediaStore.Audio.AudioColumns.GENRE_ID


			/**
			 * A non human readable key calculated from the GENRE, used for
			 * searching, sorting and grouping
			 *
			 * + "genre_key"
			 * + type: String
			 * + ReadOnly
			 *
			 * @see MediaStore.Audio.keyFor
			 */
			@Deprecated(
				message =
				"""
					These keys are generated using java.util.Locale#ROOT, which means they don't reflect
					locale-specific sorting preferences. To apply locale-specific sorting preferences, use
          ContentResolver#QUERY_ARG_SQL_SORT_ORDER with COLLATE LOCALIZED, or
					ContentResolver#QUERY_ARG_SORT_LOCALE.
				""",
				level = DeprecationLevel.WARNING
			)
			val GENRE_KEY = MediaStore.Audio.AudioColumns.GENRE_KEY

			/**
			 * The resource URI of a localized title, if any.
			 *
			 *
			 * Conforms to this pattern:
			 *
			 *  * Scheme: [ContentResolver.SCHEME_ANDROID_RESOURCE]
			 *  * Authority: Package Name of ringtone title provider
			 *  * First Path Segment: Type of resource (must be "string")
			 *  * Second Path Segment: Resource ID of title
			 *
			 *  + "title_resource_uri"
			 *  + type: String
			 *  + ReadOnly
			 *
			 */
			val TITLE_RESOURCE_URI = MediaStore.Audio.AudioColumns.TITLE_RESOURCE_URI

			object Genres {

				val DEFAULT_SORT_ORDER = MediaStore.Audio.Genres.DEFAULT_SORT_ORDER
				val EXTERNAL_CONTENT_URI = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI
				val INTERNAL_CONTENT_URI = MediaStore.Audio.Genres.INTERNAL_CONTENT_URI

				/**
				 * Audio genre metadata columns.
				 */

				abstract class GenresColumns {
					/**
					 * The name of the genre
					 * + "name"
					 * + type: String
					 */
					val NAME = MediaStore.Audio.GenresColumns.NAME

					companion object : GenresColumns() {
						// convenient reference
					}

				}
			}

			object Playlists {

				val DEFAULT_SORT_ORDER = MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER
				val EXTERNAL_CONTENT_URI = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI
				val INTERNAL_CONTENT_URI = MediaStore.Audio.Playlists.INTERNAL_CONTENT_URI

				/**
				 * Audio playlist metadata columns.
				 */

				abstract class PlaylistsColumns : MediaColumns() {
					/**
					 * The name of the playlist
					 *
					 * + "name"
					 * + type: String
					 */
					val NAME = MediaStore.Audio.PlaylistsColumns.NAME

					companion object : MediaColumns() {
						// convenient reference
					}

				}
			}

			object Artist {

				val DEFAULT_SORT_ORDER = MediaStore.Audio.Artists.DEFAULT_SORT_ORDER
				val EXTERNAL_CONTENT_URI = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI
				val INTERNAL_CONTENT_URI = MediaStore.Audio.Artists.INTERNAL_CONTENT_URI

				abstract class ArtistsColumns : MediaColumns() {
					/**
					 * A non human readable key calculated from the ARTIST, used for
					 * searching, sorting and grouping
					 *
					 * + "artist_key"
					 * + type: String
					 * + ReadOnly
					 *
					 * @see MediaStore.Audio.keyFor
					 */
					@Deprecated(
						message =
						"""
							These keys are generated using java.util.Locale#ROOT,	which means they don't reflect
							locale-specific sorting preferences. To apply locale-specific sorting preferences, use
							ContentResolver#QUERY_ARG_SQL_SORT_ORDER with COLLATE LOCALIZED, or
              ContentResolver#QUERY_ARG_SORT_LOCALE.
						""",
						level = DeprecationLevel.WARNING
					)
					val ARTIST_KEY = MediaStore.Audio.ArtistColumns.ARTIST_KEY

					/**
					 * The number of albums in the database for this artist
					 *
					 * + "number_of_albums"
					 * + type: Int
					 * + ReadOnly
					 */
					val NUMBER_OF_ALBUMS = MediaStore.Audio.ArtistColumns.NUMBER_OF_ALBUMS

					/**
					 * The number of albums in the database for this artist
					 *
					 * + "number_of_tracks"
					 * + type: Int
					 * + ReadOnly
					 */
					val NUMBER_OF_TRACKS = MediaStore.Audio.ArtistColumns.NUMBER_OF_TRACKS

					companion object : ArtistsColumns() {
						// convenient reference
					}

				}
			}

			object Album {

				val DEFAULT_SORT_ORDER = MediaStore.Audio.Albums.DEFAULT_SORT_ORDER
				val EXTERNAL_CONTENT_URI = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
				val INTERNAL_CONTENT_URI = MediaStore.Audio.Albums.INTERNAL_CONTENT_URI

				/**
				 * Columns representing an album
				 */
				abstract class AlbumColumns : MediaColumns() {
					/**
					 * The id for the album
					 *
					 * + "album_id"
					 * + type: Int
					 * + ReadOnly
					 */
					val ALBUM_ID = MediaStore.Audio.AlbumColumns.ALBUM_ID

					/**
					 * The ID of the artist whose songs appear on this album.
					 *
					 * + "artist_id"
					 * + type: Int
					 * + ReadOnly
					 */
					val ARTIST_ID = MediaStore.Audio.AlbumColumns.ARTIST_ID

					/**
					 * A non human readable key calculated from the ARTIST, used for
					 * searching, sorting and grouping
					 *
					 * + "artist_key"
					 * + type: String
					 * + ReadOnly
					 *
					 * @see MediaStore.Audio.keyFor
					 */
					@Deprecated(
						message =
						"""
							These keys are generated using java.util.Locale#ROOT, which means they don't reflect
							locale-specific sorting preferences. To apply locale-specific sorting preferences, use
              ContentResolver#QUERY_ARG_SQL_SORT_ORDER with COLLATE LOCALIZED, or
							ContentResolver#QUERY_ARG_SORT_LOCALE.
						""",
						level = DeprecationLevel.WARNING
					)
					val ARTIST_KEY = MediaStore.Audio.AlbumColumns.ARTIST_KEY

					/**
					 * The number of songs on this album
					 *
					 * + "numsongs"
					 * + type: Int
					 * + ReadOnly
					 */
					val NUMBER_OF_SONGS = MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS

					/**
					 * This column is available when getting album info via artist,
					 * and indicates the number of songs on the album by the given
					 * artist.
					 *
					 * + "numsongs_by_artist"
					 * + type: Int
					 * + ReadOnly
					 */
					val NUMBER_OF_SONGS_FOR_ARTIST = MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS_FOR_ARTIST

					/**
					 * The year in which the earliest songs
					 * on this album were released. This will often
					 * be the same as [.LAST_YEAR], but for compilation albums
					 * they might differ.
					 *
					 * + "minyear"
					 * + type: Int
					 * + ReadOnly
					 */
					val FIRST_YEAR = MediaStore.Audio.AlbumColumns.FIRST_YEAR

					/**
					 * The year in which the latest songs
					 * on this album were released. This will often
					 * be the same as [.FIRST_YEAR], but for compilation albums
					 * they might differ.
					 *
					 * + "maxyear"
					 * + type: Int
					 * + ReadOnly
					 */
					val LAST_YEAR = MediaStore.Audio.AlbumColumns.LAST_YEAR

					/**
					 * A non human readable key calculated from the ALBUM, used for
					 * searching, sorting and grouping
					 *
					 * + "album_key"
					 * + type: String
					 * + ReadOnly
					 *
					 * @see MediaStore.Audio.keyFor
					 */
					@Deprecated(
						message =
						"""
							These keys are generated using java.util.Locale#ROOT, which means they don't reflect
							locale-specific sorting preferences. To apply locale-specific sorting preferences, use
              ContentResolver#QUERY_ARG_SQL_SORT_ORDER with COLLATE LOCALIZED, or
							ContentResolver#QUERY_ARG_SORT_LOCALE.
						""",
						level = DeprecationLevel.WARNING
					)
					val ALBUM_KEY = MediaStore.Audio.AlbumColumns.ALBUM_KEY

					/**
					 * Cached album art.
					 *
					 * + "album_art"
					 * + type: String
					 *
					 */
					@Deprecated(
						message =
						"""
							Apps may not have filesystem permissions to directly access this path.
							Instead of trying to open this path directly, apps should use
							ContentResolver#loadThumbnail to gain access.
						""",
						level = DeprecationLevel.WARNING
					)
					val ALBUM_ART = MediaStore.Audio.AlbumColumns.ALBUM_ART

					companion object : AlbumColumns() {
						// convenient reference
					}

				}
			}

			companion object : AudioColumns() {
				// convenient reference
			}

		}
	}

	/**
	 * Collection of all media with MIME type of
	 * ```
	 * audio/`*`
	 * ```
	 */
	object Video {

		val DEFAULT_SORT_ORDER = MediaStore.Video.Media.DEFAULT_SORT_ORDER
		val EXTERNAL_CONTENT_URI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
		val INTERNAL_CONTENT_URI = MediaStore.Video.Media.INTERNAL_CONTENT_URI

		abstract class VideoColumns : MediaColumns() {
			// Promoted to MediaColumns //
			/*
				DURATION
				ARTIST
				ALBUM
				RESOLUTION
				DATE_TAKEN
				BUCKET_ID
				BUCKET_DISPLAY_NAME
				GROUP_ID
			*/

			/**
			 * The description of the video recording
			 *
			 * + "description"
			 * + type: String
			 * + ReadOnly
			 */
			val DESCRIPTION = MediaStore.Video.VideoColumns.DESCRIPTION

			/**
			 * Whether the video should be published as public or private
			 *
			 * + "isprivate"
			 * + type: Int
			 * + ReadOnly
			 */
			val IS_PRIVATE = MediaStore.Video.VideoColumns.IS_PRIVATE

			/**
			 * The user-added tags associated with a video
			 *
			 * + "tags"
			 * + type: String
			 */
			val TAGS = MediaStore.Video.VideoColumns.TAGS

			/**
			 * The YouTube category of the video
			 *
			 * + "category"
			 * + type: String
			 */
			val CATEGORY = MediaStore.Video.VideoColumns.CATEGORY

			/**
			 * The language of the video
			 *
			 * + "language"
			 * + type: String
			 */
			val LANGUAGE = MediaStore.Video.VideoColumns.LANGUAGE

			/**
			 * The latitude where the video was captured.
			 *
			 * + "latitude"
			 * + type: Float
			 * + ReadOnly
			 */
			@Deprecated(
				message =
				"""
					location details are no longer indexed for privacy reasons, and this value is now always null.
          You can still manually obtain location metadata using ExifInterface#getLatLong(float[]).
				""",
				level = DeprecationLevel.WARNING
			)
			val LATITUDE = MediaStore.Video.VideoColumns.LATITUDE

			/**
			 * The longitude where the video was captured.
			 *
			 * + "longitude"
			 * + type: Float
			 * + ReadOnly
			 */
			@Deprecated(
				message =
				"""
					location details are no longer indexed for privacy reasons, and this value is now always null.
          You can still manually obtain location metadata using ExifInterface#getLatLong(float[]).
				""",
				level = DeprecationLevel.WARNING
			)
			val LONGITUDE = MediaStore.Video.VideoColumns.LONGITUDE

			/**
			 * The mini thumb id.
			 *
			 * + "mini_thumb_magic"
			 * + type: Int
			 */
			@Deprecated(
				message =
				"""
					all thumbnails should be obtained via MediaStore.Images.Thumbnails#getThumbnail, as this
          value is no longer supported.
				""",
				level = DeprecationLevel.WARNING
			)
			val MINI_THUMB_MAGIC = MediaStore.Video.VideoColumns.MINI_THUMB_MAGIC

			/**
			 * The position within the video item at which playback should be
			 * resumed.
			 *
			 * + "bookmark"
			 * + type: Long
			 */
			@DurationValue(TimeUnit.MILLISECONDS)
			val BOOKMARK = MediaStore.Video.VideoColumns.BOOKMARK

			/**
			 * The color standard of this media file, if available.
			 *
			 * + "color_standard"
			 * + type: Int
			 * + ReadOnly
			 *
			 * @see MediaFormat.COLOR_STANDARD_BT709
			 *
			 * @see MediaFormat.COLOR_STANDARD_BT601_PAL
			 *
			 * @see MediaFormat.COLOR_STANDARD_BT601_NTSC
			 *
			 * @see MediaFormat.COLOR_STANDARD_BT2020
			 */
			val COLOR_STANDARD = MediaStore.Video.VideoColumns.COLOR_STANDARD

			/**
			 * The color transfer of this media file, if available.
			 *
			 * + "color_transfer"
			 * + type: Int
			 * + ReadOnly
			 *
			 * @see MediaFormat.COLOR_TRANSFER_LINEAR
			 *
			 * @see MediaFormat.COLOR_TRANSFER_SDR_VIDEO
			 *
			 * @see MediaFormat.COLOR_TRANSFER_ST2084
			 *
			 * @see MediaFormat.COLOR_TRANSFER_HLG
			 */
			val COLOR_TRANSFER = MediaStore.Video.VideoColumns.COLOR_TRANSFER

			/**
			 * The color range of this media file, if available.
			 *
			 * + "color_range"
			 * + type: Int
			 * + ReadOnly
			 *
			 * @see MediaFormat.COLOR_RANGE_LIMITED
			 *
			 * @see MediaFormat.COLOR_RANGE_FULL
			 */
			val COLOR_RANGE = MediaStore.Video.VideoColumns.COLOR_RANGE

			companion object : VideoColumns() {
				// convenient reference
			}

		}
	}

}

