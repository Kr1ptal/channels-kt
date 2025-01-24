package io.channels.core.operator

import io.channels.core.ChannelReceiver
import io.channels.core.waiting.WaitStrategy
import java.util.function.Consumer
import java.util.function.Function

/**
 * Map each element from [parent] using [mapper], from type [T] to [R]. If [mapper] returns null, the element is
 * skipped.
 *
 * NOTE: Size of the channel represents the number of elements in [parent], regardless of [mapper] result.
 * */
class MapNotNullChannel<T : Any, R : Any>(
    private val parent: ChannelReceiver<T>,
    private val mapper: Function<T, R?>,
) : ChannelReceiver<R> {
    override fun onStateChange(listener: Runnable) {
        parent.onStateChange(listener)
    }

    override fun forEach(waitStrategy: WaitStrategy, consumer: Consumer<in R>) {
        parent.forEach(waitStrategy) { next ->
            val mapped = mapper.apply(next)
            if (mapped != null) {
                consumer.accept(mapped)
            }
        }
    }

    override fun poll(): R? {
        while (true) {
            val next = parent.poll()
            if (next == null) {
                return null
            }

            val mapped = mapper.apply(next)
            if (mapped != null) {
                return mapped
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