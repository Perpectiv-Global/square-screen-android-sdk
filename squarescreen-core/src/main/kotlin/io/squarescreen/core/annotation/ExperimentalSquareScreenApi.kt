package io.squarescreen.core.annotation

@RequiresOptIn(
    message = "This API is experimental and may change without notice in future versions.",
    level = RequiresOptIn.Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalSquareScreenApi
