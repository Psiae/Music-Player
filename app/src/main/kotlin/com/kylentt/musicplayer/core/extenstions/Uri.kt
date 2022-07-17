package com.kylentt.musicplayer.core.extenstions

import android.net.Uri

fun Uri?.orEmpty(): Uri = this ?: Uri.EMPTY
fun Uri?.orEmptyString(): String = this?.toString() ?: ""
