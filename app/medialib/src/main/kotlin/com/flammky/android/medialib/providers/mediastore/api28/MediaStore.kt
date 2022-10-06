package com.flammky.android.medialib.providers.mediastore.api28

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.flammky.android.medialib.temp.provider.mediastore.base.BaseColumns
import com.flammky.common.kotlin.time.annotation.DurationValue
import java.util.concurrent.TimeUnit

/**
 *  [GoogleSource](https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-9.0.0_r61/core/java/android/provider/MediaStore.java)
 */

object MediaStore28 {

	init {
		/*check(!VersionHelper.hasQ()) {
			val n = if (VersionHelper.isPie()) "MediaStore29" else "MediaStore30"
			"Invalid Usage of API 28 on API ${Build.VERSION.SDK_INT}, use $n instead"
		}*/
	}

	fun getVersion(context: Context): String = MediaStore.getVersion(context)

	abstract class MediaColumns : BaseColumns() {

		/**
		 * Path to the file on disk.
		 *
		 *
		 * Note that apps may not have filesystem permissions to directly access
		 * this path. Instead of trying to open this path directly, apps should
		 * use [ContentResolver.openFileDescriptor] to gain
		 * access.
		 *
		 * "_data"
		 * Type: String
		 */
		val DATA: String = MediaStore.MediaColumns.DATA

		/**
		 * The size of the file in bytes
		 *
		 * "_size"
		 * Type: Long
		 */
		val SIZE: String = MediaStore.MediaColumns.SIZE

		/**
		 * The display name of the file
		 *
		 * "_display_name"
		 * Type: String
		 */
		val DISPLAY_NAME: String = MediaStore.MediaColumns.DISPLAY_NAME

		/**
		 * The title of the content
		 *
		 * "title"
		 * Type: String
		 */
		val TITLE: String = MediaStore.MediaColumns.TITLE

		/**
		 * The time the file was added to the media provider
		 * Units are seconds since 1970.
		 *
		 * "date_added"
		 * Type: Long
		 */
		@DurationValue(TimeUnit.SECONDS)
		val DATE_ADDED: String = MediaStore.MediaColumns.DATE_ADDED

		/**
		 * The time the file was last modified
		 * Units are seconds since 1970.
		 * NOTE: This is for internal use by the media scanner.  Do not modify this field.
		 *
		 * "date_modified"
		 * Type: Long
		 */
		@DurationValue(TimeUnit.SECONDS)
		val DATE_MODIFIED: String = MediaStore.MediaColumns.DATE_MODIFIED

		/**
		 * The MIME type of the file
		 * "mime_type"
		 * Type: String
		 */
		val MIME_TYPE: String = MediaStore.MediaColumns.MIME_TYPE

		/**
		 * The width of the image/video in pixels.
		 * "width"
		 * Type: Long
		 */
		val WIDTH: String = MediaStore.MediaColumns.WIDTH

		/**
		 * The height of the image/video in pixels.
		 * "height"
		 * Type: Long
		 */
		val HEIGHT: String = MediaStore.MediaColumns.HEIGHT

		/**
		 * The MTP object handle of a newly transfered file.
		 * Used to pass the new file's object handle through the media scanner
		 * from MTP to the media provider
		 * For internal use only by MTP, media scanner and media provider.
		 * <P>Type: INTEGER</P>
		 * @hide
		 */

		protected val MEDIA_SCANNER_NEW_OBJECT_ID: String = "media_scanner_new_object_id"

		/**
		 * Non-zero if the media file is drm-protected
		 * <P>Type: INTEGER (boolean)</P>
		 * @hide
		 */
		protected val IS_DRM: String = "is_drm"

		companion object :
			MediaColumns() {
			// convenient reference
		}

	}

	object Files {

		private val EXTERNAL_CONTENT_URI: Uri? = MediaStore.Files.getContentUri("external")

		/**
		 * Fields for master table for all media files.
		 * Table also contains MediaColumns._ID, DATA, SIZE and DATE_MODIFIED.
		 */

