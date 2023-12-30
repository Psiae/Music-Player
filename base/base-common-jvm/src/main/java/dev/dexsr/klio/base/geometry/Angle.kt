package dev.dexsr.klio.base.geometry

import kotlin.math.cos
import kotlin.math.sin

sealed class Angle() {

    data class Degree(val degree: Float): Angle()

    data class Radians(val radians: Float): Angle()

    companion object {

    }
}

fun Angle.Companion.Radians(radians: Double) = Angle.Radians(radians.toFloat())
fun Angle.Companion.Degree(degree: Double) = Angle.Degree(degree.toFloat())

val Angle.degree
    get() = when (this) {
        is Angle.Degree -> degree
        is Angle.Radians -> with(Geometry) { degrees(radians) }
    }

val Angle.radians
    get() = when(this) {
        is Angle.Radians -> radians
        is Angle.Degree -> with(Geometry) { radians(degree) }
    }

operator fun Angle.plus(other: Angle): Angle = when(this) {
    is Angle.Radians -> Angle.Radians(radians + other.radians)
    is Angle.Degree -> Angle.Degree(degree + other.degree)
}

operator fun Angle.minus(other: Angle): Angle = when(this) {
    is Angle.Radians -> Angle.Radians(radians - other.radians)
    is Angle.Degree -> Angle.Degree(degree - other.degree)
}

data class Vector2D(
    val x: Float = 0f,
    val y: Float = 0f
) {

}

@Suppress("FunctionName")
fun Vector2D(angle: Angle, length: Float): Vector2D {
    val radians = angle.radians
    return Vector2D(
        x = length * cos(radians),
        y = length * sin(radians)
    )
}

operator fun Vector2D.div(other: Int): Vector2D {
    return Vector2D(
        x = x / other,
        y = y / other
    )
}

operator fun Vector2D.div(other: Float): Vector2D {
    return Vector2D(
        x = x / other,
        y = y / other
    )
}
