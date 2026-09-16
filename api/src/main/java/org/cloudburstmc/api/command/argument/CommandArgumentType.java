package org.cloudburstmc.api.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A command argument that defines authoritative parsing and its advertised command syntax.
 *
 * <p>The presentation must describe the same values accepted by {@link #parse}. Values returned by
 * {@link #getValues()} are included in command data. Runtime Brigadier suggestions do not replace that
 * advertised data.</p>
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
     * Returns whether this argument must remain required in advertised command syntax when its parent can execute.
     *
     * @return {@code true} when the argument must not be folded into an optional parameter
     */
    boolean isRequiredInSyntax();

    /**
     * Returns the availability constraints applied to individual enum values.
     *
     * @return immutable constraints keyed by enum value
     */
    Map<String, Set<CommandArgumentConstraint>> getValueConstraints();

    /**
     * Returns the required token suffix for postfix arguments.
     *
     * @return required suffix, or {@code null} when this is not a postfix argument
     */
    @Nullable
    String getPostfix();
}
