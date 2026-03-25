package org.cloudburstmc.api.permission;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.plugin.PluginContainer;

import java.util.Set;

/**
 * Represents an object that can hold and check permissions.
 *
 * <p>Permissions are evaluated by first checking the effective permission map built from
 * defaults and {@link PermissionAttachment}s. If no explicit value is present, the
 * {@link Permission}'s {@link PermissionDefault} is consulted against the subject's
 * operator status.</p>
 */
public interface Permissible extends ServerOperator {

    /**
     * Returns whether the named permission has been explicitly set (via defaults or an attachment).
     *
     * @param name the permission name (case-insensitive)
     * @return true if the permission is explicitly set
     */
    boolean isPermissionSet(String name);

    /**
     * Returns whether the given permission has been explicitly set.
     *
     * @param permission the permission to check
     * @return true if the permission is explicitly set
     */
    boolean isPermissionSet(Permission permission);

    /**
     * Returns whether this subject currently has the named permission.
     *
     * <p>Falls back to the registered {@link Permission}'s {@link PermissionDefault} when the
     * permission has not been explicitly set. Returns the global default if unregistered.</p>
     *
     * @param name the permission name (case-insensitive)
     * @return true if the permission is granted
     */
    boolean hasPermission(String name);

    /**
     * Returns whether this subject currently has the given permission.
     *
     * @param permission the permission to check
     * @return true if the permission is granted
     */
    boolean hasPermission(Permission permission);

    /**
     * Adds a new {@link PermissionAttachment} for the given plugin, with no initial permissions.
     *
     * @param plugin the owning plugin; must be loaded
     * @return the new attachment
     * @throws IllegalArgumentException if the plugin is not loaded
     */
    PermissionAttachment addAttachment(PluginContainer plugin);

    /**
     * Adds a new {@link PermissionAttachment} with a single initial permission set to {@code true}.
     *
     * @param plugin the owning plugin; must be loaded
     * @param name   the permission node name; must not be {@code null}
     * @return the new attachment
     * @throws IllegalArgumentException if the plugin is not loaded or {@code name} is null
     */
    PermissionAttachment addAttachment(PluginContainer plugin, String name);

    /**
     * Adds a new {@link PermissionAttachment} with a single initial permission set to the given value.
     *
     * @param plugin the owning plugin; must be loaded
     * @param name   the permission node name
     * @param value  the permission value
     * @return the new attachment
     * @throws IllegalArgumentException if the plugin is not loaded
     */
    PermissionAttachment addAttachment(PluginContainer plugin, String name, boolean value);

    /**
     * Adds a new {@link PermissionAttachment} that is automatically removed after the given
     * number of ticks.
     *
     * @param plugin the owning plugin; must be loaded
     * @param ticks  number of server ticks before the attachment is automatically removed
     * @return the new attachment, or {@code null} if the timed removal could not be scheduled
     * @throws IllegalArgumentException if the plugin is not loaded or ticks is not positive
     */
    @Nullable
    PermissionAttachment addAttachment(PluginContainer plugin, long ticks);

    /**
     * Adds a new {@link PermissionAttachment} with a single initial permission that is
     * automatically removed after the given number of ticks.
     *
     * @param plugin the owning plugin; must be loaded
     * @param name   the permission node name
     * @param value  the permission value
     * @param ticks  number of server ticks before the attachment is automatically removed
     * @return the new attachment, or {@code null} if the timed removal could not be scheduled
     * @throws IllegalArgumentException if the plugin is not loaded or ticks is not positive
     */
    @Nullable
    PermissionAttachment addAttachment(PluginContainer plugin, String name, boolean value, long ticks);

    /**
     * Removes the given {@link PermissionAttachment} and triggers permission recalculation.
     *
     * @param attachment the attachment to remove
     * @throws IllegalArgumentException if the attachment does not belong to this permissible
     */
    void removeAttachment(PermissionAttachment attachment);

    /**
     * Recalculates all effective permissions by replaying defaults and all attachments.
     *
     * <p>This is called automatically when attachments are added or removed, or when
     * permission defaults change. Does not normally need to be called directly.</p>
     */
    void recalculatePermissions();

    /**
     * Returns an unmodifiable snapshot of all currently effective permissions.
     *
     * <p>Each entry captures the resolved boolean value and the source attachment, or
     * {@code null} for default-sourced permissions.</p>
     *
     * @return set of effective permission info records
     */
    Set<PermissionAttachmentInfo> getEffectivePermissions();
}
