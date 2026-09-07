package io.channels.core

import java.util.concurrent.ThreadFactory

/**
 * Provider for [ThreadFactory] instances. It first tries to use a virtual thread, if not available, it falls back
 * to platform threads.
 * */
internal object ThreadFactoryProvider {
    private val THREAD_FACTORY: ThreadFactory

    init {
        // Plain reflection rather than java.lang.invoke: MethodHandle invocation compiles to the
        // `invoke-polymorphic` dex opcode, which raises the minimum Android API level of any consumer APK to 26.
        val virtualThreadFactory = runCatching {
            val ofVirtual = Thread::class.java.getMethod("ofVirtual")
            val builder = ofVirtual.invoke(null)

            // Thread.Builder.OfVirtual is public, so `factory` is reachable reflectively
            ofVirtual.returnType.getMethod("factory").invoke(builder) as? ThreadFactory
        }.getOrNull()

        THREAD_FACTORY = virtualThreadFactory ?: ThreadFactory { r -> Thread(r) }
    }

    /**
     * Create either a virtual or platform thread, depending on the java version.
     * */
    fun maybeVirtualThread(runnable: Runnable): Thread {
        return THREAD_FACTORY.newThread(runnable)
    }
}
