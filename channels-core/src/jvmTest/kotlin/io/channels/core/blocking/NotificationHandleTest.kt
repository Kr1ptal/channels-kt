package io.channels.core.blocking

import io.channels.core.ChannelState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class NotificationHandleTest : FunSpec({
    val waits =
        listOf<Pair<String, (NotificationHandle) -> Unit>>(
            "busy spin" to NotificationHandle::waitWithBusySpin,
            "yield" to NotificationHandle::waitWithYield,
            "parking" to NotificationHandle::waitWithParking,
            "sleep" to { it.waitWithSleep(100_000) },
        )

    waits.forEach { (name, wait) ->
        test("$name wait observes a state change when the channel is still empty") {
            val state = EmptyChannelState()
            val handle = NotificationHandle(state)
            val returned = CountDownLatch(1)
            val waiter = thread(isDaemon = true) {
                wait(handle)
                returned.countDown()
            }

            try {
                state.checked.await(5, TimeUnit.SECONDS) shouldBe true

                handle.signalStateChange()

                returned.await(5, TimeUnit.SECONDS) shouldBe true
            } finally {
                state.closed = true
                handle.signalStateChange()
                waiter.join(5_000)
            }
        }
    }
})

private class EmptyChannelState : ChannelState {
    val checked = CountDownLatch(1)

    @Volatile
    var closed = false

    override val isClosed: Boolean
        get() = closed

    override val size: Int
        get() {
            checked.countDown()
            return 0
        }
}
