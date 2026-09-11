package org.cloudburstmc.api.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

/**
 * A command argument that defines both authoritative parsing and its client presentation.
 *
 * <p>The presentation must describe the same values accepted by {@link #parse}. Values returned by
 * {@link #getValues()} are included in command data sent to clients; runtime Brigadier suggestions do not
 * replace that command data.</p>
 *
 * @param <T> the parsed argument value type
 */
public interface CommandArgumentType<T> extends ArgumentType<T> {

    /**
     * Returns the semantic kind of this argument.
     *
     * @return argument kind
     */
    CommandArgumentKind getKind();

    /**
     * Returns the name shown for this argument in command UI.
     *
     * @return display name, or {@code null} to use the command node name
     */
    @Nullable
    String getDisplayName();

    /**
     * Returns the enum name used when this argument is represented as a fixed command enum.
     *
     * @return enum name, or {@code null} when this argument is not a named enum
     */
    @Nullable
    String getEnumName();

    /**
     * Returns accepted or suggested string values for this argument.
     *
     * @return immutable values
     */
    List<String> getValues();

    /**
     * Returns the required token suffix for postfix arguments.
     *
     * @return required suffix, or {@code null} when this is not a postfix argument
     */
    @Nullable
    String getPostfix();
}