		abstract class FileColumns :
			MediaColumns() {

			/**
			 * The media type (audio, video, image or playlist)
			 * of the file, or 0 for not a media file
			 *
			 * "media_type"
			 * Type: Int
			 */
			val MEDIA_TYPE = MediaStore.Files.FileColumns.MEDIA_TYPE

			/**
			 * The index of the parent directory of the file
			 *
			 * "parent"
			 * Type: Int
			 */
			val PARENT = MediaStore.Files.FileColumns.PARENT

			/**
			 * The MTP storage ID of the file
			 *
			 * "storage_id"
			 * Type: Int
			 */
			protected val STORAGE_ID = "storage_id"

			/**
			 * The MTP format code of the file
			 *
			 * "format"
			 * Type: Int
			 */
			protected val FORMAT = "format"

			companion object :
				FileColumns() {

				// convenient reference

				/**
				 * Constant for the [.MEDIA_TYPE] column indicating that file
				 * is not an audio, image, video or playlist file.
				 *
				 * + 0
				 */
				const val MEDIA_TYPE_NONE = MediaStore.Files.FileColumns.MEDIA_TYPE_NONE

				/**
				 * Constant for the [.MEDIA_TYPE] column indicating that file is an image file.
				 *
				 * + 1
				 */
				const val MEDIA_TYPE_IMAGE = MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE

				/**
				 * Constant for the [.MEDIA_TYPE] column indicating that file is an audio file.
				 *
				 * + 2
				 */
				const val MEDIA_TYPE_AUDIO = MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO

				/**
				 * Constant for the [.MEDIA_TYPE] column indicating that file is a video file.
				 *
				 * + 3
				 */
				const val MEDIA_TYPE_VIDEO = MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO

				/**
				 * Constant for the [.MEDIA_TYPE] column indicating that file is a playlist file.
				 *
				 * + 4
				 */
				const val MEDIA_TYPE_PLAYLIST = MediaStore.Files.FileColumns.MEDIA_TYPE_PLAYLIST
			}
		}
	}

	object Images {

		val DEFAULT_SORT_ORDER = MediaStore.Images.Media.DEFAULT_SORT_ORDER
		val EXTERNAL_CONTENT_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
		val INTERNAL_CONTENT_URI = MediaStore.Images.Media.INTERNAL_CONTENT_URI

		abstract class ImageColumns :
			MediaColumns() {

			/**
			 * The description of the image
			 *
			 * "description"
			 * Type: String
			 */
			val DESCRIPTION = MediaStore.Images.ImageColumns.DESCRIPTION

			/**
			 * The picasa id of the image
			 *
			 * "picasa_id"
			 * Type: String
			 */
			val PICASA_ID = MediaStore.Images.ImageColumns.PICASA_ID

			/**
			 * Whether the video should be published as public or private
			 *
			 * "isprivate"
			 * Type: Int
			 */
			val IS_PRIVATE = MediaStore.Images.ImageColumns.IS_PRIVATE

			/**
			 * The latitude where the image was captured.
			 *
			 * "latitude"
			 * Type: Double
			 */
			val LATITUDE = MediaStore.Images.ImageColumns.LATITUDE

			/**
			 * The longitude where the image was captured.
			 *
			 * "longitude"
			 * Type: Double
			 */
			val LONGITUDE = MediaStore.Images.ImageColumns.LONGITUDE

			/**
			 * The date & time that the image was taken in units
			 * of milliseconds since jan 1, 1970.
			 *
			 * "datetaken"
			 * Type: Int
			 */
			@DurationValue(TimeUnit.MILLISECONDS)
			val DATE_TAKEN = MediaStore.Images.ImageColumns.DATE_TAKEN

			/**
			 * The orientation for the image expressed as degrees.
			 * Only degrees 0, 90, 180, 270 will work.
			 *
			 * "orientation"
			 * Type: Int
			 */
			val ORIENTATION = MediaStore.Images.ImageColumns.ORIENTATION

			/**
			 * The mini thumb id.
			 *
			 * "mini_thumb_magic"
			 * Type: Int
			 */
			val MINI_THUMB_MAGIC = MediaStore.Images.ImageColumns.MINI_THUMB_MAGIC

			/**
			 * The bucket id of the image. This is a read-only property that
			 * is automatically computed from the DATA column.
			 *
			 * "bucket_id"
			 * Type: String
			 */
			val BUCKET_ID = MediaStore.Images.ImageColumns.BUCKET_ID

			/**
			 * The bucket display name of the image. This is a read-only property that
			 * is automatically computed from the DATA column.
			 *
			 * "bucket_display_name"
			 * Type: String
			 */
			val BUCKET_DISPLAY_NAME = MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME

			companion object :
				ImageColumns() {
				// convenient reference
			}

		}
	}

