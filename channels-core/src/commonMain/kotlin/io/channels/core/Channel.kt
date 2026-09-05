package io.channels.core

import io.channels.core.blocking.NotificationHandle
import io.channels.core.operator.FilterChannel
import io.channels.core.operator.MapChannel
import io.channels.core.operator.MapNotNullChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

/**
 * A channel that is both a sender and a receiver. A channel can possibly have multiple senders, depending on the
 * underlying implementation, but can have only a single receiver.
 * */
interface Channel<T : Any> : ChannelSender<T>, ChannelReceiver<T>

/**
 * A sender end of a channel. The same sender can possibly be used by multiple threads at the same time, but only
 * if the underlying queue supports it.
 * */
interface ChannelSender<in T : Any> : ChannelState, AutoCloseable {
    /**
     * Offer an element to the channel, returning true if the element was added to the channel, false otherwise. This
     * method is non-blocking.
     * */
    fun offer(element: T): Boolean
}

/**
 * A receiver end of a channel. Same instance can only be used by one thread at a time, otherwise the behavior is
 * undefined. Only one receive or iteration may be active at a time, including while suspended.
 * */
interface ChannelReceiver<out T : Any> : PlatformChannelReceiver<T> {
    /** Get the [NotificationHandle] used to coordinate state-change subscribers. */
    override val notificationHandle: NotificationHandle

    /** Suspend until the next element is available, or return null when this receiver is closed and empty. */
    suspend fun receive(): T? {
        // Keep the suspension loop separate so an immediately available value needs no coroutine state machine.
        val next = poll()
        if (next != null) return next

        // Re-poll after observing closure: a producer may have offered the final value after the first poll.
        if (isClosed) return poll()

        return receiveAllocating()
    }

    /** Suspend and consume elements until this receiver is closed. */
    suspend fun forEachSuspend(consumer: suspend (@UnsafeVariance T) -> Unit) {
        try {
            while (true) {
                consumer(receive() ?: break)
            }
        } finally {
            close()
        }
    }

    /** Wrap this receiver in a coroutine [ReceiveChannel]. */
    fun asCoroutineReceiver(ctx: CoroutineContext = Dispatchers.Default): ReceiveChannel<T> {
        val result = Channel<@UnsafeVariance T>(RENDEZVOUS)

        CoroutineScope(ctx).launch {
            try {
                forEachSuspend(result::send)
            } finally {
                result.close()
                close()
            }
        }

        return result
    }

    /** Convert this receiver into a coroutine [Flow]. */
    fun asFlow(): Flow<T> = flow { forEachSuspend { emit(it) } }

    /**
     * Remove and return the next element from the channel, or null if the channel is empty. This method will never
     * block.
     * */
    override fun poll(): T?

    /**
     * Map each element from this channel using [mapper], from type [T] to [R].
     * */
    fun <R : Any> map(mapper: ChannelFunction<in T, R>): ChannelReceiver<R> = MapChannel(this, mapper)

    /**
     * Map each element from this channel using [mapper], from type [T] to [R]. If [mapper] returns null, the element
     * is skipped.
     * */
    fun <R : Any> mapNotNull(mapper: ChannelFunction<in T, R?>): ChannelReceiver<R> = MapNotNullChannel(this, mapper)

    /**
     * Filter elements from this channel using [predicate].
     * */
    fun filter(predicate: ChannelPredicate<in T>): ChannelReceiver<T> = FilterChannel(this, predicate)
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

/** Only entered after an empty poll, keeping suspension allocations off the receive fast path. */
private suspend fun <T : Any> ChannelReceiver<T>.receiveAllocating(): T? {
    while (true) {
        val next = poll()
        if (next != null) return next

        if (isClosed) return poll()

        notificationHandle.awaitStateChange()
    }
}
