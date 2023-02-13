package com.flammky.musicplayer.core.sdk

data class BuildCode(
    val CODE_INT: Int
) {
    operator fun compareTo (other: BuildCode): Int {
        return when {
            CODE_INT > other.CODE_INT -> 1
            CODE_INT < other.CODE_INT -> -1
            else -> 0
        }
    }
}
