package org.cloudburstmc.api.permission;

import net.kyori.adventure.util.TriState;
import org.cloudburstmc.api.plugin.PluginContainer;

import java.util.Map;
import java.util.Set;

/**
 * Represents a subject whose access is controlled by permission definitions and attachments.
 *
 * <p>A directly granted default takes precedence over a value inherited from another default.
 * Attachment values override defaults, direct values take precedence over values inherited by the
 * same attachment, and later attachments take precedence over earlier attachments.</p>
 */
public interface Permissible extends ServerOperator {

    /**
     * Returns whether a permission has an effective value supplied by a default or attachment.
     *
     * @param name permission node name
     * @return whether the permission is explicitly resolved
     */
    boolean isPermissionSet(String name);

    /**
     * Returns whether a permission has an effective value supplied by a default or attachment.
     *
     * @param permission permission definition
     * @return whether the permission is explicitly resolved
     */
    default boolean isPermissionSet(Permission permission) {
        return isPermissionSet(permission.name());
    }

    /**
     * Checks the effective value of a permission.
     *
     * @param name permission node name
     * @return whether access is granted
     */
    boolean hasPermission(String name);

    /**
     * Checks the effective value of a permission.
     *
     * @param permission permission definition
     * @return whether access is granted
     */
    default boolean hasPermission(Permission permission) {
        return hasPermission(permission.name());
    }

    /**
     * Returns the resolved override state without collapsing an unset permission to {@code false}.
     *
     * @param name permission node name
     * @return the resolved state
     */
    default TriState permissionValue(String name) {
        return isPermissionSet(name) ? TriState.byBoolean(hasPermission(name)) : TriState.NOT_SET;
    }

    /**
     * Returns the resolved override state without collapsing an unset permission to {@code false}.
     *
     * @param permission permission definition
     * @return the resolved state
     */
    default TriState permissionValue(Permission permission) {
        return permissionValue(permission.name());
    }

    /**
     * Creates an empty plugin-owned attachment.
     *
     * @param plugin owning plugin
     * @return the attachment
     */
    PermissionAttachment addAttachment(PluginContainer plugin);

    /**
     * Creates a plugin-owned attachment with its initial overrides applied atomically.
     *
     * @param plugin      owning plugin
     * @param permissions initial overrides
     * @return the attachment
     */
    PermissionAttachment addAttachment(PluginContainer plugin, Map<String, Boolean> permissions);

    /**
     * Creates an attachment that is removed after a number of server ticks.
     *
     * @param plugin      owning plugin
     * @param permissions initial overrides
     * @param ticks       lifetime in server ticks
     * @return the attachment
     */
    PermissionAttachment addTemporaryAttachment(PluginContainer plugin, Map<String, Boolean> permissions, long ticks);

    /**
     * Returns the currently resolved permission values.
     *
     * @return immutable snapshot of effective permissions
     */
    Set<EffectivePermission> getEffectivePermissions();
}
