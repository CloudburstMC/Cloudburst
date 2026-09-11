package org.cloudburstmc.api.permission;

/**
 * Defines who receives a permission when no attachment overrides it.
 */
public enum PermissionDefault {
    /**
     * Grants the permission to every subject.
     */
    EVERYONE,
    /**
     * Denies the permission to every subject.
     */
    NONE,
    /**
     * Grants the permission to operators.
     */
    OPERATORS,
    /**
     * Grants the permission to non-operators.
     */
    NON_OPERATORS;

    /**
     * Resolves this policy for an operator state.
     *
     * @param operator whether the subject is an operator
     * @return whether the permission is granted
     */
    public boolean grants(boolean operator) {
        return switch (this) {
            case EVERYONE -> true;
            case NONE -> false;
            case OPERATORS -> operator;
            case NON_OPERATORS -> !operator;
        };
    }
}
