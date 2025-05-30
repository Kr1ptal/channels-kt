package io.channels.coroutines

import io.channels.core.ChannelReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * Iterate over the elements of this channel, calling [consumer] for each element. The coroutine is suspended until the
 * next element is available or the channel is closed.
 * */
suspend fun <T : Any> ChannelReceiver<T>.forEachSuspend(consumer: suspend (T) -> Unit) {
    val blockingStrategy = CoroutineBlockingStrategy()
    withBlockingStrategy(blockingStrategy)

    try {
        while (true) {
            val next = poll()
            if (next != null) {
                consumer(next)
                continue
            }

            // check after polling, so we still drain the queue even if unsubscribed
            if (isClosed) {
                break
            }

            // if no next element, suspend until next event
            blockingStrategy.notifications.receive()
        }
    } finally {
        blockingStrategy.notifications.close()
        close()
    }
}

/**
 * Wrap this [ChannelReceiver] into a coroutine [ReceiveChannel].
 * */
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
