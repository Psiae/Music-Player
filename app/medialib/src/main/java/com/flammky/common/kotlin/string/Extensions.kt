package com.flammky.common.kotlin.string

fun String.notEmptyOrNull(): String? = this.ifEmpty { null }
fun String.addPrefix(prefix: String): String = prefix + this

fun String.setPrefix(prefix: String): String {
	return if (!startsWith(prefix)) { addPrefix(prefix) } else this
}

