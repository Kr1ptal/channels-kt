package io.channels.core

import io.channels.core.blocking.NotificationHandle
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

/**
 * A [Channel] that can send / receive a single element. After the element is sent and received, the channel is closed
 * and cannot be used again.
 * */
class OneShotChannel<T : Any> @JvmOverloads constructor(value: T? = null) : Channel<T> {
    private val state = AtomicReference<Any>(value)
    private var notificationHandle = NotificationHandle(this)

    override fun offer(element: T): Boolean {
        if (state.compareAndSet(null, element)) {
            notificationHandle.signalDataAvailable()
            return true
        }
        return false
    }

    override fun close() {
        if (state.getAndSet(CONSUMED) !== CONSUMED) {
            notificationHandle.signalDataAvailable()
        }
    }

    override fun take(): T? {
        while (true) {
            val ret = poll()
            if (ret != null) {
                return ret
            }

            // check after polling, so we still drain the queue even if unsubscribed
            if (isClosed) {
                break
            }

            // if no next element, wait until next event is available
            notificationHandle.waitWithParking()
        }

        return null
    }

    @Suppress("UNCHECKED_CAST")
    override fun poll(): T? {
        val ret = state.getAndUpdate { if (it == null) null else CONSUMED }
        return if (ret == null || ret === CONSUMED) null else ret as T
    }

    override val isClosed: Boolean
        get() = state.get() === CONSUMED

    override val size: Int
        get() {
            val ret = state.get()
            return if (ret == null || ret === CONSUMED) 0 else 1
        }

    override fun forEach(consumer: Consumer<in T>) {
        consumer.accept(take() ?: return)
    }

    companion object {
        private val CONSUMED = Any()
    }
}
