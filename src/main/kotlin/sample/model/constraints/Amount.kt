package sample.model.constraints

import jakarta.validation.*
import jakarta.validation.constraints.*
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.*

/** Constraint annotation representing required amount. */
@MustBeDocumented
@Constraint(validatedBy = [])
@Target(FUNCTION, FIELD, ANNOTATION_CLASS, CONSTRUCTOR, VALUE_PARAMETER)
@Retention(RUNTIME)
@ReportAsSingleViolation
@NotNull
@DecimalMin(value = "0.0", inclusive = false)
@DecimalMax(value = "99999999999999.99")
annotation class Amount(
        val message: String = "{error.domain.amount}",
        val groups: Array<kotlin.reflect.KClass<*>> = [],
        val payload: Array<kotlin.reflect.KClass<out Payload>> = [],
        @get:OverridesAttribute(constraint = DecimalMin::class, name = "value")
        val min: String = "0.01",
        @get:OverridesAttribute(constraint = DecimalMax::class, name = "value")
        val max: String = "99999999999999.99"
) {
    @Target(FUNCTION, FIELD, ANNOTATION_CLASS, CONSTRUCTOR, VALUE_PARAMETER)
    @Retention(RUNTIME)
    @MustBeDocumented
    annotation class List(val value: Array<Amount>)
}
