package com.flammky.common.kotlin.charsequence

import android.net.Uri
import androidx.core.net.toUri

fun CharSequence?.orEmpty(): CharSequence = this ?: ""
fun CharSequence?.orEmptyString(): String = (this ?: "").toString()
fun CharSequence?.orEmptyUri(): Uri = orEmptyString().toUri()
