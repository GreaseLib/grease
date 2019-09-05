package org.sert2521.sertain.events

import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

@UseExperimental(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
val events = BroadcastChannel<Event>(Channel.BUFFERED)

@UseExperimental(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
suspend fun <E : Event> fire(event: E) {
    events.send(event)
}

@UseExperimental(kotlinx.coroutines.ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
suspend inline fun <reified E : Event> subscribe(noinline action: suspend (E) -> Unit) =
        events.asFlow().filter { it is E }.map { it as E }.apply { collect(action) }

@UseExperimental(kotlinx.coroutines.ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
suspend inline fun <T, reified E : TargetedEvent<T>> subscribe(target: T, noinline action: suspend (E) -> Unit) =
        events.asFlow()
            .filter { it is E && (it as? TargetedEvent<*>)?.target == target }
            .map { it as E }
            .apply { collect(action) }
