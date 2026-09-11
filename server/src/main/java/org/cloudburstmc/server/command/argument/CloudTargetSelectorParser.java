package org.cloudburstmc.server.command.argument;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.api.util.Identifier;

import java.util.*;

/**
 * Parser for command target selector input.
 *
 * <p>Supported selector roots are {@code @p}, {@code @a}, {@code @r}, {@code @s},
 * {@code @e}, and {@code @n}. Supported options are exposed through {@link #OPTIONS}.</p>
 */
public class CloudTargetSelectorParser {
    /**
     * Selector option names accepted by this parser.
     */
    public static final Set<String> OPTIONS = Collections.unmodifiableSet(new LinkedHashSet<>(List.of(
            "x", "y", "z", "dx", "dy", "dz", "r", "rm", "distance", "c", "limit", "sort",
            "name", "type", "m", "gamemode", "l", "lm", "rx", "rxm", "ry", "rym", "tag")));

    private static final DynamicCommandExceptionType ERROR_UNSUPPORTED_PLAYER_SELECTOR =
            new DynamicCommandExceptionType(value -> new LiteralMessage("Unsupported player selector " + value));
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_SELECTOR_TYPE =
            new DynamicCommandExceptionType(value -> new LiteralMessage("Unknown selector type " + value));
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_SELECTOR_OPTION =
            new DynamicCommandExceptionType(value -> new LiteralMessage("Unknown selector option " + value));
    private static final DynamicCommandExceptionType ERROR_INVALID_SELECTOR_OPTION =
            new DynamicCommandExceptionType(value -> new LiteralMessage("Invalid selector option " + value));
    private static final SimpleCommandExceptionType ERROR_EXPECTED_SELECTOR_OPTION =
            new SimpleCommandExceptionType(new LiteralMessage("Expected selector option"));
    private static final SimpleCommandExceptionType ERROR_EXPECTED_SELECTOR_OPTION_VALUE =
            new SimpleCommandExceptionType(new LiteralMessage("Expected selector option value"));
    private static final SimpleCommandExceptionType ERROR_EXPECTED_SELECTOR_OPTIONS_END =
            new SimpleCommandExceptionType(new LiteralMessage("Expected end of selector options"));

    private final String input;
    private final StringReader reader;
    private final boolean playersOnlyArgument;
    private CloudSelectorKind kind = CloudSelectorKind.NAME;
    private boolean playersOnly = true;
    private int maxResults = 1;
    private @Nullable Float x;
    private @Nullable Float y;
    private @Nullable Float z;
    private @Nullable Float dx;
    private @Nullable Float dy;
    private @Nullable Float dz;
    private @Nullable Float minDistance;
    private @Nullable Float maxDistance;
    private @Nullable String name;
    private boolean invertedName;
    private @Nullable Identifier type;
    private boolean invertedType;
    private @Nullable GameMode gameMode;
    private boolean invertedGameMode;
    private @Nullable Integer minLevel;
    private @Nullable Integer maxLevel;
    private @Nullable Float minPitch;
    private @Nullable Float maxPitch;
    private @Nullable Float minYaw;
    private @Nullable Float maxYaw;
    private final List<CloudSelectorTag> tags = new ArrayList<>();
    private CloudSelectorSort sort = CloudSelectorSort.ARBITRARY;

    private CloudTargetSelectorParser(String input, boolean playersOnlyArgument) {
        this.input = Objects.requireNonNull(input, "input");
        this.reader = new StringReader(input);
        this.playersOnlyArgument = playersOnlyArgument;
    }

    /**
     * Parses target selector input.
     *
     * @param input               literal target name or selector expression
     * @param playersOnlyArgument whether entity-only selectors should be rejected
     * @return parsed selector
     * @throws CommandSyntaxException if the input is not a valid selector
     */
    public static CloudTargetSelector parse(String input, boolean playersOnlyArgument) throws CommandSyntaxException {
        return new CloudTargetSelectorParser(input, playersOnlyArgument).parse();
    }

    private CloudTargetSelector parse() throws CommandSyntaxException {
        if (this.reader.canRead() && this.reader.peek() == '@') {
            this.reader.skip();
            this.parseSelector();
        } else {
            this.name = this.reader.readString();
            if (this.name.isBlank()) {
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(this.reader, "target");
            }
        }

        if (this.reader.canRead()) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(this.reader);
        }

        if (this.playersOnlyArgument && !this.playersOnly && this.kind != CloudSelectorKind.SELF) {
            throw ERROR_UNSUPPORTED_PLAYER_SELECTOR.createWithContext(this.reader, this.input);
        }

        CloudSelectorPosition selectorPosition = this.x == null && this.y == null && this.z == null ? null : new CloudSelectorPosition(this.x, this.y, this.z);
        CloudSelectorBox selectorBox = this.dx == null && this.dy == null && this.dz == null ? null : new CloudSelectorBox(valueOrZero(this.dx), valueOrZero(this.dy), valueOrZero(this.dz));

