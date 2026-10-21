package org.cloudburstmc.server.permission;

import org.cloudburstmc.api.permission.Permission;
import org.cloudburstmc.api.permission.PermissionDefault;
import org.cloudburstmc.server.command.defaults.TimeCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DefaultPermissionsTest {

    @Test
    void timeCommandPermissionsAreRegisteredUnderTheTimeParent() {
        CloudPermissionManager manager = new CloudPermissionManager();
        DefaultPermissions.registerCorePermissions(manager);
        Permission parent = manager.getPermission("cloudburst.command.time").orElseThrow();

        for (String name : new TimeCommand().getAccessPermissions()) {
            Permission permission = manager.getPermission(name).orElseThrow();

            assertEquals(PermissionDefault.OPERATORS, permission.defaultValue());
            assertEquals(Boolean.TRUE, parent.children().get(name));
        }

        assertFalse(manager.getPermission("cloudburst.command.time.start").isPresent());
        assertFalse(manager.getPermission("cloudburst.command.time.stop").isPresent());
        assertFalse(manager.getPermission("cloudburst.command.time.pause").isPresent());
        assertFalse(manager.getPermission("cloudburst.command.time.resume").isPresent());
        assertFalse(manager.getPermission("cloudburst.command.time.rate").isPresent());
    }

    @Test
    void registersCompleteBuiltInGraph() {
        CloudPermissionManager manager = new CloudPermissionManager();
        DefaultPermissions.registerCorePermissions(manager);

        assertTrue(manager.getPermission("cloudburst.command").isPresent());
        assertTrue(manager.getPermission("cloudburst.command.ban").isPresent());
        assertTrue(manager.getPermission("cloudburst.command.ban-ip").isPresent());
        assertTrue(manager.getPermission("cloudburst.command.banlist").isPresent());
        assertTrue(manager.getPermission("cloudburst.command.pardon").isPresent());
        assertTrue(manager.getPermission("cloudburst.command.pardon-ip").isPresent());
        assertTrue(manager.getPermission("cloudburst.command.gamemode.other").isPresent());
        assertTrue(manager.getPermission("cloudburst.command.kill.self").isPresent());
        assertTrue(manager.getPermission("cloudburst.command.setblock").isPresent());
    }
}
