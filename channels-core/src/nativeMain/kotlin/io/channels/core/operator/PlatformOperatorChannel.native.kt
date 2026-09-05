package io.channels.core.operator

import io.channels.core.ChannelReceiver

/** Platform-specific consumption support for operators that transform elements from a parent receiver. */
actual abstract class PlatformOperatorChannel<T : Any, R : Any> actual constructor() : ChannelReceiver<R> {
    protected actual abstract val parent: ChannelReceiver<T>

    protected actual abstract fun transform(value: T): R?
}
