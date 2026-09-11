package org.cloudburstmc.server.registry;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.SuggestionContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.plugin.PluginDescription;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.CloudCommandSourceStack;
import org.cloudburstmc.server.command.CommandTreeValidator;
import org.cloudburstmc.server.command.ServerCommand;
import org.cloudburstmc.server.command.argument.CloudCommandArguments;
import org.cloudburstmc.server.command.defaults.*;
import org.cloudburstmc.server.command.network.CommandNetworkData;
import org.cloudburstmc.server.utils.Utils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Owns the server command tree and the metadata used to expose that tree to command senders.
 */
@Log4j2
public class CloudCommandRegistry implements Commands {
    private static final Pattern ROOT_LABEL_PATTERN = Pattern.compile("^[a-z0-9_\\-/.]+$");
    private static final Pattern ALIAS_LABEL_PATTERN = Pattern.compile("^[a-z0-9_\\-/.?]+$");
    private final CommandArguments arguments = new CloudCommandArguments();
    private final CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
    private final Set<String> referencedPermissions = new LinkedHashSet<>();
    private Map<String, RegisteredCommand> registeredCommands = new LinkedHashMap<>();

    private volatile boolean closed;

    @Override
    public CommandArguments arguments() {
        return this.arguments;
    }

    /**
     * Registers the commands provided by the server.
     *
     * @throws RegistryException if registration is closed or a command label conflicts with an existing registration
     */
    public void registerBuiltIns() {
        this.registerGameplayCommands();
        this.registerAdministrationCommands();
    }

    @Override
    public synchronized Set<String> register(
            PluginDescription plugin,
            LiteralCommandNode<CommandSourceStack> command,
            String description,
            Collection<String> aliases
    ) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(aliases, "aliases");
        checkClosed();

        CommandTreeValidator.validate(command);

        String label = command.getLiteral().toLowerCase(Locale.ROOT);
        Preconditions.checkArgument(ROOT_LABEL_PATTERN.matcher(label).matches(), "Invalid command name: %s", label);
        if (!command.getLiteral().equals(label)) {
            command = copyLiteral(label, command);
        }

        String prefixedLabel = prefixedLabel(plugin, label, ROOT_LABEL_PATTERN, "name");
        Set<String> normalizedAliases = new LinkedHashSet<>();
        Set<String> qualifiedLabels = new LinkedHashSet<>();
        qualifiedLabels.add(prefixedLabel);

        for (String alias : aliases) {
            String normalizedAlias = Objects.requireNonNull(alias, "alias").toLowerCase(Locale.ROOT);
            Preconditions.checkArgument(ALIAS_LABEL_PATTERN.matcher(normalizedAlias).matches(),
                    "Invalid command alias: %s",
                    normalizedAlias);
            Preconditions.checkArgument(!normalizedAlias.equals(label),
                    "Command alias duplicates command name: %s",
                    normalizedAlias);
            Preconditions.checkArgument(normalizedAliases.add(normalizedAlias),
                    "Duplicate command alias: %s", normalizedAlias);
            qualifiedLabels.add(prefixedLabel(plugin, normalizedAlias, ALIAS_LABEL_PATTERN, "alias"));
        }

        for (String qualifiedLabel : qualifiedLabels) {
            if (isRegistered(qualifiedLabel)) {
                throw new RegistryException("Command " + qualifiedLabel + " already registered.");
            }
        }

        String primaryLabel = label;
        if (isRegistered(primaryLabel)) {
            primaryLabel = prefixedLabel;
            command = copyLiteral(primaryLabel, command);
        }

        Map<String, LiteralCommandNode<CommandSourceStack>> nodes = new LinkedHashMap<>();
        nodes.put(primaryLabel, command);

        if (!primaryLabel.equals(prefixedLabel)) {
            nodes.put(prefixedLabel, copyLiteral(prefixedLabel, command));
        }

        for (String normalizedAlias : normalizedAliases) {
            String prefixedAlias = prefixedLabel(plugin, normalizedAlias, ALIAS_LABEL_PATTERN, "alias");
            if (!isRegistered(normalizedAlias)) {
                nodes.put(normalizedAlias, copyLiteral(normalizedAlias, command));
            }

            if (!prefixedAlias.equals(primaryLabel) && !prefixedAlias.equals(prefixedLabel)) {
                nodes.put(prefixedAlias, copyLiteral(prefixedAlias, command));
            }
        }

