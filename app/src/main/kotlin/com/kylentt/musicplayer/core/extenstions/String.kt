package com.kylentt.musicplayer.core.extenstions

fun String.notEmptyOrNull(): String? = this.ifEmpty { null }

fun String.addPrefix(prefix: String): String = prefix + this

fun String.setPrefix(prefix: String): String {
	if (!this.startsWith(prefix)) {
		return this.addPrefix(prefix)
	}
	return this
}
