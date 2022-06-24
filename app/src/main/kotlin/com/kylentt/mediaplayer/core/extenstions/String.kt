package com.kylentt.mediaplayer.core.extenstions

fun String.notEmptyOrNull(): String? = this.ifEmpty { null }