        for (Map.Entry<String, LiteralCommandNode<CommandSourceStack>> entry : nodes.entrySet()) {
            registerNode(entry.getKey(), primaryLabel, entry.getValue(), description, CommandNetworkData.ANY_NOT_CHEAT);
        }

        return Collections.unmodifiableSet(new LinkedHashSet<>(nodes.keySet()));
    }

    /**
     * Validates command permissions and prevents further registration.
     *
     * @throws RegistryException if registration is already closed or a referenced permission is not registered
     */
    public void close() throws RegistryException {
        checkClosed();
        validatePermissions(this.referencedPermissions);

        this.closed = true;
        this.registeredCommands = ImmutableMap.copyOf(this.registeredCommands);
    }

    /**
     * Returns primary command labels and descriptions available to a sender.
     *
     * @param sender sender used to evaluate command requirements
     * @return immutable map from primary labels to description keys
     */
    public Map<String, String> visibleCommands(CommandSender sender) {
        Objects.requireNonNull(sender, "sender");
        CommandSourceStack source = CloudCommandSourceStack.from(sender);
        Map<String, String> commands = new LinkedHashMap<>();

        for (CommandNode<CommandSourceStack> child : this.dispatcher.getRoot().getChildren()) {
            RegisteredCommand registeredCommand = this.registeredCommands.get(child.getName());
            if (registeredCommand == null || !registeredCommand.primary()) {
                continue;
            }

            if (child.canUse(source)) {
                commands.put(child.getName(), registeredCommand.description());
            }
        }

        return Collections.unmodifiableMap(commands);
    }

    /**
     * Returns the description key associated with a command label.
     *
     * @param label command label
     * @return description key, or an empty string when the label is not registered
     */
    public String commandDescription(String label) {
        Objects.requireNonNull(label, "label");
        RegisteredCommand registeredCommand = this.registeredCommands.get(label.toLowerCase(Locale.ROOT));
        return registeredCommand == null ? "" : registeredCommand.description();
    }

    /**
     * Returns command usage lines generated from the nodes available to a source.
     *
     * @param label  command root label
     * @param source source used to evaluate node requirements
     * @return immutable usage lines, or an empty list when the root is unavailable
     */
    public List<String> usages(String label, CommandSourceStack source) {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(source, "source");

        String normalizedLabel = label.toLowerCase(Locale.ROOT);
        CommandNode<CommandSourceStack> root = this.dispatcher.getRoot().getChild(normalizedLabel);
        if (root == null || !root.canUse(source)) {
            return List.of();
        }

        LinkedHashSet<String> usages = new LinkedHashSet<>();
        if (root.getCommand() != null) {
            usages.add("/" + normalizedLabel);
        }

        for (String usage : this.dispatcher.getSmartUsage(root, source).values()) {
            usages.add("/" + normalizedLabel + " " + usage);
        }

        return List.copyOf(usages);
    }

    /**
     * Returns whether a command root is available to a source.
     *
     * @param label  command root label
     * @param source source used to evaluate the root requirement
     * @return whether the root exists and is available
     */
    public boolean canUse(String label, CommandSourceStack source) {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(source, "source");

        CommandNode<CommandSourceStack> root = this.dispatcher.getRoot().getChild(label.toLowerCase(Locale.ROOT));
        return root != null && root.canUse(source);
    }

    /**
     * Computes completion suggestions from the registered command tree.
     *
     * @param commandLine command line without a leading slash
     * @param cursor      cursor position in the command line
     * @param source      source used to evaluate requirements and suggestion providers
     * @return future containing completion suggestions
     */
    public CompletableFuture<Suggestions> suggestions(String commandLine, int cursor, CommandSourceStack source) {
        Objects.requireNonNull(commandLine, "commandLine");
        Objects.requireNonNull(source, "source");
        if (cursor < 0 || cursor > commandLine.length()) {
            throw new IllegalArgumentException("Cursor is outside the command line: " + cursor);
        }

        return availableSuggestions(this.dispatcher.parse(commandLine, source), cursor);
    }

    @Override
    public Set<String> labels(CommandSender sender) {
        Objects.requireNonNull(sender, "sender");
        CommandSourceStack source = CloudCommandSourceStack.from(sender);
        List<String> labels = new ArrayList<>();

        for (CommandNode<CommandSourceStack> child : this.dispatcher.getRoot().getChildren()) {
            if (this.registeredCommands.containsKey(child.getName()) && child.canUse(source)) {
                labels.add(child.getName());
            }
        }

        labels.sort(String.CASE_INSENSITIVE_ORDER);
        return Collections.unmodifiableSet(new LinkedHashSet<>(labels));
    }

    /**
     * Returns a snapshot of the registered command roots.
     *
     * @return immutable collection of root nodes
     */
    public Collection<CommandNode<CommandSourceStack>> commandRoots() {
        return List.copyOf(this.dispatcher.getRoot().getChildren());
    }

    /**
     * Returns whether a label is the primary label for its command.
     *
     * @param label command label
     * @return whether the label is registered as primary
     */
    public boolean isPrimaryLabel(String label) {
        String normalizedLabel = Objects.requireNonNull(label, "label").toLowerCase(Locale.ROOT);
        RegisteredCommand registeredCommand = this.registeredCommands.get(normalizedLabel);
        return registeredCommand != null && registeredCommand.primary();
    }

    /**
     * Returns the metadata used to advertise a command to clients.
     *
     * @param label command label
     * @return advertisement metadata, or an empty value for unknown and server-only commands
     */
    public Optional<CommandNetworkData> commandNetworkData(String label) {
        Objects.requireNonNull(label, "label");
        RegisteredCommand registeredCommand = this.registeredCommands.get(label.toLowerCase(Locale.ROOT));
        if (registeredCommand instanceof AdvertisedRegistration advertisedRegistration) {
            return Optional.of(advertisedRegistration.networkData());
        }

        return Optional.empty();
    }

    /**
     * Returns the alternate labels registered for a primary command label.
     *
     * @param primaryLabel primary command label
     * @return immutable list of aliases and qualified labels
     */
    public List<String> aliasesFor(String primaryLabel) {
        String normalizedPrimaryLabel = Objects.requireNonNull(primaryLabel, "primaryLabel").toLowerCase(Locale.ROOT);
        List<String> aliases = new ArrayList<>();

        for (RegisteredCommand entry : this.registeredCommands.values()) {
            if (entry.primaryLabel().equals(normalizedPrimaryLabel) && !entry.primary()) {
                aliases.add(entry.label());
            }
        }

        return List.copyOf(aliases);
    }

    @Override
    public boolean dispatch(CommandSender sender, String commandLine) {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(commandLine, "commandLine");

        String trimmedCommand = commandLine.trim();
        if (trimmedCommand.startsWith("/")) {
            trimmedCommand = trimmedCommand.substring(1);
        }

        if (trimmedCommand.isEmpty()) {
            return false;
        }

        String[] parts = trimmedCommand.split(" ", 2);
        String label = parts[0].toLowerCase(Locale.ROOT);
        if (this.dispatcher.getRoot().getChild(label) == null) {
            return false;
        }

        String dispatchedCommand = parts.length == 1 ? label : label + " " + parts[1];

        try {
            this.dispatcher.execute(dispatchedCommand, CloudCommandSourceStack.from(sender));
        } catch (CommandSyntaxException e) {
            sender.sendMessage(Component.text(e.getMessage()).color(NamedTextColor.RED));
        } catch (Exception e) {
            sender.sendMessage(Component.translatable("commands.generic.exception").color(NamedTextColor.RED));
            log.error(CloudServer.getInstance().getLanguage().translate("cloudburst.command.exception", commandLine, label, Utils.getExceptionMessage(e)));
        }

        return true;
    }

    private void registerGameplayCommands() {
        this.registerInternal(new DefaultGamemodeCommand());
        this.registerInternal(new DeopCommand());
        this.registerInternal(new DifficultyCommand());
        this.registerInternal(new EffectCommand());
        this.registerInternal(new EnchantCommand());
        this.registerInternal(new GamemodeCommand());
        this.registerInternal(new GameruleCommand());
        this.registerInternal(new GiveCommand());
        this.registerInternal(new KickCommand());
        this.registerInternal(new KillCommand());
        this.registerInternal(new ListCommand());
        this.registerInternal(new MeCommand());
        this.registerInternal(new OpCommand());
        this.registerInternal(new ParticleCommand());
        this.registerInternal(new SayCommand());
        this.registerInternal(new SeedCommand());
        this.registerInternal(new SetBlockCommand());
        this.registerInternal(new SetWorldSpawnCommand());
        this.registerInternal(new SpawnpointCommand());
        this.registerInternal(new TeleportCommand());
        this.registerInternal(new TellCommand());
        this.registerInternal(new TimeCommand());
        this.registerInternal(new TitleCommand());
        this.registerInternal(new WeatherCommand());
        this.registerInternal(new WhitelistCommand());
        this.registerInternal(new XpCommand());
    }

    private void registerAdministrationCommands() {
        this.registerInternal(new DebugPasteCommand());
        this.registerInternal(new GarbageCollectorCommand());
        this.registerInternal(new HelpCommand());
        this.registerInternal(new PluginsCommand());
        this.registerInternal(new SaveCommand());
        this.registerInternal(new SaveOffCommand());
        this.registerInternal(new SaveOnCommand());
        this.registerInternal(new StatusCommand());
        this.registerInternal(new StopCommand());
        this.registerInternal(new TimingsCommand());
        this.registerInternal(new VersionCommand());
    }

    private synchronized void registerInternal(ServerCommand command) {
        Objects.requireNonNull(command, "command");
        checkClosed();

        String label = command.getName().toLowerCase(Locale.ROOT);
        Preconditions.checkArgument(ROOT_LABEL_PATTERN.matcher(label).matches(), "Invalid command name: %s", label);
        if (isRegistered(label)) {
            throw new RegistryException("Command " + label + " already registered.");
        }

        Set<String> aliases = new LinkedHashSet<>();
        for (String alias : command.getAliases()) {
            String normalizedAlias = Objects.requireNonNull(alias, "alias").toLowerCase(Locale.ROOT);
            Preconditions.checkArgument(ALIAS_LABEL_PATTERN.matcher(normalizedAlias).matches(),
                    "Invalid alias name '%s' for command '%s'",
                    normalizedAlias,
                    label);
            Preconditions.checkArgument(!normalizedAlias.equals(label),
                    "Command alias duplicates command name: %s",
                    normalizedAlias);
            Preconditions.checkArgument(aliases.add(normalizedAlias), "Duplicate command alias: %s", normalizedAlias);

            if (isRegistered(normalizedAlias)) {
                throw new RegistryException("Command alias " + normalizedAlias + " is already registered.");
            }
        }

        Map<String, LiteralCommandNode<CommandSourceStack>> nodes = new LinkedHashMap<>();
        LiteralCommandNode<CommandSourceStack> commandNode = commandNode(label, command);

        nodes.put(label, commandNode);
        for (String alias : aliases) {
            nodes.put(alias, copyLiteral(alias, commandNode));
        }

        for (LiteralCommandNode<CommandSourceStack> node : nodes.values()) {
            CommandTreeValidator.validate(node);
        }

        for (Map.Entry<String, LiteralCommandNode<CommandSourceStack>> entry : nodes.entrySet()) {
            String registeredLabel = entry.getKey();
            if (command instanceof AdvertisedCommand advertisedCommand) {
                registerNode(registeredLabel, label, entry.getValue(), command.getDescription(),
                        advertisedCommand.networkData());
            } else {
                registerNode(entry.getValue(),
                        new ServerOnlyRegistration(registeredLabel, label, command.getDescription()));
            }
        }

        this.referencedPermissions.addAll(command.getAccessPermissions());
    }

    private LiteralCommandNode<CommandSourceStack> commandNode(String label, ServerCommand command) {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(label.toLowerCase(Locale.ROOT))
                .requires(source -> hasAnyPermission(source.sender(), command.getAccessPermissions()));

        command.configure(builder, label, this.arguments);
        return builder.build();
    }

    private void registerNode(
            String label,
            String primaryLabel,
            LiteralCommandNode<CommandSourceStack> node,
            String description,
            CommandNetworkData networkData
    ) {
        Objects.requireNonNull(networkData, "networkData");
        registerNode(node, new AdvertisedRegistration(label, primaryLabel, description, networkData));
    }

    private void registerNode(
            LiteralCommandNode<CommandSourceStack> node,
            RegisteredCommand command
    ) {
        if (isRegistered(command.label())) {
            throw new RegistryException("Command " + command.label() + " already registered.");
        }

        this.dispatcher.getRoot().addChild(node);
        this.registeredCommands.put(command.label(), command);
    }

    private boolean isRegistered(String label) {
        return this.registeredCommands.containsKey(label) || this.dispatcher.getRoot().getChild(label) != null;
    }

    private void checkClosed() {
        if (this.closed) {
            throw new RegistryException("Registration is closed");
        }
    }

    private static String prefixedLabel(PluginDescription plugin, String label, Pattern labelPattern, String labelType) {
        String pluginId = plugin.getId().toLowerCase(Locale.ROOT);
        Preconditions.checkArgument(ROOT_LABEL_PATTERN.matcher(pluginId).matches(),
                "Invalid plugin command namespace: %s",
                pluginId);
        Preconditions.checkArgument(labelPattern.matcher(label).matches(),
                "Invalid command %s: %s",
                labelType,
                label);
        return pluginId + ":" + label;
    }

    private static boolean hasAnyPermission(CommandSender sender, Collection<String> permissions) {
        if (permissions.isEmpty()) {
            return true;
        }

        for (String permission : permissions) {
            if (sender.hasPermission(permission)) {
                return true;
            }
        }

        return false;
    }

    private static void validatePermissions(Collection<String> permissions) {
        for (String permission : permissions) {
            if (CloudServer.getInstance().getPermissionManager().getPermission(permission).isEmpty()) {
                throw new RegistryException("Command references an unregistered permission: " + permission);
            }
        }
    }

    private static CompletableFuture<Suggestions> availableSuggestions(ParseResults<CommandSourceStack> parse, int cursor) {
        SuggestionContext<CommandSourceStack> suggestionContext = parse.getContext().findSuggestionContext(cursor);
        String input = parse.getReader().getString();
        String truncatedInput = input.substring(0, cursor);
        String normalizedInput = truncatedInput.toLowerCase(Locale.ROOT);
        int start = Math.min(suggestionContext.startPos, cursor);
        CommandContext<CommandSourceStack> context = parse.getContext().build(truncatedInput);

        List<CompletableFuture<Suggestions>> futures = new ArrayList<>();
        for (CommandNode<CommandSourceStack> node : suggestionContext.parent.getChildren()) {
            if (!node.canUse(context.getSource())) {
                continue;
            }

            try {
                futures.add(node.listSuggestions(context, new SuggestionsBuilder(truncatedInput, normalizedInput, start)));
            } catch (CommandSyntaxException ignored) {
            }
        }

        if (futures.isEmpty()) {
            return Suggestions.empty();
        }

        CompletableFuture<?>[] pending = futures.toArray(CompletableFuture<?>[]::new);
        return CompletableFuture.allOf(pending).thenApply(ignored -> Suggestions.merge(input, futures.stream()
                .map(CompletableFuture::join)
                .toList()));
    }

    private static LiteralCommandNode<CommandSourceStack> copyLiteral(String label, LiteralCommandNode<CommandSourceStack> source) {
        return copyLiteral(label, source, source.getRequirement());
    }

    private static LiteralCommandNode<CommandSourceStack> copyLiteral(
            String label, LiteralCommandNode<CommandSourceStack> source,
            Predicate<CommandSourceStack> requirement) {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(label).requires(requirement);
        if (source.getCommand() != null) {
            builder.executes(source.getCommand());
        }

        if (source.getRedirect() != null) {
            builder.forward(source.getRedirect(), source.getRedirectModifier(), source.isFork());
        } else {
            copyChildren(builder, source);
        }

        return builder.build();
    }

    private static void copyChildren(ArgumentBuilder<CommandSourceStack, ?> target, CommandNode<CommandSourceStack> source) {
        for (CommandNode<CommandSourceStack> child : source.getChildren()) {
            target.then(copyNode(child));
        }
    }

    private static CommandNode<CommandSourceStack> copyNode(CommandNode<CommandSourceStack> source) {
        ArgumentBuilder<CommandSourceStack, ?> builder = source.createBuilder();
        copyChildren(builder, source);
        return builder.build();
    }

    private sealed interface RegisteredCommand permits AdvertisedRegistration, ServerOnlyRegistration {
        String label();

        String primaryLabel();

        String description();

        default boolean primary() {
            return this.label().equals(this.primaryLabel());
        }
    }

    private record AdvertisedRegistration(
            String label,
            String primaryLabel,
            String description,
            CommandNetworkData networkData
    ) implements RegisteredCommand {
    }

    private record ServerOnlyRegistration(
            String label,
            String primaryLabel,
            String description
    ) implements RegisteredCommand {
    }
}
