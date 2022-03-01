package com.kylentt.mediaplayer.core.util

object Constants {

    // Song
    const val ALBUM_ART_PATH = "content://media/external/audio/albumart"

    const val SONG_DATA = "data"
    const val SONG_BYTE = "byteSize"
    const val SONG_FILE_NAME = "fileName"
    const val SONG_FILE_PARENT = "fileParent"
    const val SONG_FILE_PARENT_ID = "fileParentId"
    const val SONG_LAST_MODIFIED = "lastModified"

    // Service
    const val MEDIA_SESSION_ID = "Kylentt"
    const val NOTIFICATION_CHANNEL_ID = "Ky_NOTIFICATION_ID"
    const val NOTIFICATION_ID = 301
    const val NOTIFICATION_NAME = "Ky_NOTIFICATION"

    const val ACTION = "ACTION"
    const val ACTION_CANCEL = "ACTION_CANCEL"
    const val ACTION_PREV = "ACTION_PREV"
    const val ACTION_PLAY = "ACTION_PLAY"
    const val ACTION_PAUSE = "ACTION_PAUSE"
    const val ACTION_NEXT = "ACTION_NEXT"
    const val ACTION_UNIT = "ACTION_UNIT"
    const val ACTION_FADE ="ACTION_FADE"

    const val ACTION_REPEAT_OFF_TO_ONE = "ACTION_REPEAT_OFF_TO_ONE"
    const val ACTION_REPEAT_ONE_TO_ALL = "ACTION_REPEAT_ONE_TO_ALL"
    const val ACTION_REPEAT_ALL_TO_OFF = "ACTION_REPEAT_ALL_TO_OFF"

    const val ACTION_CANCEL_CODE = 400
    const val ACTION_PLAY_CODE = 401
    const val ACTION_PAUSE_CODE = 402
    const val ACTION_NEXT_CODE = 403
    const val ACTION_PREV_CODE = 404
    const val ACTION_REPEAT_OFF_TO_ONE_CODE = 405
    const val ACTION_REPEAT_ONE_TO_ALL_CODE = 406
    const val ACTION_REPEAT_ALL_TO_OFF_CODE = 407

    const val PLAYBACK_INTENT = "com.kylennt.mediaplayer.PLAYBACK_INTENT"

}