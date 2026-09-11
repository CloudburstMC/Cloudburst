package org.cloudburstmc.api.command.argument.resolver;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.cloudburstmc.api.command.CommandSourceStack;

/**
 * Resolves a parsed command argument against a command source.
 *
 * @param <T> the resolved value type
 */
@FunctionalInterface
public interface ArgumentResolver<T> {

    /**
     * Resolves this argument using the command source.
     *
     * @param source the command source
     * @return the resolved value
     * @throws CommandSyntaxException if the value cannot be resolved
     */
    T resolve(CommandSourceStack source) throws CommandSyntaxException;
}
