package org.cloudburstmc.api.command.argument;

/**
 * Restricts when an enum value is available in advertised command syntax.
 */
public enum CommandArgumentConstraint {
    /**
     * Requires cheats to be enabled.
     */
    CHEATS_ENABLED,
    /**
     * Requires operator permissions.
     */
    OPERATOR_PERMISSIONS,
    /**
     * Requires host permissions.
     */
    HOST_PERMISSIONS
}
