package org.cloudburstmc.api.command.argument;

import com.mojang.brigadier.arguments.ArgumentType;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A command argument that defines authoritative parsing and its advertised command syntax.
 *
 * <p>Values returned by {@link #getValues()} are included in command data. They may be a supported subset
 * of the values accepted by {@link #parse}, allowing valid internal entries to remain available without
 * advertising them. Runtime Brigadier suggestions do not replace the advertised data.</p>
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
     * Returns the current accepted or suggested string values for this argument.
     *
     * <p>Registry-backed arguments may return a new immutable snapshot after their registry changes.</p>
     *
     * @return immutable value snapshot
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
