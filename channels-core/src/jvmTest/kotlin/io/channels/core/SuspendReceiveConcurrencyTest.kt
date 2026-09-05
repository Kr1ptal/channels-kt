package io.channels.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class SuspendReceiveConcurrencyTest : FunSpec({
    test("concurrent producers resume the single receiver without losing values or double resuming") {
        withTimeout(15_000) {
            coroutineScope {
                val channel = QueueChannel.mpscUnbounded<Int>()
                val receiver = async(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
                    buildList {
                        while (true) add(channel.receive() ?: break)
                    }
                }
                List(4) { producer ->
                    launch(Dispatchers.Default) {
                        repeat(2_000) { channel.offer(producer * 2_000 + it) }
                    }
                }.joinAll()
                channel.close()

                receiver.await().sorted() shouldBe (0 until 8_000).toList()
            }
        }
    }
})
