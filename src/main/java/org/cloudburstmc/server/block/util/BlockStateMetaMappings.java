package org.cloudburstmc.server.block.util;

import com.nukkitx.blockstateupdater.BlockStateUpdaterBase;
import com.nukkitx.blockstateupdater.BlockStateUpdaters;
import com.nukkitx.nbt.NbtMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.utils.Identifier;

@UtilityClass
@Log4j2
public class BlockStateMetaMappings {

    private final Reference2IntMap<BlockState> state2meta = new Reference2IntOpenHashMap<>();
    private final Reference2ObjectMap<Identifier, Int2ReferenceMap<BlockState>> meta2state = new Reference2ObjectOpenHashMap<>();

    static {
        init();
    }

    @SneakyThrows
    public void init() {

        BlockStateUpdaterBase.LEGACY_BLOCK_DATA_MAP.forEach((name, states) -> {
            Identifier type = Identifier.fromString(name);

            Int2ReferenceMap<BlockState> mapping = new Int2ReferenceOpenHashMap<>();

            for (int i = 0; i < states.length; i++) {
                NbtMap map = BlockStateUpdaters.updateBlockState(NbtMap.builder()
                        .putString("name", name)
                        .putShort("val", (short) i)
                        .build(), 0);

                BlockState state = BlockPalette.INSTANCE.getBlockState(map);
                if (state == null) state = mapping.get(0);
                if (state == null) continue;

                state2meta.put(state, i);
                mapping.put(i, state);
            }

            if (!mapping.isEmpty()) {
                meta2state.put(type, mapping);
            }
        });
    }

    public boolean hasMeta(Identifier type, int meta) {
        return getStateFromMeta(type, meta) != null;
    }

    public int getMetaFromState(BlockState state) {
        return state2meta.getOrDefault(state, -1);
    }

    public BlockState getStateFromMeta(ItemStack item) {
        return getStateFromMeta(item.getId(), item.getMeta());
    }

    public BlockState getStateFromMeta(Identifier type, int meta) {
        Int2ReferenceMap<BlockState> states = meta2state.get(type);

        if (states == null) {
            return BlockPalette.INSTANCE.getDefaultState(type);
        }

        BlockState state = states.get(meta);
        if (state == null) {
            state = BlockPalette.INSTANCE.getDefaultState(type);
        }

        return state;
    }
}
