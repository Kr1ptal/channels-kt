package io.channels.core

/**
 * iOS implementation - no-op annotation.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
actual annotation class PlatformStatic

/**
 * iOS implementation - no-op annotation.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.BINARY)
actual annotation class PlatformOverloads
