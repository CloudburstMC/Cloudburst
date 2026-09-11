package org.cloudburstmc.api.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.cloudburstmc.api.command.argument.CommandArgumentType;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.permission.Permission;
import org.cloudburstmc.api.plugin.PluginDescription;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Registers, discovers, and dispatches Brigadier command trees.
 *
 * <p>Command registration is available during the command registration phase. Registered trees are immutable from
 * the service's perspective; build the complete tree before passing it to
 * {@link #register(PluginDescription, LiteralCommandNode, String, Collection)}.</p>
 *
 * <pre>{@code
 * @Listener
 * public void registerCommands(CommandRegistrationEvent event) {
 *     LiteralCommandNode<CommandSourceStack> command = Commands.literal("greet")
 *             .executes(context -> {
 *                 context.getSource().sendSuccess(Component.text("Hello"));
 *                 return Command.SINGLE_SUCCESS;
 *             })
 *             .build();
 *
 *     event.getCommands().register(this.pluginDescription, command, "Sends a greeting");
 * }
 * }</pre>
 */
public interface Commands {

    /**
     * Returns the runtime-backed command argument factory.
     *
     * @return the command argument factory
     */
    CommandArguments arguments();

    /**
     * Creates a literal command node builder.
     *
     * @param name the literal name
     * @return a new builder
     */
    static LiteralArgumentBuilder<CommandSourceStack> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    /**
     * Creates an argument command node builder.
     *
     * @param name the argument name
     * @param type the parser and client presentation for the argument
     * @param <T>  the parsed argument value type
     * @return a new builder
     */
    static <T> RequiredArgumentBuilder<CommandSourceStack, T> argument(String name, CommandArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    /**
     * Creates a command requirement backed by a permission.
     *
     * <p>Attach the returned predicate to every node whose execution or suggestions require the permission.</p>
     *
     * @param permission permission node name
     * @return a requirement that tests the command sender
     */
    static Predicate<CommandSourceStack> requiresPermission(String permission) {
        String normalizedPermission = Permission.normalizeName(permission);
        return source -> source.sender().hasPermission(normalizedPermission);
    }

    /**
     * Registers a command tree for a plugin.
     *
     * <p>The unqualified root literal is used when available. A stable label prefixed with the plugin ID is always
     * registered. Unqualified aliases are used when available, while plugin-prefixed aliases are always registered.
     * Every alternate label resolves to the same command tree.</p>
     *
     * @param plugin      metadata for the owning plugin
     * @param command     the root command node
     * @param description description shown in command help and command UI
     * @param aliases     root aliases to register
     * @return immutable set of labels that were registered, including namespaced labels and aliases
     */
    Set<String> register(PluginDescription plugin, LiteralCommandNode<CommandSourceStack> command,
                         String description, Collection<String> aliases);

    /**
     * Registers a command tree for a plugin without aliases.
     *
     * @param plugin      metadata for the owning plugin
     * @param command     the root command node
     * @param description description shown in command help and command UI
     * @return labels that were registered
     */
    default Set<String> register(PluginDescription plugin, LiteralCommandNode<CommandSourceStack> command,
                                 String description) {
        return this.register(plugin, command, description, List.of());
    }

    /**
     * Registers a command tree for a plugin without a description.
     *
     * @param plugin  metadata for the owning plugin
     * @param command the root command node
     * @param aliases root aliases to register
     * @return labels that were registered
     */
    default Set<String> register(PluginDescription plugin, LiteralCommandNode<CommandSourceStack> command,
                                 Collection<String> aliases) {
        return this.register(plugin, command, "", aliases);
    }

    /**
     * Registers a command tree without a description or aliases.
     *
     * @param plugin  metadata for the owning plugin
     * @param command the root command node
     * @return labels that were registered
     */
    default Set<String> register(PluginDescription plugin, LiteralCommandNode<CommandSourceStack> command) {
        return this.register(plugin, command, "", List.of());
    }

    /**
     * Dispatches a command line.
     *
     * @param sender      the command sender
     * @param commandLine the command line, with or without a leading slash
     * @return {@code true} if a command was found
     */
    boolean dispatch(CommandSender sender, String commandLine);

    /**
     * Returns every root command label visible to a sender, including aliases and namespaced labels.
     *
     * @param sender the sender
     * @return immutable set of visible labels
     */
    Set<String> labels(CommandSender sender);

}
