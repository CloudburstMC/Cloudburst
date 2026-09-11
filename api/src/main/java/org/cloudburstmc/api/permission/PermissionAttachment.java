package org.cloudburstmc.api.permission;

import org.cloudburstmc.api.plugin.PluginContainer;

import java.util.Map;

/**
 * A plugin-owned set of permission overrides applied to one {@link Permissible}.
 *
 * <p>Mutations are applied atomically and immediately refresh the subject's effective permissions.</p>
 */
public interface PermissionAttachment {

    /**
     * @return the plugin that owns this attachment
     */
    PluginContainer getPlugin();

    /**
     * @return the subject receiving this attachment
     */
    Permissible getPermissible();

    /**
     * @return an immutable snapshot of the current overrides
     */
    Map<String, Boolean> getPermissions();

    /**
     * Replaces all overrides in this attachment.
     *
     * @param permissions permission names and values
     */
    void setPermissions(Map<String, Boolean> permissions);

    /**
     * Sets one permission override.
     *
     * @param name  permission node name
     * @param value override value
     */
    void setPermission(String name, boolean value);

    /**
     * Sets one permission override.
     *
     * @param permission permission definition
     * @param value      override value
     */
    default void setPermission(Permission permission, boolean value) {
        setPermission(permission.name(), value);
    }

    /**
     * Removes one override.
     *
     * @param name permission node name
     */
    void unsetPermission(String name);

    /**
     * Removes one override.
     *
     * @param permission permission definition
     */
    default void unsetPermission(Permission permission) {
        unsetPermission(permission.name());
    }

    /**
     * Removes all overrides from this attachment.
     */
    void clearPermissions();

    /**
     * Removes this attachment from its subject.
     *
     * @return whether the attachment was active
     */
    boolean remove();
}
