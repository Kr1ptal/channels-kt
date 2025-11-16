package io.channels.core

/**
 * Platform-specific annotation that on JVM maps to @JvmStatic, ignored on other platforms.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
expect annotation class PlatformStatic()

/**
 * Platform-specific annotation that on JVM maps to @JvmOverloads, ignored on other platforms.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.BINARY)
expect annotation class PlatformOverloads()
