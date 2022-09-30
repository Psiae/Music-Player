@file:RequiresApi(Build.VERSION_CODES.Q)
@file:OnlyAPI(Build.VERSION_CODES.Q)

package com.flammky.android.medialib.temp.provider.mediastore.api29

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.flammky.android.core.sdk.VersionHelper
import com.flammky.common.kotlin.time.annotation.DurationValue
import com.flammky.android.medialib.temp.provider.mediastore.base.BaseColumns
import com.flammky.android.medialib.temp.annotation.OnlyAPI
import com.flammky.android.medialib.temp.annotation.StorageDataUnit
import com.flammky.android.medialib.temp.annotation.StorageDataValue
import java.util.concurrent.TimeUnit


/**
 * [GoogleSource](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-10.0.0_r47/core/java/android/provider/MediaStore.java)
 */

internal object MediaStore29 {

	init {
		check(VersionHelper.isQ()) {
			val n = if (VersionHelper.hasR()) "MediaStore30" else "MediaStore28"
			"Invalid Usage of API 29 on API ${Build.VERSION.SDK_INT}, use $n instead"
		}
	}

	abstract class MediaColumns : BaseColumns() {

		/**
		 * Absolute filesystem path to the media item on disk.
		 *
		 *
		 * Note that apps may not have filesystem permissions to directly access
		 * this path. Instead of trying to open this path directly, apps should
		 * use [ContentResolver.openFileDescriptor] to gain
		 * access.
		 *
		 * + "_data"
		 * + Type: String
		 */
		@Deprecated(
			"""
			Apps may not have filesystem permissions to directly access this path.
			Instead of trying to open this path directly,
			apps should use {@link ContentResolver#openFileDescriptor(Uri, String)} to gain access.
		""",
			level = DeprecationLevel.WARNING
		)
		val DATA = MediaStore.MediaColumns.DATA

		/**
		 * The size of the media item in `bytes`.
		 *
		 * + "_size"
		 * + Type: Long
		 * + ReadOnly
		 */
		@StorageDataValue(StorageDataUnit.BYTE)
		val SIZE = MediaStore.MediaColumns.SIZE

		/**
		 * The display name of the media item.
		 *
		 * For example, an item stored at
		 * `/storage/0000-0000/DCIM/Vacation/IMG1024.JPG` would have a
		 * display name of `IMG1024.JPG`.
		 *
		 * + "_display_name"
		 * + Type: String
		 *
		 */
		val DISPLAY_NAME = MediaStore.MediaColumns.DISPLAY_NAME

		/**
		 * The title of the media item.
		 *
		 * + "title"
		 * + Type: String
		 * + ReadOnly
		 */
		val TITLE = MediaStore.MediaColumns.TITLE

		/**
		 * The time the media item was first added.
		 *
		 * + "date_added"
		 * + Type: Long
		 * + ReadOnly
		 * + In Second since Epoch time
		 */
		@DurationValue(TimeUnit.SECONDS)
		val DATE_ADDED = MediaStore.MediaColumns.DATE_ADDED

		/**
		 * The time the media item was last modified.
		 *
		 * + "date_modified"
		 * + Type: Long
		 * + ReadOnly
		 * + In Second since Epoch time
		 */
		@DurationValue(TimeUnit.SECONDS)
		val DATE_MODIFIED = MediaStore.MediaColumns.DATE_MODIFIED

		/**
		 * The time the media item was taken.
		 *
		 * + "datetaken"
		 * + Type: Long
		 * + ReadOnly
		 * + In Millisecond since Epoch time
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
		 * + "mime_type"
		 * + Type: String
		 */
		val MIME_TYPE = MediaStore.MediaColumns.MIME_TYPE