	object Audio {

		val DEFAULT_SORT_ORDER = MediaStore.Audio.Media.DEFAULT_SORT_ORDER
		val EXTERNAL_CONTENT_URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
		val INTERNAL_CONTENT_URI = MediaStore.Audio.Media.INTERNAL_CONTENT_URI

		/**
		 * Columns for audio file that show up in multiple tables.
		 */

		abstract class AudioColumns :
			MediaColumns() {

			/**
			 * A non human readable key calculated from the TITLE, used for
			 * searching, sorting and grouping
			 *
			 * "title_key"
			 * Type: String
			 */
			val TITLE_KEY = MediaStore.Audio.AudioColumns.TITLE_KEY

			/**
			 * The duration of the audio file, in ms
			 *
			 * "duration"
			 * Type: Long
			 */
			val DURATION = MediaStore.Audio.AudioColumns.DURATION

			/**
			 * The position, in ms, playback was at when playback for this file
			 * was last stopped.
			 *
			 * "bookmark"
			 * Type: Long
			 */
			val BOOKMARK = MediaStore.Audio.AudioColumns.BOOKMARK

			/**
			 * The id of the artist who created the audio file, if any
			 *
			 * "artist_id"
			 * Type: Long
			 */
			val ARTIST_ID = MediaStore.Audio.AudioColumns.ARTIST_ID

			/**
			 * The artist who created the audio file, if any
			 *
			 * "artist"
			 * Type: String
			 */
			val ARTIST = MediaStore.Audio.AudioColumns.ARTIST

			/**
			 * A non human readable key calculated from the ARTIST, used for
			 * searching, sorting and grouping
			 *
			 * "artist_key"
			 * Type: String
			 */
			val ARTIST_KEY = MediaStore.Audio.AudioColumns.ARTIST_KEY

			/**
			 * The composer of the audio file, if any
			 *
			 * "composer"
			 * Type: String
			 */
			val COMPOSER = MediaStore.Audio.AudioColumns.COMPOSER

			/**
			 * The id of the album the audio file is from, if any
			 *
			 * "album_id"
			 * Type: Long
			 */
			val ALBUM_ID = MediaStore.Audio.AudioColumns.ALBUM_ID

			/**
			 * The album the audio file is from, if any
			 *
			 * "album"
			 * Type: String
			 */
			val ALBUM = MediaStore.Audio.AudioColumns.ALBUM

			/**
			 * A non human readable key calculated from the ALBUM, used for
			 * searching, sorting and grouping
			 *
			 * "album_key"
			 * Type: String
			 */
			val ALBUM_KEY = MediaStore.Audio.AudioColumns.ALBUM_KEY

			/**
			 * The track number of this song on the album, if any.
			 * This number encodes both the track number and the
			 * disc number. For multi-disc sets, this number will
			 * be 1xxx for tracks on the first disc, 2xxx for tracks
			 * on the second disc, etc.
			 *
			 * "track"
			 * Type: Int
			 */
			val TRACK = MediaStore.Audio.AudioColumns.TRACK

			/**
			 * The year the audio file was recorded, if any
			 *
			 * "year"
			 * Type: Int
			 */
			val YEAR = MediaStore.Audio.AudioColumns.YEAR

			/**
			 * Non-zero if the audio file is music
			 *
			 * "is_music"
			 * Type: Int (boolean)
			 */
			val IS_MUSIC = MediaStore.Audio.AudioColumns.IS_MUSIC

			/**
			 * Non-zero if the audio file is a podcast
			 *
			 * "is_podcast"
			 * Type: Int (boolean)
			 */
			val IS_PODCAST = MediaStore.Audio.AudioColumns.IS_PODCAST

			/**
			 * Non-zero if the audio file may be a ringtone
			 *
			 * "is_ringtone"
			 * Type: Int (boolean)
			 */
			val IS_RINGTONE = MediaStore.Audio.AudioColumns.IS_RINGTONE

			/**
			 * Non-zero if the audio file may be an alarm
			 *
			 * "is_alarm"
			 * Type: Int (boolean)
			 */
			val IS_ALARM = MediaStore.Audio.AudioColumns.IS_ALARM

			/**
			 * Non-zero if the audio file may be a notification sound
			 *
			 * "is_notification"
			 * Type: Int (boolean)
			 */
			val IS_NOTIFICATION = MediaStore.Audio.AudioColumns.IS_NOTIFICATION

			/**
			 * The genre of the audio file, if any
			 *
			 * "genre"
			 * Type: String
			 *
			 * Does not exist in the database - only used by the media scanner for inserts.
			 */
			@RequiresApi(Build.VERSION_CODES.R)
			protected val GENRE = "genre"

			/**
			 * The resource URI of a localized title, if any
			 *
			 * "title_resource_uri"
			 * Type: String
			 *
			 * Conforms to this pattern:
			 * Scheme: [ContentResolver.SCHEME_ANDROID_RESOURCE]
			 * Authority: Package Name of ringtone title provider
			 * First Path Segment: Type of resource (must be "string")
			 * Second Path Segment: Resource ID of title
			 */
			protected val TITLE_RESOURCE_URI = "title_resource_uri"

			/**
			 * The artist credited for the album that contains the audio file
			 *
			 * "album_artist"
			 * Type: String
			 */
			@RequiresApi(Build.VERSION_CODES.R)
			protected open val ALBUM_ARTIST = "album_artist"

			/**
			 * Whether the song is part of a compilation
			 *
			 * "compilation"
			 * Type: String
			 */
			@RequiresApi(Build.VERSION_CODES.R)
			protected val COMPILATION = "compilation"

			companion object :
				AudioColumns() {
				// convenient reference
			}

		}

