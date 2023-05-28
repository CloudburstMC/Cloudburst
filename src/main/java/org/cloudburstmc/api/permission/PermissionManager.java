package org.cloudburstmc.api.permission;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface PermissionManager {

    Optional<Permission> getPermission(String name);

    boolean addPermission(Permission nukkitPermission);

    void removePermission(String name);

    void removePermission(Permission permission);

    Map<String, Permission> getDefaultPermissions(boolean op);

    void recalculatePermissionDefaults(Permission nukkitPermission);

    void subscribeToPermission(String permission, Permissible permissible);

    void unsubscribeFromPermission(String permission, Permissible permissible);

    Set<Permissible> getPermissionSubscriptions(String permission);

    void subscribeToDefaultPerms(boolean op, Permissible permissible);

    void unsubscribeFromDefaultPerms(boolean op, Permissible permissible);

    Set<Permissible> getDefaultPermSubscriptions(boolean op);

    Map<String, Permission> getPermissions();
}
