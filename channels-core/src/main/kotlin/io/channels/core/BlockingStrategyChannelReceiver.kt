package io.channels.core

import io.channels.core.blocking.BlockingStrategy
import java.util.function.Consumer

/**
 * A [Channel] that uses a custom [BlockingStrategy] to block until the next element is available.
 * */
internal class BlockingStrategyChannelReceiver<T : Any>(
    private val parent: ChannelReceiver<T>,
    private val blockingStrategy: BlockingStrategy,
) : ChannelReceiver<T> {
    init {
        parent.onStateChange(blockingStrategy::signalStateChange)
    }

    override fun onStateChange(listener: Runnable) {
        parent.onStateChange(listener)
    }

    override fun take(): T {
        while (true) {
            val ret = parent.poll()
            if (ret != null) {
                return ret
            }

            // check after polling, so we still drain the queue even if unsubscribed
            if (isClosed) {
                break
            }

            // if no next element, wait until next event is available
            blockingStrategy.waitForStateChange(this)
        }

        throw InterruptedException("Channel is closed")
    }

    override fun forEach(consumer: Consumer<in T>) {
        while (true) {
            consumer.accept(take())
        }
    }

    override fun poll(): T? {
        return parent.poll()
    }

    override val isClosed: Boolean
        get() = parent.isClosed

    override val size: Int
        get() = parent.size

    override fun close() {
        parent.close()
    }
}
