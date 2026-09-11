package org.cloudburstmc.api.permission;

import lombok.Builder;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Defines a permission node, its default value, and the child values inherited from it.
 *
 * <p>Definitions are immutable. Replace a registered definition through the
 * {@link PermissionManager} when its metadata or children must change.</p>
 *
 * @param name         normalized permission node name
 * @param description  human-readable description
 * @param defaultValue policy used when no attachment overrides this node
 * @param children     child node values inherited from this node
 */
@Builder(toBuilder = true)
public record Permission(
        String name,
        String description,
        PermissionDefault defaultValue,
        Map<String, Boolean> children
) {

    private static final Pattern NAME_PATTERN = Pattern.compile("[a-z0-9][a-z0-9_.-]*");

    /**
     * Creates a permission definition.
     */
    public Permission {
        name = normalizeName(name);
        description = Objects.requireNonNullElse(description, "");
        defaultValue = Objects.requireNonNullElse(defaultValue, PermissionDefault.OPERATORS);

        LinkedHashMap<String, Boolean> normalizedChildren = new LinkedHashMap<>();
        if (children != null) {
            children.forEach((child, value) -> normalizedChildren.put(
                    normalizeName(child), Objects.requireNonNull(value, "child permission value")));
        }
        children = Collections.unmodifiableMap(normalizedChildren);
    }

    /**
     * Creates a permission definition without children.
     *
     * @param name         permission node name
     * @param description  human-readable description
     * @param defaultValue default grant policy
     */
    public Permission(String name, String description, PermissionDefault defaultValue) {
        this(name, description, defaultValue, Map.of());
    }

    /**
     * Creates an operator-only permission definition without children.
     *
     * @param name        permission node name
     * @param description human-readable description
     */
    public Permission(String name, String description) {
        this(name, description, PermissionDefault.OPERATORS, Map.of());
    }

    /**
     * Normalizes and validates a permission node name.
     *
     * @param name permission node name
     * @return lowercase normalized name
     * @throws IllegalArgumentException if the name is empty or contains unsupported characters
     */
    public static String normalizeName(String name) {
        String normalized = Objects.requireNonNull(name, "name").strip().toLowerCase(Locale.ROOT);
        if (!NAME_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid permission name: " + name);
        }
        return normalized;
    }
}
