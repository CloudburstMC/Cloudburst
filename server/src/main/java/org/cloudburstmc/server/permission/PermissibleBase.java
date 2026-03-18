package org.cloudburstmc.server.permission;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.permission.*;
import org.cloudburstmc.api.plugin.PluginContainer;
import org.cloudburstmc.api.scheduler.GlobalScheduler;
import org.cloudburstmc.server.CloudServer;

import java.util.*;

/**
 * Concrete {@link Permissible} implementation used by players and the console via composition.
 *
 * <p>All public mutating and reading methods are {@code synchronized} to guard against concurrent
 * modification during permission recalculation.</p>
 *
 * <p>This class is NOT thread-safe beyond the per-method synchronization noted above. Callers
 * must not rely on cross-method atomicity.</p>
 *
 * <p>{@link CloudPermissionManager} is single-threaded and holds no lock of its own.
 * {@code recalculatePermissions()} acquires {@code this} lock and then calls into the manager
 * without holding any other lock, so no deadlock can arise from the manager side.</p>
 */
@Log4j2
public class PermissibleBase implements Permissible {

    private final PermissionManager manager;
    private final @Nullable ServerOperator opable;

    /**
     * The outer permissible whose identity is stored in {@link PermissionAttachmentInfo} records.
     * Equals {@code opable} when it also implements {@link Permissible}; otherwise {@code this}.
     */
    private final Permissible parent;

    private final List<PermissionAttachment> attachments = new ArrayList<>();

    /**
     * The flattened, case-normalized effective permission map.
     * Keys are always lowercase.
     */
    private final Map<String, PermissionAttachmentInfo> permissions = new HashMap<>();

    public PermissibleBase(PermissionManager manager, @Nullable ServerOperator opable) {
        if (manager == null) {
            throw new IllegalArgumentException("PermissionManager cannot be null");
        }
        this.manager = manager;
        this.opable = opable;
        this.parent = (opable instanceof Permissible permissible) ? permissible : this;
        recalculatePermissions();
    }

    @Override
    public synchronized boolean isOp() {
        return opable != null && opable.isOp();
    }

    @Override
    public synchronized void setOp(boolean value) {
        if (opable == null) {
            throw new UnsupportedOperationException("Cannot change op value as no ServerOperator is set");
        }
        opable.setOp(value);
    }

