package io.channels.core

import io.channels.core.blocking.NotificationHandle
import kotlinx.atomicfu.atomic

/**
 * A [Channel] that can send / receive a single element. After the element is sent and received, the channel is closed
 * and cannot be used again.
 * */
class OneShotChannel<T : Any> @JvmOverloads constructor(value: T? = null) : Channel<T> {
    private val state = atomic<Any?>(value)

    override val notificationHandle = NotificationHandle(this)

    override fun offer(element: T): Boolean {
        if (state.compareAndSet(null, element)) {
            notificationHandle.signalStateChange()
            return true
        }
        return false
    }

    override fun close() {
        if (state.getAndSet(CONSUMED) !== CONSUMED) {
            notificationHandle.signalStateChange()
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
        while (true) {
            val current = state.value
            if (current == null || current === CONSUMED) {
                return null
            }
            if (state.compareAndSet(current, CONSUMED)) {
                return current as T
            }
        }
    }

    override val isClosed: Boolean
        get() = state.value === CONSUMED

    override val size: Int
        get() {
            val ret = state.value
            return if (ret == null || ret === CONSUMED) 0 else 1
        }

    override fun forEach(consumer: ChannelConsumer<in T>) {
        consumer.accept(take() ?: return)
    }

    companion object {
        private val CONSUMED = Any()
    }
}
