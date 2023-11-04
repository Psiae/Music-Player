package dev.dexsr.klio.base.resource

import java.util.*
import java.io.File as jFile

sealed class LocalImage<T>(val value: T) {

    object None : LocalImage<Int>(-1)

    class File(file: jFile) : LocalImage<java.io.File>(file) {

        override fun equals(other: Any?): Boolean {
            return this === other || other is File && other.value == value
        }

        override fun hashCode(): Int {
            return Objects.hash(value)
        }
    }
    class Resource(id: Int) : LocalImage<Int>(id) {

        override fun equals(other: Any?): Boolean {
            return this === other || other is Resource && other.value == value
        }

        override fun hashCode(): Int {
            return Objects.hash(value)
        }
    }

    companion object {}
}
