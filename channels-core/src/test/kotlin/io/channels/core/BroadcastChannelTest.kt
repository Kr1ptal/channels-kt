package io.channels.core

import io.channels.core.blocking.BlockingStrategy
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread

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
        val receivers = ConcurrentLinkedQueue<ChannelReceiver<String>>()
        val receiver = channel.subscribe()

        val subscriberThread = thread {
            repeat(100) {
                receivers.add(channel.subscribe())
                Thread.sleep(1)
            }
        }

        val unsubscriberThread = thread {
            while (subscriberThread.isAlive) {
                receivers.poll()?.close()
                Thread.sleep(1)
            }
        }

        // Continuously offer while subscriptions change
        repeat(100) {
            channel.offer("msg$it")
            Thread.sleep(1)
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

    test("close is idempotent") {
        val channel = BroadcastChannel.mpscBounded<String>(2)
        val receiver = channel.subscribe()

        var closedCount = 0
        receiver.withBlockingStrategy(object : BlockingStrategy {
            override fun waitForStateChange(status: ChannelState) {
                // do nothing
            }

            override fun signalStateChange() {
                closedCount++
            }
        })

        repeat(3) { channel.close() }

        closedCount shouldBe 1
        channel.isClosed shouldBe true
        receiver.isClosed shouldBe true
    }

    test("forEach processes all elements until close") {
        val channel = BroadcastChannel.mpscBounded<String>(2)
        val receiver = channel.subscribe()
        val elements = mutableListOf<String>()

        thread {
            channel.offer("hello")
            channel.offer("world")
            Thread.sleep(100)
            channel.close()
        }

        receiver.forEach { elements.add(it) }

        elements shouldBe listOf("hello", "world")
        receiver.isClosed shouldBe true
    }
})
