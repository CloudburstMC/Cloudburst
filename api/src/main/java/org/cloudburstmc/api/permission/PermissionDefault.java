package org.cloudburstmc.api.permission;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Represents the possible default grant states for a {@link Permission}.
 *
 * <p>The default controls whether a {@link Permissible} has the permission before any
 * {@link PermissionAttachment} is applied.</p>
 */
public enum PermissionDefault {
    /**
     * Granted to everyone regardless of operator status.
     */
    TRUE("true"),
    /**
     * Denied to everyone by default; must be explicitly granted.
     */
    FALSE("false"),
    /**
     * Granted only to operators.
     */
    OP("op", "isop", "operator", "isoperator", "admin", "isadmin"),
    /**
     * Granted to non-operators only.
     */
    NOT_OP("!op", "notop", "!operator", "notoperator", "!admin", "notadmin");

    private static final Map<String, PermissionDefault> LOOKUP = new HashMap<>();

    static {
        for (PermissionDefault value : values()) {
            for (String name : value.names) {
                LOOKUP.put(name, value);
            }
        }
    }

    private final String[] names;

    PermissionDefault(String... names) {
        this.names = names;
    }

    /**
     * Looks up a {@code PermissionDefault} by name, accepting all known aliases.
     *
     * @param name the name to look up (case-insensitive, non-alpha characters ignored except '!')
     * @return the matching default, or {@code null} if not recognized
     */
    public static @Nullable PermissionDefault getByName(String name) {
        if (name == null) {
            return null;
        }
        return LOOKUP.get(name.toLowerCase(Locale.ROOT).replaceAll("[^a-z!]", ""));
    }

    /**
     * Returns whether this default grants the permission for the given operator state.
     *
     * @param op whether the subject is an operator
     * @return true if the permission is granted by default
     */
    public boolean getValue(boolean op) {
        return switch (this) {
            case TRUE -> true;
            case FALSE -> false;
            case OP -> op;
            case NOT_OP -> !op;
        };
    }

    @Override
    public String toString() {
        return names[0];
    }
}
