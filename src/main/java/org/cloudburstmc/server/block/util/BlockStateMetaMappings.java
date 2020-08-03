package org.cloudburstmc.server.block.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.utils.Identifier;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@UtilityClass
@Log4j2
public class BlockStateMetaMappings {

    private final Reference2IntMap<BlockState> state2meta = new Reference2IntOpenHashMap<>();
    private final Reference2ObjectMap<Identifier, Int2ReferenceMap<BlockState>> meta2state = new Reference2ObjectOpenHashMap<>();

    @SuppressWarnings("rawtypes")
    @SneakyThrows
    public void init() {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, List<Map<String, Comparable>>> data = mapper.readValue(
                BlockStateMetaMappings.class.getClassLoader().getResourceAsStream("legacy_block_data_map.json"),
                new TypeReference<Map<String, List<Map<String, Comparable>>>>() {
                });

        data.forEach((name, states) -> {
            Identifier type = Identifier.fromString(name);
            BlockState defaultState = BlockPalette.INSTANCE.getDefaultState(type);

            Int2ReferenceMap<BlockState> mapping = new Int2ReferenceOpenHashMap<>();
            meta2state.put(type, mapping);

            for (int i = 0; i < states.size(); i++) {
                Map<String, Comparable> traits = states.get(i);
                BlockState state = defaultState;

                for (Entry<String, Comparable> entry : traits.entrySet()) {
                    String key = entry.getKey();

                    BlockTrait<?> trait = BlockTraits.fromVanilla(key);

                    if (trait == null) {
                        log.warn("Unknown trait {}", key);
                        return;
                    }

                    state = state.withTrait(trait, entry.getValue());
                }

                state2meta.put(state, i);
                mapping.put(i, state);
            }
        });
    }

    public boolean hasMeta(Identifier type, int meta) {
        return getStateFromMeta(type, meta) != null;
    }

    public int getMetaFromState(BlockState state) {
        return state2meta.getOrDefault(state, -1);
    }

    public BlockState getStateFromMeta(Item item) {
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
