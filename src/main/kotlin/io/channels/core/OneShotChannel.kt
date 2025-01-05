package io.channels.core

import io.channels.core.waiting.WaitStrategy
import java.util.concurrent.atomic.AtomicReference

/**
 * A [Channel] that can send / receive a single element. After the element is sent and received, the channel is closed
 * and cannot be used again.
 * */
class OneShotChannel<T> : Channel<T> {
    private val state = AtomicReference<Any>(null)

    @Volatile
    private var iterator: BlockingIterator<T>? = null

    override fun offer(element: T): Boolean {
        if (state.compareAndSet(null, element)) {
            iterator?.signalStateChange()
            return true
        }
        return false
    }

    override fun close() {
        iterator?.signalStateChange()
        state.set(CLOSED)
    }

    @Suppress("UNCHECKED_CAST")
    override fun tryPoll(): T? {
        val ret = state.getAndUpdate { if (it == null) null else CLOSED }
        return if (ret == null || ret === CLOSED) null else ret as T
    }

    override val size: Int
        get() {
            val ret = state.get()
            return if (ret == null || ret === CLOSED) 0 else 1
        }

    override fun iterator(waitStrategy: WaitStrategy): Iterator<T> {
        val iter = iterator
        if (iter != null) {
            return iter
        }

        if (state.get() === CLOSED) {
            return EmptyIterator
        }

        val ret = BlockingIterator(this, waitStrategy) { state.get() === CLOSED }
        return ret.also { iterator = it }
    }

    companion object {
        private val CLOSED = Any()
    }
}
