package io.channels.core.operator

import io.channels.core.ChannelReceiver

/** Platform-specific consumption support for operators that transform elements from a parent receiver. */
expect abstract class PlatformOperatorChannel<T : Any, R : Any>() : ChannelReceiver<R> {
    protected abstract val parent: ChannelReceiver<T>

    protected abstract fun transform(value: T): R?
}
