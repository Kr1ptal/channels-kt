package io.channels.core

import io.channels.core.blocking.BlockingStrategy
import io.channels.core.operator.FilterChannel
import io.channels.core.operator.MapChannel
import io.channels.core.operator.MapNotNullChannel
import java.io.Closeable
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate

/**
 * A channel that is both a sender and a receiver. A channel can possibly have multiple senders, depending on the
 * underlying implementation, but can have only a single receiver.
 * */
interface Channel<T : Any> : ChannelSender<T>, ChannelReceiver<T>

/**
 * A [ChannelSender] that delegates all calls to [parent]. This is useful to make the underlying [ChannelSender]
 * implementation unavailable and unmodifiable to the public.
 * */
class DelegatingChannelSender<T : Any>(private val parent: ChannelSender<T>) : ChannelSender<T> by parent

/**
 * A [ChannelReceiver] that delegates all calls to [parent]. This is useful to make the underlying [ChannelReceiver]
 * implementation unavailable and unmodifiable to the public.
 * */
class DelegatingChannelReceiver<T : Any>(private val parent: ChannelReceiver<T>) : ChannelReceiver<T> by parent

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
     * Invokes [listener] **after** a new element is added to the channel or the channel is closed.
     *
     * NOTE: the provided [listener] should be lightweight and not perform any blocking operations. It can also be
     * called from multiple threads.
     * */
    fun onStateChange(listener: Runnable)

    /**
     * Iterates over the elements of this channel, calling [consumer] for each element. This blocks the calling thread
     * until the channel is closed.
     * */
    fun forEach(consumer: Consumer<in T>)

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
     * Returns a [ChannelReceiver] that uses [blockingStrategy] to block until the next element is available.
     * */
    fun withBlockingStrategy(blockingStrategy: BlockingStrategy): ChannelReceiver<T> {
        return BlockingStrategyChannelReceiver(this, blockingStrategy)
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
