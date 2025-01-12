package io.channels.core.operation

import io.channels.core.ChannelReceiver
import io.channels.core.waiting.WaitStrategy
import java.util.function.Function

/**
 * Map each element from [parent] using [mapper], from type [T] to [R].
 * */
class MapChannel<T : Any, R : Any>(
    private val parent: ChannelReceiver<T>,
    private val mapper: Function<T, R>,
) : ChannelReceiver<R> {
    override fun iterator(waitStrategy: WaitStrategy): Iterator<R> {
        val iter = parent.iterator(waitStrategy)

        return object : Iterator<R> {
            override fun hasNext(): Boolean {
                return iter.hasNext()
            }

            override fun next(): R {
                return mapper.apply(iter.next())
            }
        }
    }

    override fun tryPoll(): R? {
        val next = parent.tryPoll()
        if (next == null) {
            return null
        }

        return mapper.apply(next)
    }

    override val size: Int
        get() = parent.size

    override fun close() {
        parent.close()
    }
}