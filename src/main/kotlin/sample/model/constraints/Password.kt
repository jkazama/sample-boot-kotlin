package sample.model.constraints

import jakarta.validation.*
import jakarta.validation.constraints.*
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.*
import sample.util.Regex

/**
 * Constraint annotation representing required password. low: The actual definition depends largely
 * on the project, so it is arbitrary in the sample.
 */
@MustBeDocumented
@Constraint(validatedBy = [])
@Target(FUNCTION, FIELD, ANNOTATION_CLASS, CONSTRUCTOR, VALUE_PARAMETER)
@Retention(RUNTIME)
@ReportAsSingleViolation
@NotBlank
@Size
@Pattern(regexp = "")
annotation class Password(
        val message: String = "{error.domain.password}",
        val groups: Array<kotlin.reflect.KClass<*>> = [],
        val payload: Array<kotlin.reflect.KClass<out Payload>> = [],
        @get:OverridesAttribute(constraint = Size::class, name = "max") val max: Int = 256,
        @get:OverridesAttribute(constraint = Pattern::class, name = "regexp")
        val regexp: String = Regex.rAscii,
        @get:OverridesAttribute(constraint = Pattern::class, name = "flags")
        val flags: Array<Pattern.Flag> = []
) {
    @Target(FUNCTION, FIELD, ANNOTATION_CLASS, CONSTRUCTOR, VALUE_PARAMETER)
    @Retention(RUNTIME)
    @MustBeDocumented
    annotation class List(val value: Array<Password>)
}
