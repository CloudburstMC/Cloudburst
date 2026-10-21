package org.cloudburstmc.server.block;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;

import java.util.*;
import java.util.stream.Stream;

/**
 * Parses named block states using registered serialized property names and values.
 * The format is {@code [namespace:]name{property=value,other=value}}.
 * Omitted properties use the block's declared defaults.
 */
public class BlockStateParser {

    private final StringReader reader;

    private BlockStateParser(String input) {
        this.reader = new StringReader(Objects.requireNonNull(input, "input"));
    }

    /**
     * Resolves one state. The property section is optional.
     *
     * @param input the complete state expression
     * @return the registered state
     * @throws IllegalArgumentException if the expression or state is invalid
     */
    public static BlockState parse(String input) {
        BlockStateParser parser = new BlockStateParser(input);
        ParsedState parsed = parser.read(false);
        BlockState state = BlockPalette.INSTANCE.getState(parsed.id(), parsed.properties());
        if (state == null) {
            throw parser.error("No registered state matches the properties");
        }

        return state;
    }

    /**
     * Resolves matching states, allowing {@code *} as a property value.
     * Properties without a wildcard still use their declared defaults when omitted.
     *
     * @param input the complete state expression
     * @return the registered states matching the expression
     * @throws IllegalArgumentException if the expression is invalid or matches no states
     */
    public static Stream<BlockState> parseMatching(String input) {
        BlockStateParser parser = new BlockStateParser(input);
        ParsedState parsed = parser.read(true);
        List<BlockState> matches = BlockPalette.INSTANCE.getStates(parsed.id()).entrySet().stream()
                .filter(entry -> parsed.properties().entrySet().stream()
                        .allMatch(property -> Objects.equals(entry.getKey().get(property.getKey()), property.getValue())))
                .map(Map.Entry::getValue)
                .toList();
        if (matches.isEmpty()) {
            throw parser.error("No registered state matches the properties");
        }

        return matches.stream();
    }

    private ParsedState read(boolean allowWildcards) {
        this.reader.skipWhitespace();
        Identifier id = Identifier.parse(this.readToken());
        if (BlockPalette.INSTANCE.getState(id) == null) {
            throw this.error("Unknown block " + id);
        }

        Map<String, Object> properties = new HashMap<>(BlockPalette.INSTANCE.getDefaultProperties(id));
        this.reader.skipWhitespace();
        if (this.reader.canRead() && this.reader.peek() == '{') {
            this.readProperties(id, properties, allowWildcards);
        }

        this.reader.skipWhitespace();
        if (this.reader.canRead()) {
            throw this.error("Unexpected trailing input");
        }

        return new ParsedState(id, Map.copyOf(properties));
    }

    private void readProperties(Identifier id, Map<String, Object> properties, boolean allowWildcards) {
        this.reader.skip();
        this.reader.skipWhitespace();
        if (this.reader.canRead() && this.reader.peek() == '}') {
            this.reader.skip();
            return;
        }

        Map<NbtMap, BlockState> states = BlockPalette.INSTANCE.getStates(id);
        Set<String> specified = new HashSet<>();
        while (true) {
            String name = this.readToken();
            if (!specified.add(name)) {
                throw this.error("Duplicate property " + name);
            }
            if (!properties.containsKey(name)) {
                throw this.error("Unknown property " + name + " for " + id);
            }

            this.expect('=');
            String value = this.readToken();
            if (allowWildcards && value.equals("*")) {
                properties.remove(name);
            } else {
                properties.put(name, this.resolveValue(states, name, value));
            }

            this.reader.skipWhitespace();
            if (this.reader.canRead() && this.reader.peek() == '}') {
                this.reader.skip();
                return;
            }

            this.expect(',');
        }
    }

    private Object resolveValue(Map<NbtMap, BlockState> states, String name, String input) {
        for (NbtMap properties : states.keySet()) {
            Object value = properties.get(name);
            if (value == null) {
                continue;
            }

            if (String.valueOf(value).equals(input)) {
                return value;
            }

            if (value instanceof Byte number && (number == 0 && input.equals("false") || number == 1 && input.equals("true"))) {
                return value;
            }
        }

        throw this.error("Invalid value " + input + " for property " + name);
    }

    private String readToken() {
        this.reader.skipWhitespace();
        if (this.reader.canRead() && StringReader.isQuotedStringStart(this.reader.peek())) {
            try {
                String value = this.reader.readString();
                if (value.isEmpty()) {
                    throw this.error("Expected a name or value");
                }
                return value;
            } catch (CommandSyntaxException exception) {
                throw new IllegalArgumentException(exception.getMessage(), exception);
            }
        }

        int start = this.reader.getCursor();
        while (this.reader.canRead()) {
            char character = this.reader.peek();
            if (!StringReader.isAllowedInUnquotedString(character) && character != ':' && character != '/' && character != '*') {
                break;
            }
            this.reader.skip();
        }

        if (this.reader.getCursor() == start) {
            throw this.error("Expected a name or value");
        }

        return this.reader.getString().substring(start, this.reader.getCursor());
    }

    private void expect(char character) {
        this.reader.skipWhitespace();
        if (!this.reader.canRead() || this.reader.peek() != character) {
            throw this.error("Expected '" + character + "'");
        }
        this.reader.skip();
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message + " at position " + this.reader.getCursor() + " in block state \"" + this.reader.getString() + '"');
    }

    private record ParsedState(Identifier id, Map<String, Object> properties) {
    }
}
