package io.channels.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class OneShotChannelTest : FunSpec({
    test("only a single offer is accepted") {
        val channel = OneShotChannel<String>()
        channel.offer("hello") shouldBe true
        channel.offer("world") shouldBe false
        channel.size shouldBe 1

        channel.poll() shouldBe "hello"
        channel.isClosed shouldBe true
    }

    test("offer after poll is rejected") {
        val channel = OneShotChannel<String>()
        channel.offer("hello") shouldBe true
        channel.size shouldBe 1
        channel.poll() shouldBe "hello"

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

    test("poll after close returns null") {
        val channel = OneShotChannel<String>()

        channel.offer("hello") shouldBe true
        channel.size shouldBe 1
        channel.poll() shouldBe "hello"

        channel.poll() shouldBe null
    }
})