		/**
		 * The MTP object handle of a newly transfered file.
		 * Used to pass the new file's object handle through the media scanner
		 * from MTP to the media provider
		 * For internal use only by MTP, media scanner and media provider.
		 *
		 * + "media_scanner_new_object_id"
		 * + Type: Int
		 * + Hidden
		 */
		protected val MEDIA_SCANNER_NEW_OBJECT_ID = "media_scanner_new_object_id"

		/**
		 * Non-zero if the media file is drm-protected
		 *
		 * + "is_drm"
		 * + Type: Int
		 * + Hidden
		 */
		protected val IS_DRM = "is_drm"

		/**
		 * Flag indicating if a media item is pending, and still being inserted
		 * by its owner. While this flag is set, only the owner of the item can
		 * open the underlying file; requests from other apps will be rejected.
		 *
		 * + "is_pending"
		 * + Type: Int
		 *
		 * @see MediaStore.setIncludePending
		 */
		val IS_PENDING = MediaStore.MediaColumns.IS_PENDING

		/**
		 * Flag indicating if a media item is trashed.
		 *
		 * + "is_trashed"
		 * + Type: Int
		 * + Removed
		 *
		 * @see MediaStore.MediaColumns.IS_TRASHED
		 *
		 * @see MediaStore.setIncludeTrashed
		 * @see MediaStore.trash
		 * @see MediaStore.untrash
		 * @removed
		 */
		protected val IS_TRASHED = "is_trashed"

		/**
		 * The time the media item should be considered expired. Typically only
		 * meaningful in the context of [.IS_PENDING].
		 *
		 * + "date_expires"
		 * + Type: Long
		 */
		@DurationValue(TimeUnit.SECONDS)
		val DATE_EXPIRES = MediaStore.MediaColumns.DATE_EXPIRES

		/**
		 * The width of the media item, in pixels.
		 *
		 * + "width"
		 * + Type: Int
		 * + ReadOnly
		 */
		val WIDTH = MediaStore.MediaColumns.WIDTH

		/**
		 * The height of the media item, in pixels.
		 *
		 * + "height"
		 * + Type: Int
		 * + ReadOnly
		 */
		val HEIGHT = MediaStore.MediaColumns.HEIGHT

		/**
		 * Package name that contributed this media. The value may be
		 * `NULL` if ownership cannot be reliably determined.
		 *
		 * + "owner_package_name"
		 * + Type: String
		 * + ReadOnly
		 */
		val OWNER_PACKAGE_NAME = MediaStore.MediaColumns.OWNER_PACKAGE_NAME

		/**
		 * Volume name of the specific storage device where this media item is
		 * persisted. The value is typically one of the volume names returned
		 * from [MediaStore.getExternalVolumeNames].
		 *
		 * This is a read-only column that is automatically computed.
		 *
		 * + "volume_name"
		 * + Type: String
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
		 * + Type: String
		 */
		val RELATIVE_PATH = MediaStore.MediaColumns.RELATIVE_PATH


		/**
		 * The primary directory name this media exists under. The value may be
		 * `NULL` if the media doesn't have a primary directory name.
		 *
		 * + "primary_directory"
		 * + Type: String
		 *
		 * @removed
		 */
		@Deprecated(message = "Replaced by {@link #RELATIVE_PATH}.", level = DeprecationLevel.HIDDEN)
		protected val PRIMARY_DIRECTORY = "primary_directory"

		/**
		 * The secondary directory name this media exists under. The value may
		 * be `NULL` if the media doesn't have a secondary directory name.
		 *
		 * + "secondary_directory"
		 * + Type: String
		 *
		 * @removed
		 */
		@Deprecated(message = "Replaced by {@link #RELATIVE_PATH}.", level = DeprecationLevel.HIDDEN)
		protected val SECONDARY_DIRECTORY = "secondary_directory"

