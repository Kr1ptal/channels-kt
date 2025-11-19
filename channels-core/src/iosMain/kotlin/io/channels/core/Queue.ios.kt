package io.channels.core

/**
 * iOS implementation of Queue interface.
 * Unlike JVM which uses a typealias to java.util.Queue, iOS requires an actual interface definition.
 */
actual interface Queue<T : Any> {
    /**
     * Inserts the specified element into this queue if it is possible to do so
     * immediately without violating capacity restrictions.
     *
     * @return true if the element was added to this queue, false otherwise
     */
    actual fun offer(element: T): Boolean

    /**
     * Retrieves and removes the head of this queue, or returns null if this queue is empty.
     */
    actual fun poll(): T?

    /**
     * Returns the number of elements in this queue.
     */
    actual val size: Int
}
