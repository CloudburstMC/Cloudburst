package org.cloudburstmc.server.block;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.nukkitx.blockstateupdater.BlockStateUpdaters;
import com.nukkitx.nbt.*;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.block.serializer.BlockSerializer;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static net.daporkchop.lib.common.math.PMath.mix32;

@Log4j2
public class BlockPalette {

    public static final BlockPalette INSTANCE = new BlockPalette();

    //Runtime ID mappings
    private final Reference2IntMap<BlockState> stateRuntimeMap = new Reference2IntOpenHashMap<>();
    private final Int2ReferenceMap<BlockState> runtimeStateMap = new Int2ReferenceOpenHashMap<>();
    private final AtomicInteger runtimeIdAllocator = new AtomicInteger();

    //NBT Mappings
    private final Object2ReferenceMap<NbtMap, BlockState> serializedStateMap = new Object2ReferenceLinkedOpenCustomHashMap<>(new Hash.Strategy<NbtMap>() {
        @Override
        public int hashCode(NbtMap o) {
            return mix32(o.hashCode());
        }

        @Override
        public boolean equals(NbtMap a, NbtMap b) {
            return Objects.equals(a, b);
        }
    });
    private final Reference2ObjectMap<BlockState, NbtMap> stateSerializedMap = new Reference2ObjectLinkedOpenHashMap<>();
    private final Reference2ReferenceMap<Identifier, Object2ReferenceMap<NbtMap, BlockState>> stateTraitMap = new Reference2ReferenceOpenHashMap<>();

    private final Reference2ReferenceMap<Identifier, BlockType> typeMap = new Reference2ReferenceOpenHashMap<>();
    private final Reference2ReferenceMap<Identifier, BlockState> defaultStateMap = new Reference2ReferenceOpenHashMap<>();
    private final Reference2ReferenceMap<Identifier, BlockState> identifier2stateMap = new Reference2ReferenceOpenHashMap<>();
    private final Reference2ReferenceMap<BlockState, Identifier> state2identifierMap = new Reference2ReferenceOpenHashMap<>();
    private final Map<String, Set<Object>> vanillaTraitMap = new HashMap<>();
    private final SortedMap<String, Set<NbtMap>> sortedPalette = new Object2ReferenceRBTreeMap<>();
    //private final Reference2ReferenceMap<Identifier, BlockState> stateMap = new Reference2ReferenceOpenHashMap<>();

    public void addBlock(BlockType type, BlockSerializer serializer) {
        if (this.defaultStateMap.containsKey(type.getId())) {
            log.warn("Duplicate block type: {}", type);
        }

        this.defaultStateMap.put(type.getId(), type.getDefaultState());

        type.getStates().forEach(state -> {
            List<NbtMap> tags = (List<NbtMap>) serialize(type, serializer, state.getTraits());
            for (NbtMap nbt : tags) {
                Identifier id;
                if (nbt.containsKey("name")) {
                    id = Identifier.fromString(nbt.getString("name"));
                } else {
                    id = type.getId();
                    nbt = nbt.toBuilder().putString("name", id.toString()).build();
                }

                var statesTag = nbt.getCompound("states");
                var traitMap = stateTraitMap.computeIfAbsent(id, v -> new Object2ReferenceOpenHashMap<>());
                traitMap.put(statesTag, state);

                ItemTypes.addType(id, type);
                if (id != type.getId()) {
                    defaultStateMap.putIfAbsent(id, state);
                }

                var paletteEntry = sortedPalette.computeIfAbsent(id.toString(), (v) -> new LinkedHashSet<>());
                paletteEntry.add(nbt);

                statesTag.forEach((traitName, traitValue) -> {
                    var traitValues = vanillaTraitMap.computeIfAbsent(traitName, k -> new LinkedHashSet<>());
                    traitValues.add(traitValue);
                });

                typeMap.putIfAbsent(id, type);
                identifier2stateMap.putIfAbsent(id, state);
                state2identifierMap.putIfAbsent(state, id);
                stateSerializedMap.put(state, nbt);
                serializedStateMap.put(nbt, state);
            }
        });

    }

