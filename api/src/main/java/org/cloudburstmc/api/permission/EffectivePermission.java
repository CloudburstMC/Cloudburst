package org.cloudburstmc.api.permission;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Describes a resolved permission value and the attachment that supplied it.
 *
 * @param name       permission node name
 * @param granted    resolved value
 * @param attachment source attachment, or {@code null} when granted by a default
 */
public record EffectivePermission(String name, boolean granted, @Nullable PermissionAttachment attachment) {
    public EffectivePermission {
        name = Permission.normalizeName(name);
    }
}
