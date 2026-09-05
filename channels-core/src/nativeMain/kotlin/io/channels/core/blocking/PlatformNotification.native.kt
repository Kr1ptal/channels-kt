package io.channels.core.blocking

import io.channels.core.ChannelState

/** This target has no blocking waiters to notify. */
actual abstract class PlatformNotification protected actual constructor(actual val channelState: ChannelState) {
    protected actual fun signalPlatformStateChange() = Unit
}
