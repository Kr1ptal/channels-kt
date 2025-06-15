package io.channels.core

import io.channels.core.blocking.NotificationHandle
import org.jctools.queues.MpscArrayQueue
import org.jctools.queues.MpscUnboundedXaddArrayQueue
import org.jctools.queues.SpscArrayQueue
import org.jctools.queues.SpscUnboundedArrayQueue
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer

/**
 * A [Queue]-based [Channel].
 * */
class QueueChannel<T : Any> @JvmOverloads constructor(
    private val queue: Queue<T>,
    private val onClose: Runnable = Runnable {},
) : Channel<T> {
    private val closed = AtomicBoolean(false)
    private val notificationHandle = NotificationHandle(this)

    override fun offer(element: T): Boolean {
        if (!isClosed && queue.offer(element)) {
            notificationHandle.signalDataAvailable()
            return true
        }

        return false
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            onClose.run()
            notificationHandle.signalDataAvailable()
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

            // if no next element, wait until next event is available
            notificationHandle.waitWithParking()
        }
        return null
    }

    override fun poll(): T? {
        return queue.poll()
    }

    override val isClosed: Boolean
        get() = closed.get()

    override val size: Int
        get() = queue.size

    override fun forEach(consumer: Consumer<in T>) {
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
        fun <T : Any> mpscUnbounded(onClose: Runnable = Runnable {}): QueueChannel<T> {
            return QueueChannel(MpscUnboundedXaddArrayQueue(DEFAULT_CHUNK_SIZE), onClose)
        }

        /**
         * Returns a [QueueChannel] that uses an MPSC (multiple-producer, single-consumer) queue with a bounded
         * capacity.
         * */
        @JvmStatic
        @JvmOverloads
        fun <T : Any> mpscBounded(capacity: Int, onClose: Runnable = Runnable {}): QueueChannel<T> {
            return QueueChannel(MpscArrayQueue(capacity), onClose)
        }

        /**
         * Returns a [QueueChannel] that uses an unbounded SPSC (single-producer, single-consumer) queue.
         * */
        @JvmStatic
        @JvmOverloads
        fun <T : Any> spscUnbounded(onClose: Runnable = Runnable {}): QueueChannel<T> {
            return QueueChannel(SpscUnboundedArrayQueue(DEFAULT_CHUNK_SIZE), onClose)
        }

        /**
         * Returns a [QueueChannel] that uses an SPSC (single-producer, single-consumer) queue with a bounded
         * capacity.
         * */
        @JvmStatic
        @JvmOverloads
        fun <T : Any> spscBounded(capacity: Int, onClose: Runnable = Runnable {}): QueueChannel<T> {
            return QueueChannel(SpscArrayQueue<T>(capacity), onClose)
        }
    }
}
