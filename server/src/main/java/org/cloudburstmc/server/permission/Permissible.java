package org.cloudburstmc.server.permission;

import org.cloudburstmc.api.plugin.PluginContainer;

import java.util.Map;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public interface Permissible extends ServerOperator {

    boolean isPermissionSet(String name);

    boolean isPermissionSet(Permission permission);

    boolean hasPermission(String name);

    boolean hasPermission(Permission permission);

    PermissionAttachment addAttachment(PluginContainer plugin);

    PermissionAttachment addAttachment(PluginContainer plugin, String name);

    PermissionAttachment addAttachment(PluginContainer plugin, String name, Boolean value);

    void removeAttachment(PermissionAttachment attachment);

    void recalculatePermissions();

    Map<String, PermissionAttachmentInfo> getEffectivePermissions();

}
