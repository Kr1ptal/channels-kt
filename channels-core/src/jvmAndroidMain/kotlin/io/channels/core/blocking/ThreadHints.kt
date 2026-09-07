/*  Copyright 2016 Gil Tene
 *  Modified by Kriptal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.channels.core.blocking

/**
 * This class captures possible hints that may be used by some
 * runtimes to improve code performance. It is intended to capture hinting
 * behaviours that are implemented in or anticipated to be spec'ed under the
 * [Thread] class in some Java SE versions, but missing in prior
 * versions.
 */
internal object ThreadHints {
    /**
     * Resolved once, then invoked through a plain virtual call on the spin-wait path.
     *
     * Deliberately not a [java.lang.invoke.MethodHandle]: `MethodHandle.invokeExact` compiles to the
     * `invoke-polymorphic` dex opcode, which raises the minimum Android API level of any consumer APK to 26.
     */
    private val ON_SPIN_WAIT: Runnable = try {
        // Probe for java.lang.Thread.onSpinWait() (Java 9+, Android API 33+) before referencing it. On runtimes
        // without it the reference below is never loaded, so it can never fail verification.
        Thread::class.java.getMethod("onSpinWait")
        object : Runnable {
            override fun run() = Thread.onSpinWait()
        }
    } catch (_: Throwable) {
        object : Runnable {
            override fun run() = Unit
        }
    }

    /**
     * Indicates that the caller is momentarily unable to progress, until the
     * occurrence of one or more actions on the part of other activities.  By
     * invoking this method within each iteration of a spin-wait loop construct,
     * the calling thread indicates to the runtime that it is busy-waiting. The runtime
     * may take action to improve the performance of invoking spin-wait loop constructions.
     */
    fun onSpinWait() {
        // Call java.lang.Thread.onSpinWait() on runtimes that support it. Do nothing otherwise.
        ON_SPIN_WAIT.run()
    }
}
