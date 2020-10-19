package org.cloudburstmc.server.block;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.nukkitx.blockstateupdater.BlockStateUpdaters;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.cloudburstmc.server.block.serializer.BlockSerializer;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.utils.Identifier;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static net.daporkchop.lib.random.impl.FastPRandom.mix32;

@Log4j2
public class BlockPalette {

    public static final BlockPalette INSTANCE = new BlockPalette();

    private final Reference2IntMap<BlockState> stateRuntimeMap = new Reference2IntOpenHashMap<>();
    private final Int2ReferenceMap<BlockState> runtimeStateMap = new Int2ReferenceOpenHashMap<>();
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
    private final AtomicInteger runtimeIdAllocator = new AtomicInteger();
    private final Reference2ReferenceMap<BlockType, BlockState> defaultStateMap = new Reference2ReferenceOpenHashMap<>();
    private final Reference2ReferenceMap<Identifier, BlockState> stateMap = new Reference2ReferenceOpenHashMap<>();
    private final Reference2ReferenceMap<Identifier, Object2ReferenceMap<NbtMap, BlockState>> stateTraitMap = new Reference2ReferenceOpenHashMap<>();
    private final Map<String, Set<Object>> vanillaTraitMap = new HashMap<>();

    public void addBlock(BlockType type, BlockSerializer serializer, BlockTrait<?>[] traits) {
        if (this.defaultStateMap.containsKey(type)) {
            log.warn("Duplicate block type: {}", type);
        }

        Map<NbtMap, CloudBlockState> states = getBlockPermutations(type, serializer, traits);

        Map<Map<BlockTrait<?>, Comparable<?>>, CloudBlockState> map = new HashMap<>();
        for (CloudBlockState state : states.values()) {
            map.put(state.getTraits(), state);
        }

        BlockState defaultState = map.get(Arrays.stream(traits).filter(t -> !t.isOnlySerialize()).collect(Collectors.toMap(t -> t, BlockTrait::getDefaultValue)));
        this.defaultStateMap.put(type, defaultState);

        states.forEach((nbt, state) -> {
            if (!state.isInitialized()) {
                state.initialize(defaultState, map);

                int runtimeId = this.runtimeIdAllocator.getAndIncrement();
                this.stateRuntimeMap.put(state, runtimeId);
                this.runtimeStateMap.put(runtimeId, state);
                this.stateMap.putIfAbsent(state.getId(), state.defaultState());
            }

            val stateMap = nbt.getCompound("states");

            val traitMap = stateTraitMap.computeIfAbsent(state.getId(), (v) -> new Object2ReferenceOpenHashMap<>());
            traitMap.put(stateMap, state);

            stateMap.forEach((traitName, traitValue) -> {
                val traitValues = vanillaTraitMap.computeIfAbsent(traitName, (k) -> new LinkedHashSet<>());
                traitValues.add(traitValue);
            });

            this.stateSerializedMap.put(state, nbt);
            this.serializedStateMap.put(nbt, state);
        });
    }

    public Map<String, Set<Object>> getVanillaTraitMap() {
        return vanillaTraitMap;
    }

    public BlockState getState(Identifier id) {
        return this.stateMap.get(id);
    }

    public BlockState getState(Identifier id, Map<String, Object> traits) {
        return Optional.ofNullable(this.stateTraitMap.get(id)).map(s -> s.get(traits)).orElse(null);
    }

    public Set<String> getTraits(Identifier blockId) {
        return Optional.ofNullable(this.stateTraitMap.get(blockId)).map(m -> Iterables.getLast(m.keySet()).keySet()).orElse(null);
    }

    public BlockState getDefaultState(BlockType blockType) {
        return this.defaultStateMap.get(blockType);
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

    public Map<BlockState, NbtMap> getStateMap() {
        return this.stateSerializedMap;
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

    private static Map<NbtMap, CloudBlockState> getBlockPermutations(BlockType type, BlockSerializer serializer, BlockTrait<?>[] traits) {
        if (traits == null || traits.length == 0) {
            Preconditions.checkNotNull(type.getId(), "", type);
            val tags = serialize(type, serializer, ImmutableMap.of());
            val state = new CloudBlockState(type.getId(), type, ImmutableMap.of(),
                    Reference2IntMaps.emptyMap()/*, ImmutableList.copyOf(tags)*/);
            // No traits so 1 permutation.
            return tags.stream().collect(Collectors.toMap(nbt -> nbt, (s) -> state));
        }

        Reference2IntMap<BlockTrait<?>> traitPalette = new Reference2IntOpenHashMap<>();
        int id = 0;
        for (BlockTrait<?> trait : traits) {
            traitPalette.put(trait, id++);
        }

        Map<Map<BlockTrait<?>, Comparable<?>>, CloudBlockState> duplicated = new Object2ReferenceOpenHashMap<>();
        Map<NbtMap, CloudBlockState> permutations = new Object2ReferenceLinkedOpenHashMap<>();
        int n = traits.length;

        // To keep track of next element in each of the n arrays
        int[] indices = new int[n];

        // Initialize with first element's index
        for (int i = 0; i < n; i++) {
            indices[i] = 0;
        }

        while (true) {
            // Add current combination
            ImmutableMap.Builder<BlockTrait<?>, Comparable<?>> builder = ImmutableMap.builder();
            ImmutableMap.Builder<BlockTrait<?>, Comparable<?>> serializeBuilder = ImmutableMap.builder();
            for (int i = 0; i < n; i++) {
                BlockTrait<?> trait = traits[i];
                Comparable<?> val = trait.getPossibleValues().get(indices[i]);

                if (!trait.isOnlySerialize()) {
                    builder.put(trait, val);
                }
                serializeBuilder.put(trait, val);
            }

            val traitMap = builder.build();
            Collection<NbtMap> tags = serialize(type, serializer, serializeBuilder.build());
            Preconditions.checkArgument(!tags.isEmpty(), "Block state must have at least one nbt tag");
            Preconditions.checkArgument(
                    tags.stream().map(m -> m.getString("name")).collect(Collectors.toSet()).size() == 1,
                    "Block state cannot represent multiple block ids"
            );

            CloudBlockState state = duplicated.get(traitMap);

            if (state == null) {
                state = new CloudBlockState(
                        Identifier.fromString(Iterables.getLast(tags).getString("name")),
                        type,
                        traitMap,
                        traitPalette
//                        ImmutableList.copyOf(tags)
                );
                duplicated.put(traitMap, state);
            }

//            if (state.getId() == BlockIds.HAY_BLOCK) {
//                for (NbtMap tag : tags) {
//                    log.info("palette: " + tag);
//                }
//            }

            for (NbtMap tag : tags) {
                permutations.put(tag, state);
            }

            // Find the rightmost array that has more elements left after the current element in that array
            int next = n - 1;
            while (next >= 0 && (indices[next] + 1 >= traits[next].getPossibleValues().size())) {
                next--;
            }

            // No such array is found so no more combinations left
            if (next < 0) break;

            // If found move to next element in that array
            indices[next]++;

            // For all arrays to the right of this array current index again points to first element
            for (int i = next + 1; i < n; i++) {
                indices[i] = 0;
            }
        }

        return permutations;
    }
}
