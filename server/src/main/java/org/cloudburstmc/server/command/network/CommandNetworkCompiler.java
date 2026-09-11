package org.cloudburstmc.server.command.network;

import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.argument.CommandArgumentType;
import org.cloudburstmc.protocol.bedrock.data.command.*;
import org.cloudburstmc.protocol.bedrock.packet.AvailableCommandsPacket;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.CloudCommandSourceStack;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudCommandRegistry;

import java.util.*;

@UtilityClass
public class CommandNetworkCompiler {
    private static final Set<CommandEnumConstraint> ALIAS_CONSTRAINTS =
            Collections.unmodifiableSet(EnumSet.of(CommandEnumConstraint.ALLOW_ALIASES));

    public static AvailableCommandsPacket compile(CloudCommandRegistry registry, CloudPlayer player) {
        AvailableCommandsPacket packet = new AvailableCommandsPacket();
        List<CommandData> commands = packet.getCommands();
        CommandSourceStack source = CloudCommandSourceStack.from(player);

        for (CommandNode<CommandSourceStack> child : registry.commandRoots()) {
            if (!(child instanceof LiteralCommandNode<CommandSourceStack> root)) {
                continue;
            }

            if (!registry.isPrimaryLabel(root.getLiteral())) {
                continue;
            }

            if (!child.canUse(source)) {
                continue;
            }

            Optional<CommandNetworkData> networkData = registry.commandNetworkData(root.getLiteral());
            if (networkData.isEmpty()) {
                continue;
            }

            CommandData command = toNetwork(registry, root, source, networkData.get());
            if (command != null) {
                commands.add(command);
            }
        }

        validateEnumDefinitions(commands);
        return packet;
    }

    public static void validateEnumDefinitions(Collection<CommandData> commands) {
        Map<String, CommandEnumData> definitions = new HashMap<>();
        for (CommandData command : commands) {
            validateEnumDefinition(definitions, command.getAliases());
            for (CommandOverloadData overload : command.getOverloads()) {
                for (CommandParamData parameter : overload.getOverloads()) {
                    validateEnumDefinition(definitions, parameter.getEnumData());
                }
            }
        }
    }

    private static void validateEnumDefinition(Map<String, CommandEnumData> definitions,
                                               CommandEnumData definition) {
        if (definition == null) {
            return;
        }

        CommandEnumData existing = definitions.putIfAbsent(definition.getName(), definition);
        if (existing != null && !existing.equals(definition)) {
            throw new IllegalStateException("Conflicting command enum definition: " + definition.getName());
        }
    }

    private static CommandData toNetwork(CloudCommandRegistry registry, LiteralCommandNode<CommandSourceStack> root,
                                         CommandSourceStack source, CommandNetworkData networkData) {
        List<CommandOverloadData> overloads = compileOverloads(root, source);
        if (overloads.isEmpty()) {
            return null;
        }

        return new CommandData(root.getLiteral(), commandDescription(registry.commandDescription(root.getLiteral())),
                networkData.flags(), networkData.permission(),
                commandAliases(root.getLiteral(), registry.aliasesFor(root.getLiteral())),
                Collections.emptyList(), overloads.toArray(CommandOverloadData[]::new));
    }

    static List<CommandOverloadData> compileOverloads(CommandNode<CommandSourceStack> root, CommandSourceStack source) {
        List<ExecutablePath> paths = new ArrayList<>();
        collectExecutablePaths(root, source, new ArrayList<>(), new ArrayList<>(), paths,
                Collections.newSetFromMap(new IdentityHashMap<>()));
        return compilePaths(paths);
    }

    private static String commandDescription(String descriptionKey) {
        String description = CloudServer.getInstance().getLanguage().translate(descriptionKey);
        return description.length() > 950 ? description.substring(0, 947) + "..." : description;
    }

    private static CommandEnumData commandAliases(String root, Collection<String> aliases) {
        if (aliases.isEmpty()) {
            return null;
        }

        Map<String, Set<CommandEnumConstraint>> enumValues = new LinkedHashMap<>();
        enumValues.put(root, ALIAS_CONSTRAINTS);
        for (String alias : aliases) {
            enumValues.put(alias, ALIAS_CONSTRAINTS);
        }

        return new CommandEnumData(root + "CommandAliases", enumValues, false);
    }

    private static void collectExecutablePaths(CommandNode<CommandSourceStack> node, CommandSourceStack source,
                                               List<CommandNode<CommandSourceStack>> nodes,
                                               List<CommandParamData> path,
                                               List<ExecutablePath> paths,
                                               Set<CommandNode<CommandSourceStack>> visiting) {
        if (!node.canUse(source) || !visiting.add(node)) {
            return;
        }

        if (node.getCommand() != null) {
            paths.add(new ExecutablePath(List.copyOf(nodes), List.copyOf(path)));
        }

        if (node.getRedirect() != null) {
            collectExecutablePaths(node.getRedirect(), source, nodes, path, paths, visiting);
        }

        for (CommandNode<CommandSourceStack> child : node.getChildren()) {
            if (!child.canUse(source)) {
                continue;
            }
            nodes.add(child);
            path.add(toNetworkParameter(child));
            collectExecutablePaths(child, source, nodes, path, paths, visiting);
            path.removeLast();
            nodes.removeLast();
        }
        visiting.remove(node);
    }

