package io.channels.core

/**
 * JavaScript queue factory.
 *
 * Kotlin/JS values are confined to a single event loop, so the MPSC and SPSC
 * variants share the same array-backed implementation.
 */
internal actual object DefaultQueueFactory {
    actual fun <T : Any> mpscUnbounded(): Queue<T> = ArrayQueue()

    actual fun <T : Any> mpscBounded(capacity: Int): Queue<T> = ArrayQueue(capacity)

    actual fun <T : Any> spscUnbounded(): Queue<T> = ArrayQueue()

    actual fun <T : Any> spscBounded(capacity: Int): Queue<T> = ArrayQueue(capacity)
}

private class ArrayQueue<T : Any>(private val capacity: Int = Int.MAX_VALUE) : Queue<T> {
    private val elements = ArrayDeque<T>()

    init {
        require(capacity > 0) { "Capacity must be positive: $capacity" }
    }

    override val size: Int
        get() = elements.size

    override fun offer(element: T): Boolean {
        if (elements.size >= capacity) return false
        elements.addLast(element)
        return true
    }

    override fun poll(): T? = elements.removeFirstOrNull()
}
