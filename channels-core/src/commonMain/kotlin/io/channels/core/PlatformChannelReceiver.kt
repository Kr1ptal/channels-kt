package io.channels.core

import io.channels.core.blocking.NotificationHandle

/**
 * Receiver primitives shared by all targets. JVM and Android add blocking member functions to this interface,
 * inherited by [ChannelReceiver] and its implementations. Other targets expose only the non-blocking primitives.
 */
expect interface PlatformChannelReceiver<out T : Any> : ChannelState, AutoCloseable {
    val notificationHandle: NotificationHandle

    fun poll(): T?
}
