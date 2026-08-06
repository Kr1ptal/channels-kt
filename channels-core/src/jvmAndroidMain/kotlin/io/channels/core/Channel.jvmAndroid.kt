package io.channels.core

import java.util.concurrent.ThreadFactory

/**
 * Iterates over the elements of this channel, calling [consumer] for each element. This does not block the calling
 * thread and instead iterates on a new daemon thread. If available, a virtual thread is created, falling back
 * to platform threads.
 * */
fun <T : Any> ChannelReceiver<T>.forEachAsync(consumer: ChannelConsumer<in T>): ChannelReceiver<T> {
    return forEachAsync(null, consumer)
}

/**
 * Iterates over the elements of this channel, calling [consumer] for each element. This does not block the calling
 * thread and instead iterates on a new daemon thread with optional [threadName]. If available, a virtual thread
 * is created, falling back to platform threads.
 * */
fun <T : Any> ChannelReceiver<T>.forEachAsync(
    threadName: String?,
    consumer: ChannelConsumer<in T>,
): ChannelReceiver<T> {
    val thread = ThreadFactoryProvider.maybeVirtualThread { forEach(consumer) }

    if (threadName != null) {
        thread.name = threadName
    }
    thread.isDaemon = true
    thread.start()
    return this
}

/**
 * Iterates over the elements of this channel, calling [consumer] for each element. This does not block the calling
 * thread and instead iterates on a new thread, created by provided [threadFactory].
 * */
fun <T : Any> ChannelReceiver<T>.forEachAsync(
    threadFactory: ThreadFactory,
    consumer: ChannelConsumer<in T>,
): ChannelReceiver<T> {
    threadFactory.newThread { forEach(consumer) }.start()
    return this
}
