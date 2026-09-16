package org.cloudburstmc.server.command.network;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArgumentConstraint;
import org.cloudburstmc.api.command.argument.CommandArgumentType;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.protocol.bedrock.data.command.*;
import org.cloudburstmc.server.command.defaults.GameruleCommand;
import org.cloudburstmc.server.registry.CloudCommandRegistry;
import org.cloudburstmc.server.registry.CloudGameRuleRegistry;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CommandNetworkCompilerTest {

    @Test
    void foldsContiguousExecutablePrefixesIntoOptionalParameters() {
        LiteralCommandNode<CommandSourceStack> root = Commands.literal("test")
                .executes(context -> 1)
                .then(Commands.literal("first")
                        .executes(context -> 1)
                        .then(Commands.argument("value", CommandArgumentTypes.integer())
                                .executes(context -> 1)))
                .build();

        List<CommandOverloadData> overloads = CommandNetworkCompiler.compileOverloads(root, source());

        assertEquals(1, overloads.size());
        assertEquals(2, overloads.getFirst().getOverloads().length);
        assertTrue(Arrays.stream(overloads.getFirst().getOverloads())
                .allMatch(CommandParamData::isOptional));
    }

    @Test
    void representsAnExecutableRootWithOneOptionalArgumentAsOneOverload() {
        LiteralCommandNode<CommandSourceStack> root = Commands.literal("test")
                .executes(context -> 1)
                .then(Commands.argument("target", new CloudCommandRegistry().arguments().entity())
                        .executes(context -> 1))
                .build();

        List<CommandOverloadData> overloads = CommandNetworkCompiler.compileOverloads(root, source());

        assertEquals(1, overloads.size());
        assertEquals(1, overloads.getFirst().getOverloads().length);
        assertTrue(overloads.getFirst().getOverloads()[0].isOptional());
    }

    @Test
    void preservesRequiredArgumentsAfterExecutableParents() {
        CommandArgumentType<String> rule = CommandArgumentTypes.fixedEnumMapped(
                "rule", "Rule", true,
                Map.of("first", Set.of(CommandArgumentConstraint.CHEATS_ENABLED)),
                value -> value, "first", "second");
        LiteralCommandNode<CommandSourceStack> root = Commands.literal("test")
                .executes(context -> 1)
                .then(Commands.argument("rule", rule)
                        .executes(context -> 1)
                        .then(Commands.argument("value", CommandArgumentTypes.bool())
                                .executes(context -> 1)))
                .build();

        List<CommandOverloadData> overloads = CommandNetworkCompiler.compileOverloads(root, source());

        assertEquals(2, overloads.size());
        assertEquals(0, overloads.get(1).getOverloads().length);
        CommandParamData[] parameters = overloads.getFirst().getOverloads();
        assertEquals(2, parameters.length);
        assertFalse(parameters[0].isOptional());
        assertTrue(parameters[0].getOptions().contains(CommandParamOption.HAS_SEMANTIC_CONSTRAINT));
        assertTrue(parameters[1].isOptional());
    }

    @Test
    void groupsGameRulesByValueType() {
        CloudCommandRegistry commandRegistry = new CloudCommandRegistry();
        GameruleCommand command = new GameruleCommand(CloudGameRuleRegistry.get());
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("gamerule");
        command.configure(builder, "gamerule", commandRegistry.arguments());

        List<CommandOverloadData> overloads = CommandNetworkCompiler.compileOverloads(builder.build(), source());

        assertEquals(4, overloads.size());
        assertEquals(1, overloads.stream().filter(overload -> overload.getOverloads().length == 0).count());

        CommandParamData[] booleanOverload = overload(overloads, "BoolGameRule");
        assertFalse(booleanOverload[0].isOptional());
        assertTrue(booleanOverload[0].getOptions().contains(CommandParamOption.HAS_SEMANTIC_CONSTRAINT));
        assertEquals(Set.of(CommandEnumConstraint.CHEATS_ENABLED), booleanOverload[0].getEnumData().getValues().get("commandblockoutput"));
        assertEquals(Set.of(), booleanOverload[0].getEnumData().getValues().get("dofiretick"));
        assertEquals(List.of("true", "false"), List.copyOf(booleanOverload[1].getEnumData().getValues().keySet()));
        assertTrue(booleanOverload[1].isOptional());

        CommandParamData[] integerOverload = overload(overloads, "IntGameRule");
        assertFalse(integerOverload[0].isOptional());
        assertEquals(Set.of(CommandEnumConstraint.CHEATS_ENABLED), integerOverload[0].getEnumData().getValues().get("functioncommandlimit"));
        assertEquals(Set.of(), integerOverload[0].getEnumData().getValues().get("spawnradius"));
        assertEquals(CommandParam.INT, integerOverload[1].getType());
        assertTrue(integerOverload[1].isOptional());

        CommandParamData[] waypointOverload = overload(overloads, "playerwaypointsRule");
        assertFalse(waypointOverload[0].isOptional());
        assertFalse(waypointOverload[1].isOptional());
        assertEquals("playerwaypointsValues", waypointOverload[1].getEnumData().getName());
    }

    @Test
    void keepsSeparateOverloadsAcrossNonExecutableNodes() {
        LiteralCommandNode<CommandSourceStack> root = Commands.literal("test")
                .executes(context -> 1)
                .then(Commands.literal("branch")
                        .then(Commands.argument("value", CommandArgumentTypes.integer())
                                .executes(context -> 1)))
                .build();

        List<CommandOverloadData> overloads = CommandNetworkCompiler.compileOverloads(root, source());

        assertEquals(List.of(2, 0), overloads.stream()
                .map(overload -> overload.getOverloads().length)
                .toList());
        assertFalse(overloads.stream()
                .flatMap(overload -> Arrays.stream(overload.getOverloads()))
                .anyMatch(CommandParamData::isOptional));
    }

    @Test
    void excludesDescendantsHiddenFromSource() {
        LiteralCommandNode<CommandSourceStack> root = Commands.literal("test")
                .then(Commands.literal("visible").executes(context -> 1))
                .then(Commands.literal("hidden")
                        .requires(source -> false)
                        .executes(context -> 1))
                .build();

        List<CommandOverloadData> overloads = CommandNetworkCompiler.compileOverloads(root, source());

        assertEquals(1, overloads.size());
        assertEquals("visible", overloads.getFirst().getOverloads()[0].getName());
    }

    @Test
    void expandsRedirectTargetSyntax() {
        LiteralCommandNode<CommandSourceStack> target = Commands.literal("target")
                .then(Commands.argument("value", CommandArgumentTypes.integer()).executes(context -> 1))
                .build();
        LiteralCommandNode<CommandSourceStack> root = Commands.literal("test").redirect(target).build();

        List<CommandOverloadData> overloads = CommandNetworkCompiler.compileOverloads(root, source());

        assertEquals(1, overloads.size());
        assertEquals("value", overloads.getFirst().getOverloads()[0].getName());
    }

    @Test
    void preservesMessageArgumentPresentation() {
        LiteralCommandNode<CommandSourceStack> root = Commands.literal("test")
                .then(Commands.argument("message", CommandArgumentTypes.message()).executes(context -> 1))
                .build();

        List<CommandOverloadData> overloads = CommandNetworkCompiler.compileOverloads(root, source());

        assertEquals(CommandParam.MESSAGE_ROOT, overloads.getFirst().getOverloads()[0].getType());
    }

    @Test
    void preservesNativeRotationPresentation() {
        LiteralCommandNode<CommandSourceStack> root = Commands.literal("test")
                .then(Commands.argument("rotation", CommandArgumentTypes.rotation()).executes(context -> 1))
                .build();

        CommandParamData parameter = CommandNetworkCompiler.compileOverloads(root, source())
                .getFirst()
                .getOverloads()[0];

        assertEquals(CommandParam.R_VALUE, parameter.getType());
    }

    @Test
    void preservesNativeItemRegistryMetadata() {
        CommandArgumentType<ItemType> item = CommandArgumentTypes.item(null, identifier -> Optional.empty(),
                List.of(Identifier.parse("minecraft:stone")));
        LiteralCommandNode<CommandSourceStack> root = Commands.literal("test")
                .then(Commands.argument("item", item).executes(context -> 1))
                .build();

        CommandParamData parameter = CommandNetworkCompiler.compileOverloads(root, source())
                .getFirst()
                .getOverloads()[0];

        assertTrue(parameter.getOptions().contains(CommandParamOption.HAS_SEMANTIC_CONSTRAINT));
        assertEquals(List.of(CommandEnumConstraint.ALLOW_ALIASES), List.copyOf(parameter.getEnumData().getValues().get("minecraft:stone")));
        assertTrue(parameter.getEnumData().getValues().containsKey("stone"));
    }

    @Test
    void rejectsConflictingGlobalEnumDefinitions() {
        CommandEnumData first = new CommandEnumData("Mode", Map.of("first", Set.of()), false);
        CommandEnumData second = new CommandEnumData("Mode", Map.of("second", Set.of()), false);

        assertThrows(IllegalStateException.class, () -> CommandNetworkCompiler.validateEnumDefinitions(List.of(command("first", first), command("second", second))));
    }

    @Test
    void acceptsSharedGlobalEnumDefinitions() {
        CommandEnumData definition = new CommandEnumData("Boolean", Map.of("false", Set.of(), "true", Set.of()), false);
        CommandNetworkCompiler.validateEnumDefinitions(List.of(command("first", definition), command("second", definition)));
    }

    private static CommandData command(String name, CommandEnumData definition) {
        CommandParamData parameter = new CommandParamData();
        parameter.setName("value");
        parameter.setEnumData(definition);
        return new CommandData(name, "", Set.of(), CommandPermission.ANY, null, Collections.emptyList(),
                new CommandOverloadData[]{new CommandOverloadData(false, new CommandParamData[]{parameter})});
    }

    private static CommandParamData[] overload(List<CommandOverloadData> overloads, String enumName) {
        return overloads.stream()
                .map(CommandOverloadData::getOverloads)
                .filter(parameters -> parameters.length > 0)
                .filter(parameters -> parameters[0].getEnumData() != null)
                .filter(parameters -> enumName.equals(parameters[0].getEnumData().getName()))
                .findFirst()
                .orElseThrow();
    }

    private static CommandSourceStack source() {
        return new CommandSourceStack() {
            @Override
            public CommandSender sender() {
                throw new UnsupportedOperationException();
            }

            @Override
            public @Nullable Entity executor() {
                return null;
            }

            @Override
            public Location location() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void sendSuccess(Component message, boolean broadcast) {
            }

            @Override
            public void sendFailure(Component message) {
            }

            @Override
            public CommandSourceStack withLocation(Location location) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CommandSourceStack withExecutor(Entity executor) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
