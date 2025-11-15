package io.channels.core

import io.channels.core.blocking.NotificationHandle
import kotlinx.atomicfu.atomic
import org.jctools.queues.MpscArrayQueue
import org.jctools.queues.MpscUnboundedXaddArrayQueue
import org.jctools.queues.SpscArrayQueue
import org.jctools.queues.SpscUnboundedArrayQueue
import java.util.*

private typealias OnCloseCallback = () -> Unit

/**
 * A [Queue]-based [Channel].
 * */
class QueueChannel<T : Any> @JvmOverloads constructor(
    private val queue: Queue<T>,
    private val onClose: OnCloseCallback = {},
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
        private const val DEFAULT_CHUNK_SIZE = 64

        /**
         * Returns a [QueueChannel] that uses an unbounded MPSC (multiple-producer, single-consumer) queue.
         * */
        @JvmStatic
        @JvmOverloads
        fun <T : Any> mpscUnbounded(onClose: OnCloseCallback = {}): QueueChannel<T> {
            return QueueChannel(MpscUnboundedXaddArrayQueue(DEFAULT_CHUNK_SIZE), onClose)
        }

        /**
         * Returns a [QueueChannel] that uses an MPSC (multiple-producer, single-consumer) queue with a bounded
         * capacity.
         * */
        @JvmStatic
        @JvmOverloads
        fun <T : Any> mpscBounded(capacity: Int, onClose: OnCloseCallback = {}): QueueChannel<T> {
            return QueueChannel(MpscArrayQueue(capacity), onClose)
        }

        /**
         * Returns a [QueueChannel] that uses an unbounded SPSC (single-producer, single-consumer) queue.
         * */
        @JvmStatic
        @JvmOverloads
        fun <T : Any> spscUnbounded(onClose: OnCloseCallback = {}): QueueChannel<T> {
            return QueueChannel(SpscUnboundedArrayQueue(DEFAULT_CHUNK_SIZE), onClose)
        }

        /**
         * Returns a [QueueChannel] that uses an SPSC (single-producer, single-consumer) queue with a bounded
         * capacity.
         * */
        @JvmStatic
        @JvmOverloads
        fun <T : Any> spscBounded(capacity: Int, onClose: OnCloseCallback = {}): QueueChannel<T> {
            return QueueChannel(SpscArrayQueue<T>(capacity), onClose)
        }
    }
}
