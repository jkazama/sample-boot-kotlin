package sample.model.constraints

import jakarta.validation.*
import jakarta.validation.constraints.*
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.*

/** Constraint annotation representing string ID. */
@MustBeDocumented
@Constraint(validatedBy = [])
@Target(FUNCTION, FIELD, ANNOTATION_CLASS, CONSTRUCTOR, VALUE_PARAMETER)
@Retention(RUNTIME)
@ReportAsSingleViolation
@Size
@Pattern(regexp = "")
annotation class IdStrEmpty(
        val message: String = "{error.domain.idStr}",
        val groups: Array<kotlin.reflect.KClass<*>> = [],
        val payload: Array<kotlin.reflect.KClass<out Payload>> = [],
        @get:OverridesAttribute(constraint = Size::class, name = "max") val max: Int = 32,
        @get:OverridesAttribute(constraint = Pattern::class, name = "regexp")
        val regexp: String = "^\\p{ASCII}*$",
        @get:OverridesAttribute(constraint = Pattern::class, name = "flags")
        val flags: Array<Pattern.Flag> = []
) {
    @Target(FUNCTION, FIELD, ANNOTATION_CLASS, CONSTRUCTOR, VALUE_PARAMETER)
    @Retention(RUNTIME)
    @MustBeDocumented
    annotation class List(val value: Array<IdStrEmpty>)
}
