package io.channels.coroutines

import io.channels.core.ChannelReceiver
import kotlinx.coroutines.channels.Channel

/**
 * Iterate over the elements of this channel, calling [consumer] for each element. The coroutine is suspended until the
 * next element is available or the channel is closed.
 * */
suspend fun <T : Any> ChannelReceiver<T>.forEachSuspend(consumer: suspend (T) -> Unit) {
    // size of 1 makes sure that even if the notification is sent between polling a null value and receiving on this
    // channel, we will not miss the notification as it will be buffered.
    val notifications = Channel<Unit>(1)
    onStateChange { notifications.trySend(Unit) }

    while (true) {
        val next = tryPoll()
        if (next != null) {
            consumer(next)
            continue
        }

        // check after polling, so we still drain the queue even if unsubscribed
        if (isClosed) {
            break
        }

        // if no next element, suspend until next event
        notifications.receive()
    }
}
