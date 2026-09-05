package io.channels.core.operator

import io.channels.core.QueueChannel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class MapChannelTest : FunSpec({
    test("maps elements synchronously") {
        val source = QueueChannel.spscUnbounded<Int>()
        val mapped = source.map { it * 2 }

        source.offer(1)
        source.offer(2)
        source.offer(3)

        mapped.poll() shouldBe 2
        mapped.poll() shouldBe 4
        mapped.poll() shouldBe 6
    }

    test("forEachSuspend processes all mapped elements") {
        val source = QueueChannel.spscUnbounded<Int>()
        val mapped = source.map { it * 2 }
        val results = mutableListOf<Int>()

        source.offer(1)
        source.offer(2)
        source.offer(3)
        source.close()

        mapped.forEachSuspend { results.add(it) }
        results shouldBe listOf(2, 4, 6)
    }

    test("propagates close state") {
        val source = QueueChannel.spscUnbounded<Int>()
        val mapped = source.map { it * 2 }

        mapped.close()
        mapped.isClosed shouldBe true
        mapped.poll() shouldBe null
    }
})
