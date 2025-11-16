package io.channels.core

import co.touchlab.stately.collections.ConcurrentMutableMap
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlin.concurrent.Volatile

/**
 * A [SubscriptionChannel] that supports multiple subscribers and sends (broadcasts) elements to all subscribers.
 * */
class BroadcastChannel<T : Any>(
    private val channelFactory: ChannelFactory<T>,
) : ChannelSender<T>, SubscriptionChannel<T> {
    private val id = atomic(0)
    private val idToSubscription = ConcurrentMutableMap<Int, Channel<T>>()
    private val subscriptions: AtomicRef<List<Channel<T>>> = atomic(emptyList())
    private val closed = atomic(false)

    @Volatile
    private var seqLock = 0

    override fun subscribe(): ChannelReceiver<T> {
        seqLock++

        val id = id.getAndIncrement()
        val onClose = unsub@{
            val sub = idToSubscription.remove(id) ?: return@unsub
            subscriptions.update { it - sub }
        }

        val ret = channelFactory.newChannel(onClose)

        // if the broadcast channel is closed, close the receiver as well
        if (isClosed) {
            ret.close()
        } else {
            idToSubscription[id] = ret
            subscriptions.update { it + ret }
        }

        seqLock++

        return ret
    }

    /**
     * Offer an element to all subscribed channels, returning true if the element was added to at least one
     * channel, false otherwise.
     * */
    override fun offer(element: T): Boolean {
        val currentSubs = subscriptions.value
        if (currentSubs.isEmpty()) {
            return false
        }

        var success = false

        // need to use for-each iterator in case elements get removed
        for (sub in currentSubs) {
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
                val subscriptions = this@BroadcastChannel.subscriptions.value
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
        get() = closed.value

    /**
     * Current number of subscribers.
     * */
    override val size: Int
        get() = subscriptions.value.size

    /**
     * Factory for creating [io.channels.core.BroadcastChannel]'s subscription channels.
     * */
    fun interface ChannelFactory<T : Any> {
        fun newChannel(onClose: () -> Unit): Channel<T>
    }

    companion object {
        /**
         * Returns a [BroadcastChannel] that uses an unbounded MPSC (multiple-producer, single-consumer) queue for
         * each subscriber.
         * */
        @PlatformStatic
        fun <T : Any> mpscUnbounded(): BroadcastChannel<T> {
            return BroadcastChannel { QueueChannel.mpscUnbounded(it) }
        }

        /**
         * Returns a [BroadcastChannel] that uses an MPSC (multiple-producer, single-consumer) queue with a bounded
         * capacity for each subscriber.
         * */
        @PlatformStatic
        fun <T : Any> mpscBounded(capacity: Int): BroadcastChannel<T> {
            return BroadcastChannel { QueueChannel.mpscBounded(capacity, it) }
        }

        /**
         * Returns a [BroadcastChannel] that uses an unbounded SPSC (single-producer, single-consumer) queue for
         * each subscriber.
         * */
        @PlatformStatic
        fun <T : Any> spscUnbounded(): BroadcastChannel<T> {
            return BroadcastChannel { QueueChannel.spscUnbounded(it) }
        }

        /**
         * Returns a [BroadcastChannel] that uses an SPSC (single-producer, single-consumer) queue with a bounded
         * capacity for each subscriber.
         * */
        @PlatformStatic
        fun <T : Any> spscBounded(capacity: Int): BroadcastChannel<T> {
            return BroadcastChannel { QueueChannel.spscBounded(capacity, it) }
        }
    }
}
