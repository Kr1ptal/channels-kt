package io.channels.core.operator

import io.channels.core.ChannelReceiver
import io.channels.core.blocking.NotificationHandle
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
    override val notificationHandle: NotificationHandle
        get() = parent.notificationHandle

    override fun forEach(consumer: Consumer<in T>) {
        parent.forEach { next ->
            if (predicate.test(next)) {
                consumer.accept(next)
            }
        }
    }

    override fun take(): T? {
        while (true) {
            val next = parent.take() ?: break
            if (predicate.test(next)) {
                return next
            }
        }
        return null
    }

    override fun poll(): T? {
        while (true) {
            val next = parent.poll()
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
