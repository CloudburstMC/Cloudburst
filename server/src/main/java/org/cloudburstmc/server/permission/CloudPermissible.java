package org.cloudburstmc.server.permission;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import org.cloudburstmc.api.permission.*;
import org.cloudburstmc.api.plugin.PluginContainer;
import org.cloudburstmc.server.CloudServer;

import java.util.*;

/**
 * Resolves immutable permission definitions and plugin attachments for one subject.
 */
public final class CloudPermissible implements Permissible {

    private final CloudPermissionManager manager;
    private final ServerOperator operator;
    private final Permissible subject;
    private final Runnable changeListener;
    private final List<CloudPermissionAttachment> attachments = new ArrayList<>();
    private final Map<String, EffectivePermission> effectivePermissions = new LinkedHashMap<>();
    private boolean valid = true;

    public CloudPermissible(PermissionManager manager, ServerOperator operator, Runnable changeListener) {
        if (!(Objects.requireNonNull(manager, "manager") instanceof CloudPermissionManager cloudManager)) {
            throw new IllegalArgumentException("Unsupported permission manager implementation: " + manager.getClass().getName());
        }

        this.manager = cloudManager;
        this.operator = Objects.requireNonNull(operator, "operator");
        this.subject = operator instanceof Permissible permissible ? permissible : this;
        this.changeListener = Objects.requireNonNull(changeListener, "changeListener");
        this.manager.attach(this);
        this.rebuild();
    }

    @Override
    public boolean isOp() {
        return this.operator.isOp();
    }

    @Override
    public void setOp(boolean value) {
        this.operator.setOp(value);
    }

    @Override
    public synchronized boolean isPermissionSet(String name) {
        return this.effectivePermissions.containsKey(Permission.normalizeName(name));
    }

    @Override
    public synchronized boolean hasPermission(String name) {
        EffectivePermission permission = this.effectivePermissions.get(Permission.normalizeName(name));
        return permission != null && permission.granted();
    }

    @Override
    public PermissionAttachment addAttachment(PluginContainer plugin) {
        return this.addAttachment(plugin, Map.of());
    }

    @Override
    public synchronized PermissionAttachment addAttachment(PluginContainer plugin, Map<String, Boolean> permissions) {
        this.requireValid();
        this.requireLoaded(plugin);
        CloudPermissionAttachment attachment = new CloudPermissionAttachment(this, this.subject, plugin, permissions);
        this.attachments.add(attachment);
        this.refresh();
        return attachment;
    }

    @Override
    public PermissionAttachment addTemporaryAttachment(PluginContainer plugin, Map<String, Boolean> permissions, long ticks) {
        if (ticks <= 0) {
            throw new IllegalArgumentException("Attachment lifetime must be positive");
        }

        PermissionAttachment attachment = this.addAttachment(plugin, permissions);
        try {
            CloudServer.getInstance().getGlobalScheduler().runDelayed(
                    plugin, task -> attachment.remove(), ticks);
        } catch (RuntimeException exception) {
            attachment.remove();
            throw new IllegalStateException("Unable to schedule permission attachment removal", exception);
        }

        return attachment;
    }

    @Override
    public synchronized Set<EffectivePermission> getEffectivePermissions() {
        return Set.copyOf(this.effectivePermissions.values());
    }

    public synchronized void refresh() {
        if (!this.valid) {
            return;
        }

        this.rebuild();
        this.changeListener.run();
    }

    public synchronized void invalidate() {
        if (!this.valid) {
            return;
        }

        this.valid = false;
        this.manager.detach(this);

        for (CloudPermissionAttachment attachment : this.attachments) {
            attachment.deactivate();
        }

        this.attachments.clear();
        this.effectivePermissions.clear();
    }

    synchronized boolean removeAttachment(CloudPermissionAttachment attachment) {
        if (!this.valid || !this.attachments.remove(attachment)) {
            return false;
        }

        attachment.deactivate();
        this.refresh();
        return true;
    }

    private void rebuild() {
        try (Timing ignored = Timings.permissibleCalculationTimer.startTiming()) {
            this.effectivePermissions.clear();
            boolean operator = this.isOp();
            Collection<Permission> definitions = this.manager.permissions();
            for (Permission permission : definitions) {
                if (permission.defaultValue().grants(operator)) {
                    this.apply(this.effectivePermissions, permission.name(), true, null, new LinkedHashSet<>());
                }
            }

            for (Permission permission : definitions) {
                if (permission.defaultValue().grants(operator)) {
                    this.effectivePermissions.put(permission.name(),
                            new EffectivePermission(permission.name(), true, null));
                }
            }

            for (CloudPermissionAttachment attachment : this.attachments) {
                Map<String, Boolean> overrides = attachment.getPermissions();
                Map<String, EffectivePermission> resolved = new LinkedHashMap<>();
                overrides.forEach((name, value) ->
                        this.apply(resolved, name, value, attachment, new LinkedHashSet<>()));
                overrides.forEach((name, value) -> {
                    String normalizedName = Permission.normalizeName(name);
                    resolved.put(normalizedName, new EffectivePermission(normalizedName, value, attachment));
                });
                this.effectivePermissions.putAll(resolved);
            }
        }
    }

    private void apply(
            Map<String, EffectivePermission> target,
            String name,
            boolean value,
            CloudPermissionAttachment attachment,
            Set<String> path
    ) {
        String normalizedName = Permission.normalizeName(name);
        if (!path.add(normalizedName)) {
            return;
        }

        target.put(normalizedName, new EffectivePermission(normalizedName, value, attachment));
        this.manager.getPermission(normalizedName).ifPresent(permission ->
                permission.children().forEach((child, inheritedValue) ->
                        this.apply(target, child, inheritedValue == value, attachment, path)));
        path.remove(normalizedName);
    }

    private void requireLoaded(PluginContainer plugin) {
        Objects.requireNonNull(plugin, "plugin");
        if (!CloudServer.getInstance().getPluginManager().isLoaded(plugin.getDescription().getId())) {
            throw new IllegalArgumentException("Plugin is not loaded: " + plugin.getDescription().getId());
        }
    }

    private void requireValid() {
        if (!this.valid) {
            throw new IllegalStateException("Permission subject is no longer active");
        }
    }
}
