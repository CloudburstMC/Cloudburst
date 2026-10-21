package org.cloudburstmc.server.block;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.blockstateupdater.BlockStateUpdaters;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;

import java.util.Objects;

/**
 * Reads and writes named block states for persistence.
 */
@UtilityClass
public class BlockStateCodec {

    public static BlockState read(NbtMap tag) {
        Objects.requireNonNull(tag, "tag");
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
            throw new IllegalArgumentException("Unknown persisted block: " + id);
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

    public static NbtMap write(BlockState state) {
        return BlockPalette.INSTANCE.getSerialized(Objects.requireNonNull(state, "state"));
    }
}
