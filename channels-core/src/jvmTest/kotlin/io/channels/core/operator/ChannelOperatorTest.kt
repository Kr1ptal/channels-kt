package io.channels.core.operator

import io.channels.core.QueueChannel
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ChannelOperatorTest : FunSpec({
    test("operators can be chained") {
        val source = QueueChannel.spscUnbounded<Int>()
        val result = source.mapNotNull { if (it % 2 == 0) it * 2 else null }
            .filter { it > 5 }
            .map { it.toString() }

        source.offer(1) // filtered by "mapNotNull"
        source.offer(2) // 4 filtered by "filter"
        source.offer(3) // filtered by "mapNotNull"
        source.offer(4) // 8 passes through
        source.offer(5) // filtered by "mapNotNull"

        result.poll() shouldBe "8"
        result.poll() shouldBe null
    }
})
