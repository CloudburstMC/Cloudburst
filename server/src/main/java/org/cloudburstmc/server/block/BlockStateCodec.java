package org.cloudburstmc.server.block;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.blockstateupdater.BlockStateUpdaters;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;

import java.util.Objects;
import java.util.function.Function;

/**
 * Reads and writes named block states for persistence.
 * Reads upgrade saved states and fill omitted properties from registered defaults.
 */
@UtilityClass
public class BlockStateCodec {

    /**
     * Decodes a registered named state, rejecting unknown names and invalid properties.
     * Legacy numeric metadata must be converted before calling this method.
     */
    public static BlockState read(NbtMap tag) {
        return read(tag, id -> {
            throw new IllegalArgumentException("Unknown persisted block: " + id);
        });
    }

    /**
     * Decodes a state using the supplied resolver only when its block name is unregistered.
     * Malformed data and invalid properties on registered blocks remain errors.
     * The resolver receives the upgraded identifier and must return a non-null state.
     */
    public static BlockState read(NbtMap tag, Function<Identifier, BlockState> unknownBlock) {
        Objects.requireNonNull(tag, "tag");
        Objects.requireNonNull(unknownBlock, "unknownBlock");
        if (tag.containsKey("val")) {
            throw new UnsupportedOperationException("Numeric block metadata is no longer supported: " + tag);
        }

        if (tag.getString("name").isBlank()) {
            throw new IllegalArgumentException("Persisted block state has no name");
        }

        if (tag.containsKey("states") && !tag.containsKey("states", NbtType.COMPOUND)) {
            throw new IllegalArgumentException("Persisted block properties must be a compound: " + tag);
        }

        NbtMap normalized = tag.containsKey("states", NbtType.COMPOUND)
                ? tag
                : tag.toBuilder().putCompound("states", NbtMap.EMPTY).build();

        BlockState state = BlockPalette.INSTANCE.getBlockState(normalized);
        if (state != null) {
            return state;
        }

        NbtMap updated = BlockStateUpdaters.updateBlockState(normalized, normalized.getInt("version"));
        state = BlockPalette.INSTANCE.getBlockState(updated);
        if (state != null) {
            return state;
        }

        Identifier id = Identifier.parse(updated.getString("name"));
        BlockState defaultState = BlockPalette.INSTANCE.getState(id);
        if (defaultState == null) {
            return Objects.requireNonNull(unknownBlock.apply(id), "resolved unknown block");
        }

        NbtMap defaults = BlockPalette.INSTANCE.getSerialized(defaultState).getCompound("states");
        NbtMapBuilder states = updated.getCompound("states", NbtMap.EMPTY).toBuilder();
        defaults.forEach(states::putIfAbsent);

        state = BlockPalette.INSTANCE.getBlockState(updated.toBuilder().putCompound("states", states.build()).build());
        if (state == null) {
            throw new IllegalArgumentException("Invalid persisted block state: " + tag);
        }

        return state;
    }

    /**
     * Returns the canonical persisted tag for a registered state.
     * Unregistered states are rejected.
     */
    public static NbtMap write(BlockState state) {
        return BlockPalette.INSTANCE.getSerialized(Objects.requireNonNull(state, "state"));
    }
}
