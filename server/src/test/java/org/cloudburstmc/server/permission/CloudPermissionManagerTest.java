package org.cloudburstmc.server.permission;

import org.cloudburstmc.api.permission.Permission;
import org.cloudburstmc.api.permission.PermissionDefault;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CloudPermissionManagerTest {

    @Test
    void registersDefinitionsAtomically() {
        CloudPermissionManager manager = new CloudPermissionManager();
        Permission first = permission("test.first", Map.of());
        Permission duplicate = permission("test.first", Map.of());

        assertThrows(IllegalArgumentException.class, () -> manager.registerAll(List.of(first, duplicate)));
        assertTrue(manager.permissions().isEmpty());
    }

    @Test
    void rejectsInheritanceCyclesAtomically() {
        CloudPermissionManager manager = new CloudPermissionManager();
        Permission first = permission("test.first", Map.of("test.second", true));
        Permission second = permission("test.second", Map.of("test.first", true));

        assertThrows(IllegalArgumentException.class, () -> manager.registerAll(List.of(first, second)));
        assertTrue(manager.permissions().isEmpty());
    }

    @Test
    void rejectsMissingChildrenAtomically() {
        CloudPermissionManager manager = new CloudPermissionManager();
        Permission parent = permission("test.parent", Map.of("test.missing", true));

        assertThrows(IllegalArgumentException.class, () -> manager.register(parent));
        assertTrue(manager.permissions().isEmpty());
    }

    @Test
    void unregisterRemovesParentEdges() {
        CloudPermissionManager manager = new CloudPermissionManager();
        manager.registerAll(List.of(
                permission("test.parent", Map.of("test.child", true)),
                permission("test.child", Map.of())
        ));

        manager.unregister("test.child");

        assertEquals(Map.of(), manager.getPermission("test.parent").orElseThrow().children());
    }

    private static Permission permission(String name, Map<String, Boolean> children) {
        return new Permission(name, "", PermissionDefault.NONE, children);
    }
}
