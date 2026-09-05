package io.channels.core.blocking

import io.channels.core.ChannelState

/**
 * Platform state for [NotificationHandle]. JVM and Android add blocking wait members; other targets only
 * support the common coroutine and callback notifications. One handle shares its state across all wait strategies.
 */
expect abstract class PlatformNotification protected constructor(channelState: ChannelState) {
    val channelState: ChannelState

    protected fun signalPlatformStateChange()
}
