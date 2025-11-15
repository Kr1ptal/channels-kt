package io.channels.core

/**
 * Platform-specific queue interface for channel implementations.
 * On JVM, this is an alias for java.util.Queue.
 */
expect interface Queue<T : Any> {
    /**
     * Inserts the specified element into this queue if it is possible to do so
     * immediately without violating capacity restrictions.
     *
     * @return true if the element was added to this queue, false otherwise
     */
    fun offer(element: T): Boolean

    /**
     * Retrieves and removes the head of this queue, or returns null if this queue is empty.
     */
    fun poll(): T?

    /**
     * Returns the number of elements in this queue.
     */
    val size: Int
}