		object Genres {

			val DEFAULT_SORT_ORDER = MediaStore.Audio.Genres.DEFAULT_SORT_ORDER
			val EXTERNAL_CONTENT_URI = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI
			val INTERNAL_CONTENT_URI = MediaStore.Audio.Genres.INTERNAL_CONTENT_URI

			/**
			 * Columns representing an audio genre
			 */

			abstract class GenresColumns private constructor() : BaseColumns() {

				/**
				 * + The name of the genre
				 * + type: String
				 */
				val NAME = MediaStore.Audio.GenresColumns.NAME

				companion object :
					GenresColumns() {
					// convenient reference
				}

			}
		}

		object Playlist {

			val DEFAULT_SORT_ORDER = MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER
			val EXTERNAL_CONTENT_URI = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI
			val INTERNAL_CONTENT_URI = MediaStore.Audio.Playlists.INTERNAL_CONTENT_URI

			/**
			 * Columns representing a playlist
			 */
			abstract class PlaylistsColumns private constructor() : BaseColumns() {

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
				 * + TYPE: String
				 *
				 */
				val DATA = MediaStore.Audio.PlaylistsColumns.DATA

				/**
				 * The time the file was added to the media provider
				 * Units are seconds since 1970.
				 *
				 * + "date_added"
				 * + type: Long
				 *
				 */
				@DurationValue(TimeUnit.SECONDS)
				val DATE_ADDED = MediaStore.Audio.AudioColumns.DATE_ADDED

