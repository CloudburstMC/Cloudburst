package org.cloudburstmc.server.permission;

import org.cloudburstmc.api.permission.Permission;
import org.cloudburstmc.api.permission.PermissionDefault;
import org.cloudburstmc.api.permission.ServerOperator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CloudPermissibleTest {

    @Test
    void resolvesDefaultPermissionGraph() {
        CloudPermissionManager manager = new CloudPermissionManager();
        manager.registerAll(List.of(
                permission("test.parent", PermissionDefault.EVERYONE, Map.of("test.allowed", true, "test.denied", false)),
                permission("test.allowed", PermissionDefault.NONE, Map.of()),
                permission("test.denied", PermissionDefault.NONE, Map.of())
        ));

        CloudPermissible permissible = new CloudPermissible(manager, new TestOperator(false), () -> {});

        assertTrue(permissible.hasPermission("test.parent"));
        assertTrue(permissible.hasPermission("test.allowed"));
        assertFalse(permissible.hasPermission("test.denied"));
        assertTrue(permissible.isPermissionSet("test.denied"));
    }

    @Test
    void refreshesSubjectsAfterDefinitionReplacement() {
        CloudPermissionManager manager = new CloudPermissionManager();
        manager.register(permission("test.permission", PermissionDefault.NONE, Map.of()));
        AtomicInteger changes = new AtomicInteger();
        CloudPermissible permissible = new CloudPermissible(manager, new TestOperator(false), changes::incrementAndGet);

        manager.replace(permission("test.permission", PermissionDefault.EVERYONE, Map.of()));

        assertTrue(permissible.hasPermission("test.permission"));
        assertEquals(1, changes.get());
    }

    @Test
    void directDefaultOverridesInheritedDefaultRegardlessOfRegistrationOrder() {
        CloudPermissionManager manager = new CloudPermissionManager();
        manager.registerAll(List.of(
                permission("test.child", PermissionDefault.EVERYONE, Map.of()),
                permission("test.parent", PermissionDefault.EVERYONE, Map.of("test.child", false))
        ));

        CloudPermissible permissible = new CloudPermissible(manager, new TestOperator(false), () -> {});
        assertTrue(permissible.hasPermission("test.child"));
    }

    private static Permission permission(String name, PermissionDefault defaultValue, Map<String, Boolean> children) {
        return new Permission(name, "", defaultValue, children);
    }

    private static final class TestOperator implements ServerOperator {

        private boolean operator;

        private TestOperator(boolean operator) {
            this.operator = operator;
        }

        @Override
        public boolean isOp() {
            return this.operator;
        }

        @Override
        public void setOp(boolean value) {
            this.operator = value;
        }
    }
}
