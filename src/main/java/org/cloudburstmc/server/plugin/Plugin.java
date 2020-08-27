package org.cloudburstmc.server.plugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Plugin {

    String id();

    String name() default "";

    String version();

    String description() default "";

    String url() default "";

    String[] authors() default {};

    Dependency[] dependencies() default {};
}
