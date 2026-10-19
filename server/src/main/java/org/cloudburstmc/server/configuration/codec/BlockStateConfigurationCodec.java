package org.cloudburstmc.server.configuration.codec;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.configuration.ConfigurationCodec;
import org.cloudburstmc.api.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Stores block trait names and values independently of runtime palette IDs.
 */
public class BlockStateConfigurationCodec implements ConfigurationCodec<BlockState> {
    private final Function<Identifier, BlockState> blockResolver;

    public BlockStateConfigurationCodec(Function<Identifier, BlockState> blockResolver) {
        this.blockResolver = Objects.requireNonNull(blockResolver, "blockResolver");
    }

    @Override
    public Map<String, Object> encode(BlockState value) {
        Map<String, Object> traits = new LinkedHashMap<>();
        value.getTraits().forEach((trait, setting) ->
                traits.put(trait.getName().toString(), valueName(setting)));

        return Map.of("block", value.getType().getId().toString(), "traits", traits);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public BlockState decode(Map<String, Object> values) {
        if (!(values.get("block") instanceof String id) || !(values.get("traits") instanceof Map<?, ?> traits)) {
            throw new IllegalArgumentException("Block state requires block and traits");
        }

        BlockState state = blockResolver.apply(Identifier.parse(id));
        if (state == null) {
            throw new IllegalArgumentException("Unknown block: " + id);
        }

        BlockType type = state.getType();

        for (Map.Entry<?, ?> entry : traits.entrySet()) {
            if (!(entry.getKey() instanceof String name) || !(entry.getValue() instanceof String text)) {
                throw new IllegalArgumentException("Block trait names and values must be strings: " + entry);
            }

            BlockTrait trait = type.getTraits().stream()
                    .filter(candidate -> candidate.getName().toString().equals(name))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown block trait: " + entry.getKey()));

            state = state.withTrait(trait, parseValue(trait, text));
        }

        return state;
    }

    private static String valueName(Comparable<?> value) {
        return value instanceof Enum<?> enumeration ? enumeration.name() : value.toString();
    }

    private static <T extends Comparable<T>> T parseValue(BlockTrait<T> trait, String text) {
        return trait.getPossibleValues().stream()
                .filter(candidate -> valueName(candidate).equalsIgnoreCase(text))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid value for block trait " + trait.getName() + ": " + text));
    }
}