    public void generateRuntimeIds() {
        if (!this.runtimeStateMap.isEmpty() || !this.stateRuntimeMap.isEmpty()) {
            log.warn("Palette runtime IDs have already been generated!");
            return;
        }

        List<NbtMap> vanillaPalette;
        InputStream stream = Bootstrap.class.getClassLoader().getResourceAsStream("data/block_palette.nbt");

        if (stream == null) {
            throw new AssertionError("Unable to load block palette");
        }

        try (NBTInputStream nbtStream = NbtUtils.createGZIPReader(stream)) {
            NbtMap tag = (NbtMap) nbtStream.readTag();
            vanillaPalette = tag.getList("blocks", NbtType.COMPOUND);
        } catch (IOException e) {
            throw new AssertionError("Unable to load block palette");
        }

        for (int i = 0; i < vanillaPalette.size(); i++) {
            NbtMapBuilder builder = vanillaPalette.get(i).toBuilder();
            builder.remove("name_hash"); // Temporary workaround - Added in 1.19.20
            NbtMap nbt = builder.build();

            BlockState state = serializedStateMap.get(nbt);

            if (state == null) {
                log.warn("Block state not implemented for nbt {}", nbt);
                continue;
            }

            this.runtimeStateMap.put(i, state);
            this.stateRuntimeMap.putIfAbsent(state, i);
        }
    }

    public Map<String, Set<Object>> getVanillaTraitMap() {
        return vanillaTraitMap;
    }

    public BlockType getType(Identifier id) {
        return typeMap.get(id);
    }

    public BlockState getState(Identifier id) {
        return this.identifier2stateMap.get(id);
    }

    public Identifier getIdentifier(BlockState state) {
        return this.state2identifierMap.get(state);
    }

    public BlockState getState(Identifier id, Map<String, Object> traits) {
        return Optional.ofNullable(stateTraitMap.get(id)).map(s -> s.get(traits)).orElse(null);
    }

    public Set<String> getTraits(Identifier blockId) {
        return Optional.ofNullable(this.stateTraitMap.get(blockId)).map(m -> Iterables.getLast(m.keySet()).keySet()).orElse(null);
    }

    public BlockState getDefaultState(BlockType blockType) {
        return this.defaultStateMap.get(blockType.getId());
    }

    public BlockState getBlockState(int runtimeId) {
        BlockState blockState = this.runtimeStateMap.get(runtimeId);
        if (blockState == null) {
            throw new IllegalArgumentException("Invalid runtime ID: " + runtimeId);
        }
        return blockState;
    }

    @Nullable
    public BlockState getBlockState(NbtMap tag) {
        return this.serializedStateMap.get(tag);
    }

    public int getRuntimeId(BlockState blockState) {
        int runtimeId = this.stateRuntimeMap.getInt(blockState);
        if (runtimeId == -1) {
            throw new IllegalArgumentException("Invalid BlockState: " + blockState);
        }
        return runtimeId;
    }

    public NbtMap getSerialized(BlockState state) {
        NbtMap serializedTag = this.stateSerializedMap.get(state);
        if (serializedTag == null) {
            throw new IllegalArgumentException("Invalid BlockState: " + state);
        }
        return serializedTag;
    }

    public Map<NbtMap, BlockState> getSerializedPalette() {
        return this.serializedStateMap;
    }

    public List<Identifier> getBlockIdentifiers() {
        return ImmutableList.copyOf(typeMap.keySet());
    }

    public Map<Integer, BlockState> getRuntimeMap() {
        return ImmutableMap.copyOf(this.runtimeStateMap);
    }

    private static Collection<NbtMap> serialize(BlockType type, BlockSerializer serializer, Map<BlockTrait<?>, Comparable<?>> traits) {
        List<NbtMapBuilder> tags = new LinkedList<>();
        serializer.serialize(tags, type, traits);

        for (NbtMapBuilder tagBuilder : tags) {
            if (tagBuilder.containsKey("name")) {
                BlockStateUpdaters.serializeCommon(tagBuilder, (String) tagBuilder.get("name"));
            } else {
                Preconditions.checkState(type.getId() != null, "BlockType has not an identifier assigned");
                BlockStateUpdaters.serializeCommon(tagBuilder, type.getId().toString());
            }
        }

        return tags.stream().map(NbtMapBuilder::build).collect(Collectors.toList());
    }

}