        return new CloudTargetSelector(this.input, this.kind, this.playersOnly, this.maxResults, selectorPosition,
                selectorBox, this.minDistance, this.maxDistance, this.name, this.invertedName, this.type,
                this.invertedType, this.gameMode, this.invertedGameMode, this.minLevel, this.maxLevel,
                this.minPitch, this.maxPitch, this.minYaw, this.maxYaw, this.tags, this.sort);
    }

    private void parseSelector() throws CommandSyntaxException {
        if (!this.reader.canRead()) {
            throw ERROR_UNKNOWN_SELECTOR_TYPE.createWithContext(this.reader, "@");
        }

        char selector = this.reader.read();
        switch (selector) {
            case 'a' -> {
                this.kind = CloudSelectorKind.ALL_PLAYERS;
                this.playersOnly = true;
                this.maxResults = Integer.MAX_VALUE;
            }
            case 'e' -> {
                this.kind = CloudSelectorKind.ALL_ENTITIES;
                this.playersOnly = false;
                this.maxResults = Integer.MAX_VALUE;
            }
            case 'n' -> {
                this.kind = CloudSelectorKind.NEAREST_ENTITY;
                this.playersOnly = false;
                this.maxResults = 1;
                this.sort = CloudSelectorSort.NEAREST;
            }
            case 'p' -> {
                this.kind = CloudSelectorKind.NEAREST_PLAYER;
                this.playersOnly = true;
                this.maxResults = 1;
                this.sort = CloudSelectorSort.NEAREST;
            }
            case 'r' -> {
                this.kind = CloudSelectorKind.RANDOM_PLAYER;
                this.playersOnly = true;
                this.maxResults = 1;
                this.sort = CloudSelectorSort.RANDOM;
            }
            case 's' -> {
                this.kind = CloudSelectorKind.SELF;
                this.playersOnly = false;
                this.maxResults = 1;
            }
            default -> throw ERROR_UNKNOWN_SELECTOR_TYPE.createWithContext(this.reader, "@" + selector);
        }

        if (this.reader.canRead() && this.reader.peek() == '[') {
            this.reader.skip();
            this.parseOptions();
        }
    }

    private void parseOptions() throws CommandSyntaxException {
        Map<String, String> options = new LinkedHashMap<>();
        while (this.reader.canRead() && this.reader.peek() != ']') {
            this.reader.skipWhitespace();
            int keyStart = this.reader.getCursor();
            String key = this.reader.readString();

            if (key.isBlank()) {
                this.reader.setCursor(keyStart);
                throw ERROR_EXPECTED_SELECTOR_OPTION.createWithContext(this.reader);
            }

            if (!OPTIONS.contains(key)) {
                throw ERROR_UNKNOWN_SELECTOR_OPTION.createWithContext(this.reader, key);
            }

            if (!this.reader.canRead() || this.reader.peek() != '=') {
                throw ERROR_EXPECTED_SELECTOR_OPTION_VALUE.createWithContext(this.reader);
            }

            this.reader.skip();
            this.reader.skipWhitespace();
            String value = this.readOptionValue(key.equals("tag"));
            if (key.equals("tag")) {
                this.parseTag(value);
            } else if (options.putIfAbsent(key, value) != null) {
                throw ERROR_INVALID_SELECTOR_OPTION.createWithContext(this.reader, key);
            }

            if (this.reader.canRead() && this.reader.peek() == ',') {
                this.reader.skip();
                this.reader.skipWhitespace();
                if (!this.reader.canRead() || this.reader.peek() == ']') {
                    throw ERROR_EXPECTED_SELECTOR_OPTION.createWithContext(this.reader);
                }
            }
        }

        if (!this.reader.canRead() || this.reader.peek() != ']') {
            throw ERROR_EXPECTED_SELECTOR_OPTIONS_END.createWithContext(this.reader);
        }

        this.reader.skip();
        this.applyOptions(options);
    }

    private String readOptionValue(boolean allowEmpty) throws CommandSyntaxException {
        int start = this.reader.getCursor();
        int squareDepth = 0;
        int objectDepth = 0;
        char quote = 0;
        boolean escaped = false;
        while (this.reader.canRead()) {
            char current = this.reader.peek();
            if (quote != 0) {
                this.reader.skip();
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
            } else if (current == '{') {
                objectDepth++;
            } else if (current == ']' && squareDepth > 0) {
                squareDepth--;
            } else if (current == '}') {
                objectDepth--;
                if (objectDepth < 0) {
                    throw ERROR_INVALID_SELECTOR_OPTION.createWithContext(this.reader, "value");
                }
            } else if ((current == ',' || current == ']') && squareDepth == 0 && objectDepth == 0) {
                break;
            }
            this.reader.skip();
        }

        if (quote != 0 || squareDepth != 0 || objectDepth != 0) {
            throw ERROR_INVALID_SELECTOR_OPTION.createWithContext(this.reader, "value");
        }

        String value = this.input.substring(start, this.reader.getCursor()).trim();
        if (value.isEmpty() && !allowEmpty) {
            this.reader.setCursor(start);
            throw ERROR_EXPECTED_SELECTOR_OPTION_VALUE.createWithContext(this.reader);
        }

        return value;
    }

    private void applyOptions(Map<String, String> options) throws CommandSyntaxException {
        this.x = parseFloat(options.get("x"), "x");
        this.y = parseFloat(options.get("y"), "y");
        this.z = parseFloat(options.get("z"), "z");
        this.dx = parseFloat(options.get("dx"), "dx");
        this.dy = parseFloat(options.get("dy"), "dy");
        this.dz = parseFloat(options.get("dz"), "dz");

        if (options.containsKey("distance") && (options.containsKey("r") || options.containsKey("rm"))) {
            throw ERROR_INVALID_SELECTOR_OPTION.createWithContext(this.reader, "distance");
        }

        this.maxDistance = parseFloat(options.get("r"), "r");
        this.minDistance = parseFloat(options.get("rm"), "rm");
        parseDistance(options.get("distance"));
        validateRange(this.minDistance, this.maxDistance, 0.0f, Float.MAX_VALUE, "r");

        parseLimit(options);
        parseSort(options.get("sort"));
        parseName(options.get("name"));
        parseType(options.get("type"));
        parseGameModeOptions(options);
        this.maxLevel = parseInteger(options.get("l"), "l");
        this.minLevel = parseInteger(options.get("lm"), "lm");
        validateRange(this.minLevel, this.maxLevel, 0, Integer.MAX_VALUE, "l");
        this.maxPitch = parseFloat(options.get("rx"), "rx");
        this.minPitch = parseFloat(options.get("rxm"), "rxm");
        validateRange(this.minPitch, this.maxPitch, -90.0f, 90.0f, "rx");
        this.maxYaw = parseFloat(options.get("ry"), "ry");
        this.minYaw = parseFloat(options.get("rym"), "rym");
        validateRange(this.minYaw, this.maxYaw, -180.0f, 180.0f, "ry");
    }

    private void parseDistance(@Nullable String value) throws CommandSyntaxException {
        if (value == null) {
            return;
        }

        int range = value.indexOf("..");
        if (range < 0) {
            float distance = parseRequiredFloat(value, "distance");
            this.minDistance = distance;
            this.maxDistance = distance;
            return;
        }

        String min = value.substring(0, range);
        String max = value.substring(range + 2);
        if (min.isEmpty() && max.isEmpty()) {
            throw ERROR_INVALID_SELECTOR_OPTION.createWithContext(this.reader, "distance");
        }
        if (!min.isEmpty()) {
            this.minDistance = parseRequiredFloat(min, "distance");
        }
        if (!max.isEmpty()) {
            this.maxDistance = parseRequiredFloat(max, "distance");
        }
    }

    private void parseLimit(Map<String, String> options) throws CommandSyntaxException {
        if (options.containsKey("limit") && options.containsKey("c")) {
            throw ERROR_INVALID_SELECTOR_OPTION.createWithContext(this.reader, "limit");
        }
        String value = options.containsKey("limit") ? options.get("limit") : options.get("c");
        if (value == null) {
            return;
        }

        int limit = parseRequiredInteger(value, options.containsKey("limit") ? "limit" : "c");
        if (limit == 0) {
            throw ERROR_INVALID_SELECTOR_OPTION.createWithContext(this.reader, options.containsKey("limit") ? "limit" : "c");
        }
        if (limit == Integer.MIN_VALUE) {
            throw ERROR_INVALID_SELECTOR_OPTION.createWithContext(this.reader, options.containsKey("limit") ? "limit" : "c");
        }

        this.maxResults = Math.abs(limit);
        if (limit < 0) {
            this.sort = CloudSelectorSort.FURTHEST;
        }
    }

    private void parseSort(@Nullable String value) throws CommandSyntaxException {
        if (value == null) {
            return;
        }

        this.sort = switch (value) {
            case "nearest" -> CloudSelectorSort.NEAREST;
            case "furthest" -> CloudSelectorSort.FURTHEST;
            case "random" -> CloudSelectorSort.RANDOM;
            case "arbitrary" -> CloudSelectorSort.ARBITRARY;
            default -> throw ERROR_INVALID_SELECTOR_OPTION.createWithContext(this.reader, "sort");
        };
    }

    private void parseName(@Nullable String value) throws CommandSyntaxException {
        if (value == null) {
            return;
        }

        this.invertedName = value.startsWith("!");
        this.name = parseStringValue(this.invertedName ? value.substring(1) : value, "name");

        if (this.name.isBlank()) {
            throw ERROR_INVALID_SELECTOR_OPTION.createWithContext(this.reader, "name");
        }
    }

    private void parseType(@Nullable String value) throws CommandSyntaxException {
        if (value == null) {
            return;
        }

        this.invertedType = value.startsWith("!");
        String id = this.invertedType ? value.substring(1) : value;
        if (id.isBlank()) {
            throw ERROR_INVALID_SELECTOR_OPTION.createWithContext(this.reader, "type");
        }

        if (id.equals("player") || id.equals("minecraft:player")) {
            if (!this.invertedType) {
                this.playersOnly = true;
            }
            this.type = EntityTypes.PLAYER.getId();
            return;
        }

        Identifier identifier;
        try {
            identifier = Identifier.parse(id);
        } catch (IllegalArgumentException e) {
            throw ERROR_INVALID_SELECTOR_OPTION.createWithContext(this.reader, "type");
        }

        if (EntityTypes.values().stream().noneMatch(type -> type.getId().equals(identifier))) {
            throw ERROR_INVALID_SELECTOR_OPTION.createWithContext(this.reader, "type");
        }
        this.type = identifier;
    }

    private void parseTag(String value) throws CommandSyntaxException {
        boolean inverted = value.startsWith("!");
        String tag = parseStringValue(inverted ? value.substring(1) : value, "tag");
        this.tags.add(new CloudSelectorTag(tag, inverted));
    }

    private void parseGameMode(@Nullable String value, String option) throws CommandSyntaxException {
        if (value == null) {
            return;
        }

        this.invertedGameMode = value.startsWith("!");
        String mode = this.invertedGameMode ? value.substring(1) : value;
        if (mode.isBlank()) {
            throw ERROR_INVALID_SELECTOR_OPTION.createWithContext(this.reader, option);
        }

        GameMode parsed = GameMode.from(mode.toLowerCase(Locale.ROOT));
        if (parsed == null) {
            throw ERROR_INVALID_SELECTOR_OPTION.createWithContext(this.reader, option);
        }

        this.gameMode = parsed;
        this.playersOnly = true;
    }

    private void parseGameModeOptions(Map<String, String> options) throws CommandSyntaxException {
        if (options.containsKey("m") && options.containsKey("gamemode")) {
            throw ERROR_INVALID_SELECTOR_OPTION.createWithContext(this.reader, "gamemode");
        }
        String option = options.containsKey("gamemode") ? "gamemode" : "m";
        parseGameMode(options.get(option), option);
    }

    private @Nullable Float parseFloat(@Nullable String value, String option) throws CommandSyntaxException {
        return value == null ? null : parseRequiredFloat(value, option);
    }

    private float parseRequiredFloat(String value, String option) throws CommandSyntaxException {
        try {
            float parsed = Float.parseFloat(value);
            if (!Float.isFinite(parsed)) {
                throw ERROR_INVALID_SELECTOR_OPTION.createWithContext(this.reader, option);
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw ERROR_INVALID_SELECTOR_OPTION.createWithContext(this.reader, option);
        }
    }

    private @Nullable Integer parseInteger(@Nullable String value, String option) throws CommandSyntaxException {
        return value == null ? null : parseRequiredInteger(value, option);
    }

    private int parseRequiredInteger(String value, String option) throws CommandSyntaxException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw ERROR_INVALID_SELECTOR_OPTION.createWithContext(this.reader, option);
        }
    }

    private String parseStringValue(String value, String option) throws CommandSyntaxException {
        StringReader valueReader = new StringReader(value);
        try {
            String parsed = valueReader.readString();
            if (valueReader.canRead()) {
                throw ERROR_INVALID_SELECTOR_OPTION.createWithContext(this.reader, option);
            }
            return parsed;
        } catch (CommandSyntaxException exception) {
            throw ERROR_INVALID_SELECTOR_OPTION.createWithContext(this.reader, option);
        }
    }

    private <T extends Number & Comparable<T>> void validateRange(@Nullable T min, @Nullable T max,
                                                                  T lowerBound, T upperBound, String option)
            throws CommandSyntaxException {
        if ((min != null && (min.compareTo(lowerBound) < 0 || min.compareTo(upperBound) > 0))
                || (max != null && (max.compareTo(lowerBound) < 0 || max.compareTo(upperBound) > 0))
                || (min != null && max != null && min.compareTo(max) > 0)) {
            throw ERROR_INVALID_SELECTOR_OPTION.createWithContext(this.reader, option);
        }
    }

    private static float valueOrZero(@Nullable Float value) {
        return value == null ? 0 : value;
    }
}
