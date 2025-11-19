package io.channels.core

import kotlinx.atomicfu.atomic
import kotlinx.cinterop.ExperimentalForeignApi

/**
 * iOS implementation of DefaultQueueFactory using Foundation collections. Uses a custom, lock-free queue
 * implementation.
 */
@OptIn(ExperimentalForeignApi::class)
internal actual object DefaultQueueFactory {
    actual fun <T : Any> mpscUnbounded(): Queue<T> {
        return LockFreeQueue()
    }

    actual fun <T : Any> mpscBounded(capacity: Int): Queue<T> {
        require(capacity > 0) { "Capacity must be positive: $capacity" }
        return LockFreeQueue(capacity = capacity)
    }

    actual fun <T : Any> spscUnbounded(): Queue<T> {
        return LockFreeQueue()
    }

    actual fun <T : Any> spscBounded(capacity: Int): Queue<T> {
        require(capacity > 0) { "Capacity must be positive: $capacity" }
        return LockFreeQueue(capacity = capacity)
    }
}

/**
 * Lock-free MPMC queue with optional capacity bound.
 *
 * - Multi-producer, multi-consumer
 * - Lock-free (Michael–Scott linked queue)
 * - Bounded: offer() returns false when capacity is reached
 */
private class LockFreeQueue<T : Any>(private val capacity: Int = Int.MAX_VALUE) : Queue<T> {
    init {
        require(capacity > 0) { "capacity must be > 0" }
    }

    // Node in a singly-linked list
    private class Node<E : Any>(val value: E?) {
        val next = atomic<Node<E>?>(null)
    }

    // Dummy node to start the queue
    private val head = atomic(Node<T>(null))
    private val tail = atomic(head.value)

    // Logical size: counts enqueued + "reserved" slots
    private val _size = atomic(0)

    override val size: Int
        get() = _size.value

    override fun offer(element: T): Boolean {
        // 1) Reserve a slot against capacity
        while (true) {
            val s = _size.value
            if (s >= capacity) {
                return false // queue is full
            }
            if (_size.compareAndSet(s, s + 1)) {
                break // reserved one slot
            }
        }

        // 2) Enqueue node using Michael–Scott algorithm
        val newNode = Node(element)

        while (true) {
            val curTail = tail.value
            val tailNext = curTail.next.value

            if (curTail === tail.value) {
                if (tailNext == null) {
                    // Tail is real tail: try to link the new node
                    if (curTail.next.compareAndSet(null, newNode)) {
                        // Best-effort tail advance
                        tail.compareAndSet(curTail, newNode)
                        return true
                    }
                    // else: someone else inserted first; retry
                } else {
                    // Tail is lagging behind; help advance it
                    tail.compareAndSet(curTail, tailNext)
                }
            }
        }
    }

    override fun poll(): T? {
        while (true) {
            val curHead = head.value
            val curTail = tail.value
            val headNext = curHead.next.value

            if (curHead === head.value) {
                if (headNext == null) {
                    // Empty queue
                    return null
                }

                if (curHead === curTail) {
                    // Tail is behind, help move it
                    tail.compareAndSet(curTail, headNext)
                } else {
                    val value = headNext.value
                    // Try to swing head forward
                    if (head.compareAndSet(curHead, headNext)) {
                        if (value != null) {
                            // We successfully removed one logical element
                            _size.decrementAndGet()
                        }
                        return value
                    }
                    // else: lost race, retry
                }
            }
        }
    }
}
