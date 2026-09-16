package org.cloudburstmc.server.block;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockRegistrationAccess;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.trait.BlockTrait;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.blockstateupdater.BlockStateUpdaters;
import org.cloudburstmc.nbt.*;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.cloudburstmc.protocol.common.DefinitionRegistry;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.block.serializer.BlockSerializer;
import org.cloudburstmc.server.block.util.BlockStateHash;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;
import org.cloudburstmc.server.registry.VanillaRegistryDiagnostics;
import org.jspecify.annotations.NonNull;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static net.daporkchop.lib.common.math.PMath.mix32;

@Log4j2
public class BlockPalette implements DefinitionRegistry<BlockDefinition> {

    public static final BlockPalette INSTANCE = new BlockPalette();

    //Runtime ID mappings
    private final Reference2ReferenceMap<BlockState, CloudBlockDefinition> stateDefinitionMap = new Reference2ReferenceOpenHashMap<>();
    private final Int2ReferenceMap<CloudBlockDefinition> runtimeDefinitionMap = new Int2ReferenceOpenHashMap<>();
    private final Object2ReferenceMap<Identifier, CloudBlockDefinition> identifierFirstDefinitionMap = new Object2ReferenceOpenHashMap<>();

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
    private final Object2ReferenceMap<Identifier, Object2ReferenceMap<NbtMap, BlockState>> stateTraitMap = new Object2ReferenceOpenHashMap<>();

    private final Object2ReferenceMap<Identifier, BlockType> typeMap = new Object2ReferenceOpenHashMap<>();
    private final Object2ReferenceMap<Identifier, BlockState> defaultStateMap = new Object2ReferenceOpenHashMap<>();
    private final Object2ReferenceMap<Identifier, BlockState> identifier2stateMap = new Object2ReferenceOpenHashMap<>();
    private final Reference2ReferenceMap<BlockState, Identifier> state2identifierMap = new Reference2ReferenceOpenHashMap<>();

    private final Reference2ObjectMap<BlockType, Set<Identifier>> type2identifierMap = new Reference2ObjectOpenHashMap<>();
    @Getter
    private final Map<String, Set<Object>> vanillaTraitMap = new HashMap<>();
    private final SortedMap<String, Set<NbtMap>> sortedPalette = new Object2ReferenceRBTreeMap<>();

    public void addBlock(BlockType type, BlockSerializer serializer) {
        if (this.defaultStateMap.containsKey(type.getId())) {
            log.warn("Duplicate block type: {}", type);
        }

        this.defaultStateMap.put(type.getId(), type.getDefaultState());

        Set<Identifier> typeIdentifiers = new ObjectOpenHashSet<>();
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
        List<RuntimeRegistration> registrations = new ArrayList<>(vanillaPalette.size());
        List<MissingBlockProperty> missingProperties = new ArrayList<>();

        for (int runtimeId = 0; runtimeId < vanillaPalette.size(); runtimeId++) {
            NbtMap entry = vanillaPalette.get(runtimeId);
            NbtMap serializedState = stripRuntimeOnlyTags(entry);
            BlockState state = serializedStateMap.get(serializedState);

            if (state == null) {
                VanillaRegistryDiagnostics.missingVanillaBlockState(entry.getString("name"), serializedState.getCompound("states", NbtMap.EMPTY));
                continue;
            }

            int fnvHash = BlockStateHash.compute(entry.getString("name"), entry.getCompound("states", NbtMap.EMPTY));
            long blockStateHash = Integer.toUnsignedLong(fnvHash);
            BlockPropertyData.StateData stateData = BlockPropertyData.get(blockStateHash);
            if (stateData == null) {
                missingProperties.add(new MissingBlockProperty(state, blockStateHash));
                continue;
            }

            registrations.add(new RuntimeRegistration(runtimeId, entry, serializedState, state, blockStateHash, stateData));
        }

        if (!missingProperties.isEmpty()) {
            throw missingBlockProperties(missingProperties);
        }

        for (RuntimeRegistration registration : registrations) {
            registerRuntimeDefinition(registration);
        }
    }

    public BlockType getType(Identifier id) {
        return typeMap.get(id);
    }

    public Set<Identifier> getTypeIdentifiers(BlockType type) {
        Set<Identifier> identifiers = type2identifierMap.get(type);
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
    public boolean isRegistered(BlockDefinition definition) {
        return definition instanceof CloudBlockDefinition cloudDefinition
                && this.runtimeDefinitionMap.get(cloudDefinition.getRuntimeId()) == cloudDefinition;
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

    private void registerRuntimeDefinition(RuntimeRegistration registration) {
        BlockState state = registration.state();
        BlockPropertyData.StateData stateData = registration.stateData();
        CloudBlockDefinition definition = new CloudBlockDefinition(
                state,
                registration.serializedState(),
                registration.runtimeId(),
                registration.blockStateHash(),
                stateData.translationKey()
        );

        VoxelShape collision = CloudVoxelShapes.fromBoxes(stateData.collisionBoxes());
        VoxelShape outline = stateData.outlineShape() == null ? collision : CloudVoxelShapes.fromBoxes(stateData.outlineShape());

        BlockRegistrationAccess.bindData(state, new BlockRegistrationAccess.Properties(
                collision, outline, stateData.hardness(), stateData.explosionResistance(),
                stateData.friction(), stateData.translucency(), stateData.thickness(), stateData.burnOdds(),
                stateData.flameOdds(), stateData.lightDampening(), stateData.lightEmission(), stateData.solid(),
                stateData.requiresCorrectToolForDrops(), parseMapColor(stateData.mapColor()),
                stateData.canContainLiquidSource(), stateData.liquidReactionOnTouch()));

        this.runtimeDefinitionMap.put(registration.runtimeId(), definition);
        this.stateDefinitionMap.putIfAbsent(state, definition);
        this.identifierFirstDefinitionMap.putIfAbsent(Identifier.parse(registration.vanillaEntry().getString("name")), definition);
    }

    private static IllegalStateException missingBlockProperties(List<MissingBlockProperty> missingProperties) {
        long typeCount = missingProperties.stream()
                .map(missing -> missing.state().getType())
                .distinct()
                .count();
        String examples = missingProperties.stream()
                .limit(10)
                .map(MissingBlockProperty::toString)
                .collect(Collectors.joining(", "));
        return new IllegalStateException(
                "Block property data is incompatible with the vanilla palette: "
                        + missingProperties.size() + " state(s) across " + typeCount
                        + " block type(s) are missing. Update the data export. Examples: " + examples);
    }

    private static Color parseMapColor(String value) {
        Preconditions.checkArgument(value.length() == 9 && value.charAt(0) == '#',
                "Invalid map colour: %s", value);
        return new Color(Integer.parseInt(value.substring(1, 3), 16),
                Integer.parseInt(value.substring(3, 5), 16),
                Integer.parseInt(value.substring(5, 7), 16),
                Integer.parseInt(value.substring(7, 9), 16));
    }

    private record MissingBlockProperty(BlockState state, long blockStateHash) {

        @Override
        public @NonNull String toString() {
            return this.state + " (state hash " + Long.toUnsignedString(this.blockStateHash) + ')';
        }
    }

    private record RuntimeRegistration(
            int runtimeId,
            NbtMap vanillaEntry,
            NbtMap serializedState,
            BlockState state,
            long blockStateHash,
            BlockPropertyData.StateData stateData
    ) {
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
