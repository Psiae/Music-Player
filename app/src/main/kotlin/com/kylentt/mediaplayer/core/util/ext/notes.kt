package com.kylentt.mediaplayer.core.util.ext

// Just a note.kt, Nothing else

// Github notes
/** Currently the service is started (instantiated and onCreate) when an app builds a controller
 * and binds to the service. As long as the controller is not released, the service is in a bound
 * state and will never be stopped by the system. When the controller is released,the services
 * looses the bound state and it continues in the foreground when the player is still playing
 * (playWhenReady=true). If not playing it is stopSelf'd. This is due to the restrictions for
 * background services since O (API 26) or S (API 31) */

/** The localConfiguration property is removed when it is sent
 * from a MediaController to the MediaSession.  */

// Todos
// TODO: Make this app has `Flow` & smooth like feeling :)
// TODO: Separate Controller. UI & Presenter use MediaController. Service use Scoped ExoPlayer
    // In case Presenter must command ExoPlayer directly, should be done via broadcast.
    // control such as setMediaItems must be done via Controller, however if its not possible
    // then can be done through lambda listener but broadcast is a must to access service
// TODO: Find Applicable MediaBrowser use case
    // Might not be Implemented until Remote Source is Implemented
// TODO: Remote Downloadable Source ( no Streaming for now, but will be implemented for myself )
    // Because of Firebase Storage Daily Limitation, Unless someone gimme money ofc
// TODO: Make Command go one place then decouple if necessary
// TODO: Make Customizable Queue
    // Since both UI & Exoplayer use MediaItems this should be possible with smooth transition
// TODO: PlayerWrapper sync fix
    // Idk what might be causing it so wait for further Media3 release for now
// TODO: Handle Intent Filter
    // Handle Intent when user want to use this App as Media Player
// TODO: Handle other app provider & make my own
    // Handle Uri provider from Intent, especially those custom one such as ColorOS

// Idk, but useful

// MediaStore External Content Uri column
/*
2022-03-12 06:38:58.251 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column title_key
2022-03-12 06:38:58.251 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column instance_id
2022-03-12 06:38:58.251 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column compilation
2022-03-12 06:38:58.267 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column disc_number
2022-03-12 06:38:58.269 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column duration
2022-03-12 06:38:58.269 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column is_ringtone
2022-03-12 06:38:58.270 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column album_artist
2022-03-12 06:38:58.277 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column resolution
2022-03-12 06:38:58.278 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column orientation
2022-03-12 06:38:58.278 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column artist
2022-03-12 06:38:58.278 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column author
2022-03-12 06:38:58.279 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column height
2022-03-12 06:38:58.279 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column is_drm
2022-03-12 06:38:58.280 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column bucket_display_name
2022-03-12 06:38:58.280 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column is_audiobook
2022-03-12 06:38:58.281 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column owner_package_name
2022-03-12 06:38:58.282 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column volume_name
2022-03-12 06:38:58.282 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column title_resource_uri
2022-03-12 06:38:58.283 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column date_modified
2022-03-12 06:38:58.284 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column writer
2022-03-12 06:38:58.284 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column date_expires
2022-03-12 06:38:58.284 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column composer
2022-03-12 06:38:58.291 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column _display_name
2022-03-12 06:38:58.291 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column is_recording
2022-03-12 06:38:58.292 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column datetaken
2022-03-12 06:38:58.292 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column mime_type
2022-03-12 06:38:58.293 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column is_notification
2022-03-12 06:38:58.293 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column bitrate
2022-03-12 06:38:58.295 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column cd_track_number
2022-03-12 06:38:58.296 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column _id
2022-03-12 06:38:58.297 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column xmp
2022-03-12 06:38:58.297 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column year
2022-03-12 06:38:58.297 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column _data
2022-03-12 06:38:58.298 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column _size
2022-03-12 06:38:58.299 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column album
2022-03-12 06:38:58.299 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column genre
2022-03-12 06:38:58.300 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column is_alarm
2022-03-12 06:38:58.301 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column title
2022-03-12 06:38:58.301 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column track
2022-03-12 06:38:58.301 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column width
2022-03-12 06:38:58.308 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column is_music
2022-03-12 06:38:58.308 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column album_key
2022-03-12 06:38:58.309 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column is_favorite
2022-03-12 06:38:58.309 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column is_trashed
2022-03-12 06:38:58.309 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column group_id
2022-03-12 06:38:58.310 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column document_id
2022-03-12 06:38:58.310 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column artist_id
2022-03-12 06:38:58.311 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column generation_added
2022-03-12 06:38:58.311 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column artist_key
2022-03-12 06:38:58.311 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column genre_key
2022-03-12 06:38:58.311 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column is_download
2022-03-12 06:38:58.312 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column generation_modified
2022-03-12 06:38:58.312 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column is_pending
2022-03-12 06:38:58.312 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column date_added
2022-03-12 06:38:58.313 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column is_podcast
2022-03-12 06:38:58.313 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column capture_framerate
2022-03-12 06:38:58.313 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column album_id
2022-03-12 06:38:58.313 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column num_tracks
2022-03-12 06:38:58.314 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column original_document_id
2022-03-12 06:38:58.314 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column genre_id
2022-03-12 06:38:58.314 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column bucket_id
2022-03-12 06:38:58.315 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column bookmark
2022-03-12 06:38:58.315 20024-20070/com.kylentt.mediaplayer D/LocalSourceImpl: fetchSongFromMediaStore Column relative_path

 */

// content providers column for Intent Handling playlist
/*
29012-29054/com.kylentt.mediaplayer D/MainActivity$onCreate: MainActivity column 0 = document_id
29012-29054/com.kylentt.mediaplayer D/MainActivity$onCreate: MainActivity column 1 = mime_type
29012-29054/com.kylentt.mediaplayer D/MainActivity$onCreate: MainActivity column 2 = _display_name
29012-29054/com.kylentt.mediaplayer D/MainActivity$onCreate: MainActivity column 3 = summary
29012-29054/com.kylentt.mediaplayer D/MainActivity$onCreate: MainActivity column 4 = last_modified
29012-29054/com.kylentt.mediaplayer D/MainActivity$onCreate: MainActivity column 5 = flags
29012-29054/com.kylentt.mediaplayer D/MainActivity$onCreate: MainActivity column 6 = _size
 */

// AndroidQ ColorOS : FileName, ByteSize, _DATA