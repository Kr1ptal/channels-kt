package io.channels.core.operator

import io.channels.core.ChannelFunction
import io.channels.core.ChannelReceiver
import io.channels.core.blocking.NotificationHandle

/**
 * Map each element from [parent] using [mapper], from type [T] to [R]. If [mapper] returns null, the element is
 * skipped.
 *
 * NOTE: Size of the channel represents the number of elements in [parent], regardless of [mapper] result.
 * */
class MapNotNullChannel<T : Any, R : Any>(
    protected override val parent: ChannelReceiver<T>,
    private val mapper: ChannelFunction<T, R?>,
) : PlatformOperatorChannel<T, R>() {
    override fun transform(value: T): R? = mapper.apply(value)

    override val notificationHandle: NotificationHandle
        get() = parent.notificationHandle

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
