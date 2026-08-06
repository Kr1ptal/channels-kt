package io.channels.core

import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.util.concurrent.ThreadFactory

/**
 * Provider for [ThreadFactory] instances. It first tries to use a virtual thread, if not available, it falls back
 * to platform threads.
 * */
internal object ThreadFactoryProvider {
    private val THREAD_FACTORY: ThreadFactory

    init {
        val virtualThreadFactory = runCatching {
            val threadBuilderClass = Thread::class.java.getMethod("ofVirtual").returnType // Thread.Builder.OfVirtual

            val factory = MethodHandles.lookup().findVirtual(
                threadBuilderClass,
                "factory",
                MethodType.methodType(ThreadFactory::class.java), // The method returns a ThreadFactory
            ).bindTo(Thread::class.java.getMethod("ofVirtual").invoke(null))

            // invoke while catching to see if we need to set "--enable-preview" flag
            factory.invokeExact() as? ThreadFactory
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
