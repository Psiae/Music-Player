package com.kylentt.musicplayer.common.kotlin.list

fun <T> List<T>?.orEmpty() = this ?: emptyList()
