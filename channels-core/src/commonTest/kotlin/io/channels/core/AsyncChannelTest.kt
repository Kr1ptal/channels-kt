package io.channels.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlin.time.Duration.Companion.milliseconds

class AsyncChannelTest : FunSpec({
    test("receive returns an available element") {
        val channel = QueueChannel.spscUnbounded<String>()
        channel.offer("hello")

        channel.receive() shouldBe "hello"
    }

    test("receive suspends until an element is available") {
        val channel = QueueChannel.spscUnbounded<String>()
        val result = async(start = CoroutineStart.UNDISPATCHED) { channel.receive() }

        channel.offer("hello")

        result.await() shouldBe "hello"
    }

    test("receive returns null when the receiver closes") {
        val channel = QueueChannel.spscUnbounded<String>()
        val result = async(start = CoroutineStart.UNDISPATCHED) { channel.receive() }

        channel.close()

        result.await() shouldBe null
    }

    test("receive drains buffered values after close") {
        val channel = QueueChannel.spscUnbounded<String>()
        channel.offer("hello")
        channel.close()

        channel.receive() shouldBe "hello"
        channel.receive() shouldBe null
    }

    test("receive observes an offer between the empty poll and waiter registration") {
        val channel = QueueChannel.spscUnbounded<String>()
        val receiver = AfterEmptyPollReceiver(channel) { channel.offer("hello") }

        withTimeout(5_000) { receiver.receive() } shouldBe "hello"
    }

    test("receive drains an offer followed by close after the empty poll") {
        val channel = QueueChannel.spscUnbounded<String>()
        val receiver = AfterEmptyPollReceiver(channel) {
            channel.offer("hello")
            channel.close()
        }

        receiver.receive() shouldBe "hello"
        receiver.receive() shouldBe null
    }

    test("cancelling a suspended receive leaves the channel reusable") {
        val channel = QueueChannel.spscUnbounded<String>()
        val cancelled = async(start = CoroutineStart.UNDISPATCHED) { channel.receive() }
        cancelled.cancelAndJoin()

        channel.isClosed shouldBe false
        val next = async(start = CoroutineStart.UNDISPATCHED) { channel.receive() }
        channel.offer("hello")

        withTimeout(5_000) { next.await() } shouldBe "hello"
    }

    test("cancellation after a signal does not consume the offered value") {
        runTest {
            val channel = QueueChannel.spscUnbounded<String>()
            val receiver = async(start = CoroutineStart.UNDISPATCHED) { channel.receive() }
            channel.offer("hello")
            // The continuation is scheduled, but the test dispatcher has not run it yet.
            receiver.cancelAndJoin()

            channel.receive() shouldBe "hello"
            channel.isClosed shouldBe false
        }
    }

    test("an undispatched receiver can register its next wait during a signal") {
        val channel = QueueChannel.spscUnbounded<Int>()
        val receiver = async(Dispatchers.Unconfined) {
            List(100) { channel.receive() }
        }
        repeat(100) { channel.offer(it) }

        withTimeout(5_000) { receiver.await() } shouldBe (0..99).toList()
    }

    test("filtered receive waits again after rejecting a signalled element") {
        val channel = QueueChannel.spscUnbounded<Int>()
        val receiver = channel.filter { it % 2 == 0 }.mapNotNull { if (it > 0) it.toString() else null }
        val result = async(Dispatchers.Unconfined) { receiver.receive() }
        channel.offer(1)
        channel.offer(0)
        result.isCompleted shouldBe false
        channel.offer(2)

        withTimeout(5_000) { result.await() } shouldBe "2"
    }

    test("forEachSuspend consumes elements until close") {
        val channel = QueueChannel.spscUnbounded<String>()
        val elements = mutableListOf<String>()
        val consumer = launch { channel.forEachSuspend { elements.add(it) } }

        channel.offer("hello")
        channel.offer("world")
        channel.close()
        consumer.join()

        elements shouldBe listOf("hello", "world")
    }

    test("forEachSuspend consumes a OneShotChannel") {
        val channel = OneShotChannel<String>()
        val elements = mutableListOf<String>()
        val consumer = launch { channel.forEachSuspend { elements.add(it) } }

        channel.offer("hello")
        consumer.join()

        elements shouldBe listOf("hello")
        channel.isClosed shouldBe true
    }

    test("forEachSuspend consumes a BroadcastChannel receiver") {
        val channel = BroadcastChannel.spscUnbounded<String>()
        val receiver = channel.subscribe()
        val elements = mutableListOf<String>()
        val consumer = launch { receiver.forEachSuspend { elements.add(it) } }

        channel.offer("hello")
        channel.offer("world")
        channel.close()
        consumer.join()

        elements shouldBe listOf("hello", "world")
    }

    test("cancelling forEachSuspend closes its receiver") {
        val channel = QueueChannel.spscUnbounded<String>()
        val consumer = launch { channel.forEachSuspend {} }

        yield()
        consumer.cancel()
        consumer.join()

        channel.isClosed shouldBe true
    }

    test("asCoroutineReceiver exposes QueueChannel elements") {
        val channel = QueueChannel.spscUnbounded<String>()
        val receiver = channel.asCoroutineReceiver()
        val elements = mutableListOf<String>()
        val consumer = launch {
            for (element in receiver) elements.add(element)
        }

        channel.offer("hello")
        channel.offer("world")
        channel.close()
        consumer.join()

        elements shouldBe listOf("hello", "world")
    }

    test("asCoroutineReceiver exposes a OneShotChannel element") {
        val channel = OneShotChannel<String>()
        val receiver = channel.asCoroutineReceiver()
        val elements = mutableListOf<String>()

        val consumer = launch {
            for (element in receiver) elements.add(element)
        }
        channel.offer("hello")
        consumer.join()

        elements shouldBe listOf("hello")
    }

    test("asCoroutineReceiver handles backpressure") {
        val channel = QueueChannel.spscUnbounded<Int>()
        val receiver = channel.asCoroutineReceiver()
        val producer = launch {
            repeat(500) { channel.offer(it) }
            channel.close()
        }

        var sum = 0
        for (element in receiver) {
            sum += element
            delay(1.milliseconds)
        }

        producer.join()
        sum shouldBe (0..499).sum()
    }

    test("cancelling a coroutine receiver closes its source") {
        val channel = QueueChannel.spscUnbounded<String>()
        val receiver = channel.asCoroutineReceiver()

        channel.offer("hello")
        receiver.cancel()
        delay(50)

        channel.isClosed shouldBe true
    }

    test("asFlow exposes channel elements") {
        val channel = QueueChannel.spscUnbounded<String>()
        channel.offer("hello")
        channel.offer("world")
        channel.close()

        channel.asFlow().toList() shouldBe listOf("hello", "world")
    }
})

/** Simulates a producer acting immediately after the receiver's unsuccessful poll. */
private class AfterEmptyPollReceiver<T : Any>(
    private val delegate: ChannelReceiver<T>,
    private val afterEmptyPoll: () -> Unit,
) : ChannelReceiver<T> {
    private var triggered = false

    override val notificationHandle get() = delegate.notificationHandle
    override val isClosed get() = delegate.isClosed
    override val size get() = delegate.size

    override fun close() = delegate.close()

    override fun poll(): T? {
        val next = delegate.poll()
        if (next == null && !triggered) {
            triggered = true
            afterEmptyPoll()
        }
        return next
    }
}
