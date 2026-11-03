package org.cloudburstmc.server.command.network;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestion;
import me.lucko.spark.common.command.Command;
import me.lucko.spark.common.command.CommandModule;
import me.lucko.spark.common.command.modules.*;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.protocol.bedrock.data.command.*;
import org.cloudburstmc.server.command.CommandTreeValidator;
import org.cloudburstmc.server.diagnostics.spark.SparkCommandTree;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class SparkCommandTreeTest {

    @Test
    void advertisesCommandsAliasesAndActionsWithFreeTextFlags() {
        List<CommandOverloadData> overloads = compile(tree("spark", new ArrayList<>()), Set.of("spark"));

        assertEquals(7, overloads.size());
        assertEquals(Set.of("profiler", "sampler", "start", "stop", "health", "help"), enumValues(overloads));
        for (CommandOverloadData overload : overloads) {
            CommandParamData[] parameters = overload.getOverloads();
            if (parameters.length == 0) {
                continue;
            }
            assertFalse(parameters[0].isOptional());
            assertEquals(Set.of(parameters[0].getName()), Objects.requireNonNull(parameters[0].getEnumData()).getValues().keySet());
            if (!parameters[0].getName().equals("help")) {
                assertEquals(CommandParam.TEXT, parameters[parameters.length - 1].getType());
                assertTrue(parameters[parameters.length - 1].isOptional());
            }
        }
    }

    @Test
    void reusesSelectorEnumsWithoutOverlappingDefinitions() {
        List<CommandOverloadData> overloads = compile(tree("spark", new ArrayList<>()), Set.of("spark"));
        Map<String, Set<String>> definitions = selectorDefinitions(overloads);

        assertEquals(Set.of("profiler", "sampler", "health", "help"), definitions.keySet());
        definitions.values().forEach(names -> assertEquals(1, names.size()));
    }

    @Test
    void hidesCommandsAndTheirAliasesWithoutThePrimaryPermission() {
        List<CommandOverloadData> overloads = compile(tree("spark", new ArrayList<>()), Set.of("spark.health"));

        assertEquals(Set.of("health", "help"), enumValues(overloads));
    }

    @Test
    void primaryPermissionAlsoExposesTheCommandsAliases() {
        List<CommandOverloadData> overloads = compile(tree("spark", new ArrayList<>()), Set.of("spark.profiler"));

        assertEquals(Set.of("profiler", "sampler", "start", "stop", "help"), enumValues(overloads));
    }

    @Test
    void suggestsHelpAlongsidePermittedCommands() throws Exception {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.register(tree("spark", new ArrayList<>()));
        Set<String> suggestions = dispatcher.getCompletionSuggestions(
                        dispatcher.parse("spark h", source(Set.of("spark.health"))))
                .get().getList().stream().map(Suggestion::getText).collect(Collectors.toSet());

        assertEquals(Set.of("health", "help"), suggestions);
    }

    @Test
    void doesNotAdvertiseHelpWithoutSparkAccess() {
        List<CommandOverloadData> overloads = compile(tree("spark", new ArrayList<>()), Set.of());

        assertTrue(enumValues(overloads).isEmpty());
    }

    @Test
    void allSparkModulesStayWithinACompactClientCatalog() {
        List<Command> commands = new ArrayList<>();
        List<CommandModule> modules = List.<CommandModule>of(new SamplerModule(), new HealthModule(),
                new TickMonitoringModule(), new GcMonitoringModule(), new HeapAnalysisModule(), new ActivityLogModule());
        modules.forEach(module -> module.registerCommands(commands::add));
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("spark");
        SparkCommandTree.configure(root, commands, context -> 1,
                (context, suggestions) -> suggestions.buildFuture());

        List<CommandOverloadData> overloads = compile(root, Set.of("spark"));
        CommandData command = new CommandData("spark", "", Set.of(CommandData.Flag.NOT_CHEAT),
                CommandPermission.ANY, null, List.of(), overloads.toArray(CommandOverloadData[]::new));
        CommandNetworkCompiler.validateEnumDefinitions(List.of(command));

        int expected = 2;
        for (Command entry : commands) {
            expected += entry.aliases().size() * (entry.allowSubCommand() ? 2 : 1);
        }
        assertEquals(expected, overloads.size());
        assertTrue(overloads.size() <= 23, "Too many Spark usage hints: " + overloads.size());
        assertFalse(enumValues(overloads).stream().anyMatch(value -> value.startsWith("--")));
        Map<String, Set<String>> selectors = selectorDefinitions(overloads);
        Set<String> aliases = commands.stream().flatMap(entry -> entry.aliases().stream()).collect(Collectors.toSet());
        aliases.add("help");
        assertEquals(aliases, selectors.keySet());
        selectors.values().forEach(definitions -> assertEquals(1, definitions.size()));

        for (CommandOverloadData overload : overloads) {
            assertFalse(overload.isChaining());
            boolean optional = false;
            CommandParamData[] parameters = overload.getOverloads();
            assertTrue(parameters.length <= 3);
            if (parameters.length == 0) {
                continue;
            }
            assertFalse(parameters[0].isOptional(), "Command names must render as required literal keywords");
            assertEquals(Set.of(parameters[0].getName()), Objects.requireNonNull(parameters[0].getEnumData()).getValues().keySet());
            for (int index = 0; index < parameters.length; index++) {
                CommandParamData parameter = parameters[index];
                assertFalse(optional && !parameter.isOptional(), "Required parameter after an optional parameter");
                optional |= parameter.isOptional();

                if (parameter.getEnumData() != null) {
                    assertFalse(parameter.getEnumData().isSoft());
                } else {
                    assertEquals(parameters.length - 1, index);
                    assertEquals(CommandParam.TEXT, parameter.getType());
                }
            }

            if (parameters.length == 3) {
                String alias = Objects.requireNonNull(parameters[0].getEnumData()).getValues().keySet().iterator().next();
                Command owner = commands.stream().filter(entry -> entry.aliases().contains(alias)).findFirst().orElseThrow();
                Set<String> actions = owner.arguments().stream().map(Command.ArgumentInfo::subCommandName)
                        .filter(name -> !name.isBlank()).collect(Collectors.toSet());
                assertEquals(actions, Objects.requireNonNull(parameters[1].getEnumData()).getValues().keySet());
            }
        }
    }

    @Test
    void forwardsOriginalArgumentsForActionsAliasesAndUnknownSyntax() throws Exception {
        List<String> forwarded = new ArrayList<>();
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.register(tree("cloudburst:spark", forwarded));
        CommandSourceStack source = source(Set.of("spark"));

        dispatcher.execute("cloudburst:spark", source);
        dispatcher.execute("cloudburst:spark help", source);
        dispatcher.execute("cloudburst:spark profiler start --thread \"worker pool\" --timeout 60 --thread main", source);
        dispatcher.execute("cloudburst:spark profiler --timeout 60 --thread main", source);
        dispatcher.execute("cloudburst:spark sampler stop --comment \"idle profile\"", source);
        dispatcher.execute("cloudburst:spark profiler future-action --new-flag value", source);
        dispatcher.execute("cloudburst:spark future-command --new-flag value", source);

        assertEquals(List.of(
                "",
                "help",
                "profiler start --thread \"worker pool\" --timeout 60 --thread main",
                "profiler --timeout 60 --thread main",
                "sampler stop --comment \"idle profile\"",
                "profiler future-action --new-flag value",
                "future-command --new-flag value"
        ), forwarded);
    }

    @Test
    void selectsDeclaredCommandsAndActionsBeforeTheFallback() throws Exception {
        List<List<String>> parsedNodes = new ArrayList<>();
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("spark");
        SparkCommandTree.configure(root, commands(), context -> {
            parsedNodes.add(context.getNodes().stream().map(node -> node.getNode().getName()).toList());
            return 1;
        }, (context, suggestions) -> suggestions.buildFuture());

        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.register(root);
        CommandSourceStack source = source(Set.of("spark"));

        dispatcher.execute("spark health", source);
        dispatcher.execute("spark sampler stop", source);
        dispatcher.execute("spark profiler start --timeout 60", source);
        dispatcher.execute("spark future-command", source);

        assertEquals(List.of(
                List.of("spark", "health"),
                List.of("spark", "sampler", "action"),
                List.of("spark", "profiler", "action", "args"),
                List.of("spark", "args")
        ), parsedNodes);
    }

    @Test
    void completionRetainsTheCommandAndActionPrefix() throws Exception {
        List<String> prefixes = new ArrayList<>();
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("spark");
        SparkCommandTree.configure(root, commands(), context -> 1, (context, suggestions) -> {
            prefixes.add(SparkCommandTree.arguments(context));
            return suggestions.buildFuture();
        });

        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.register(root);
        String input = "spark profiler start --timeout 60 --th";
        dispatcher.getCompletionSuggestions(dispatcher.parse(input, source(Set.of("spark")))).get();

        assertFalse(prefixes.isEmpty());
        assertTrue(prefixes.stream().allMatch(prefix -> prefix.equals("profiler start --timeout 60 --th")));
    }

    @Test
    void forwardsScientificNumbersAndChoiceCasing() throws Exception {
        List<String> forwarded = new ArrayList<>();
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.register(tree("spark", forwarded));
        CommandSourceStack source = source(Set.of("spark"));

        dispatcher.execute("spark profiler start --interval 1e3 --timeout 60", source);
        dispatcher.execute("spark heapdump --compress GZIP", source);

        assertEquals(List.of("profiler start --interval 1e3 --timeout 60", "heapdump --compress GZIP"), forwarded);
    }

    private static List<CommandOverloadData> compile(LiteralArgumentBuilder<CommandSourceStack> root, Set<String> permissions) {
        CommandTreeValidator.validate(root.build());
        return CommandNetworkCompiler.compileOverloads(root.build(), source(permissions), CommandNetworkData.NOT_CHEAT);
    }

    private static Map<String, Set<String>> selectorDefinitions(List<CommandOverloadData> overloads) {
        Map<String, Set<String>> definitions = new HashMap<>();
        for (CommandOverloadData overload : overloads) {
            if (overload.getOverloads().length == 0) {
                continue;
            }
            CommandEnumData selector = Objects.requireNonNull(overload.getOverloads()[0].getEnumData());
            selector.getValues().keySet().forEach(value -> definitions.computeIfAbsent(value,
                    key -> new HashSet<>()).add(selector.getName()));
        }
        return definitions;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> tree(String label, List<String> forwarded) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(label);
        SparkCommandTree.configure(root, commands(), context -> {
            forwarded.add(SparkCommandTree.arguments(context));
            return 1;
        }, (context, suggestions) -> suggestions.buildFuture());
        return root;
    }

    private static List<Command> commands() {
        Command profiler = Command.builder().aliases("profiler", "sampler").allowSubCommand(true)
                .argumentUsage("start", "timeout", "seconds")
                .argumentUsage("start", "thread", "thread name")
                .argumentUsage("stop", "", null)
                .executor((platform, sender, response, arguments) -> {}).build();
        Command health = Command.builder().aliases("health")
                .executor((platform, sender, response, arguments) -> {}).build();
        return List.of(profiler, health);
    }

    private static Set<String> enumValues(List<CommandOverloadData> overloads) {
        return overloads.stream().flatMap(overload -> Arrays.stream(overload.getOverloads()))
                .map(CommandParamData::getEnumData).filter(Objects::nonNull)
                .flatMap(definition -> definition.getValues().keySet().stream()).collect(Collectors.toSet());
    }

    private static CommandSourceStack source(Set<String> permissions) {
        CommandSender sender = InterfaceProxy.create(CommandSender.class, (proxy, method, arguments) -> {
            if (method.getName().equals("hasPermission") && arguments != null
                    && arguments[0] instanceof String permission) {
                return permissions.contains(permission);
            }
            throw new AssertionError("Unexpected sender access: " + method);
        });
        return InterfaceProxy.create(CommandSourceStack.class, Map.of("sender", sender));
    }
}
