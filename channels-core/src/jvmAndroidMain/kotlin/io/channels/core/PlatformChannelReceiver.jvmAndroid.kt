package io.channels.core

import io.channels.core.blocking.BlockingStrategyReceiver
import io.channels.core.blocking.NotificationHandle
import java.util.concurrent.ThreadFactory
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/** JVM/Android blocking and background-consumption members inherited by [ChannelReceiver], including from Java. */
actual interface PlatformChannelReceiver<out T : Any> : ChannelState, AutoCloseable {
    actual val notificationHandle: NotificationHandle

    actual fun poll(): T?

    /**
     * Remove and return the next element, blocking until one is available. Returns null when the receiver is closed and
     * empty. This API is available only on JVM and Android.
     */
    fun take(): T? {
        while (true) {
            val value = poll()
            if (value != null || isClosed) return value
            notificationHandle.waitWithParking()
        }
    }

    /** Consume elements synchronously until this receiver is closed. This API is available only on JVM and Android. */
    fun forEach(consumer: ChannelConsumer<in T>) {
        while (true) {
            consumer.accept(take() ?: break)
        }
    }

    /**
     * Iterates over the elements of this channel, calling [consumer] for each element. This does not block the calling
     * thread and instead iterates on a new daemon thread. If available, a virtual thread is created, falling back
     * to platform threads.
     * */
    fun forEachAsync(consumer: ChannelConsumer<in T>): ChannelReceiver<T> {
        return forEachAsync(null, consumer)
    }

    /**
     * Iterates over the elements of this channel, calling [consumer] for each element. This does not block the calling
     * thread and instead iterates on a new daemon thread with optional [threadName]. If available, a virtual thread
     * is created, falling back to platform threads.
     * */
    fun forEachAsync(
        threadName: String?,
        consumer: ChannelConsumer<in T>,
    ): ChannelReceiver<T> {
        val receiver = this as ChannelReceiver<T>
        val thread = ThreadFactoryProvider.maybeVirtualThread { forEach(consumer) }

        if (threadName != null) {
            thread.name = threadName
        }
        thread.isDaemon = true
        thread.start()
        return receiver
    }

    /**
     * Iterates over the elements of this channel, calling [consumer] for each element. This does not block the calling
     * thread and instead iterates on a new thread, created by provided [threadFactory].
     * */
    fun forEachAsync(
        threadFactory: ThreadFactory,
        consumer: ChannelConsumer<in T>,
    ): ChannelReceiver<T> {
        val receiver = this as ChannelReceiver<T>
        threadFactory.newThread { forEach(consumer) }.start()
        return receiver
    }

    /** Create a JVM/Android receiver wrapper with a custom synchronous wait strategy. */
    fun withBlockingStrategy(
        waitFunction: (NotificationHandle) -> Unit,
    ): ChannelReceiver<T> = BlockingStrategyReceiver(this, waitFunction)

    /** Create a JVM/Android receiver that busy-spins while waiting for an element. */
    fun withBusySpinBlockingStrategy(): ChannelReceiver<T> = withBlockingStrategy(
        NotificationHandle::waitWithBusySpin,
    )

    /** Create a JVM/Android receiver that parks while waiting for an element. */
    fun withParkingBlockingStrategy(): ChannelReceiver<T> = withBlockingStrategy(
        NotificationHandle::waitWithParking,
    )

    /** Create a JVM/Android receiver that sleeps for 100 nanoseconds between polls. */
    fun withSleepBlockingStrategy(): ChannelReceiver<T> = withSleepBlockingStrategy(
        100,
        DurationUnit.NANOSECONDS,
    )

    /** Create a JVM/Android receiver that sleeps for [duration] between polls. */
    fun withSleepBlockingStrategy(
        duration: Long,
        unit: DurationUnit,
    ): ChannelReceiver<T> {
        val sleepNanos = duration.toDuration(unit).inWholeNanoseconds
        return withBlockingStrategy { it.waitWithSleep(sleepNanos) }
    }

    /** Create a JVM/Android receiver that yields while waiting for an element. */
    fun withYieldingBlockingStrategy(): ChannelReceiver<T> = withBlockingStrategy(
        NotificationHandle::waitWithYield,
    )
}
