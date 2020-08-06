package org.cloudburstmc.server.block;

import com.google.common.collect.ImmutableMap;
import com.nukkitx.blockstateupdater.BlockStateUpdaters;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.server.block.serializer.BlockSerializer;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.utils.Identifier;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Log4j2
public class BlockPalette {
    public static final BlockPalette INSTANCE = new BlockPalette();

    private final Reference2IntMap<BlockState> stateRuntimeMap = new Reference2IntOpenHashMap<>();
    private final Int2ReferenceMap<BlockState> runtimeStateMap = new Int2ReferenceOpenHashMap<>();
    private final Object2ReferenceMap<NbtMap, BlockState> serializedStateMap = new Object2ReferenceLinkedOpenHashMap<>();
    private final Reference2ObjectMap<BlockState, NbtMap> stateSerializedMap = new Reference2ObjectLinkedOpenHashMap<>();
    private final AtomicInteger runtimeIdAllocator = new AtomicInteger();
    private final Reference2ReferenceMap<Identifier, BlockState> defaultStateMap = new Reference2ReferenceOpenHashMap<>();

    public void addBlock(Identifier identifier, BlockSerializer serializer, BlockTrait<?>[] traits) {
        if (this.defaultStateMap.containsKey(identifier))   {
            log.warn("Duplicate block identifier: {}", identifier);
        }

        List<CloudBlockState> states = getBlockPermutations(identifier, traits);

        Map<Map<BlockTrait<?>, Comparable<?>>, CloudBlockState> map = new HashMap<>();
        for (CloudBlockState state : states) {
            map.put(state.getTraits(), state);
        }

        BlockState defaultState = map.get(Arrays.stream(traits).collect(Collectors.toMap(t -> t, BlockTrait::getDefaultValue)));
        this.defaultStateMap.put(identifier, defaultState);

        for (CloudBlockState state : states) {
            state.buildStateTable(defaultState, map);

            int runtimeId = this.runtimeIdAllocator.getAndIncrement();
            this.stateRuntimeMap.put(state, runtimeId);
            this.runtimeStateMap.put(runtimeId, state);

            NbtMapBuilder tagBuilder = NbtMap.builder();
            BlockStateUpdaters.serializeCommon(tagBuilder, identifier.toString());

            NbtMapBuilder statesBuilder = NbtMap.builder();
            serializer.serialize(statesBuilder, state);
            tagBuilder.putCompound("states", statesBuilder.build());

            NbtMap tag = tagBuilder.build();
            this.stateSerializedMap.put(state, tag);
            this.serializedStateMap.put(tag, state);
        }
    }

    public BlockState getDefaultState(Identifier blockType) {
        return this.defaultStateMap.get(blockType);
    }

    public BlockState getBlockState(int runtimeId) {
        BlockState blockState = this.runtimeStateMap.get(runtimeId);
        if (blockState == null) {
            throw new IllegalArgumentException("Invalid runtime ID: " + runtimeId);
        }
        return blockState;
    }

    public BlockState getBlockState(NbtMap tag) {
        BlockState blockState = this.serializedStateMap.get(tag);
        if (blockState == null) {
            throw new IllegalArgumentException("Invalid block state\n" + tag);
        }
        return blockState;
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

    private static List<CloudBlockState> getBlockPermutations(Identifier identifier, BlockTrait<?>[] traits) {
        if (traits == null || traits.length == 0) {
            // No traits so 1 permutation.
            return Collections.singletonList(new CloudBlockState(identifier, ImmutableMap.of(),
                    Reference2IntMaps.emptyMap()));
        }

        Reference2IntMap<BlockTrait<?>> traitPalette = new Reference2IntOpenHashMap<>();
        List<CloudBlockState> permutations = new ObjectArrayList<>();
        int id = 0;
        for (BlockTrait<?> trait : traits) {
            traitPalette.put(trait, id++);
        }

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
            permutations.add(new CloudBlockState(identifier, builder.build(), traitPalette));

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
