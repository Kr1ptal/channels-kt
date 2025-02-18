package io.channels.core

import io.channels.core.blocking.BlockingStrategy
import io.channels.core.blocking.BusySpinBlockingStrategy
import io.channels.core.blocking.ParkingBlockingStrategy
import io.channels.core.blocking.SleepingBlockingStrategy
import io.channels.core.blocking.YieldingBlockingStrategy
import io.channels.core.operator.FilterChannel
import io.channels.core.operator.MapChannel
import io.channels.core.operator.MapNotNullChannel
import java.io.Closeable
import java.time.Duration
import java.util.concurrent.ThreadFactory
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate

/**
 * A channel that is both a sender and a receiver. A channel can possibly have multiple senders, depending on the
 * underlying implementation, but can have only a single receiver.
 * */
interface Channel<T : Any> : ChannelSender<T>, ChannelReceiver<T>

/**
 * A sender end of a channel. Same sender can possibly be used by multiple threads at the same time, but only if the
 * underlying queue supports it.
 * */
interface ChannelSender<in T : Any> : ChannelState, Closeable {
    /**
     * Offer an element to the channel, returning true if the element was added to the channel, false otherwise. This
     * method is non-blocking.
     * */
    fun offer(element: T): Boolean
}

/**
 * A receiver end of a channel. Same instance can only be used by one thread at a time, otherwise the behavior is
 * undefined.
 * */
interface ChannelReceiver<out T : Any> : ChannelState, Closeable {
    /**
     * Iterates over the elements of this channel, calling [consumer] for each element. This blocks the calling thread
     * until the channel is closed.
     * */
    fun forEach(consumer: Consumer<in T>)

    /**
     * Iterates over the elements of this channel, calling [consumer] for each element. This does not block the calling
     * thread and instead iterates on a new daemon thread with optional [threadName]. If available, a virtual thread
     * is created, falling back to platform threads.
     * */
    fun forEachAsync(threadName: String? = null, consumer: Consumer<in T>): ChannelReceiver<T> {
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
    fun forEachAsync(threadFactory: ThreadFactory, consumer: Consumer<in T>): ChannelReceiver<T> {
        threadFactory.newThread { forEach(consumer) }.start()
        return this
    }

    /**
     * Remove and return the next element from the channel, blocking the calling thread until an element is available.
     * If the channel is closed and empty, it returns null.
     *
     * See [poll] for a non-blocking version.
     * */
    fun take(): T?

    /**
     * Remove and return the next element from the channel, or null if the channel is empty. This method will never
     * block.
     *
     * See [take] for a blocking version.
     * */
    fun poll(): T?

    /**
     * Replaces the current [BlockingStrategy] with provided [blockingStrategy].
     * */
    fun withBlockingStrategy(blockingStrategy: BlockingStrategy): ChannelReceiver<T>

    /**
     * Replaces the current [BlockingStrategy] with [BusySpinBlockingStrategy].
     *
     * This strategy does not park and will use as much CPU cycles as possible, but has the lowest latency jitter.
     * Should be used sparingly to avoid CPU starvation.
     * */
    fun withBusySpinBlockingStrategy(): ChannelReceiver<T> {
        return withBlockingStrategy(BusySpinBlockingStrategy)
    }

    /**
     * Replaces the current [BlockingStrategy] with [ParkingBlockingStrategy].
     *
     * Uses the fewest CPU cycles, but will lead to higher latency jitter if parked.
     * */
    fun withParkingBlockingStrategy(): ChannelReceiver<T> {
        return withBlockingStrategy(ParkingBlockingStrategy())
    }

    /**
     * Replaces the current [BlockingStrategy] with [SleepingBlockingStrategy] with sleep duration of 100 nanoseconds.
     *
     * This strategy can provide a good balance between latency and CPU usage.
     * */
    fun withSleepBlockingStrategy(): ChannelReceiver<T> {
        return withBlockingStrategy(SleepingBlockingStrategy(Duration.ofNanos(100)))
    }

    /**
     * Replaces the current [BlockingStrategy] with [SleepingBlockingStrategy] with provided [duration].
     *
     * The [duration] is used to determine how long to wait. A higher number will lead to higher latency, but use
     * less CPU cycles, and vice versa. This strategy can provide a good balance between latency and CPU usage.
     * */
    fun withSleepBlockingStrategy(duration: Duration): ChannelReceiver<T> {
        return withBlockingStrategy(SleepingBlockingStrategy(duration))
    }

    /**
     * Replaces the current [BlockingStrategy] with [YieldingBlockingStrategy].
     *
     * This strategy does not park and will use as much CPU cycles as possible, but can yield the CPU to other threads
     * if needed.
     * */
    fun withYieldingBlockingStrategy(): ChannelReceiver<T> {
        return withBlockingStrategy(YieldingBlockingStrategy)
    }

    /**
     * Map each element from this channel using [mapper], from type [T] to [R].
     * */
    fun <R : Any> map(mapper: Function<in T, R>): ChannelReceiver<R> = MapChannel(this, mapper)

    /**
     * Map each element from this channel using [mapper], from type [T] to [R]. If [mapper] returns null, the element
     * is skipped.
     * */
    fun <R : Any> mapNotNull(mapper: Function<in T, R?>): ChannelReceiver<R> = MapNotNullChannel(this, mapper)

    /**
     * Filter elements from this channel using [predicate].
     * */
    fun filter(predicate: Predicate<in T>): ChannelReceiver<T> = FilterChannel(this, predicate)
}

/**
 * State properties of a [ChannelReceiver] / [ChannelSender].
 * */
interface ChannelState {
    /**
     * Return whether the channel is closed.
     * */
    val isClosed: Boolean

    /**
     * Current channel size.
     * */
    val size: Int

    /**
     * Return whether the channel is empty.
     * */
    val isEmpty: Boolean
        get() = size == 0
}
