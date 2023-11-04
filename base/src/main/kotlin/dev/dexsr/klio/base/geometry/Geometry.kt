package dev.dexsr.klio.base.geometry

import dev.dexsr.klio.base.commonkt.ktMath

object Geometry {

    // 0.017453292519943295
    public const val DEGREES_TO_RADIANS: Double = ktMath.PI / 180.0
    // 57.29577951308232
    public const val RADIANS_TO_DEGREES: Double = 180.0 / ktMath.PI

    fun radians(angleDegree: Double): Double = angleDegree * DEGREES_TO_RADIANS
    fun degrees(radians: Double): Double = radians * RADIANS_TO_DEGREES

    fun radians(angleDegree: Float): Float = (angleDegree * DEGREES_TO_RADIANS).toFloat()
    fun degrees(radians: Float): Float = (radians * RADIANS_TO_DEGREES).toFloat()
}

fun Double.degreeToRadians(): Double = Geometry.radians(this)
fun Double.radiansToDegree(): Double = Geometry.degrees(this)

fun Float.degreeToRadians(): Float = Geometry.radians(this)
fun Float.radiansToDegree(): Float = Geometry.degrees(this)

