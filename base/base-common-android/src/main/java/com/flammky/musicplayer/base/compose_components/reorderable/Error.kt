package dev.flammky.compose_components.reorderable

internal inline fun internalReorderableError(msg: String): Nothing = error(
    """
        InternalReorderableError, please file a bug report with the stacktrace.
        msg=$msg
    """
)

internal inline fun publicReorderableError(msg: String): Nothing = error(
    """
        PublicReorderableError, please refer to usage documentation.
        msg=$msg
    """
)

internal inline fun internalReorderableStateCheck(
    state: Boolean,
    lazyMsg: () -> Any
) {
    if (!state) internalReorderableError(lazyMsg().toString())
}

internal inline fun publicReorderableStateCheck(
    state: Boolean,
    lazyMsg: () -> Any
) {
    if (!state) publicReorderableError(lazyMsg().toString())
}