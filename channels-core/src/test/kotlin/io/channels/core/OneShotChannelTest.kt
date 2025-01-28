package io.channels.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.concurrent.thread

class OneShotChannelTest : FunSpec({
    test("only a single offer is accepted") {
        val channel = OneShotChannel<String>()
        channel.offer("hello") shouldBe true
        channel.offer("world") shouldBe false
        channel.size shouldBe 1

        channel.take() shouldBe "hello"
        channel.isClosed shouldBe true
    }

    test("offer after take is rejected") {
        val channel = OneShotChannel<String>()
        channel.offer("hello") shouldBe true
        channel.size shouldBe 1
        channel.take() shouldBe "hello"

        channel.offer("world") shouldBe false
        channel.size shouldBe 0
        channel.isClosed shouldBe true
    }

    test("poll after offer returns an element") {
        val channel = OneShotChannel<String>()

        channel.offer("hello") shouldBe true
        channel.size shouldBe 1
        channel.poll() shouldBe "hello"
        channel.isClosed shouldBe true
    }

    test("poll before offer / after close returns null") {
        val channel = OneShotChannel<String>()

        channel.poll() shouldBe null
        channel.offer("hello") shouldBe true
        channel.poll() shouldBe "hello"

        channel.close()
        channel.poll() shouldBe null
    }

    test("take after close throws exception") {
        val channel = OneShotChannel<String>()

        channel.offer("hello") shouldBe true
        channel.size shouldBe 1
        channel.take() shouldBe "hello"

        shouldThrow<InterruptedException> { channel.take() }
    }

    test("take blocks until element is available") {
        val channel = OneShotChannel<String>()
        thread {
            Thread.sleep(250)
            channel.offer("hello")
        }

        channel.size shouldBe 0
        channel.take() shouldBe "hello"
    }

    test("take throws on close without any element") {
        val channel = OneShotChannel<String>()
        thread {
            Thread.sleep(250)
            channel.close()
        }

        channel.size shouldBe 0
        shouldThrow<InterruptedException> { channel.take() }
        channel.isClosed shouldBe true
    }

    test("close is idempotent") {
        val channel = OneShotChannel<String>()
        var closedCount = 0

        channel.onStateChange { closedCount++ }
        repeat(3) { channel.close() }

        closedCount shouldBe 1
        channel.isClosed shouldBe true
    }

    test("for-each terminates after a single element") {
        val channel = OneShotChannel<String>()
        thread {
            channel.offer("hello")
        }

        var count = 0
        channel.forEach {
            it shouldBe "hello"
            count++
        }
        count shouldBe 1
        channel.isClosed shouldBe true
    }

    test("for-each terminates immediately if channel is closed") {
        val channel = OneShotChannel<String>()
        channel.close()

        var count = 0
        channel.forEach { count++ }

        count shouldBe 0
        channel.isClosed shouldBe true
    }
})
