package dev.dexsr.klio.base

class ExhaustiveWhenExpressionException(msg: String?) : IllegalStateException(msg)


@Suppress("NOTHING_TO_INLINE")
inline fun exhaustiveWhenExpressionError(msg: String? = null): Nothing = throw ExhaustiveWhenExpressionException(msg)
