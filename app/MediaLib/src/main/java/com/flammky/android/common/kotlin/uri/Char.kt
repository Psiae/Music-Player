package com.flammky.android.common.kotlin.uri

import android.net.Uri
import androidx.core.net.toUri
import com.flammky.common.kotlin.string.notNullOrEmptyToString

fun CharSequence?.notNullOrEmptyToUri(): Uri = notNullOrEmptyToString().toUri()