		/**
		 * The primary bucket ID of this media item. This can be useful to
		 * present the user a first-level clustering of related media items.
		 * This is a read-only column that is automatically computed.
		 *
		 * + "bucket_id"
		 * + Type: Int
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
		 * + Type: Int
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
		 * + Type: Int
		 * + ReadOnly
		 *
		 * @removed
		 */
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
		 * + Type: String
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
		 * + Type: String
		 * + ReadOnly
		 *
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
		 * + Type: String
		 * + ReadOnly
		 */
		val ORIGINAL_DOCUMENT_ID = MediaStore.MediaColumns.ORIGINAL_DOCUMENT_ID

		/**
		 * The duration of the media item.
		 *
		 * + "duration"
		 * + Type: Long
		 * + ReadOnly
		 */
		@DurationValue(TimeUnit.MILLISECONDS)
		val DURATION = MediaStore.MediaColumns.DURATION

		/**
		 * The orientation for the media item, expressed in degrees. For
		 * example, 0, 90, 180, or 270 degrees.
		 *
		 * + "orientation"
		 * + Type: Int
		 * + ReadOnly
		 */
		val ORIENTATION = MediaStore.MediaColumns.ORIENTATION

		/**
		 * Hash of the media item on disk.
		 *
		 * Contains a 20-byte binary blob which is the SHA-1 hash of the file as
		 * persisted on disk. For performance reasons, the hash may not be
		 * immediately available, in which case a `NULL` value will be
		 * returned. If the underlying file is modified, this value will be
		 * cleared and recalculated.
		 *
		 * If you require the hash of a specific item, you can call
		 * [ContentResolver.canonicalize], which will block until the
		 * hash is calculated.
		 *
		 * + "_hash"
		 * + Type: String
		 * + ReadOnly
		 *
		 * @removed
		 */
		@Deprecated("removed", level = DeprecationLevel.HIDDEN) // read-only
		protected val HASH = "_hash"

		companion object : MediaColumns() {
			// convenient reference
		}
	}

	object Files {

		private val EXTERNAL_CONTENT_URI: Uri? =
			MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

		abstract class FileColumns : MediaColumns() {

			/**
			 * The MTP storage ID of the file
			 *
			 * + "storage_id"
			 * + Type: Int
			 * + Hidden
			 *
			 * @hide
			 */
			@Deprecated(message = "hidden", level = DeprecationLevel.HIDDEN)
			protected val STORAGE_ID = "storage_id"

			/**
			 * The MTP format code of the file
			 *
			 * + "format"
			 * + Type: Int
			 * + Hidden
			 * + ReadOnly
			 *
			 * @hide
			 */
			protected val FORMAT = "format"

			/**
			 * The index of the parent directory of the file
			 *
			 * + "parent"
			 * + Type: Int
			 * + ReadOnly
			 */
			val PARENT = MediaStore.Files.FileColumns.PARENT

			/**
			 * The media type (audio, video, image or playlist)
			 * of the file, or 0 for not a media file
			 *
			 * + "media_type"
			 * + Type: Int
			 */
			val MEDIA_TYPE = MediaStore.Files.FileColumns.MEDIA_TYPE

			/**
			 * Column indicating if the file is part of Downloads collection.
			 *
			 * + "is_download"
			 * + Type: Int
			 * + Hidden
			 * + ReadOnly
			 *
			 * @hide
			 */
			protected val IS_DOWNLOAD = "is_download"

			companion object : FileColumns() {

				// convenient reference

				/**
				 * Constant for the [MEDIA_TYPE] column indicating that file
				 * is not an audio, image, video or playlist file.
				 *
				 * + 0
				 */
				val MEDIA_TYPE_NONE = MediaStore.Files.FileColumns.MEDIA_TYPE_NONE

				/**
				 * Constant for the [MEDIA_TYPE] column indicating that file is an image file.
				 *
				 * + 1
				 */
				val MEDIA_TYPE_IMAGE = MediaStore.Files.FileColumns.MEDIA_TYPE_NONE

				/**
				 * Constant for the [MEDIA_TYPE] column indicating that file is an audio file.
				 *
				 * + 2
				 */
				val MEDIA_TYPE_AUDIO = MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO

				/**
				 * Constant for the [MEDIA_TYPE] column indicating that file is a video file.
				 *
				 * + 3
				 */
				val MEDIA_TYPE_VIDEO = MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO

				/**
				 * Constant for the [MEDIA_TYPE] column indicating that file is a playlist file.
				 *
				 * + 4
				 */
				val MEDIA_TYPE_PLAYLIST = MediaStore.Files.FileColumns.MEDIA_TYPE_PLAYLIST
			}

		}
	}

