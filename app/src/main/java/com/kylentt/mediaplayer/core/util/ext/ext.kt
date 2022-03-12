package com.kylentt.mediaplayer.core.util.ext

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri

fun Uri.getEmbed(context: Context): ByteArray? {
    val mtr = MediaMetadataRetriever()
    mtr.setDataSource(context.applicationContext, this)
    return mtr.embeddedPicture
}

fun Any?.isNull() = this == null

fun Int?.orInv() = this ?: -1
fun Long?.orInv() = this ?: -1L

fun Int?.orZero() = this ?: 0
fun Long?.orZero() = this ?: 0L

fun Long.removeSuffix(suffix: String): Long {
    val s = this.toString()
    return if (s.endsWith(suffix)) s.removeSuffix(suffix).toLong() else this
}

fun <T> T?.orDefault(default: T) = this ?: default
