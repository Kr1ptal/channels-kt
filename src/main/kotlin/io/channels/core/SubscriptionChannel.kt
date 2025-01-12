package io.channels.core

import java.io.Closeable

/**
 * A channel that can be subscribed to.
 * */
interface SubscriptionChannel<T : Any> : Closeable {
    /**
     * Subscribe to this channel, returning a [ChannelReceiver]. When the [SubscriptionChannel] is closed, the
     * receiver will be closed as well. If the [ChannelReceiver] is closed, it closes only itself.
     * */
    fun subscribe(): ChannelReceiver<T>
}