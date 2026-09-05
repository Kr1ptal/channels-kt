package io.channels.core.blocking

import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock

/**
 * JVM and Android implementation of [PlatformLock] using java.util.concurrent.locks.
 *
 * This implementation wraps [ReentrantLock] and its associated [Condition]
 * to provide the integrated lock + condition API.
 */
internal class PlatformLock {
    private val lock = ReentrantLock()
    private val condition: Condition = lock.newCondition()

    fun lock() {
        lock.lock()
    }

    fun unlock() {
        lock.unlock()
    }

    fun await() {
        condition.await()
    }

    fun signalAll() {
        condition.signalAll()
    }
}

internal inline fun <T> PlatformLock.withLock(block: () -> T): T {
    lock()
    try {
        return block()
    } finally {
        unlock()
    }
}