	object Audio {

		val DEFAULT_SORT_ORDER: String = MediaStore.Audio.Media.DEFAULT_SORT_ORDER
		val INTERNAL_CONTENT_URI: Uri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI
		val EXTERNAL_CONTENT_URI: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

		/**
		 * Audio metadata columns.
		 */
		abstract class AudioColumns : MediaColumns() {
			// Promoted to MediaColumns
			/*
				DURATION
			*/

			/**
			 * A non human readable key calculated from the TITLE, used for
			 * searching, sorting and grouping
			 *
			 * + "title_key"
			 * + type: String
			 * + ReadOnly
			 */
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
			 * The artist who created the audio file, if any
			 *
			 * + "artist"
			 * + type: String
			 * + ReadOnly
			 */
			val ARTIST = MediaStore.Audio.AudioColumns.ARTIST

			/**
			 * The artist credited for the album that contains the audio file
			 *
			 * + "album_artist"
			 * + type: String
			 * + ReadOnly
			 * + Hidden
			 * @hide
			 */
			protected val ALBUM_ARTIST = "album_artist"

			/**
			 * Whether the song is part of a compilation
			 *
			 * + "compilation"
			 * + type: String
			 * + Hidden
			 * @hide
			 */
			@Deprecated(message = "", level = DeprecationLevel.WARNING)
			protected val COMPILATION = "compilation"

			/**
			 * A non human readable key calculated from the ARTIST, used for
			 * searching, sorting and grouping
			 *
			 * + "artist_key"
			 * + type: String
			 * + ReadOnly
			 */
			val ARTIST_KEY = MediaStore.Audio.AudioColumns.ARTIST_KEY

			/**
			 * The composer of the audio file, if any
			 *
			 * + "composer"
			 * + type: String
			 * + ReadOnly
			 */
			val COMPOSER = MediaStore.Audio.AudioColumns.COMPOSER

			/**
			 * The id of the album the audio file is from, if any
			 *
			 * + "album_id"
			 * + type: Int
			 * + ReadOnly
			 */
			val ALBUM_ID = MediaStore.Audio.AudioColumns.ALBUM_ID

			/**
			 * The album the audio file is from, if any
			 *
			 * + "album"
			 * + type: String
			 * + ReadOnly
			 */
			val ALBUM = MediaStore.Audio.AudioColumns.ALBUM

			/**
			 * A non human readable key calculated from the ALBUM, used for
			 * searching, sorting and grouping
			 *
			 * + "album_key"
			 * + type: String
			 * + ReadOnly
			 */
			val ALBUM_KEY = MediaStore.Audio.AudioColumns.ALBUM_KEY

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
			 */
			val TRACK = MediaStore.Audio.AudioColumns.TRACK

			/**
			 * The year the audio file was recorded, if any
			 *
			 * + "year"
			 * + type: Int
			 * + ReadOnly
			 */
			val YEAR = MediaStore.Audio.AudioColumns.YEAR

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
			 * The genre of the audio file, if any
			 * Does not exist in the database - only used by the media scanner for inserts.
			 *
			 * + "genre"
			 * + type: String
			 * + Hidden
			 *
			 * @hide
			 */
			@Deprecated(message = "", level = DeprecationLevel.HIDDEN)
			protected val GENRE = "genre"

