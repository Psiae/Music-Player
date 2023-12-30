package dev.dexsr.klio.base.kt

fun String.substringAfterOrNull(
    delimiter: String
): String? {
    val index = indexOf(delimiter)
    return if (index == -1) null else substring(index + delimiter.length, length)
}

fun String.suffix(str: String, ignoreCase: Boolean = false) = if (!endsWith(str, ignoreCase)) this + str else this
fun String.prefix(str: String, ignoreCase: Boolean = false) = if (!startsWith(str, ignoreCase)) str + this else this

fun String.prefix(char: Char, ignoreCase: Boolean = false) = if (!startsWith(char, ignoreCase)) char + this else this
