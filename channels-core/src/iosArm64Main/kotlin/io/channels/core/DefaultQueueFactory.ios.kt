package io.channels.core

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSLock
import platform.Foundation.NSMutableArray

/**
 * iOS implementation of DefaultQueueFactory using Foundation collections.
 *
 * Uses NSMutableArray with NSLock for thread-safe queue operations.
 * While not as performant as JCTools lock-free queues on JVM, this provides
 * correct multi-threaded behavior for MPSC and SPSC patterns.
 */
@OptIn(ExperimentalForeignApi::class)
internal actual object DefaultQueueFactory {
    private const val DEFAULT_INITIAL_CAPACITY = 64

    actual fun <T : Any> mpscUnbounded(): Queue<T> {
        return FoundationQueue(capacity = null)
    }

    actual fun <T : Any> mpscBounded(capacity: Int): Queue<T> {
        require(capacity > 0) { "Capacity must be positive: $capacity" }
        return FoundationQueue(capacity = capacity)
    }

    actual fun <T : Any> spscUnbounded(): Queue<T> {
        return FoundationQueue(capacity = null)
    }

    actual fun <T : Any> spscBounded(capacity: Int): Queue<T> {
        require(capacity > 0) { "Capacity must be positive: $capacity" }
        return FoundationQueue(capacity = capacity)
    }
}

/**
 * Thread-safe queue implementation using Foundation NSMutableArray and NSLock.
 *
 * @param capacity Maximum capacity of the queue, or null for unbounded
 */
@OptIn(ExperimentalForeignApi::class)
private class FoundationQueue<T : Any>(
    private val capacity: Int?,
) : Queue<T> {
    private val array = NSMutableArray()
    private val lock = NSLock()

    override fun offer(element: T): Boolean {
        lock.lock()
        try {
            // Check capacity for bounded queues
            if (capacity != null && array.count.toInt() >= capacity) {
                return false
            }
            array.addObject(element)
            return true
        } finally {
            lock.unlock()
        }
    }

    override fun poll(): T? {
        lock.lock()
        try {
            if (array.count == 0uL) {
                return null
            }
            @Suppress("UNCHECKED_CAST")
            val element = array.objectAtIndex(0u) as T
            array.removeObjectAtIndex(0u)
            return element
        } finally {
            lock.unlock()
        }
    }

    override val size: Int
        get() {
            lock.lock()
            try {
                return array.count.toInt()
            } finally {
                lock.unlock()
            }
        }
}
