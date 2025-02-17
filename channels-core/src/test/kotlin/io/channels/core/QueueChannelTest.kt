package io.channels.core

import io.channels.core.blocking.BlockingStrategy
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.concurrent.thread

class QueueChannelTest : FunSpec({
    test("offers are accepted until capacity is reached") {
        val channel = QueueChannel.mpscBounded<String>(2)
        channel.offer("hello") shouldBe true
        channel.offer("world") shouldBe true
        channel.offer("rejected") shouldBe false
        channel.size shouldBe 2

        channel.take() shouldBe "hello"
        channel.take() shouldBe "world"
        channel.size shouldBe 0
    }

    test("offer after close is rejected") {
        val channel = QueueChannel.mpscBounded<String>(2)
        channel.offer("hello") shouldBe true
        channel.size shouldBe 1
        channel.close()

        channel.offer("world") shouldBe false
        channel.size shouldBe 1
        channel.isClosed shouldBe true
        channel.take() shouldBe "hello"
    }

    test("poll returns elements in order") {
        val channel = QueueChannel.mpscBounded<String>(2)

        channel.offer("hello") shouldBe true
        channel.offer("world") shouldBe true
        channel.size shouldBe 2

        channel.poll() shouldBe "hello"
        channel.poll() shouldBe "world"
        channel.poll() shouldBe null
    }

    test("poll before offer returns null") {
        val channel = QueueChannel.mpscBounded<String>(2)

        channel.poll() shouldBe null
        channel.offer("hello") shouldBe true
        channel.poll() shouldBe "hello"
        channel.poll() shouldBe null
    }

    test("take after close returns null when channel is empty") {
        val channel = QueueChannel.mpscBounded<String>(2)

        channel.offer("hello") shouldBe true
        channel.take() shouldBe "hello"
        channel.close()

        channel.take() shouldBe null
    }

    test("take blocks until element is available") {
        val channel = QueueChannel.mpscBounded<String>(2)
        thread {
            Thread.sleep(250)
            channel.offer("hello")
        }

        channel.size shouldBe 0
        channel.take() shouldBe "hello"
    }

    test("take returns null on close without any element") {
        val channel = QueueChannel.mpscBounded<String>(2)
        thread {
            Thread.sleep(250)
            channel.close()
        }

        channel.size shouldBe 0
        channel.take() shouldBe null
        channel.isClosed shouldBe true
    }

    test("close is idempotent") {
        val channel = QueueChannel.mpscBounded<String>(2)
        var closedCount = 0

        channel.withBlockingStrategy(object : BlockingStrategy {
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
    }

    test("for-each processes all elements until close") {
        val channel = QueueChannel.mpscBounded<String>(2)
        val elements = mutableListOf<String>()

        thread {
            channel.offer("hello")
            channel.offer("world")
            Thread.sleep(100)
            channel.close()
        }

        channel.forEach { elements.add(it) }

        elements shouldBe listOf("hello", "world")
        channel.isClosed shouldBe true
    }

    test("for-each async processes all elements until close") {
        val channel = QueueChannel.mpscBounded<String>(2)
        val elements = mutableListOf<String>()

        val receiver = channel.forEachAsync("test-processor") { elements.add(it) }

        channel.offer("hello")
        channel.offer("world")
        Thread.sleep(200)

        receiver.close()

        elements shouldBe listOf("hello", "world")
        channel.isClosed shouldBe true
    }

    test("for-each terminates immediately if channel is closed and empty") {
        val channel = QueueChannel.mpscBounded<String>(2)
        channel.close()

        var count = 0
        channel.forEach { count++ }

        count shouldBe 0
        channel.isClosed shouldBe true
    }

    test("unbounded queue accepts all offers") {
        val channel = QueueChannel.mpscUnbounded<String>()

        repeat(1000) {
            channel.offer("item$it") shouldBe true
        }

        channel.size shouldBe 1000

        repeat(1000) {
            channel.take() shouldBe "item$it"
        }

        channel.size shouldBe 0
    }
})