    @Override
    public synchronized boolean isPermissionSet(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Permission name cannot be null");
        }
        return permissions.containsKey(name.toLowerCase(Locale.ROOT));
    }

    @Override
    public synchronized boolean isPermissionSet(Permission permission) {
        if (permission == null) {
            throw new IllegalArgumentException("Permission cannot be null");
        }
        return isPermissionSet(permission.getName());
    }

    @Override
    public synchronized boolean hasPermission(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Permission name cannot be null");
        }

        String key = name.toLowerCase(Locale.ROOT);
        PermissionAttachmentInfo info = permissions.get(key);
        if (info != null) {
            return info.value();
        }

        Optional<Permission> optPerm = manager.getPermission(key);
        boolean op = isOp();
        return optPerm.map(permission -> permission.getDefault().getValue(op)).orElseGet(() -> Permission.DEFAULT_PERMISSION.getValue(op));
    }

    @Override
    public synchronized boolean hasPermission(Permission permission) {
        if (permission == null) {
            throw new IllegalArgumentException("Permission cannot be null");
        }

        String key = permission.getName().toLowerCase(Locale.ROOT);
        PermissionAttachmentInfo info = permissions.get(key);
        if (info != null) {
            return info.value();
        }

        return permission.getDefault().getValue(isOp());
    }

    @Override
    public synchronized PermissionAttachment addAttachment(PluginContainer plugin) {
        return addAttachmentInternal(plugin, null, false);
    }

    @Override
    public synchronized PermissionAttachment addAttachment(PluginContainer plugin, String name) {
        if (name == null) {
            throw new IllegalArgumentException("Permission name cannot be null");
        }
        return addAttachmentInternal(plugin, name, true);
    }

    @Override
    public synchronized PermissionAttachment addAttachment(PluginContainer plugin, String name, boolean value) {
        return addAttachmentInternal(plugin, name, value);
    }

    @Override
    public synchronized @Nullable PermissionAttachment addAttachment(PluginContainer plugin, long ticks) {
        if (ticks <= 0) {
            throw new IllegalArgumentException("Ticks must be positive");
        }

        PermissionAttachment attachment = addAttachmentInternal(plugin, null, false);
        return scheduleRemoval(plugin, attachment, ticks);
    }

    @Override
    public synchronized @Nullable PermissionAttachment addAttachment(PluginContainer plugin, String name, boolean value, long ticks) {
        if (ticks <= 0) {
            throw new IllegalArgumentException("Ticks must be positive");
        }
        if (name == null) {
            throw new IllegalArgumentException("Permission name cannot be null");
        }

        PermissionAttachment attachment = addAttachmentInternal(plugin, name, value);
        return scheduleRemoval(plugin, attachment, ticks);
    }

    /**
     * Creates a new attachment and adds it to this permissible.
     *
     * <p>If {@code name} is non-null, {@link PermissionAttachment#setPermission} is called
     * (which triggers recalculation internally). Otherwise, {@link #recalculatePermissions()}
     * is called directly.</p>
     */
    private PermissionAttachment addAttachmentInternal(PluginContainer plugin, @Nullable String name, boolean value) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }

        if (!CloudServer.getInstance().getPluginManager().isLoaded(plugin.getDescription().getId())) {
            throw new IllegalArgumentException("Plugin " + plugin.getDescription().getId() + " is not loaded");
        }

        PermissionAttachment result = new PermissionAttachment(plugin, parent);
        attachments.add(result);
        if (name != null) {
            result.setPermission(name, value);
        } else {
            recalculatePermissions();
        }

        return result;
    }

    /**
     * Schedules removal of the given attachment after {@code ticks} ticks.
     *
     * <p>If scheduling fails, the attachment is removed immediately and {@code null} is returned
     * so callers can detect the failure per the {@link Permissible} contract.</p>
     */
    private @Nullable PermissionAttachment scheduleRemoval(PluginContainer plugin, PermissionAttachment attachment, long ticks) {
        try {
            GlobalScheduler scheduler = CloudServer.getInstance().getGlobalScheduler();
            scheduler.runDelayed(plugin, task -> attachment.remove(), ticks);
            return attachment;
        } catch (Exception ex) {
            log.error("Failed to schedule timed permission removal for plugin {}; removing attachment immediately",
                    plugin.getDescription().getId(), ex);
            attachment.remove();
            return null;
        }
    }

    @Override
    public synchronized void removeAttachment(PermissionAttachment attachment) {
        if (attachment == null) {
            throw new IllegalArgumentException("Attachment cannot be null");
        }

        if (!attachments.remove(attachment)) {
            throw new IllegalArgumentException("Given attachment is not part of Permissible object " + parent);
        }

        PermissionRemovedExecutor callback = attachment.getRemovalCallback();
        if (callback != null) {
            callback.attachmentRemoved(attachment);
        }

        recalculatePermissions();
    }

    @Override
    public synchronized void recalculatePermissions() {
        try (Timing ignored = Timings.permissibleCalculationTimer.startTiming()) {
            clearPermissions();

            boolean op = isOp();
            Map<String, Permission> defaults = manager.getDefaultPermissions(op);
            manager.subscribeToDefaultPerms(op, parent);

            for (Permission perm : defaults.values()) {
                String key = perm.getName().toLowerCase(Locale.ROOT);
                permissions.put(key, new PermissionAttachmentInfo(parent, key, null, true));
                manager.subscribeToPermission(key, parent);
                calculateChildPermissions(perm.getChildren(), false, null);
            }

            for (PermissionAttachment attachment : attachments) {
                calculateChildPermissions(attachment.getPermissions(), false, attachment);
            }
        }
    }

    /**
     * Unsubscribes from all tracked permission nodes, clears the effective permission map,
     * and clears the attachment list. Intended for permanent shutdown of a permissible
     * (e.g. player disconnect).
     *
     * <p><strong>This is a one-way teardown; call only from the player disconnect handler.</strong>
     * After calling this method no other mutating methods should be invoked. Clearing the
     * attachment list ensures that any plugin retaining a stale {@link PermissionAttachment}
     * reference and later calling {@link PermissionAttachment#remove()} cannot re-enter this
     * dead permissible and trigger a spurious {@link IllegalArgumentException}.</p>
     */
    public synchronized void invalidate() {
        clearPermissions();
        attachments.clear();
    }

    /**
     * Clears all effective permissions and unsubscribes from all tracked nodes.
     * Called only from {@link #recalculatePermissions()} (which holds the lock) and
     * {@link #invalidate()} (which also holds the lock), so no additional synchronization
     * is needed here.
     *
     * <p>We unsubscribe from <em>both</em> default-perm sets unconditionally, even though
     * {@link #recalculatePermissions()} only subscribes to the set matching the current op
     * status. This handles the case where a player's op status changes between two
     * recalculations: without the unconditional double-unsubscribe they would remain in the
     * previous set until the next GC sweep of the WeakHashMap.</p>
     */
    private void clearPermissions() {
        for (String name : permissions.keySet()) {
            manager.unsubscribeFromPermission(name, parent);
        }

        manager.unsubscribeFromDefaultPerms(false, parent);
        manager.unsubscribeFromDefaultPerms(true, parent);
        permissions.clear();
    }

    private void calculateChildPermissions(Map<String, Boolean> children, boolean invert, @Nullable PermissionAttachment attachment) {
        for (Map.Entry<String, Boolean> entry : children.entrySet()) {
            String key = entry.getKey().toLowerCase(Locale.ROOT);
            boolean value = entry.getValue() ^ invert;

            permissions.put(key, new PermissionAttachmentInfo(parent, key, attachment, value));
            manager.subscribeToPermission(key, parent);
            manager.getPermission(key).ifPresent(perm -> calculateChildPermissions(perm.getChildren(), !value, attachment));
        }
    }

    @Override
    public synchronized Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return new HashSet<>(permissions.values());
    }
}
