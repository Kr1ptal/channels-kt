package io.channels.core.blocking

import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock

/**
 * JVM implementation of [PlatformLock] using java.util.concurrent.locks.
 *
 * This implementation wraps [ReentrantLock] and its associated [Condition]
 * to provide the integrated lock + condition API.
 */
internal actual class PlatformLock {
    private val lock = ReentrantLock()
    private val condition: Condition = lock.newCondition()

    actual fun lock() {
        lock.lock()
    }

    actual fun unlock() {
        lock.unlock()
    }

    actual fun await() {
        condition.await()
    }

    actual fun signalAll() {
        condition.signalAll()
    }
}
