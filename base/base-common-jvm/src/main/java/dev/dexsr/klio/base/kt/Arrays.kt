package dev.dexsr.klio.base.kt

fun <A, B> Array<A>.contentEqualsBy(other: Array<B>, selector: (A, B) -> Boolean): Boolean {
    if (this === other) return true
    if (this.size != other.size) return false
    repeat(size) { i -> if (!selector(this[i], other[i])) return false }
    return true
}

fun <A, B> Array<A>.contentEqualsByIndexed(other: Array<B>, selector: (Int, A, B) -> Boolean): Boolean {
    if (this === other) return true
    if (this.size != other.size) return false
    repeat(size) { i -> if (!selector(i, this[i], other[i])) return false }
    return true
}
