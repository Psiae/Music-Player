package com.kylentt.mediaplayer.core.util

// Just a note, Nothing else

/** Currently the service is started (instantiated and onCreate) when an app builds a controller
 * and binds to the service. As long as the controller is not released, the service is in a bound
 * state and will never be stopped by the system. When the controller is released,the services
 * looses the bound state and it continues in the foreground when the player is still playing
 * (playWhenReady=true). If not playing it is stopSelf'd. This is due to the restrictions for
 * background services since O (API 26) or S (API 31) */

/** The localConfiguration property is removed when it is sent
 * from a MediaController to the MediaSession.  */

// TODO: Make this app has `Flow` & smooth like feeling :)
// TODO: Separate Controller, UI & Presenter use MediaController. Service use ExoPlayer
// TODO: Find Applicable MediaBrowser use case
// TODO: Remote Downloadable Source ( no Streaming for now, but will be implemented for myself )
// TODO: Make Command go one place then decouple if necessary