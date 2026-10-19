package org.cloudburstmc.server.configuration.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Constraints attached to typed server settings.
 * Reference values are required unless optional. Numeric values must always be finite.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SettingConstraint {

    double min() default Double.NEGATIVE_INFINITY;

    double max() default Double.POSITIVE_INFINITY;

    boolean minInclusive() default true;

    boolean optional() default false;

    boolean nonBlank() default false;

    boolean whole() default false;

    Class<?>[] types() default {};
}
