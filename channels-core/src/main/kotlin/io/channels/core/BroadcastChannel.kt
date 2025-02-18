package io.channels.core

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function

/**
 * A [SubscriptionChannel] that supports multiple subscribers and sends (broadcasts) elements to all subscribers.
 * */
class BroadcastChannel<T : Any>(
    private val channelFactory: Function<Runnable, Channel<T>>,
) : ChannelSender<T>, SubscriptionChannel<T> {
    private val id = AtomicInteger(0)
    private val idToSubscription = ConcurrentHashMap<Int, Channel<T>>()
    private val subscriptions = CopyOnWriteArrayList<Channel<T>>()
    private val closed = AtomicBoolean(false)

    @Volatile
    private var seqLock = 0

    override fun subscribe(): ChannelReceiver<T> {
        seqLock++

        val id = id.getAndIncrement()
        val onClose = unsub@{
            val sub = idToSubscription.remove(id) ?: return@unsub
            subscriptions.remove(sub)
        }

        val ret = channelFactory.apply(onClose)

        // if broadcast channel is closed, close the receiver as well
        if (isClosed) {
            ret.close()
        } else {
            idToSubscription[id] = ret
            subscriptions.add(ret)
        }

        seqLock++

        return ret
    }

    /**
     * Offer an element to all subscribed channels, returning true if the element was added to at least one
     * channel, false otherwise.
     * */
    override fun offer(element: T): Boolean {
        if (subscriptions.isEmpty()) {
            return false
        }

        var success = false

        // need to use for-each iterator in case elements get removed
        for (sub in subscriptions) {
            val wasSent = sub.offer(element)
            success = success || wasSent
        }

        return success
    }

    /**
     * Closes all subscribed channels, blocking until all channels are closed.
     * */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            while (true) {
                val seq = seqLock
                if (seq and 1 != 0) {
                    continue
                }

                // need to use for-each iterator in case elements get removed
                for (sub in subscriptions) {
                    sub.close()
                }

                if (seq == seqLock) {
                    break
                }
            }
        }
    }

    override val isClosed: Boolean
        get() = closed.get()

    /**
     * Current number of subscribers.
     * */
    override val size: Int
        get() = subscriptions.size

    companion object {
        /**
         * Returns a [BroadcastChannel] that uses an unbounded MPSC (multiple-producer, single-consumer) queue for
         * each subscriber.
         * */
        @JvmStatic
        fun <T : Any> mpscUnbounded(): BroadcastChannel<T> {
            return BroadcastChannel { QueueChannel.mpscUnbounded(it) }
        }

        /**
         * Returns a [BroadcastChannel] that uses an MPSC (multiple-producer, single-consumer) queue with a bounded
         * capacity for each subscriber.
         * */
        @JvmStatic
        fun <T : Any> mpscBounded(capacity: Int): BroadcastChannel<T> {
            return BroadcastChannel { QueueChannel.mpscBounded(capacity, it) }
        }

        /**
         * Returns a [BroadcastChannel] that uses an unbounded SPSC (single-producer, single-consumer) queue for
         * each subscriber.
         * */
        @JvmStatic
        fun <T : Any> spscUnbounded(): BroadcastChannel<T> {
            return BroadcastChannel { QueueChannel.spscUnbounded(it) }
        }

        /**
         * Returns a [BroadcastChannel] that uses an SPSC (single-producer, single-consumer) queue with a bounded
         * capacity for each subscriber.
         * */
        @JvmStatic
        fun <T : Any> spscBounded(capacity: Int): BroadcastChannel<T> {
            return BroadcastChannel { QueueChannel.spscBounded(capacity, it) }
        }
    }
}
