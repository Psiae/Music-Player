package com.kylentt.musicplayer.common.extenstions

fun <T> List<T>?.orEmpty() = this ?: emptyList()
