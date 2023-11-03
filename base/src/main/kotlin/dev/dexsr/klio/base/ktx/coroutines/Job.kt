package dev.dexsr.klio.base.ktx.coroutines

import dev.dexsr.klio.base.kt.sync
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletionHandler
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job

// TODO: write test

// or in other words `attachParent`
inline fun Job.cancelOnParentCancellation(parent: Job): DisposableHandle {
    return cancelOnParentCancellation(parent::invokeOnCompletion)
}

// or in other words `attachParent`
inline fun Job.cancelOnParentCancellation(
    parentInvokeOnCompletion: (handler: CompletionHandler) -> DisposableHandle,
): DisposableHandle {
    return parentInvokeOnCompletion { ex ->
        ex?.let {
            cancel(CancellationException("Parent job was cancelled", ex))
        }
    }
}

// or in other words `parent.attachChild`
fun Job.cancelParentOnCancellation(parent: Job): DisposableHandle {
    return cancelParentOnCancellation(parent::cancel)
}

// or in other words `parent.attachChild`
inline fun Job.cancelParentOnCancellation(
    crossinline parentCancel: (CancellationException) -> Unit,
): DisposableHandle {
    return invokeOnCompletion { ex ->
        ex?.let {
            parentCancel(CancellationException("Child job was cancelled", ex))
        }
    }
}

fun Job.cancelParentOnCancellationAsCompleter(parent: Job): DisposableHandle {
    return invokeOnCompletion { ex ->
        ex?.let {
            parent.cancel(CancellationException("Child job was cancelled", ex))
        }
        check(parent.isCompleted) {
            "Completer Job ($this) was completed but parent ($parent) is not"
        }
    }
}

inline fun Job.cancelParentOnCancellationAsCompleter(
    crossinline cancelParent: (CancellationException) -> Unit,
    crossinline isParentCompleted: () -> Boolean,
    crossinline parentToString: () -> String
): DisposableHandle {
    return invokeOnCompletion { ex ->
        ex?.let {
            cancelParent(CancellationException("Child job was cancelled", ex))
        }
        check(isParentCompleted()) {
            "Completer Job ($this) was completed but parent (${parentToString.invoke()}) is not"
        }
    }
}

fun Job.initAsChildJob(parent: Job): DisposableHandle {
    val disposables = mutableListOf<DisposableHandle>()

    cancelOnParentCancellation(parent).also { disposables.add(it) }
    cancelParentOnCancellation(parent).also { disposables.add(it) }

    return DisposableHandle {
        disposables.sync() { apply { forEach(DisposableHandle::dispose) ; clear() } }
    }
}

fun Job.initAsParentCompleter(parent: Job): DisposableHandle {
    val disposables = mutableListOf<DisposableHandle>()

    cancelParentOnCancellationAsCompleter(parent).also { disposables.add(it) }
    cancelOnParentCancellation(parent).also { disposables.add(it) }

    return DisposableHandle {
        disposables.sync() { apply { forEach(DisposableHandle::dispose) ; clear() } }
    }
}

fun Job.initAsParentCompleter(
    parentInvokeOnCompletion: (handler: CompletionHandler) -> DisposableHandle,
    parentCancel: (CancellationException) -> Unit,
    isParentCompleted: () -> Boolean,
    parentToString: () -> String
): DisposableHandle {
    val disposables = mutableListOf<DisposableHandle>()

    cancelParentOnCancellationAsCompleter(parentCancel, isParentCompleted, parentToString)
    cancelOnParentCancellation(parentInvokeOnCompletion)

    return DisposableHandle {
        disposables.sync() { apply { forEach(DisposableHandle::dispose) ; clear() } }
    }
}

fun Job.initAsCompleterJob(parent: Job): DisposableHandle {
    return initAsParentCompleter(parent)
}

fun Job.initAsParentJob(child: Job): DisposableHandle {
    return child.initAsChildJob(this)
}

fun Job.initCompleterAsParentJob(child: Job): DisposableHandle {
    return child.initAsParentCompleter(this)
}
