package sample.model.constraints

import jakarta.validation.*
import jakarta.validation.constraints.*
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.*
import sample.util.Regex

/** Constraint annotation representing required outline. */
@MustBeDocumented
@Constraint(validatedBy = [])
@Target(FUNCTION, FIELD, ANNOTATION_CLASS, CONSTRUCTOR, VALUE_PARAMETER)
@Retention(RUNTIME)
@ReportAsSingleViolation
@NotBlank
@Size
@Pattern(regexp = "")
annotation class Outline(
        val message: String = "{error.domain.outline}",
        val groups: Array<kotlin.reflect.KClass<*>> = [],
        val payload: Array<kotlin.reflect.KClass<out Payload>> = [],
        @get:OverridesAttribute(constraint = Size::class, name = "max") val max: Int = 100,
        @get:OverridesAttribute(constraint = Pattern::class, name = "regexp")
        val regexp: String = Regex.rWord,
        @get:OverridesAttribute(constraint = Pattern::class, name = "flags")
        val flags: Array<Pattern.Flag> = []
) {
    @Target(FUNCTION, FIELD, ANNOTATION_CLASS, CONSTRUCTOR, VALUE_PARAMETER)
    @Retention(RUNTIME)
    @MustBeDocumented
    annotation class List(val value: Array<Outline>)
}
