package io.channels.core

import io.channels.core.waiting.WaitStrategy
import org.jctools.queues.MpscArrayQueue
import org.jctools.queues.MpscUnboundedXaddArrayQueue
import org.jctools.queues.SpscArrayQueue
import org.jctools.queues.SpscUnboundedArrayQueue
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A [Queue]-based [Channel].
 * */
class QueueChannel<T : Any>(
    private val queue: Queue<T>,
    private val onClose: Runnable,
) : Channel<T> {
    private val closed = AtomicBoolean(false)

    @Volatile
    private var iterator: BlockingIterator<T>? = null

    override fun offer(element: T): Boolean {
        if (queue.offer(element)) {
            iterator?.signalStateChange()
            return true
        }

        return false
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            iterator?.signalStateChange()
            onClose.run()
        }
    }

    override fun tryPoll(): T? {
        return queue.poll()
    }

    override val size: Int
        get() = queue.size

    override fun iterator(waitStrategy: WaitStrategy): Iterator<T> {
        val iter = iterator
        if (iter != null) {
            return iter
        }

        if (closed.get()) {
            return EmptyIterator
        }

        val ret = BlockingIterator(this, waitStrategy) { closed.get() }
        return ret.also { iterator = it }
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
