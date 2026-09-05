package io.channels.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class QueueChannelTest : FunSpec({
    test("offers are accepted until capacity is reached") {
        val channel = QueueChannel.mpscBounded<String>(2)
        channel.offer("hello") shouldBe true
        channel.offer("world") shouldBe true
        channel.offer("rejected") shouldBe false
        channel.size shouldBe 2

        channel.poll() shouldBe "hello"
        channel.poll() shouldBe "world"
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
        channel.poll() shouldBe "hello"
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

    test("poll drains a closed channel before returning null") {
        val channel = QueueChannel.mpscBounded<String>(2)

        channel.offer("hello") shouldBe true
        channel.poll() shouldBe "hello"
        channel.close()

        channel.poll() shouldBe null
    }

    test("unbounded queue accepts all offers") {
        val channel = QueueChannel.mpscUnbounded<String>()

        repeat(1000) {
            channel.offer("item$it") shouldBe true
        }

        channel.size shouldBe 1000

        repeat(1000) {
            channel.poll() shouldBe "item$it"
        }

        channel.size shouldBe 0
    }
})
