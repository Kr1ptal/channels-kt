package io.channels.core.blocking

import io.channels.core.ChannelReceiver
import io.channels.core.PlatformChannelReceiver

/** JVM/Android receiver wrapper that applies a synchronous blocking strategy. */
class BlockingStrategyReceiver<T : Any>(
    private val delegate: PlatformChannelReceiver<T>,
    private val waitFunction: (NotificationHandle) -> Unit,
) : ChannelReceiver<T> {
    override val notificationHandle: NotificationHandle
        get() = delegate.notificationHandle

    override val isClosed: Boolean
        get() = delegate.isClosed

    override val size: Int
        get() = delegate.size

    override fun poll(): T? = delegate.poll()

    override fun close() = delegate.close()

    /** Blocks until the next element is available or this receiver is closed. */
    override fun take(): T? {
        while (true) {
            val value = delegate.poll()
            if (value != null || delegate.isClosed) return value
            waitFunction(notificationHandle)
        }
    }
}
