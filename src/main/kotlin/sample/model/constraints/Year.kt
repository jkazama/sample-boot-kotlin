package sample.model.constraints

import jakarta.validation.*
import jakarta.validation.constraints.*
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.*

/** Constraint annotation representing required year. */
@MustBeDocumented
@Constraint(validatedBy = [])
@Target(FUNCTION, FIELD, ANNOTATION_CLASS, CONSTRUCTOR, VALUE_PARAMETER)
@Retention(RUNTIME)
@ReportAsSingleViolation
@NotNull
@Min(1000)
@Max(9999)
annotation class Year(
        val message: String = "{error.domain.year}",
        val groups: Array<kotlin.reflect.KClass<*>> = [],
        val payload: Array<kotlin.reflect.KClass<out Payload>> = [],
        @get:OverridesAttribute(constraint = Min::class, name = "value") val min: Long = 1000,
        @get:OverridesAttribute(constraint = Max::class, name = "value") val max: Long = 9999
) {
    @Target(FUNCTION, FIELD, ANNOTATION_CLASS, CONSTRUCTOR, VALUE_PARAMETER)
    @Retention(RUNTIME)
    @MustBeDocumented
    annotation class List(val value: Array<Year>)
}
