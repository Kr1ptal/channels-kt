package io.channels.core.operator

import io.channels.core.ChannelReceiver
import io.channels.core.blocking.NotificationHandle
import java.util.function.Consumer
import java.util.function.Function

/**
 * Map each element from [parent] using [mapper], from type [T] to [R].
 * */
class MapChannel<T : Any, R : Any>(
    private val parent: ChannelReceiver<T>,
    private val mapper: Function<T, R>,
) : ChannelReceiver<R> {
    override val notificationHandle: NotificationHandle
        get() = parent.notificationHandle

    override fun forEach(consumer: Consumer<in R>) {
        parent.forEach { next ->
            consumer.accept(mapper.apply(next))
        }
    }

    override fun take(): R? {
        return mapper.apply(parent.take() ?: return null)
    }

    override fun poll(): R? {
        val next = parent.poll()
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
