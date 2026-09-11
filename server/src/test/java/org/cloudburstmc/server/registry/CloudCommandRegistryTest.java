package org.cloudburstmc.server.registry;

import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.tree.CommandNode;
import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.plugin.PluginDescription;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CloudCommandRegistryTest {

    @Test
    void preservesRootRequirement() {
        CloudCommandRegistry registry = new CloudCommandRegistry();
        registry.register(plugin(),
                Commands.literal("test")
                        .requires(Commands.requiresPermission("test.use"))
                        .executes(context -> 1)
                        .build());
        CommandNode<CommandSourceStack> root = registry.commandRoots().iterator().next();

        assertFalse(root.canUse(source(false)));
        assertTrue(root.canUse(source(true)));
    }

    @Test
    void aliasesPreserveRootRequirement() {
        CloudCommandRegistry registry = new CloudCommandRegistry();
        registry.register(plugin(),
                Commands.literal("test")
                        .requires(Commands.requiresPermission("test.use"))
                        .executes(context -> 1)
                        .build(),
                List.of("alias"));
        CommandNode<CommandSourceStack> alias = registry.commandRoots().stream()
                .filter(node -> node.getName().equals("alias"))
                .findFirst()
                .orElseThrow();
        CommandNode<CommandSourceStack> command = registry.commandRoots().stream()
                .filter(node -> node.getName().equals("test"))
                .findFirst()
                .orElseThrow();

        assertFalse(alias.canUse(source(false)));
        assertTrue(alias.canUse(source(true)));
        assertNull(alias.getRedirect());
        assertSame(command.getRequirement(), alias.getRequirement());
    }

    @Test
    void exposesEveryAvailableRootLabel() {
        CloudCommandRegistry registry = new CloudCommandRegistry();
        registry.register(plugin(),
                Commands.literal("test")
                        .requires(Commands.requiresPermission("test.use"))
                        .executes(context -> 1)
                        .build(),
                List.of("alias"));

        assertEquals(Set.of(), Set.copyOf(registry.labels(sender(false))));
        assertEquals(Set.of("test", "alias", "example:test", "example:alias"),
                Set.copyOf(registry.labels(sender(true))));
    }

    @Test
    void generatesUsageFromAvailableNodes() {
        CloudCommandRegistry registry = new CloudCommandRegistry();
        registry.register(plugin(),
                Commands.literal("test")
                        .executes(context -> 1)
                        .then(Commands.literal("visible").executes(context -> 1))
                        .then(Commands.literal("restricted")
                                .requires(Commands.requiresPermission("test.restricted"))
                                .executes(context -> 1))
                        .build());

        List<String> restricted = registry.usages("test", source(false));
        List<String> permitted = registry.usages("test", source(true));

        assertTrue(restricted.contains("/test"));
        assertTrue(restricted.stream().anyMatch(usage -> usage.contains("visible")));
        assertFalse(restricted.stream().anyMatch(usage -> usage.contains("restricted")));
        assertTrue(permitted.stream().anyMatch(usage -> usage.contains("restricted")));
    }

    @Test
    void completesOnlyAvailableCommandNodes() {
        CloudCommandRegistry registry = new CloudCommandRegistry();
        registry.register(plugin(),
                Commands.literal("test")
                        .then(Commands.literal("visible").executes(context -> 1))
                        .then(Commands.literal("restricted")
                                .requires(Commands.requiresPermission("test.restricted"))
                                .executes(context -> 1))
                        .build());

        Set<String> restricted = registry.suggestions("test ", 5, source(false)).join().getList().stream()
                .map(Suggestion::getText)
                .collect(java.util.stream.Collectors.toSet());
        Set<String> permitted = registry.suggestions("test ", 5, source(true)).join().getList().stream()
                .map(Suggestion::getText)
                .collect(java.util.stream.Collectors.toSet());

        assertEquals(Set.of("visible"), restricted);
        assertEquals(Set.of("visible", "restricted"), permitted);
    }

    @Test
    void alwaysRegistersStablePluginNamespace() {
        CloudCommandRegistry registry = new CloudCommandRegistry();
        Set<String> labels = registry.register(plugin(), Commands.literal("test").executes(context -> 1).build());

        assertEquals(Set.of("test", "example:test"), labels);
    }

    @Test
    void usesStableNamespaceWhenUnqualifiedRootIsTaken() {
        CloudCommandRegistry registry = new CloudCommandRegistry();
        registry.register(plugin("first"), Commands.literal("test").executes(context -> 1).build());
        Set<String> labels = registry.register(plugin("second"), Commands.literal("test").executes(context -> 1).build());

        assertEquals(Set.of("second:test"), labels);
        assertTrue(registry.commandRoots().stream().anyMatch(node -> node.getName().equals("test")));
        assertTrue(registry.commandRoots().stream().anyMatch(node -> node.getName().equals("first:test")));
        assertTrue(registry.commandRoots().stream().anyMatch(node -> node.getName().equals("second:test")));
    }

    private static CommandSourceStack source(boolean permitted) {
        CommandSender sender = sender(permitted);
        return InterfaceProxy.create(CommandSourceStack.class, Map.of("sender", sender));
    }

    private static CommandSender sender(boolean permitted) {
        Server server = server();
        return InterfaceProxy.create(CommandSender.class, Map.of(
                "getServer", server,
                "hasPermission", permitted));
    }

    private static Server server() {
        Level level = InterfaceProxy.create(Level.class);
        return InterfaceProxy.create(Server.class, Map.of("getDefaultLevel", level));
    }

    private static PluginDescription plugin() {
        return plugin("example");
    }

    private static PluginDescription plugin(String id) {
        return InterfaceProxy.create(PluginDescription.class, Map.of("getId", id));
    }
}
