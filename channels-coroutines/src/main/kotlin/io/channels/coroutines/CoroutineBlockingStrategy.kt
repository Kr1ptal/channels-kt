package io.channels.coroutines

import io.channels.core.ChannelState
import io.channels.core.blocking.BlockingStrategy
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking

internal class CoroutineBlockingStrategy : BlockingStrategy {
    // size of 1 makes sure that even if the notification is sent between polling a null value and receiving on this
    // channel, we will not miss the notification as it will be buffered.
    val notifications = Channel<Unit>(1)

    override fun waitForStateChange(status: ChannelState) {
        runBlocking { notifications.receive() }
    }

    override fun signalStateChange() {
        notifications.trySend(Unit)
    }
}
