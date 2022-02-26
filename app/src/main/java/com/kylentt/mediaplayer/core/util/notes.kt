package com.kylentt.mediaplayer.core.util

// Just a note, Nothing else

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
// TODO: Separate Controller, UI & Presenter use MediaController. Service use Scoped ExoPlayer
// TODO: Find Applicable MediaBrowser use case
// TODO: Remote Downloadable Source ( no Streaming for now, but will be implemented for myself )
// TODO: Make Command go one place then decouple if necessary
// TODO: Make Customizable Queue
// TODO: PlayerWrapper sync fix

// Idk, but useful
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