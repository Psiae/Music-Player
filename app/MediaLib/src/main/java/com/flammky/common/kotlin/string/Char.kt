package com.flammky.common.kotlin.string

fun CharSequence?.notNullOrEmptyToString(): String = (this ?: "").toString()