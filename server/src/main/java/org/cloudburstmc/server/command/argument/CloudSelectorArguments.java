package org.cloudburstmc.server.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.command.CommandPermissions;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.argument.CommandArgumentKind;
import org.cloudburstmc.api.command.argument.CommandArgumentType;
import org.cloudburstmc.api.command.argument.resolver.EntitySelectorResolver;
import org.cloudburstmc.api.command.argument.resolver.PlayerSelectorResolver;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Identifier;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Creates target selector arguments backed by the server's entity model.
 */
public class CloudSelectorArguments {
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_ENTITY =
            new DynamicCommandExceptionType(value -> new LiteralMessage("Can't find entity " + value));
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_PLAYER =
            new DynamicCommandExceptionType(value -> new LiteralMessage("Can't find player " + value));
    private static final SimpleCommandExceptionType ERROR_NOT_SINGLE_ENTITY =
            new SimpleCommandExceptionType(new LiteralMessage("Selector matched more than one entity"));
    private static final SimpleCommandExceptionType ERROR_NOT_SINGLE_PLAYER =
            new SimpleCommandExceptionType(new LiteralMessage("Selector matched more than one player"));

    public CommandArgumentType<PlayerSelectorResolver> player(@Nullable String displayName) {
        return new CloudSelectorArgumentType<>(displayName,
                value -> new CloudPlayerSelectorResolver(CloudTargetSelectorParser.parse(value, true), true),
                CloudSelectorArguments::suggestSinglePlayer);
    }

    public CommandArgumentType<PlayerSelectorResolver> players(@Nullable String displayName) {
        return new CloudSelectorArgumentType<>(displayName,
                value -> new CloudPlayerSelectorResolver(CloudTargetSelectorParser.parse(value, true), false),
                CloudSelectorArguments::suggestPlayers);
    }

    public CommandArgumentType<EntitySelectorResolver> entity(@Nullable String displayName) {
        return new CloudSelectorArgumentType<>(displayName,
                value -> new CloudEntitySelectorResolver(CloudTargetSelectorParser.parse(value, false), true),
                CloudSelectorArguments::suggestSingleEntity);
    }

    public CommandArgumentType<EntitySelectorResolver> entities(@Nullable String displayName) {
        return new CloudSelectorArgumentType<>(displayName,
                value -> new CloudEntitySelectorResolver(CloudTargetSelectorParser.parse(value, false), false),
                CloudSelectorArguments::suggestEntities);
    }

    private static CompletableFuture<Suggestions> suggestPlayers(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return suggestPlayerValues(context, builder, List.of("@a", "@p", "@r", "@s"));
    }

    private static CompletableFuture<Suggestions> suggestSinglePlayer(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return suggestPlayerValues(context, builder, List.of("@p", "@r", "@s"));
    }

    private static CompletableFuture<Suggestions> suggestPlayerValues(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder,
            Collection<String> selectors
    ) {
        boolean selectorsAllowed = context.getSource().sender().hasPermission(CommandPermissions.TARGET_SELECTORS);
        if (selectorsAllowed && isSelectorOption(builder.getRemaining())) {
            return suggestSelectorOptions(builder);
        }

        Set<String> values = Stream.concat(
                selectorsAllowed ? selectors.stream() : Stream.empty(),
                context.getSource().sender().getServer().getOnlinePlayers().values().stream().map(Entity::getName)
        ).collect(Collectors.toCollection(TreeSet::new));
        return suggest(values, builder);
    }

    private static CompletableFuture<Suggestions> suggestEntities(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return suggestEntityValues(context, builder, List.of("@a", "@e", "@n", "@p", "@r", "@s"));
    }

    private static CompletableFuture<Suggestions> suggestSingleEntity(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return suggestEntityValues(context, builder, List.of("@n", "@p", "@r", "@s"));
    }

    private static CompletableFuture<Suggestions> suggestEntityValues(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder,
            Collection<String> selectors
    ) {
        boolean selectorsAllowed = context.getSource().sender().hasPermission(CommandPermissions.TARGET_SELECTORS);
        if (selectorsAllowed && isSelectorOption(builder.getRemaining())) {
            return suggestSelectorOptions(builder);
        }

        Set<String> values = Stream.concat(
                selectorsAllowed ? selectors.stream() : Stream.empty(),
                context.getSource().sender().getServer().getLevels().stream()
                        .flatMap(level -> level.getEntities().stream())
                        .map(Entity::getName)
        ).collect(Collectors.toCollection(TreeSet::new));
        return suggest(values, builder);
    }

    private static boolean isSelectorOption(String remaining) {
        return remaining.startsWith("@") && remaining.contains("[");
    }