			/**
			 * The resource URI of a localized title, if any
			 * Conforms to this pattern:
			 * Scheme: [ContentResolver.SCHEME_ANDROID_RESOURCE]
			 * Authority: Package Name of ringtone title provider
			 * First Path Segment: Type of resource (must be "string")
			 * Second Path Segment: Resource ID of title
			 *
			 * + "title_resource_uri"
			 * + type: String
			 * + Hidden
			 * + ReadOnly
			 *
			 * @hide
			 */
			@Deprecated(message = "", level = DeprecationLevel.HIDDEN)
			protected val TITLE_RESOURCE_URI = "title_resource_uri"

			object Genres {

				val DEFAULT_SORT_ORDER: String = MediaStore.Audio.Genres.DEFAULT_SORT_ORDER
				val INTERNAL_CONTENT_URI: Uri = MediaStore.Audio.Genres.INTERNAL_CONTENT_URI
				val EXTERNAL_CONTENT_URI: Uri = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI

				/**
				 * Audio genre metadata columns.
				 */
				abstract class GenresColumns : BaseColumns() {

					/**
					 * The name of the genre
					 *
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

				val DEFAULT_SORT_ORDER: String = MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER
				val INTERNAL_CONTENT_URI: Uri = MediaStore.Audio.Playlists.INTERNAL_CONTENT_URI
				val EXTERNAL_CONTENT_URI: Uri = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI

				/**
				 * Audio playlist metadata columns.
				 */
				abstract class PlaylistsColumns : BaseColumns() {
					/**
					 * The name of the playlist
					 *
					 * + "name"
					 * + type: String
					 */
					val NAME = MediaStore.Audio.PlaylistsColumns.NAME

					/**
					 * Path to the playlist file on disk.
					 *
					 *
					 * Note that apps may not have filesystem permissions to directly
					 * access this path. Instead of trying to open this path directly,
					 * apps should use
					 * [ContentResolver.openFileDescriptor] to gain
					 * access.
					 *
					 * + "_data"
					 * + type: String
					 *
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
					val DATA = MediaStore.Audio.PlaylistsColumns.DATA

					/**
					 * The time the media item was first added.
					 *
					 * + "date_added"
					 * + type: Long
					 * + ReadOnly
					 */
					@DurationValue(TimeUnit.SECONDS)
					val DATE_ADDED = MediaStore.Audio.PlaylistsColumns.DATE_ADDED

					/**
					 * The time the media item was last modified.
					 *
					 * + "date_modified"
					 * + type: Long
					 * + ReadOnly
					 */
					@DurationValue(TimeUnit.SECONDS)
					val DATE_MODIFIED = MediaStore.Audio.PlaylistsColumns.DATE_MODIFIED

					companion object : PlaylistsColumns() {
						// convenient reference
					}
				}
			}

			object Artist {

				val DEFAULT_SORT_ORDER: String = MediaStore.Audio.Artists.DEFAULT_SORT_ORDER
				val INTERNAL_CONTENT_URI: Uri = MediaStore.Audio.Artists.INTERNAL_CONTENT_URI
				val EXTERNAL_CONTENT_URI: Uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI

				/**
				 * Audio artist metadata columns.
				 */
				abstract class ArtistColumns : BaseColumns() {

					/**
					 * The artist who created the audio file, if any
					 *
					 * + "artist"
					 * + type: String
					 * + ReadOnly
					 */
					val ARTIST = MediaStore.Audio.ArtistColumns.ARTIST

					/**
					 * A non human readable key calculated from the ARTIST, used for
					 * searching, sorting and grouping
					 *
					 * + "artist_key"
					 * + type: String
					 * + ReadOnly
					 */
					val ARTIST_KEY = MediaStore.Audio.ArtistColumns.ARTIST_KEY

