package org.cloudburstmc.server.permission;

import org.cloudburstmc.api.permission.Permissible;
import org.cloudburstmc.api.permission.Permission;
import org.cloudburstmc.api.permission.PermissionAttachment;
import org.cloudburstmc.api.plugin.PluginContainer;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Mutable permission overrides owned by a plugin for one permission subject.
 */
public class CloudPermissionAttachment implements PermissionAttachment {

    private final CloudPermissible owner;
    private final Permissible permissible;
    private final PluginContainer plugin;
    private final Map<String, Boolean> permissions = new LinkedHashMap<>();
    private boolean active = true;

    CloudPermissionAttachment(
            CloudPermissible owner,
            Permissible permissible,
            PluginContainer plugin,
            Map<String, Boolean> permissions
    ) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.permissible = Objects.requireNonNull(permissible, "permissible");
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.permissions.putAll(normalize(permissions));
    }

    @Override
    public PluginContainer getPlugin() {
        return this.plugin;
    }

    @Override
    public Permissible getPermissible() {
        return this.permissible;
    }

    @Override
    public synchronized Map<String, Boolean> getPermissions() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(this.permissions));
    }

    @Override
    public void setPermissions(Map<String, Boolean> permissions) {
        Map<String, Boolean> normalized = normalize(permissions);
        synchronized (this) {
            this.requireActive();
            if (this.permissions.equals(normalized)) {
                return;
            }
            this.permissions.clear();
            this.permissions.putAll(normalized);
        }

        this.owner.refresh();
    }

    @Override
    public void setPermission(String name, boolean value) {
        boolean changed;
        synchronized (this) {
            this.requireActive();
            Boolean previous = this.permissions.put(Permission.normalizeName(name), value);
            changed = !Objects.equals(previous, value);
        }

        if (changed) {
            this.owner.refresh();
        }
    }

    @Override
    public void unsetPermission(String name) {
        boolean changed;
        synchronized (this) {
            this.requireActive();
            changed = this.permissions.remove(Permission.normalizeName(name)) != null;
        }

        if (changed) {
            this.owner.refresh();
        }
    }

    @Override
    public void clearPermissions() {
        boolean changed;
        synchronized (this) {
            this.requireActive();
            changed = !this.permissions.isEmpty();
            this.permissions.clear();
        }

        if (changed) {
            this.owner.refresh();
        }
    }

    @Override
    public boolean remove() {
        return this.owner.removeAttachment(this);
    }

    synchronized void deactivate() {
        this.active = false;
    }

    private void requireActive() {
        if (!this.active) {
            throw new IllegalStateException("Permission attachment is no longer active");
        }
    }

    private static Map<String, Boolean> normalize(Map<String, Boolean> permissions) {
        Objects.requireNonNull(permissions, "permissions");
        LinkedHashMap<String, Boolean> normalized = new LinkedHashMap<>(permissions.size());
        permissions.forEach((name, value) -> normalized.put(
                Permission.normalizeName(name), Objects.requireNonNull(value, "permission value")));
        return normalized;
    }
}
