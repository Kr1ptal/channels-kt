package io.channels.core

import io.channels.core.blocking.NotificationHandle
import org.jctools.queues.MpscArrayQueue
import org.jctools.queues.MpscUnboundedXaddArrayQueue
import org.jctools.queues.SpscArrayQueue
import org.jctools.queues.SpscUnboundedArrayQueue
import java.util.*
import java.util.function.Consumer
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * A [Queue]-based [Channel].
 * */
@OptIn(ExperimentalAtomicApi::class)
class QueueChannel<T : Any> @JvmOverloads constructor(
    private val queue: Queue<T>,
    private val onClose: Runnable = Runnable {},
) : Channel<T> {
    private val closed = AtomicBoolean(false)

    override val notificationHandle = NotificationHandle(this)

    override fun offer(element: T): Boolean {
        if (!isClosed && queue.offer(element)) {
            notificationHandle.signalStateChange()
            return true
        }

        return false
    }

    override fun close() {
        if (closed.compareAndSet(expectedValue = false, newValue = true)) {
            onClose.run()
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

            // if no next element, wait until next event is available
            notificationHandle.waitWithParking()
        }
        return null
    }

    override fun poll(): T? {
        return queue.poll()
    }

    override val isClosed: Boolean
        get() = closed.load()

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
