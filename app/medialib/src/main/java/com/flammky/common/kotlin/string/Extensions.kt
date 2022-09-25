package com.flammky.common.kotlin.string

fun String.notEmptyOrNull(): String? = this.ifEmpty { null }
fun String.addPrefix(prefix: String): String = prefix + this

fun String.setPrefix(prefix: String, ignoreCase: Boolean = false): String {
	return if (!startsWith(prefix, ignoreCase)) addPrefix(prefix) else this
}

fun String.setSuffix(suffix: String, ignoreCase: Boolean = false): String {
	return if (!endsWith(suffix, ignoreCase)) this + suffix else this
}

