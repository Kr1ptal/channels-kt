package io.channels.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BroadcastChannelTest : FunSpec({
    test("elements are broadcast to all subscribers") {
        val channel = BroadcastChannel.mpscBounded<String>(2)
        val receiver1 = channel.subscribe()
        val receiver2 = channel.subscribe()

        channel.offer("hello") shouldBe true
        channel.offer("world") shouldBe true

        receiver1.take() shouldBe "hello"
        receiver1.take() shouldBe "world"
        receiver2.take() shouldBe "hello"
        receiver2.take() shouldBe "world"
    }

    test("offer returns false when no subscribers") {
        val channel = BroadcastChannel.mpscBounded<String>(2)
        channel.offer("hello") shouldBe false
    }

    test("offer returns true if at least one subscriber accepts") {
        val channel = BroadcastChannel.mpscBounded<String>(1)
        val receiver1 = channel.subscribe()
        val receiver2 = channel.subscribe()

        channel.offer("hello") shouldBe true
        receiver1.take() shouldBe "hello"

        // receiver2's queue is full, receiver1 is empty
        channel.offer("world") shouldBe true
        // both receivers are full
        channel.offer("full") shouldBe false

        receiver1.take() shouldBe "world"
        receiver2.take() shouldBe "hello"
    }

    test("closing broadcast channel closes all subscribers") {
        val channel = BroadcastChannel.mpscBounded<String>(2)
        val receiver1 = channel.subscribe()
        val receiver2 = channel.subscribe()

        channel.offer("hello")
        channel.close()

        receiver1.take() shouldBe "hello"
        receiver1.take() shouldBe null
        receiver2.take() shouldBe "hello"
        receiver2.take() shouldBe null

        receiver1.isClosed shouldBe true
        receiver2.isClosed shouldBe true
    }

    test("new subscribers after close are immediately closed") {
        val channel = BroadcastChannel.mpscBounded<String>(2)
        channel.close()

        val receiver = channel.subscribe()
        receiver.isClosed shouldBe true
        receiver.take() shouldBe null
    }

    test("unsubscribe removes receiver") {
        val channel = BroadcastChannel.mpscBounded<String>(2)
        val receiver1 = channel.subscribe()
        val receiver2 = channel.subscribe()

        channel.size shouldBe 2

        receiver1.close()
        channel.size shouldBe 1

        channel.offer("hello") shouldBe true
        receiver1.take() shouldBe null
        receiver2.take() shouldBe "hello"
    }

    test("concurrent subscribe/unsubscribe is thread-safe") {
        val channel = BroadcastChannel.spscUnbounded<String>()
        val mutex = Mutex()
        val receivers = ArrayList<ChannelReceiver<String>>()
        val receiver = channel.subscribe()

        val subscriberThread = launch(Dispatchers.Default) {
            repeat(100) {
                mutex.withLock<Unit> { receivers.add(channel.subscribe()) }
                delay(1)
            }
        }

        val unsubscriberThread = launch(Dispatchers.Default) {
            while (subscriberThread.isActive) {
                mutex.withLock<Unit> { receivers.removeFirstOrNull()?.close() }
                delay(1)
            }
        }

        // Continuously offer while subscriptions change
        repeat(100) {
            channel.offer("msg$it")
            delay(1)
        }

        subscriberThread.join()
        unsubscriberThread.join()

        repeat(100) {
            receiver.poll() shouldBe "msg$it"
        }
    }

    test("unbounded channel accepts all offers for all subscribers") {
        val channel = BroadcastChannel.mpscUnbounded<String>()
        val receiver1 = channel.subscribe()
        val receiver2 = channel.subscribe()

        repeat(1000) {
            channel.offer("item$it") shouldBe true
        }

        repeat(1000) {
            receiver1.take() shouldBe "item$it"
            receiver2.take() shouldBe "item$it"
        }
    }

    test("forEach processes all elements until close") {
        val channel = BroadcastChannel.mpscBounded<String>(2)
        val receiver = channel.subscribe()
        val elements = mutableListOf<String>()

        launch(Dispatchers.Default) {
            channel.offer("hello")
            channel.offer("world")
            delay(100)
            channel.close()
        }

        receiver.forEach { elements.add(it) }

        elements shouldBe listOf("hello", "world")
        receiver.isClosed shouldBe true
    }
})
