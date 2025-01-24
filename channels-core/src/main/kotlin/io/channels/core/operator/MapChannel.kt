package io.channels.core.operator

import io.channels.core.ChannelReceiver
import io.channels.core.waiting.WaitStrategy
import java.util.function.Consumer
import java.util.function.Function

/**
 * Map each element from [parent] using [mapper], from type [T] to [R].
 * */
class MapChannel<T : Any, R : Any>(
    private val parent: ChannelReceiver<T>,
    private val mapper: Function<T, R>,
) : ChannelReceiver<R> {
    override fun onStateChange(listener: Runnable) {
        parent.onStateChange(listener)
    }

    override fun forEach(waitStrategy: WaitStrategy, consumer: Consumer<in R>) {
        parent.forEach(waitStrategy) { next ->
            consumer.accept(mapper.apply(next))
        }
    }
    
    override fun tryPoll(): R? {
        val next = parent.tryPoll()
        if (next == null) {
            return null
        }

        return mapper.apply(next)
    }

    override val isClosed: Boolean
        get() = parent.isClosed

    override val size: Int
        get() = parent.size

    override fun close() {
        parent.close()
    }
}