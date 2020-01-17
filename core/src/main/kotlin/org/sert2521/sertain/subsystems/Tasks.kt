package org.sert2521.sertain.subsystems

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.sert2521.sertain.events.Use
import org.sert2521.sertain.events.fire
import kotlin.coroutines.coroutineContext

class TaskConfigure {
    internal val subsystems = mutableListOf<Subsystem>()

    operator fun plusAssign(subsystem: Subsystem) {
        subsystems += subsystem
    }

    internal var action: (suspend CoroutineScope.() -> Unit) = {}

    fun action(action: suspend CoroutineScope.() -> Unit) {
        this.action = action
    }
}

suspend fun doTask(name: String = "ANONYMOUS_TASK", configure: TaskConfigure.() -> Unit) {
    with(TaskConfigure().apply(configure)) {
        action.let {
            @Suppress("unchecked_cast") // Will work, ActionConfigure extends CoroutineScope
            (use(*subsystems.toTypedArray(), name = name, action = it))
        }
    }
}

suspend inline fun <reified S1: Subsystem> use(
        cancelConflicts: Boolean = true,
        name: String = "ANONYMOUS_TASK",
        crossinline action: suspend CoroutineScope.(s1: S1) -> Unit
) {
    val s1 = access<S1>()
    use(s1, cancelConflicts = cancelConflicts, name = name) { action(s1) }
}

suspend inline fun <reified S1: Subsystem, reified S2: Subsystem> use(
        cancelConflicts: Boolean = true,
        name: String = "ANONYMOUS_TASK",
        crossinline action: suspend CoroutineScope.(s1: S1, s2: S2) -> Unit
) {
    val s1 = access<S1>()
    val s2 = access<S2>()
    use(s1, s2, cancelConflicts = cancelConflicts, name = name) { action(s1, s2) }
}

suspend inline fun <reified S1: Subsystem, reified S2: Subsystem, reified S3: Subsystem> use(
        cancelConflicts: Boolean = true,
        name: String = "ANONYMOUS_TASK",
        crossinline action: suspend CoroutineScope.(s1: S1, s2: S2, s3: S3) -> Unit
) {
    val s1 = access<S1>()
    val s2 = access<S2>()
    val s3 = access<S3>()
    use(s1, s2, s3, cancelConflicts = cancelConflicts, name = name) { action(s1, s2, s3) }
}

suspend inline fun <reified S1: Subsystem, reified S2: Subsystem, reified S3: Subsystem, reified S4: Subsystem> use(
        cancelConflicts: Boolean = true,
        name: String = "ANONYMOUS_TASK",
        crossinline action: suspend CoroutineScope.(s1: S1, s2: S2, s3: S3, s4: S4) -> Unit
) {
    val s1 = access<S1>()
    val s2 = access<S2>()
    val s3 = access<S3>()
    val s4 = access<S4>()
    use(s1, s2, s3, s4, cancelConflicts = cancelConflicts, name = name) { action(s1, s2, s3, s4) }
}

suspend fun <R> use(
    vararg subsystems: Subsystem,
    cancelConflicts: Boolean = true,
    name: String = "ANONYMOUS_TASK",
    action: suspend CoroutineScope.() -> R
): R {
    val context = coroutineContext
    return suspendCancellableCoroutine { continuation ->
        CoroutineScope(context).launch {
            fire(Use(
                    subsystems.toSet(),
                    cancelConflicts,
                    name,
                    context,
                    continuation,
                    action
            ))
        }
    }
}
