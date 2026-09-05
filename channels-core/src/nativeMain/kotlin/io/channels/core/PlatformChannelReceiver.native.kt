package io.channels.core

import io.channels.core.blocking.NotificationHandle

actual interface PlatformChannelReceiver<out T : Any> : ChannelState, AutoCloseable {
    actual val notificationHandle: NotificationHandle

    actual fun poll(): T?
}