    private static List<CommandOverloadData> compilePaths(List<ExecutablePath> paths) {
        List<ExecutablePath> remaining = new ArrayList<>(paths);
        List<CommandOverloadData> overloads = new ArrayList<>();

        while (!remaining.isEmpty()) {
            ExecutablePath path = longestPath(remaining);
            int optionalStart = path.nodes().size();
            while (optionalStart > 0 && containsPrefix(paths, path, optionalStart - 1)) {
                optionalStart--;
            }

            CommandParamData[] parameters = new CommandParamData[path.parameters().size()];
            for (int index = 0; index < parameters.length; index++) {
                parameters[index] = copyParameter(path.parameters().get(index), index >= optionalStart);
            }
            overloads.add(new CommandOverloadData(false, parameters));

            int firstCoveredLength = optionalStart;
            remaining.removeIf(candidate -> candidate.nodes().size() >= firstCoveredLength
                    && isPrefix(candidate, path));
        }

        return List.copyOf(overloads);
    }

    private static ExecutablePath longestPath(List<ExecutablePath> paths) {
        ExecutablePath longest = paths.getFirst();
        for (ExecutablePath path : paths) {
            if (path.nodes().size() > longest.nodes().size()) {
                longest = path;
            }
        }
        return longest;
    }

    private static boolean containsPrefix(List<ExecutablePath> paths, ExecutablePath path, int length) {
        for (ExecutablePath candidate : paths) {
            if (candidate.nodes().size() == length && isPrefix(candidate, path)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPrefix(ExecutablePath prefix, ExecutablePath path) {
        if (prefix.nodes().size() > path.nodes().size()) {
            return false;
        }
        for (int index = 0; index < prefix.nodes().size(); index++) {
            if (prefix.nodes().get(index) != path.nodes().get(index)) {
                return false;
            }
        }
        return true;
    }

    private static CommandParamData copyParameter(CommandParamData source, boolean optional) {
        CommandParamData copy = new CommandParamData();
        copy.setName(source.getName());
        copy.setOptional(optional);
        copy.setEnumData(source.getEnumData());
        copy.setType(source.getType());
        copy.setPostfix(source.getPostfix());
        copy.getOptions().addAll(source.getOptions());
        return copy;
    }

    private static CommandParamData toNetworkParameter(CommandNode<CommandSourceStack> node) {
        CommandParamData data = new CommandParamData();
        data.setName(node.getName());

        if (node instanceof LiteralCommandNode<CommandSourceStack> literal) {
            data.setEnumData(new CommandEnumData(literal.getLiteral() + "Enum",
                    Collections.singletonMap(literal.getLiteral(), Collections.emptySet()), false));
            return data;
        }

        if (node instanceof ArgumentCommandNode<CommandSourceStack, ?> argument
                && argument.getType() instanceof CommandArgumentType<?> commandArgument) {
            return toNetworkParameter(argument, commandArgument);
        }

        throw new IllegalArgumentException("Command node has no client representation: " + node.getName());
    }

    private static CommandParamData toNetworkParameter(ArgumentCommandNode<CommandSourceStack, ?> node, CommandArgumentType<?> argument) {
        CommandParamData data = new CommandParamData();
        String displayName = argument.getDisplayName();
        data.setName(displayName == null || displayName.isBlank() ? node.getName() : displayName);

        switch (argument.getKind()) {
            case STRING -> data.setType(CommandParam.STRING);
            case TEXT -> data.setType(CommandParam.TEXT);
            case MESSAGE -> data.setType(CommandParam.MESSAGE_ROOT);
            case INTEGER -> data.setType(CommandParam.INT);
            case FLOAT -> data.setType(CommandParam.FLOAT);
            case ROTATION -> data.setType(CommandParam.R_VALUE);
            case TARGET -> data.setType(CommandParam.TARGET);
            case POSITION -> data.setType(CommandParam.POSITION);
            case BLOCK_POSITION -> data.setType(CommandParam.BLOCK_POSITION);
            case JSON -> data.setType(CommandParam.JSON);
            case POSTFIX -> data.setPostfix(argument.getPostfix());
            case ITEM, BLOCK, ENCHANTMENT, EFFECT, ENTITY_TYPE, PARTICLE -> {
                data.setEnumData(new CommandEnumData(CommandRegistryArgumentNetworkMapper.enumName(argument),
                        CommandRegistryArgumentNetworkMapper.enumValues(argument), false));
                if (CommandRegistryArgumentNetworkMapper.requiresSemanticConstraint(argument.getKind())) {
                    data.getOptions().add(CommandParamOption.HAS_SEMANTIC_CONSTRAINT);
                }
            }
            case FIXED_ENUM -> data.setEnumData(new CommandEnumData(enumName(node, argument),
                    enumValues(argument.getValues()), false));
        }

        return data;
    }

    private static String enumName(ArgumentCommandNode<CommandSourceStack, ?> node, CommandArgumentType<?> argument) {
        String enumName = argument.getEnumName();
        return enumName == null || enumName.isBlank() ? node.getName() + "Enum" : enumName;
    }

    private static Map<String, Set<CommandEnumConstraint>> enumValues(Collection<String> values) {
        LinkedHashMap<String, Set<CommandEnumConstraint>> enumValues = new LinkedHashMap<>();
        for (String value : values) {
            enumValues.put(value, Collections.emptySet());
        }
        return enumValues;
    }

    private record ExecutablePath(
            List<CommandNode<CommandSourceStack>> nodes,
            List<CommandParamData> parameters
    ) {
    }
}
