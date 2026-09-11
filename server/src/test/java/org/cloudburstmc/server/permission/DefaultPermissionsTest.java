package org.cloudburstmc.server.permission;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultPermissionsTest {

    @Test
    void registersCompleteBuiltInGraph() {
        CloudPermissionManager manager = new CloudPermissionManager();
        DefaultPermissions.registerCorePermissions(manager);

        assertTrue(manager.getPermission("cloudburst.command").isPresent());
        assertTrue(manager.getPermission("cloudburst.command.gamemode.other").isPresent());
        assertTrue(manager.getPermission("cloudburst.command.kill.self").isPresent());
        assertTrue(manager.getPermission("cloudburst.command.setblock").isPresent());
    }
}
