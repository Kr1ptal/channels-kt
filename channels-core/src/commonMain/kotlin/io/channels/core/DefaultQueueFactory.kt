package io.channels.core

/**
 * Contains platform-specific queue factory functions that are provided by default.
 * */
internal expect object DefaultQueueFactory {
    fun <T : Any> mpscUnbounded(): Queue<T>

    fun <T : Any> mpscBounded(capacity: Int): Queue<T>

    fun <T : Any> spscUnbounded(): Queue<T>

    fun <T : Any> spscBounded(capacity: Int): Queue<T>
}
