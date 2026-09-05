package io.channels.core.operator

import io.channels.core.ChannelFunction
import io.channels.core.ChannelReceiver
import io.channels.core.blocking.NotificationHandle

/**
 * Map each element from [parent] using [mapper], from type [T] to [R].
 * */
class MapChannel<T : Any, R : Any>(
    protected override val parent: ChannelReceiver<T>,
    private val mapper: ChannelFunction<T, R>,
) : PlatformOperatorChannel<T, R>() {
    override fun transform(value: T): R? = mapper.apply(value)

    override val notificationHandle: NotificationHandle
        get() = parent.notificationHandle

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
