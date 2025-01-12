package io.channels.core.operator

import io.channels.core.ChannelReceiver
import io.channels.core.waiting.WaitStrategy
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
    override fun iterator(waitStrategy: WaitStrategy): Iterator<R> {
        val iter = parent.iterator(waitStrategy)

        return object : Iterator<R> {
            private var next: R? = null

            override fun hasNext(): Boolean {
                if (next != null) {
                    return true
                }

                // loop until we find a matching element or reach the end of the stream
                while (iter.hasNext()) {
                    val next = mapper.apply(iter.next())

                    if (next != null) {
                        this.next = next
                        return true
                    }
                }

                return false
            }

            override fun next(): R {
                val ret = next ?: throw NoSuchElementException()
                next = null
                return ret
            }
        }
    }

    override fun tryPoll(): R? {
        while (true) {
            val next = parent.tryPoll()
            if (next == null) {
                return null
            }

            val mapped = mapper.apply(next)
            if (mapped != null) {
                return mapped
            }
        }
    }

    override val size: Int
        get() = parent.size

    override fun close() {
        parent.close()
    }
}