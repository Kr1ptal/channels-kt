package io.channels.core.operator

import io.channels.core.ChannelConsumer
import io.channels.core.ChannelReceiver

/** Preserve the parent's blocking consumption behavior through operator chains. */
actual abstract class PlatformOperatorChannel<T : Any, R : Any> actual constructor() : ChannelReceiver<R> {
    protected actual abstract val parent: ChannelReceiver<T>

    protected actual abstract fun transform(value: T): R?

    override fun take(): R? {
        while (true) {
            val next = parent.take() ?: return null
            return transform(next) ?: continue
        }
    }

    override fun forEach(consumer: ChannelConsumer<in R>) {
        parent.forEach { next ->
            val mapped = transform(next)
            if (mapped != null) consumer.accept(mapped)
        }
    }
}
