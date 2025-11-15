package io.channels.core

import io.channels.core.blocking.NotificationHandle
import kotlinx.atomicfu.atomic

/**
 * A [Queue]-based [Channel].
 * */
class QueueChannel<T : Any>(
    private val queue: Queue<T>,
    private val onClose: () -> Unit = {},
) : Channel<T> {
    private val closed = atomic(false)

    override val notificationHandle = NotificationHandle(this)

    override fun offer(element: T): Boolean {
        if (!isClosed && queue.offer(element)) {
            notificationHandle.signalStateChange()
            return true
        }

        return false
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            onClose.invoke()
            notificationHandle.signalStateChange()
        }
    }

    override fun take(): T? {
        while (true) {
            val ret = poll()
            if (ret != null) {
                return ret
            }

            // check after polling, so we still drain the queue even if unsubscribed
            if (isClosed) {
                break
            }

            // if no next element, wait until the next event is available
            notificationHandle.waitWithParking()
        }
        return null
    }

    override fun poll(): T? {
        return queue.poll()
    }

    override val isClosed: Boolean
        get() = closed.value

    override val size: Int
        get() = queue.size

    override fun forEach(consumer: ChannelConsumer<in T>) {
        while (true) {
            consumer.accept(take() ?: break)
        }
    }

    companion object {
        /**
         * Returns a [QueueChannel] that uses an unbounded MPSC (multiple-producer, single-consumer) queue.
         * */
        @JvmOverloads
        fun <T : Any> mpscUnbounded(onClose: () -> Unit = {}): QueueChannel<T> {
            return QueueChannel(DefaultQueueFactory.mpscUnbounded(), onClose)
        }

        /**
         * Returns a [QueueChannel] that uses an MPSC (multiple-producer, single-consumer) queue with a bounded
         * capacity.
         * */
        @JvmOverloads
        fun <T : Any> mpscBounded(capacity: Int, onClose: () -> Unit = {}): QueueChannel<T> {
            return QueueChannel(DefaultQueueFactory.mpscBounded(capacity), onClose)
        }

        /**
         * Returns a [QueueChannel] that uses an unbounded SPSC (single-producer, single-consumer) queue.
         * */
        @JvmOverloads
        fun <T : Any> spscUnbounded(onClose: () -> Unit = {}): QueueChannel<T> {
            return QueueChannel(DefaultQueueFactory.spscUnbounded(), onClose)
        }

        /**
         * Returns a [QueueChannel] that uses an SPSC (single-producer, single-consumer) queue with a bounded
         * capacity.
         * */
        @JvmOverloads
        fun <T : Any> spscBounded(capacity: Int, onClose: () -> Unit = {}): QueueChannel<T> {
            return QueueChannel(DefaultQueueFactory.spscBounded(capacity), onClose)
        }
    }
}
