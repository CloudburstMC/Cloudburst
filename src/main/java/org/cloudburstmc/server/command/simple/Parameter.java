package org.cloudburstmc.server.command.simple;

import org.cloudburstmc.protocol.bedrock.data.command.CommandParamType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author nilsbrychzy
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Parameter {
    String name();

    CommandParamType type() default CommandParamType.TEXT;

    boolean optional() default false;
}
