package io.channels.core.operator

import io.channels.core.ChannelReceiver
import io.channels.core.waiting.WaitStrategy
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
    override fun iterator(waitStrategy: WaitStrategy): Iterator<T> {
        val iter = parent.iterator(waitStrategy)

        return object : Iterator<T> {
            private var next: T? = null

            override fun hasNext(): Boolean {
                if (next != null) {
                    return true
                }

                // loop until we find a matching element or reach the end of the stream
                while (iter.hasNext()) {
                    val next = iter.next()

                    if (predicate.test(next)) {
                        this.next = next
                        return true
                    }
                }

                return false
            }

            override fun next(): T {
                val ret = next ?: throw NoSuchElementException()
                next = null
                return ret
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

    override val size: Int
        get() = parent.size

    override fun close() {
        parent.close()
    }
}
