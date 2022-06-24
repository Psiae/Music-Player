package com.kylentt.mediaplayer.core.extenstions

import android.net.Uri

fun Uri?.orEmpty(): Uri = this ?: Uri.EMPTY
