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
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.behavior.BlockBehavior;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.block.serializer.BlockSerializer;

import javax.annotation.Nullable;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static net.daporkchop.lib.random.impl.FastPRandom.mix32;

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
    private final Map<String, Set<Object>> vanillaTraitMap = new HashMap<>();
    private final SortedMap<String, List<BlockState>> sortedPalette = new Object2ReferenceRBTreeMap<>();
    //private final Reference2ReferenceMap<Identifier, BlockState> stateMap = new Reference2ReferenceOpenHashMap<>();

    public void addBlock(BlockType type, BlockSerializer serializer) {
        if (this.defaultStateMap.containsKey(type.getId())) {
            log.warn("Duplicate block type: {}", type);
        }

        this.defaultStateMap.put(type.getId(), type.getDefaultState());

        type.getStates().forEach(state -> {
            NbtMapBuilder builder = NbtMap.builder();
            serializer.serialize(builder, type, state.getTraits());
            NbtMap nbt = builder.build();
            Identifier id = nbt.containsKey("name") ? Identifier.fromString(nbt.getString("name")) : type.getId();

            var statesTag = nbt.getCompound("States");
            var traitMap = stateTraitMap.computeIfAbsent(id, v -> new Object2ReferenceOpenHashMap<>());
            traitMap.put(statesTag, state);

            var paletteEntry = sortedPalette.computeIfAbsent(id.toString(), (v) -> new ArrayList<>());
            paletteEntry.add(state);
            ItemTypes.addType(id, type);

            typeMap.putIfAbsent(id, type);
            stateSerializedMap.put(state, nbt);
            serializedStateMap.put(nbt, state);
        });

        type.getTraits().forEach((t) -> {
            var traitValues = vanillaTraitMap.computeIfAbsent(t.getVanillaName(), (k) -> new LinkedHashSet<>());
            t.getPossibleValues().forEach(v -> traitValues.add(v));
        });
    }

    public void generateRuntimeIds() {
        if (!this.runtimeStateMap.isEmpty() || !this.stateRuntimeMap.isEmpty()) {
            log.warn("Palette runtime IDs have already been generated!");
            return;
        }

        sortedPalette.forEach((id, states) -> {
            for (BlockState state : states) {
                int runtimeId = runtimeIdAllocator.getAndIncrement();
                this.runtimeStateMap.put(runtimeId, state);
                this.stateRuntimeMap.put(state, runtimeId);
            }
        });

        if (log.isTraceEnabled()) {
            Path logFile = CloudServer.getInstance().getFilePath().resolve("runtime Ids.log");

            try (FileWriter out = new FileWriter(logFile.toFile())) {
                for (int rid = 0; rid < runtimeIdAllocator.get(); rid++) {
                    out.write("Runtime ID " + rid + "=> " + runtimeStateMap.get(rid).toString() + "\n");
                }
            } catch (Exception e) {
                log.error("{}", e.getMessage());
                e.printStackTrace();
            }
            try (FileWriter out = new FileWriter(CloudServer.getInstance().getFilePath().resolve("stateMap.log").toFile())) {
                for (Map.Entry<BlockState, NbtMap> entry : stateSerializedMap.entrySet()) {
                    out.write("BlockState  " + entry.getKey() + " \n=>\n" + entry.getValue().toString() + "\n");
                }
            } catch (Exception e) {
                log.error("{}", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public Map<String, Set<Object>> getVanillaTraitMap() {
        return vanillaTraitMap;
    }

    public BlockState getState(Identifier id) {
        return this.defaultStateMap.get(id);
    }

    public BlockState getState(Identifier id, Map<String, Object> traits) {
        return Optional.ofNullable(stateTraitMap.get(id)).map(s -> s.get(traits)).orElse(BlockStates.AIR);
        //return serializedStateMap.getOrDefault(traits, defaultStateMap.get(id));
    }

    public Set<String> getTraits(Identifier blockId) {
        return this.defaultStateMap.get(blockId).getType().getTraits().stream().map(m -> m.toString()).collect(Collectors.toSet());
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

    private static Map<NbtMap, BlockState> getBlockPermutations(BlockType type, BlockSerializer serializer, BlockBehavior behavior) {
        BlockTrait<?>[] traits = type.getTraits().toArray(new BlockTrait[0]);
        if (traits.length == 0) {
            Preconditions.checkNotNull(type.getId(), "", type);
            var tags = serialize(type, serializer, ImmutableMap.of());
            // No traits so 1 permutation.
            var state = type.getDefaultState();
            return tags.stream().collect(Collectors.toMap(nbt -> nbt, (s) -> state));
        }

        Reference2IntMap<BlockTrait<?>> traitPalette = new Reference2IntOpenHashMap<>();
        int id = 0;
        for (BlockTrait<?> trait : traits) {
            traitPalette.put(trait, id++);
        }

        Map<Map<BlockTrait<?>, Comparable<?>>, BlockState> duplicated = new Object2ReferenceOpenHashMap<>();
        Map<NbtMap, BlockState> permutations = new Object2ReferenceLinkedOpenHashMap<>();
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

            var traitMap = builder.build();
            Collection<NbtMap> tags = serialize(type, serializer, serializeBuilder.build());
            Preconditions.checkArgument(!tags.isEmpty(), "Block state must have at least one nbt tag");
            Preconditions.checkArgument(
                    tags.stream().map(m -> m.getString("name")).collect(Collectors.toSet()).size() == 1,
                    "Block state cannot represent multiple block ids"
            );

            BlockState state = duplicated.get(traitMap);

            if (state == null) {
                state = new BlockState(
                        type,
                        traitMap,
                        behavior
                );
                duplicated.put(traitMap, state);
            }

            BlockState match = null;
            for (BlockState s : type.getStates()) {
                if (state == s) {
                    match = s;
                    break;
                }
            }

            Preconditions.checkNotNull(match, "Unable to match blockstate to block type");

            Identifier stateId = Identifier.fromString(tags.iterator().next().getString("name"));
            ItemTypes.addType(stateId, type);

            for (NbtMap tag : tags) {
                permutations.put(tag, match);
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
