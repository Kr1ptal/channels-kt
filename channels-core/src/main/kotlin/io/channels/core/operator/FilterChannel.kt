package io.channels.core.operator

import io.channels.core.ChannelReceiver
import io.channels.core.waiting.WaitStrategy
import java.util.function.Consumer
import java.util.function.Predicate

/**
 * Filter elements from [parent] using [predicate].
 *
 * NOTE: Size of the channel represents the number of elements in [parent], regardless of [predicate] result.
 * */
class FilterChannel<T : Any>(
    private val parent: ChannelReceiver<T>,
    private val predicate: Predicate<in T>,
) : ChannelReceiver<T> {
    override fun onStateChange(listener: Runnable) {
        parent.onStateChange(listener)
    }

    override fun forEach(waitStrategy: WaitStrategy, consumer: Consumer<in T>) {
        parent.forEach(waitStrategy) { next ->
            if (predicate.test(next)) {
                consumer.accept(next)
            }
        }
    }

    override fun tryPoll(): T? {
        while (true) {
            val next = parent.tryPoll()
            if (next == null) {
                return null
            }

            if (predicate.test(next)) {
                return next
            }
        }
    }

    override val isClosed: Boolean
        get() = parent.isClosed

    override val size: Int
        get() = parent.size

    override fun close() {
        parent.close()
    }
}