					/**
					 * The number of albums in the database for this artist
					 *
					 * + "number_of_albums"
					 * + type: String
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

					companion object : ArtistColumns() {
						// convenient reference
					}
				}
			}

			object Album {

				val DEFAULT_SORT_ORDER: String = MediaStore.Audio.Albums.DEFAULT_SORT_ORDER
				val INTERNAL_CONTENT_URI: Uri = MediaStore.Audio.Albums.INTERNAL_CONTENT_URI
				val EXTERNAL_CONTENT_URI: Uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI

				/**
				 * Audio album metadata columns.
				 */
				abstract class AlbumColumns : BaseColumns() {

					/**
					 * The id for the album
					 *
					 * + "album_id"
					 * + type: Int
					 * + ReadOnly
					 */
					val ALBUM_ID = MediaStore.Audio.AlbumColumns.ALBUM_ID

					/**
					 * The album on which the audio file appears, if any
					 *
					 * + "album"
					 * + type: Int
					 * + ReadOnly
					 */
					val ALBUM = MediaStore.Audio.AlbumColumns.ALBUM

					/**
					 * The ID of the artist whose songs appear on this album.
					 *
					 * + "artist_id"
					 * + type: Int
					 * + ReadOnly
					 */
					val ARTIST_ID = MediaStore.Audio.AlbumColumns.ARTIST_ID

					/**
					 * The name of the artist whose songs appear on this album.
					 *
					 * + "artist"
					 * + type: Int
					 * + ReadOnly
					 */
					val ARTIST = MediaStore.Audio.Artists.ARTIST

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
					 * + type: Int
					 * + ReadOnly
					 */
					val ALBUM_KEY = MediaStore.Audio.AlbumColumns.ALBUM_KEY

					/**
					 * Cached album art.
					 *
					 * + "album_art"
					 * + type: String
					 */
					@Deprecated(
						message =
						"""
				Apps may not have filesystem permissions to directly access this path.
				Instead of trying to open this path directly,
				apps should use {@link ContentResolver#loadThumbnail} to gain access.
				""",
						level = DeprecationLevel.WARNING
					)
					val ALBUM_ART = "album_art"

					companion object {
						// convenient reference
					}
				}
			}

			companion object : AudioColumns() {
				// convenient reference
			}
		}
	}

	object Download {

		val INTERNAL_CONTENT_URI: Uri = MediaStore.Downloads.INTERNAL_CONTENT_URI
		val EXTERNAL_CONTENT_URI: Uri = MediaStore.Downloads.EXTERNAL_CONTENT_URI

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
			 * + removed
			 *
			 * @removed
			 */
			@Deprecated(message = "removed", level = DeprecationLevel.HIDDEN)
			protected val DESCRIPTION = "description"

			companion object : DownloadColumns() {
				// convenient reference
			}
		}
	}

	object Images {

		val DEFAULT_SORT_ORDER: String = MediaStore.Images.Media.DEFAULT_SORT_ORDER
		val INTERNAL_CONTENT_URI: Uri = MediaStore.Images.Media.INTERNAL_CONTENT_URI
		val EXTERNAL_CONTENT_URI: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

		abstract class ImageColumns : MediaColumns() {
			// Promoted to MediaColumns
			/*
				DATE_TAKEN
				ORIENTATION
				BUCKET_ID
				BUCKET_DISPLAY_NAME
				GROUP_ID
			 */

			/**
			 * The description of the image
			 *
			 * + "description"
			 * + type: String
			 * + ReadOnly
			 */
			val DESCRIPTION = MediaStore.Images.ImageColumns.DESCRIPTION

