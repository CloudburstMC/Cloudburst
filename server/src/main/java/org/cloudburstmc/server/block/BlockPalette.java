package org.cloudburstmc.server.block;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.blockstateupdater.BlockStateUpdaters;
import org.cloudburstmc.nbt.*;
import org.cloudburstmc.protocol.common.DefinitionRegistry;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.block.serializer.BlockSerializer;
import org.cloudburstmc.server.block.util.BlockStateHash;
import org.cloudburstmc.server.registry.VanillaRegistryDiagnostics;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static net.daporkchop.lib.common.math.PMath.mix32;

@Log4j2
public class BlockPalette implements DefinitionRegistry<CloudBlockDefinition> {

    public static final BlockPalette INSTANCE = new BlockPalette();

    //Runtime ID mappings
    private final Reference2ReferenceMap<BlockState, CloudBlockDefinition> stateDefinitionMap = new Reference2ReferenceOpenHashMap<>();
    private final Int2ReferenceMap<CloudBlockDefinition> runtimeDefinitionMap = new Int2ReferenceOpenHashMap<>();
    private final Reference2ReferenceMap<Identifier, CloudBlockDefinition> identifierFirstDefinitionMap = new Reference2ReferenceOpenHashMap<>();

    //NBT Mappings
    private final Object2ReferenceMap<NbtMap, BlockState> serializedStateMap = new Object2ReferenceLinkedOpenCustomHashMap<>(
            new Hash.Strategy<NbtMap>() {
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

    private final Reference2ObjectMap<BlockType, ReferenceSet<Identifier>> type2identifierMap = new Reference2ObjectOpenHashMap<>();
    private final Map<String, Set<Object>> vanillaTraitMap = new HashMap<>();
    private final SortedMap<String, Set<NbtMap>> sortedPalette = new Object2ReferenceRBTreeMap<>();

    public void addBlock(BlockType type, BlockSerializer serializer) {
        if (this.defaultStateMap.containsKey(type.getId())) {
            log.warn("Duplicate block type: {}", type);
        }

        this.defaultStateMap.put(type.getId(), type.getDefaultState());

        ReferenceOpenHashSet<Identifier> typeIdentifiers = new ReferenceOpenHashSet<>();
        type.getStates().forEach(state -> {
            List<NbtMap> tags = (List<NbtMap>) serialize(type, serializer, state.getTraits());
            for (NbtMap nbt : tags) {
                Identifier id;
                if (nbt.containsKey("name")) {
                    id = Identifier.parse(nbt.getString("name"));
                } else {
                    id = type.getId();
                    nbt = nbt.toBuilder().putString("name", id.toString()).build();
                }

                NbtMap statesTag = nbt.getCompound("states");
                Object2ReferenceMap<NbtMap, BlockState> traitMap = stateTraitMap.computeIfAbsent(id, v -> new Object2ReferenceOpenHashMap<>());
                traitMap.put(statesTag, state);

                if (id != type.getId()) {
                    defaultStateMap.putIfAbsent(id, state);
                }

                Set<NbtMap> paletteEntry = sortedPalette.computeIfAbsent(id.toString(), (v) -> new LinkedHashSet<>());
                paletteEntry.add(nbt);

                statesTag.forEach((traitName, traitValue) -> {
                    Set<Object> traitValues = vanillaTraitMap.computeIfAbsent(traitName, k -> new LinkedHashSet<>());
                    traitValues.add(traitValue);
                });

                typeMap.putIfAbsent(id, type);
                identifier2stateMap.putIfAbsent(id, state);
                state2identifierMap.putIfAbsent(state, id);
                stateSerializedMap.put(state, nbt);

                NbtMapBuilder strippedBuilder = nbt.toBuilder();
                strippedBuilder.remove("version");
                strippedBuilder.remove("name_hash");
                strippedBuilder.remove("network_id");
                strippedBuilder.remove("block_id");
                serializedStateMap.put(strippedBuilder.build(), state);

                typeIdentifiers.add(id);
            }
        });

        type2identifierMap.put(type, typeIdentifiers);
    }

    public void generateRuntimeIds() {
        if (!this.runtimeDefinitionMap.isEmpty() || !this.stateDefinitionMap.isEmpty()) {
            log.warn("Palette runtime IDs have already been generated!");
            return;
        }

        List<NbtMap> vanillaPalette = loadVanillaPalette();
        for (int runtimeId = 0; runtimeId < vanillaPalette.size(); runtimeId++) {
            NbtMap entry = vanillaPalette.get(runtimeId);
            NbtMap serializedState = stripRuntimeOnlyTags(entry);
            BlockState state = serializedStateMap.get(serializedState);

            if (state == null) {
                VanillaRegistryDiagnostics.missingVanillaBlockState(entry.getString("name"), serializedState.getCompound("states", NbtMap.EMPTY));
                continue;
            }

            registerRuntimeDefinition(runtimeId, entry, serializedState, state);
        }
    }

    public Map<String, Set<Object>> getVanillaTraitMap() {
        return vanillaTraitMap;
    }

    public BlockType getType(Identifier id) {
        return typeMap.get(id);
    }

    public Set<Identifier> getTypeIdentifiers(BlockType type) {
        ReferenceSet<Identifier> identifiers = type2identifierMap.get(type);
        if (identifiers == null) {
            return Collections.emptySet();
        }

        return identifiers;
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
        CloudBlockDefinition definition = this.runtimeDefinitionMap.get(runtimeId);
        if (definition == null) {
            throw new IllegalArgumentException("Invalid runtime ID: " + runtimeId);
        }
        return definition.getCloudState();
    }

    @Override
    public CloudBlockDefinition getDefinition(int runtimeId) {
        CloudBlockDefinition definition = this.runtimeDefinitionMap.get(runtimeId);
        if (definition == null) {
            throw new IllegalArgumentException("Invalid runtime ID: " + runtimeId);
        }
        return definition;
    }

    @Nullable
    public CloudBlockDefinition getFirstDefinition(Identifier id) {
        return this.identifierFirstDefinitionMap.get(id);
    }

    @Nullable
    public CloudBlockDefinition getDefinitionByStates(Identifier id, NbtMap states) {
        Object2ReferenceMap<NbtMap, BlockState> traitMap = this.stateTraitMap.get(id);
        if (traitMap == null) {
            return null;
        }

        BlockState state = traitMap.get(states);
        if (state == null) {
            return null;
        }

        return this.stateDefinitionMap.get(state);
    }

    @Override
    public boolean isRegistered(CloudBlockDefinition definition) {
        return this.runtimeDefinitionMap.get(definition.getRuntimeId()) == definition;
    }

    @Nullable
    public BlockState getBlockState(NbtMap tag) {
        NbtMapBuilder strippedBuilder = tag.toBuilder();
        strippedBuilder.remove("version");
        strippedBuilder.remove("name_hash");
        strippedBuilder.remove("network_id");
        strippedBuilder.remove("block_id");
        return this.serializedStateMap.get(strippedBuilder.build());
    }

    public CloudBlockDefinition getDefinition(BlockState blockState) {
        CloudBlockDefinition definition = this.stateDefinitionMap.get(blockState);
        if (definition == null) {
            throw new IllegalArgumentException("Invalid BlockState: " + blockState);
        }
        return definition;
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

    public Map<Integer, CloudBlockDefinition> getRuntimeMap() {
        return ImmutableMap.copyOf(this.runtimeDefinitionMap);
    }

    private List<NbtMap> loadVanillaPalette() {
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

        return vanillaPalette;
    }

    private void registerRuntimeDefinition(int runtimeId, NbtMap vanillaEntry, NbtMap serializedState, BlockState state) {
        int fnvHash = BlockStateHash.compute(vanillaEntry.getString("name"), vanillaEntry.getCompound("states", NbtMap.EMPTY));
        long blockStateHash = Integer.toUnsignedLong(fnvHash);

        CloudBlockDefinition definition = new CloudBlockDefinition(state, serializedState, runtimeId, blockStateHash);
        BlockPropertyData.StateShapes shapes = BlockPropertyData.BY_STATE_HASH.get(blockStateHash);
        if (shapes != null) {
            float[] collision = shapes.collisionBoxes().length == 0 ? new float[0] : shapes.collisionBoxes();
            float[] outline = shapes.outlineShape().length == 0 ? new float[0] : shapes.outlineShape();
            state.initStateData(collision, outline);
        }

        this.runtimeDefinitionMap.put(runtimeId, definition);
        this.stateDefinitionMap.putIfAbsent(state, definition);
        this.identifierFirstDefinitionMap.putIfAbsent(Identifier.parse(vanillaEntry.getString("name")), definition);
    }

    private Collection<NbtMap> serialize(BlockType type, BlockSerializer serializer, Map<BlockTrait<?>, Comparable<?>> traits) {
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

    private NbtMap stripRuntimeOnlyTags(NbtMap entry) {
        NbtMapBuilder builder = entry.toBuilder();
        builder.remove("version"); // Remove all nbt tags which are not needed for differentiating states
        builder.remove("name_hash"); // Added in 1.19.20
        builder.remove("network_id"); // Added in 1.19.80
        builder.remove("block_id"); // Added in 1.20.60
        return builder.build();
    }
}
