package io.channels.core

import io.channels.core.operation.FilterChannel
import io.channels.core.operation.MapChannel
import io.channels.core.operation.MapNotNullChannel
import io.channels.core.waiting.ParkingWaitStrategy
import io.channels.core.waiting.WaitStrategy
import java.io.Closeable
import java.util.concurrent.ThreadFactory
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate

/**
 * A channel that is both a sender and a receiver.
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
     * Offer an element to the channel, returning true if the element was added to the channel, false otherwise.
     * */
    fun offer(element: T): Boolean
}

/**
 * A receiver end of a channel. Same instance can only be used by one thread at a time, otherwise the behavior is
 * undefined.
 * */
interface ChannelReceiver<out T : Any> : ChannelState, Closeable {
    /**
     * Returns an iterator that uses [waitStrategy] to block until the next element is available or the channel is
     * closed.
     * */
    fun iterator(waitStrategy: WaitStrategy): Iterator<T>

    /**
     * Returns a blocking iterator that uses [ParkingWaitStrategy] to block until the next element is available or
     * the channel is closed.
     * */
    fun iterator() = iterator(ParkingWaitStrategy())

    /**
     * Iterates over the elements of this channel, calling [consumer] for each element. The [ParkingWaitStrategy] is
     * used to block until the next element is available or the channel is closed.
     * */
    fun forEach(consumer: Consumer<in T>) {
        iterator().forEach { consumer.accept(it) }
    }

    /**
     * Iterates over the elements of this channel, calling [consumer] for each element. The [waitStrategy] is used to
     * block until the next element is available or the channel is closed.
     * */
    fun forEach(waitStrategy: WaitStrategy, consumer: Consumer<in T>) {
        iterator(waitStrategy).forEach { consumer.accept(it) }
    }

    /**
     * Iterates over the elements of this channel on a new thread, created by [factory], to avoid blocking the
     * calling thread. The new thread will use the [iterator] with a [ParkingWaitStrategy].
     * */
    fun forEachAsync(factory: ThreadFactory, consumer: Consumer<in T>): ChannelReceiver<T> {
        return forEachAsync(factory, ParkingWaitStrategy(), consumer)
    }

    /**
     * Iterates over the elements of this channel on a new thread, created by [factory], to avoid blocking the
     * calling thread. The new thread will use the [iterator] with provided [waitStrategy].
     * */
    fun forEachAsync(
        factory: ThreadFactory,
        waitStrategy: WaitStrategy,
        consumer: Consumer<in T>
    ): ChannelReceiver<T> {
        factory.newThread { iterator(waitStrategy).forEach { consumer.accept(it) } }.start()
        return this
    }

    /**
     * Remove and return the next element from the channel, or null if the channel is empty. This method will never
     * block.
     * */
    fun tryPoll(): T?

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
     * Current channel size.
     * */
    val size: Int

    /**
     * Return whether the channel is empty.
     * */
    val isEmpty: Boolean
        get() = size == 0
}
