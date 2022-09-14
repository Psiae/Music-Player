package com.flammky.common.kotlin.charsequence

fun CharSequence?.notNullOrEmpty(): CharSequence = this ?: ""
