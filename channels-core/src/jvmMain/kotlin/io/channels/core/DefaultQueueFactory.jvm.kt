package io.channels.core

import org.jctools.queues.MpscArrayQueue
import org.jctools.queues.MpscUnboundedXaddArrayQueue
import org.jctools.queues.SpscArrayQueue
import org.jctools.queues.SpscUnboundedArrayQueue

internal actual object DefaultQueueFactory {
    private const val DEFAULT_CHUNK_SIZE = 64

    actual fun <T : Any> mpscUnbounded(): Queue<T> {
        return MpscUnboundedXaddArrayQueue(DEFAULT_CHUNK_SIZE)
    }

    actual fun <T : Any> mpscBounded(capacity: Int): Queue<T> {
        return MpscArrayQueue(capacity)
    }

    actual fun <T : Any> spscUnbounded(): Queue<T> {
        return SpscUnboundedArrayQueue(DEFAULT_CHUNK_SIZE)
    }

    actual fun <T : Any> spscBounded(capacity: Int): Queue<T> {
        return SpscArrayQueue<T>(capacity)
    }
}
