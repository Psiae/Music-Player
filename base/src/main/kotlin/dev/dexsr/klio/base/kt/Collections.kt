package dev.dexsr.klio.base.kt

public inline fun <T, R, C : MutableCollection<in R>> Iterable<T>.mapToIndexed(destination: C, transform: (Int, T) -> R): C {
    var index = 0
    for (item in this)
        destination.add(transform(index++, item))
    return destination
}
