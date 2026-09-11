package org.cloudburstmc.api.command;

import lombok.experimental.UtilityClass;

/**
 * Permission nodes used by the command framework itself.
 */
@UtilityClass
public class CommandPermissions {

    /**
     * Allows target selectors such as {@code @a}, {@code @e}, and {@code @s} to be resolved.
     */
    public static final String TARGET_SELECTORS = "cloudburst.command.selector";
}
