package org.cloudburstmc.api.permission;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;

/**
 * Represents a single effective permission for a {@link Permissible}, capturing the resolved
 * value and the {@link PermissionAttachment} that granted it (or {@code null} for defaults).
 *
 * <p>The {@code permission} field is normalized to lowercase at construction time so that
 * lookup and equality checks are always case-insensitive.</p>
 *
 * @param permissible the permissible this info belongs to
 * @param permission  the permission node name, normalized to lowercase
 * @param attachment  the attachment that granted this permission, or {@code null} for server defaults
 * @param value       {@code true} if the permission is granted, {@code false} if it is denied
 */
public record PermissionAttachmentInfo(
        Permissible permissible,
        String permission,
        @Nullable PermissionAttachment attachment,
        boolean value
) {
    public PermissionAttachmentInfo {
        if (permissible == null) {
            throw new IllegalArgumentException("Permissible cannot be null");
        }
        if (permission == null) {
            throw new IllegalArgumentException("Permission name cannot be null");
        }
        permission = permission.toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the permissible this info belongs to.
     */
    public Permissible getPermissible() {
        return permissible;
    }

    /**
     * Returns the permission node name.
     */
    public String getPermission() {
        return permission;
    }

    /**
     * Returns the attachment that granted this permission, or {@code null} for server defaults.
     */
    public @Nullable PermissionAttachment getAttachment() {
        return attachment;
    }

    /**
     * Returns {@code true} if the permission is granted, {@code false} if it is denied.
     */
    public boolean getValue() {
        return value;
    }
}