				/**
				 * The time the file was last modified
				 * Units are seconds since 1970.
				 * NOTE: This is for internal use by the media scanner.  Do not modify this field.
				 *
				 *  + "date_modified"
				 *  + type: Long
				 */
				@DurationValue(TimeUnit.SECONDS)
				val DATE_MODIFIED = MediaStore.Audio.AudioColumns.DATE_MODIFIED

				companion object :
					PlaylistsColumns() {
					// convenient reference
				}

			}
		}

		object Artist {

			val DEFAULT_SORT_ORDER = MediaStore.Audio.Artists.DEFAULT_SORT_ORDER
			val EXTERNAL_CONTENT_URI = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI
			val INTERNAL_CONTENT_URI = MediaStore.Audio.Artists.INTERNAL_CONTENT_URI

			/**
			 * Columns representing an artist
			 */

			abstract class ArtistColumns private constructor() : BaseColumns() {

				/**
				 * The artist who created the audio file, if any
				 *
				 * + "artist"
				 * + type: String
				 */
				val ARTIST = MediaStore.Audio.ArtistColumns.ARTIST

				/**
				 * A non human readable key calculated from the ARTIST, used for
				 * searching, sorting and grouping
				 *
				 * + "artist_key"
				 * + type: String
				 */
				val ARTIST_KEY = MediaStore.Audio.ArtistColumns.ARTIST_KEY

				/**
				 * The number of albums in the database for this artist
				 *
				 * + "artist_key"
				 * + type: Int
				 */
				val NUMBER_OF_ALBUMS = MediaStore.Audio.ArtistColumns.NUMBER_OF_ALBUMS

				/**
				 * The number of albums in the database for this artist
				 *
				 * + "number_of_tracks"
				 * + type: Int
				 */
				val NUMBER_OF_TRACKS = MediaStore.Audio.ArtistColumns.NUMBER_OF_TRACKS

				companion object :
					ArtistColumns() {
					// convenient reference
				}

			}
		}

		object Album {

			/**
			 * Columns representing an album
			 */

			abstract class AlbumColumns private constructor() : BaseColumns() {

				val DEFAULT_SORT_ORDER = MediaStore.Audio.Albums.DEFAULT_SORT_ORDER
				val EXTERNAL_CONTENT_URI = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
				val INTERNAL_CONTENT_URI = MediaStore.Audio.Albums.INTERNAL_CONTENT_URI

				/**
				 * The id for the album
				 *
				 * + "album_id"
				 * + type: Int
				 */
				val ALBUM_ID = MediaStore.Audio.AlbumColumns.ALBUM_ID

				/**
				 * The album on which the audio file appears, if any
				 *
				 * + "album"
				 * + type: String
				 */
				val ALBUM = MediaStore.Audio.AlbumColumns.ALBUM

				/**
				 * The artist whose songs appear on this album
				 *
				 * + "artist"
				 * + type: String
				 */
				val ARTIST = MediaStore.Audio.AudioColumns.ARTIST

				/**
				 * The number of songs on this album
				 *
				 * + "numsongs"
				 * + type: Int
				 */
				val NUMBER_OF_SONGS = MediaStore.Audio.Albums.NUMBER_OF_SONGS

				/**
				 * This column is available when getting album info via artist,
				 * and indicates the number of songs on the album by the given
				 * artist.
				 *
				 * + "numsongs_by_artist"
				 * + type: Int
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
				 */
				val FIRST_YEAR = MediaStore.Audio.Albums.FIRST_YEAR

				/**
				 * The year in which the latest songs
				 * on this album were released. This will often
				 * be the same as [.FIRST_YEAR], but for compilation albums
				 * they might differ.
				 *
				 * + "maxyear"
				 * + type: Int
				 *
				 */
				val LAST_YEAR = MediaStore.Audio.Albums.FIRST_YEAR

