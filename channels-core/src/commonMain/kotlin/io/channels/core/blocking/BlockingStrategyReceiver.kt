package io.channels.core.blocking

import io.channels.core.ChannelConsumer
import io.channels.core.ChannelReceiver

/**
 * Wrapper that applies a specific blocking strategy to a [io.channels.core.ChannelReceiver].
 * Each wrapper is immutable and creates new instances when strategy changes.
 */
class BlockingStrategyReceiver<T : Any>(
    private val delegate: ChannelReceiver<T>,
    private val waitFunction: (NotificationHandle) -> Unit,
) : ChannelReceiver<T> {
    // Delegate all non-blocking operations
    override val notificationHandle: NotificationHandle get() = delegate.notificationHandle
    override val isClosed: Boolean get() = delegate.isClosed
    override val size: Int get() = delegate.size
    override fun poll(): T? = delegate.poll()
    override fun close() = delegate.close()

    override fun forEach(consumer: ChannelConsumer<in T>) {
        while (true) {
            consumer.accept(take() ?: break)
        }
    }

    // Blocking operation uses this wrapper's wait strategy
    override fun take(): T? {
        while (true) {
            val value = delegate.poll()
            if (value != null || delegate.isClosed) return value
            waitFunction.invoke(notificationHandle)
        }
    }
}
