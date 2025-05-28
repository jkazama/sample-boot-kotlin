package sample.model.constraints

import jakarta.validation.*
import jakarta.validation.constraints.*
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.*

/** Constraint annotation representing required absolute amount. */
@MustBeDocumented
@Constraint(validatedBy = [])
@Target(FUNCTION, FIELD, ANNOTATION_CLASS, CONSTRUCTOR, VALUE_PARAMETER)
@Retention(RUNTIME)
@ReportAsSingleViolation
@NotNull
@Digits(integer = 16, fraction = 4)
@DecimalMin("0.00")
annotation class AbsAmount(
        val message: String = "{error.domain.absAmount}",
        val groups: Array<kotlin.reflect.KClass<*>> = [],
        val payload: Array<kotlin.reflect.KClass<out Payload>> = [],
        @get:OverridesAttribute(constraint = Digits::class, name = "integer") val integer: Int = 16,
        @get:OverridesAttribute(constraint = Digits::class, name = "fraction")
        val fraction: Int = 4,
        @get:OverridesAttribute(constraint = DecimalMin::class, name = "value")
        val min: String = "0.00"
) {
    @Target(FUNCTION, FIELD, ANNOTATION_CLASS, CONSTRUCTOR, VALUE_PARAMETER)
    @Retention(RUNTIME)
    @MustBeDocumented
    annotation class List(val value: Array<AbsAmount>)
}