				/**
				 * A non human readable key calculated from the ALBUM, used for
				 * searching, sorting and grouping
				 *
				 * + "album_key"
				 * + type: String
				 */
				val ALBUM_KEY = MediaStore.Audio.Albums.ALBUM_KEY

				/**
				 * Cached album art.
				 *
				 * + "album_art"
				 * + type: String
				 */
				val ALBUM_ART = MediaStore.Audio.Albums.ALBUM_ART

				companion object :
					AlbumColumns() {
					// convenient reference
				}

			}
		}
	}

	object Video {

		val DEFAULT_SORT_ORDER = MediaStore.Video.Media.DEFAULT_SORT_ORDER
		val EXTERNAL_CONTENT_URI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
		val INTERNAL_CONTENT_URI = MediaStore.Video.Media.INTERNAL_CONTENT_URI

		abstract class VideoColumns :
			MediaColumns() {

			/**
			 * The duration of the video file, in ms
			 *
			 * + "duration"
			 * + type: Long
			 */
			@DurationValue(TimeUnit.MILLISECONDS)
			val DURATION = MediaStore.Video.VideoColumns.DURATION

			/**
			 * The artist who created the video file, if any
			 *
			 * + "artist"
			 * + type: String
			 */
			val ARTIST = MediaStore.Video.VideoColumns.ARTIST

			/**
			 * The album the video file is from, if any
			 *
			 * + "album"
			 * + type: String
			 */
			val ALBUM = MediaStore.Video.VideoColumns.ALBUM

			/**
			 * The resolution of the video file, formatted as "XxY"
			 *
			 * + "resolution"
			 * + type: String
			 */
			val RESOLUTION = MediaStore.Video.VideoColumns.RESOLUTION

			/**
			 * The description of the video recording
			 *
			 * + "description"
			 * type: String
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
			 * + type: Double
			 */
			val LATITUDE = MediaStore.Video.VideoColumns.LATITUDE

			/**
			 * The longitude where the video was captured.
			 *
			 * + "longitude"
			 * + type: Double
			 */
			val LONGITUDE = MediaStore.Video.VideoColumns.LONGITUDE

			/**
			 * The date & time that the video was taken in units
			 * of milliseconds since jan 1, 1970.
			 *
			 * + "datetaken"
			 * + type: Long
			 */
			@DurationValue(TimeUnit.MILLISECONDS)
			val DATE_TAKEN = MediaStore.Video.VideoColumns.DATE_TAKEN

			/**
			 * The mini thumb id.
			 *
			 * + "mini_thumb_magic"
			 * + type: Int
			 */
			val MINI_THUMB_MAGIC = MediaStore.Video.VideoColumns.MINI_THUMB_MAGIC

			/**
			 * The bucket id of the video. This is a read-only property that
			 * is automatically computed from the DATA column.
			 *
			 * + "bucket_id"
			 * + type: String
			 */
			val BUCKET_ID = MediaStore.Video.VideoColumns.BUCKET_ID

			/**
			 * The bucket display name of the video. This is a read-only property that
			 * is automatically computed from the DATA column.
			 *
			 * + "bucket_display_name"
			 * + type: String
			 */
			val BUCKET_DISPLAY_NAME = MediaStore.Video.VideoColumns.BUCKET_DISPLAY_NAME

			/**
			 * The bookmark for the video. Time in ms. Represents the location in the video that the
			 * video should start playing at the next time it is opened. If the value is null or
			 * out of the range 0..DURATION-1 then the video should start playing from the
			 * beginning.
			 *
			 * + "bookmark"
			 * + type: Int
			 */
			val BOOKMARK = MediaStore.Video.VideoColumns.BOOKMARK

			companion object :
				VideoColumns() {
				// convenient reference
			}

		}
	}
}
