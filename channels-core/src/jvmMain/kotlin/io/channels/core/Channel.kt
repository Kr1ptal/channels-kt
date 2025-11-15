package io.channels.core

import io.channels.core.blocking.BlockingStrategyReceiver
import io.channels.core.blocking.NotificationHandle
import java.util.concurrent.ThreadFactory
import kotlin.time.DurationUnit
import kotlin.time.toDuration

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

/**
 * Creates a new receiver wrapper with a custom blocking strategy function.
 * */
fun <T : Any> ChannelReceiver<T>.withBlockingStrategy(
    waitFunction: (NotificationHandle) -> Unit,
): ChannelReceiver<T> {
    return BlockingStrategyReceiver(this, waitFunction)
}

/**
 * Creates a new receiver with a busy spin blocking strategy.
 *
 * This strategy does not park and will use as many CPU cycles as possible but has the lowest latency jitter.
 * Should be used sparingly to avoid CPU starvation.
 * */
fun <T : Any> ChannelReceiver<T>.withBusySpinBlockingStrategy(): ChannelReceiver<T> {
    return BlockingStrategyReceiver(this, NotificationHandle::waitWithBusySpin)
}

/**
 * Creates a new receiver with a parking blocking strategy.
 *
 * Uses the fewest CPU cycles, but will lead to higher latency jitter if parked.
 * */
fun <T : Any> ChannelReceiver<T>.withParkingBlockingStrategy(): ChannelReceiver<T> {
    return BlockingStrategyReceiver(this, NotificationHandle::waitWithParking)
}

/**
 * Creates a new receiver with a sleep blocking strategy using 100-nanosecond sleep duration.
 *
 * This strategy can provide a good balance between latency and CPU usage.
 * */
fun <T : Any> ChannelReceiver<T>.withSleepBlockingStrategy(): ChannelReceiver<T> {
    return withSleepBlockingStrategy(100, DurationUnit.NANOSECONDS)
}

/**
 * Creates a new receiver with sleep blocking strategy using the provided [duration].
 *
 * The [duration] is used to determine how long to wait. A higher number will lead to higher latency, but use
 * less CPU cycles, and vice versa. This strategy can provide a good balance between latency and CPU usage.
 * */
fun <T : Any> ChannelReceiver<T>.withSleepBlockingStrategy(duration: Long, unit: DurationUnit): ChannelReceiver<T> {
    val sleepNanos = duration.toDuration(unit).inWholeNanoseconds
    return BlockingStrategyReceiver(this) { it.waitWithSleep(sleepNanos) }
}

/**
 * Creates a new receiver with a yielding blocking strategy.
 *
 * This strategy does not park and will use as many CPU cycles as possible but can yield the CPU to other threads
 * if needed.
 * */
fun <T : Any> ChannelReceiver<T>.withYieldingBlockingStrategy(): ChannelReceiver<T> {
    return BlockingStrategyReceiver(this, NotificationHandle::waitWithYield)
}