    private static CompletableFuture<Suggestions> suggest(Collection<String> values, SuggestionsBuilder builder) {
        String remaining = builder.getRemainingLowerCase();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(value);
            }
        }
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestSelectorOptions(SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();
        int optionsStart = remaining.lastIndexOf('[');
        int separator = Math.max(remaining.lastIndexOf(','), optionsStart);
        String currentOption = remaining.substring(separator + 1);

        int valueSeparator = currentOption.indexOf('=');
        if (valueSeparator < 0) {
            String prefix = currentOption.toLowerCase(Locale.ROOT);
            SuggestionsBuilder optionBuilder = builder.createOffset(builder.getStart() + separator + 1);
            CloudTargetSelectorParser.OPTIONS.stream()
                    .sorted()
                    .filter(option -> option.startsWith(prefix))
                    .forEach(option -> optionBuilder.suggest(option + "="));
            return optionBuilder.buildFuture();
        }

        String key = currentOption.substring(0, valueSeparator);
        String valuePrefix = currentOption.substring(valueSeparator + 1).toLowerCase(Locale.ROOT);
        SuggestionsBuilder valueBuilder = builder.createOffset(
                builder.getStart() + separator + valueSeparator + 2);

        switch (key) {
            case "sort" -> Stream.of("arbitrary", "furthest", "nearest", "random")
                    .filter(value -> value.startsWith(valuePrefix))
                    .forEach(valueBuilder::suggest);
            case "m", "gamemode" -> Stream.of("adventure", "creative", "spectator", "survival",
                            "!adventure", "!creative", "!spectator", "!survival")
                    .filter(value -> value.startsWith(valuePrefix))
                    .forEach(valueBuilder::suggest);
            case "type" -> selectorEntityTypeSuggestions()
                    .filter(value -> value.startsWith(valuePrefix))
                    .forEach(valueBuilder::suggest);
            default -> {
            }
        }
        return valueBuilder.buildFuture();
    }

    private static Stream<String> selectorEntityTypeSuggestions() {
        return EntityTypes.values().stream()
                .flatMap(type -> identifierSuggestions(type.getId()))
                .flatMap(value -> Stream.of(value, "!" + value))
                .sorted();
    }

    private static Stream<String> identifierSuggestions(Identifier identifier) {
        String value = identifier.toString();
        return "minecraft".equals(identifier.getNamespace())
                ? Stream.of(value, identifier.getName())
                : Stream.of(value);
    }

    private record CloudPlayerSelectorResolver(
            CloudTargetSelector selector,
            boolean singleResult
    ) implements PlayerSelectorResolver {
        @Override
        public List<Player> resolve(CommandSourceStack source) throws CommandSyntaxException {
            List<Player> players = this.selector.resolvePlayers(source);
            if (this.singleResult) {
                if (players.isEmpty()) {
                    throw ERROR_UNKNOWN_PLAYER.create(this.selector.input());
                }
                if (players.size() > 1) {
                    throw ERROR_NOT_SINGLE_PLAYER.create();
                }
            }
            return players;
        }
    }

    private record CloudEntitySelectorResolver(
            CloudTargetSelector selector,
            boolean singleResult
    ) implements EntitySelectorResolver {
        @Override
        public List<Entity> resolve(CommandSourceStack source) throws CommandSyntaxException {
            List<Entity> entities = this.selector.resolveEntities(source);
            if (this.singleResult) {
                if (entities.isEmpty()) {
                    throw ERROR_UNKNOWN_ENTITY.create(this.selector.input());
                }
                if (entities.size() > 1) {
                    throw ERROR_NOT_SINGLE_ENTITY.create();
                }
            }
            return entities;
        }
    }

    private record CloudSelectorArgumentType<T>(
            @Nullable String displayName,
            CloudSelectorParser<T> parser,
            SuggestionProvider<CommandSourceStack> suggestions
    ) implements CommandArgumentType<T> {

        private CloudSelectorArgumentType {
            Objects.requireNonNull(parser, "parser");
            Objects.requireNonNull(suggestions, "suggestions");
        }

        @Override
        public CommandArgumentKind getKind() {
            return CommandArgumentKind.TARGET;
        }

        @Override
        public @Nullable String getDisplayName() {
            return this.displayName;
        }

        @Override
        public @Nullable String getEnumName() {
            return null;
        }

        @Override
        public List<String> getValues() {
            return List.of();
        }

        @Override
        public @Nullable String getPostfix() {
            return null;
        }

        @Override
        public T parse(StringReader reader) throws CommandSyntaxException {
            int start = reader.getCursor();
            String value = readSelectorToken(reader);
            if (value.isEmpty()) {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol()
                        .createWithContext(reader, "target");
            }

            try {
                return this.parser.parse(value);
            } catch (CommandSyntaxException exception) {
                reader.setCursor(start);
                throw exception;
            }
        }

        @Override
        public Collection<String> getExamples() {
            return List.of("Player", "@p");
        }

        @Override
        public <S> CompletableFuture<Suggestions> listSuggestions(
                CommandContext<S> context,
                SuggestionsBuilder builder
        ) {
            if (!(context.getSource() instanceof CommandSourceStack)) {
                return builder.buildFuture();
            }

            @SuppressWarnings("unchecked")
            CommandContext<CommandSourceStack> typedContext = (CommandContext<CommandSourceStack>) context;
            try {
                return this.suggestions.getSuggestions(typedContext, builder);
            } catch (CommandSyntaxException exception) {
                return builder.buildFuture();
            }
        }

        private static String readSelectorToken(StringReader reader) {
            int start = reader.getCursor();
            int squareDepth = 0;
            int objectDepth = 0;
            char quote = 0;
            boolean escaped = false;

            while (reader.canRead()) {
                char current = reader.peek();
                if (quote != 0) {
                    reader.skip();
                    if (escaped) {
                        escaped = false;
                    } else if (current == '\\') {
                        escaped = true;
                    } else if (current == quote) {
                        quote = 0;
                    }
                    continue;
                }

                if (current == '"' || current == '\'') {
                    quote = current;
                } else if (current == '[') {
                    squareDepth++;
                } else if (current == ']') {
                    squareDepth--;
                } else if (current == '{') {
                    objectDepth++;
                } else if (current == '}') {
                    objectDepth--;
                } else if (Character.isWhitespace(current) && squareDepth == 0 && objectDepth == 0) {
                    break;
                }
                reader.skip();
            }
            return reader.getString().substring(start, reader.getCursor());
        }
    }

    @FunctionalInterface
    private interface CloudSelectorParser<T> {
        T parse(String value) throws CommandSyntaxException;
    }
}
