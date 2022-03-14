package com.kylentt.mediaplayer.core.util

import android.content.Context
import androidx.annotation.MainThread
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C.CONTENT_TYPE_MUSIC
import androidx.media3.common.C.USAGE_MEDIA
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

fun Int.toStrRepeat(): String {
    return when (this) {
        Player.REPEAT_MODE_OFF -> "REPEAT_MODE_OFF"
        Player.REPEAT_MODE_ONE -> "REPEAT_MODE_ONE"
        Player.REPEAT_MODE_ALL -> "REPEAT_MODE_ALL"
        else -> ExoUtil.invalidStr
    }
}

fun Int.toStrState(): String {
    return when (this) {
        Player.STATE_IDLE -> "STATE_IDLE"
        Player.STATE_BUFFERING -> "STATE_BUFFERING"
        Player.STATE_READY -> "STATE_READY"
        Player.STATE_ENDED -> "STATE_ENDED"
        else -> ExoUtil.invalidStr
    }
}

fun String.getIntRepeat(): Int {
    return when (this) {
        "REPEAT_MODE_OFF" -> Player.REPEAT_MODE_OFF
        "REPEAT_MODE_ONE" -> Player.REPEAT_MODE_ONE
        "REPEAT_MODE_ALL" -> Player.REPEAT_MODE_ALL
        else ->ExoUtil.invalidNum
    }
}

fun String.getIntState(): Int {
    return when (this) {
        "STATE_IDLE" -> Player.STATE_IDLE
        "STATE_BUFFERING" -> Player.STATE_BUFFERING
        "STATE_READY" -> Player.STATE_READY
        "STATE_ENDED" -> Player.STATE_ENDED
        else -> ExoUtil.invalidNum
    }
}

@MainThread
object ExoUtil {

    const val invalidNum = -111
    const val invalidStr = "INVALID_STR"

    fun ExoPlayer.getMediaItems(): List<MediaItem> {
        val toReturn = mutableListOf<MediaItem>()
        for (i in 0 until mediaItemCount) {
            toReturn.add(getMediaItemAt(i))
        }
        return toReturn
    }

    fun newExo(context: Context, attr: AudioAttributes = defaultAttr()) = ExoPlayer.Builder(context)
        .setAudioAttributes(attr, true)
        .setHandleAudioBecomingNoisy(true)
        .build()

    private fun defaultAttr() = AudioAttributes.Builder()
        .setContentType(CONTENT_TYPE_MUSIC)
        .setUsage(USAGE_MEDIA)
        .build()
}