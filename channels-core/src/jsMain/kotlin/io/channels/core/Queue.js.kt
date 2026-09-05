package io.channels.core

/** JavaScript implementation of [Queue]. */
actual interface Queue<T : Any> {
    actual fun offer(element: T): Boolean

    actual fun poll(): T?

    actual val size: Int
}
