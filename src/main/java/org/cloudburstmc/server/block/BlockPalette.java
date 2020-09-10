package org.cloudburstmc.server.block;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
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

    public void addBlock(BlockType type, BlockSerializer serializer, BlockTrait<?>[] traits) {
        if (this.defaultStateMap.containsKey(type)) {
            log.warn("Duplicate block type: {}", type);
        }

        List<CloudBlockState> states = getBlockPermutations(type, serializer, traits);

        Map<Map<BlockTrait<?>, Comparable<?>>, CloudBlockState> map = new HashMap<>();
        for (CloudBlockState state : states) {
            map.put(state.getTraits(), state);
        }

        BlockState defaultState = map.get(Arrays.stream(traits).collect(Collectors.toMap(t -> t, BlockTrait::getDefaultValue)));
        this.defaultStateMap.put(type, defaultState);

        for (CloudBlockState state : states) {
            state.buildStateTable(defaultState, map);

            int runtimeId = this.runtimeIdAllocator.getAndIncrement();
            this.stateRuntimeMap.put(state, runtimeId);
            this.runtimeStateMap.put(runtimeId, state);

            this.stateSerializedMap.put(state, state.getTag());
            this.serializedStateMap.put(state.getTag(), state);
        }
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

    public Map<BlockState, NbtMap> getSerializedPalette() {
        return this.stateSerializedMap;
    }

    private static NbtMap serialize(BlockType type, BlockSerializer serializer, Map<BlockTrait<?>, Comparable<?>> traits) {
        NbtMapBuilder tagBuilder = NbtMap.builder();
        serializer.serialize(tagBuilder, type, traits);

        if (tagBuilder.containsKey("name")) {
            BlockStateUpdaters.serializeCommon(tagBuilder, (String) tagBuilder.get("name"));
        } else {
            Preconditions.checkState(type.getId() != null, "BlockType has not an identifier assigned");
            BlockStateUpdaters.serializeCommon(tagBuilder, type.getId().toString());
        }

        return tagBuilder.build();
    }

    private static List<CloudBlockState> getBlockPermutations(BlockType type, BlockSerializer serializer, BlockTrait<?>[] traits) {
        if (traits == null || traits.length == 0) {
            Preconditions.checkNotNull(type.getId(), "", type);
            // No traits so 1 permutation.
            return Collections.singletonList(new CloudBlockState(type.getId(), type, ImmutableMap.of(),
                    Reference2IntMaps.emptyMap(), serialize(type, serializer, ImmutableMap.of())));
        }

        Reference2IntMap<BlockTrait<?>> traitPalette = new Reference2IntOpenHashMap<>();
        int id = 0;
        for (BlockTrait<?> trait : traits) {
            traitPalette.put(trait, id++);
        }

        List<CloudBlockState> permutations = new ObjectArrayList<>();
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
            for (int i = 0; i < n; i++) {
                BlockTrait<?> trait = traits[i];
                builder.put(trait, trait.getPossibleValues().get(indices[i]));
            }

            val traitMap = builder.build();
            NbtMap tag = serialize(type, serializer, traitMap);
            permutations.add(new CloudBlockState(Identifier.fromString(tag.getString("name")), type, traitMap, traitPalette, tag));

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