			/**
			 * The picasa id of the image
			 *
			 * + "picasa_id"
			 * + type: String
			 *
			 */
			@Deprecated(
				message =
				"""
				this value was only relevant for images hosted on Picasa, which are no longer supported.
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
				location details are no longer indexed for privacy
        reasons, and this value is now always {@code null}.
        You can still manually obtain location metadata using
        {@link ExifInterface#getLatLong(float[])}.
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
				message =
				"""
				location details are no longer indexed for privacy
        reasons, and this value is now always {@code null}.
         You can still manually obtain location metadata using
         {@link ExifInterface#getLatLong(float[])}.
			""",
				level = DeprecationLevel.WARNING
			)
			val LONGITUDE = MediaStore.Images.ImageColumns.LONGITUDE

			/**
			 * The mini thumb id.
			 *
			 * + "mini_thumb_magic"
			 * + type: Int
			 *
			 */
			@Deprecated(
				message =
				"""
				all thumbnails should be obtained via
        {@link MediaStore.Images.Thumbnails#getThumbnail}, as this
         value is no longer supported.
			""",
				level = DeprecationLevel.WARNING
			)
			val MINI_THUMB_MAGIC = MediaStore.Images.ImageColumns.MINI_THUMB_MAGIC

			companion object : ImageColumns() {
				// convenient reference
			}

		}
	}

	object Video {

		val DEFAULT_SORT_ORDER: String = MediaStore.Video.Media.DEFAULT_SORT_ORDER
		val INTERNAL_CONTENT_URI: Uri = MediaStore.Video.Media.INTERNAL_CONTENT_URI
		val EXTERNAL_CONTENT_URI: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

		abstract class VideoColumns : MediaColumns() {
			// Promoted to MediaColumns
			/*
				DURATION
				DATE_TAKEN
				BUCKET_ID
				BUCKET_DISPLAY_NAME
				GROUP_ID
			*/

			/**
			 * The artist who created the video file, if any
			 *
			 * + "artist"
			 * + type: String
			 * + ReadOnly
			 */
			val ARTIST = MediaStore.Video.VideoColumns.ARTIST

			/**
			 * The album the video file is from, if any
			 *
			 * + "album"
			 * + type: String
			 * + ReadOnly
			 */
			val ALBUM = MediaStore.Video.VideoColumns.ALBUM

			/**
			 * The resolution of the video file, formatted as "XxY"
			 *
			 * + "resolution"
			 * + type: String
			 * + ReadOnly
			 */
			val RESOLUTION = MediaStore.Video.VideoColumns.RESOLUTION

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
			 * + type: String
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
			location details are no longer indexed for privacy reasons,
			and this value is now always {@code null}.
      You can still manually obtain location metadata using {@link ExifInterface#getLatLong(float[])}.
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
			location details are no longer indexed for privacy reasons,
			and this value is now always {@code null}.
      You can still manually obtain location metadata using {@link ExifInterface#getLatLong(float[])}.
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
			all thumbnails should be obtained via {@link MediaStore.Images.Thumbnails#getThumbnail},
			as this value is no longer supported.
		""",
				level = DeprecationLevel.WARNING
			)
			val MINI_THUMB_MAGIC = MediaStore.Video.VideoColumns.MINI_THUMB_MAGIC

			/**
			 * The position within the video item at which playback should be
			 * resumed.
			 *
			 * + "bookmark"
			 * + type: Int
			 */
			@DurationValue(TimeUnit.MILLISECONDS)
			val BOOKMARK = MediaStore.Video.VideoColumns.BOOKMARK

			/**
			 * The standard of color aspects
			 *
			 * + "color_standard"
			 * + type: Int
			 * + ReadOnly
			 * @hide
			 */
			protected val COLOR_STANDARD = "color_standard"

			/**
			 * The transfer of color aspects
			 *
			 * + "color_transfer"
			 * + type: Int
			 * +ReadOnly
			 * @hide
			 */
			protected val COLOR_TRANSFER = "color_transfer"

			/**
			 * The range of color aspects
			 *
			 * + "color_range"
			 * + type: Int
			 * + ReadOnly
			 * @hide
			 */
			protected val COLOR_RANGE = "color_range"

			companion object : VideoColumns() {
				// convenient reference
			}

		}
	}
}
