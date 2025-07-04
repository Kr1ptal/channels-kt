package io.channels.coroutines

import io.channels.core.ChannelReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Iterate over the elements of this channel, calling [consumer] for each element. The coroutine is
 * suspended until the next element is available or the channel is closed.
 */
suspend fun <T : Any> ChannelReceiver<T>.forEachSuspend(consumer: suspend (T) -> Unit) {
    // size of 1 makes sure that even if the notification is sent between polling a null value and receiving on this
    // channel, we will not miss the notification as it will be buffered.
    val notifications = Channel<Unit>(1)
    val callbackHandle = notificationHandle.onStateChangeCallback {
        notifications.trySend(Unit)
    }

    try {
        while (true) {
            val next = poll()
            if (next != null) {
                consumer(next)
                continue
            }

            if (isClosed) {
                break
            }

            notifications.receive()
        }
    } finally {
        callbackHandle.unregister()
        notifications.close()
        close()
    }
}

/** Wrap this [ChannelReceiver] into a coroutine [ReceiveChannel]. */
fun <T : Any> ChannelReceiver<T>.asCoroutineReceiver(
    ctx: CoroutineContext = Dispatchers.Default,
): ReceiveChannel<T> {
    // zero-buffered channel
    val ret = Channel<T>(RENDEZVOUS)

    CoroutineScope(ctx).launch {
        try {
            forEachSuspend(ret::send)
        } finally {
            ret.close()
            close()
        }
    }

    return ret
}

/** Convert this [ChannelReceiver] into a Kotlin coroutines [Flow]. */
fun <T : Any> ChannelReceiver<T>.asFlow(): Flow<T> = flow { forEachSuspend { emit(it) } }
