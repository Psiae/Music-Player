package com.kylentt.mediaplayer.core.util

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
    // control such as setMediaItems must be done via Controller
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

// AndroidQ Color os : FileName, ByteSize, _DATA