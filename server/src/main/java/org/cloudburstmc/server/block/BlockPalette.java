package org.cloudburstmc.server.block;

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockRegistrationAccess;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.blockstateupdater.BlockStateUpdaters;
import org.cloudburstmc.nbt.*;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.cloudburstmc.protocol.common.DefinitionRegistry;
import org.cloudburstmc.server.block.serializer.BlockSerializer;
import org.cloudburstmc.server.block.util.BlockStateHash;
import org.cloudburstmc.server.registry.RegistryUtils;
import org.cloudburstmc.server.registry.VanillaRegistryDiagnostics;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Resolves registered states to their named serialization, geometry and Bedrock definitions.
 */
@NoArgsConstructor
public class BlockPalette implements DefinitionRegistry<BlockDefinition> {

    public static final BlockPalette INSTANCE = new BlockPalette();

    private final Map<Identifier, SerializedStates> blocks = new LinkedHashMap<>();
    private final Map<NbtMap, BlockState> statesByTag = new LinkedHashMap<>();
    private final Map<BlockState, NbtMap> tagsByState = new IdentityHashMap<>();
    private final Map<BlockState, Identifier> identifiersByState = new IdentityHashMap<>();
    private final Map<BlockType, Set<Identifier>> identifiersByType = new IdentityHashMap<>();

    private final Int2ReferenceMap<CloudBlockDefinition> runtimeDefinitions = new Int2ReferenceOpenHashMap<>();
    private final Map<BlockState, CloudBlockDefinition> stateDefinitions = new IdentityHashMap<>();
    private final Map<Identifier, CloudBlockDefinition> firstDefinitions = new LinkedHashMap<>();
    private final Map<BlockState, BlockGeometryData> geometryByState = new IdentityHashMap<>();

    private boolean initialized;

    public void addBlock(BlockType type, BlockSerializer serializer) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(serializer, "serializer");

        if (this.initialized) {
            throw new IllegalStateException("Block palette registration is closed");
        }

        if (this.blocks.containsKey(type.getId()) || this.identifiersByType.containsKey(type)) {
            throw new IllegalArgumentException("Duplicate block type: " + type.getId());
        }

        List<SerializedRegistration> registrations = new ArrayList<>(type.getStates().size());
        Set<NbtMap> serializedStates = new HashSet<>();
        for (BlockState state : type.getStates()) {
            NbtMap tag = serialize(state, serializer);
            Identifier identifier = Identifier.parse(tag.getString("name"));
            NbtMap key = stateKey(tag);
            SerializedStates existing = this.blocks.get(identifier);

            if (existing != null && existing.type != type) {
                throw new IllegalArgumentException("Block identifier already belongs to another type: " + identifier);
            }

            if (!serializedStates.add(key) || this.statesByTag.containsKey(key)) {
                throw new IllegalArgumentException("Duplicate serialized block state: " + key);
            }

            registrations.add(new SerializedRegistration(identifier, state, tag, key));
        }

        Set<Identifier> identifiers = new LinkedHashSet<>();
        for (SerializedRegistration registration : registrations) {
            Identifier identifier = registration.identifier();
            BlockState state = registration.state();
            NbtMap properties = registration.key().getCompound("states");

            this.blocks.computeIfAbsent(identifier, ignored -> new SerializedStates(type)).add(state, properties);
            this.statesByTag.put(registration.key(), state);
            this.tagsByState.put(state, registration.tag());
            this.identifiersByState.put(state, identifier);
            identifiers.add(identifier);
        }

        this.identifiersByType.put(type, Collections.unmodifiableSet(identifiers));
    }

    public void generateRuntimeIds() {
        if (this.initialized) {
            throw new IllegalStateException("Block palette runtime definitions are already initialized");
        }

        List<NbtMap> vanillaPalette = loadVanillaPalette();
        List<RuntimeRegistration> registrations = new ArrayList<>(vanillaPalette.size());
        List<String> missingProperties = new ArrayList<>();
        Set<BlockType> missingTypes = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<BlockState> runtimeStates = Collections.newSetFromMap(new IdentityHashMap<>());

        for (int runtimeId = 0; runtimeId < vanillaPalette.size(); runtimeId++) {
            NbtMap entry = vanillaPalette.get(runtimeId);
            NbtMap key = stateKey(entry);

            BlockState state = this.statesByTag.get(key);
            if (state == null) {
                VanillaRegistryDiagnostics.missingVanillaBlockState(entry.getString("name"), entry.getCompound("states", NbtMap.EMPTY));
                continue;
            }

            if (!runtimeStates.add(state)) {
                throw new IllegalStateException("Multiple runtime definitions for block state: " + state);
            }

            long hash = Integer.toUnsignedLong(BlockStateHash.compute(entry.getString("name"), entry.getCompound("states", NbtMap.EMPTY)));
            BlockPropertyData.StateData properties = BlockPropertyData.get(hash);
            if (properties == null) {
                missingProperties.add(state + " (state hash " + Long.toUnsignedString(hash) + ')');
                missingTypes.add(state.getType());
                continue;
            }

            registrations.add(new RuntimeRegistration(runtimeId, key, state, hash, properties));
        }

        if (!missingProperties.isEmpty()) {
            throw new IllegalStateException(
                    "Block property data is incompatible with the vanilla palette: "
                            + missingProperties.size() + " state(s) across " + missingTypes.size()
                            + " block type(s) are missing. Update the data export. Examples: "
                            + missingProperties.stream().limit(10).collect(Collectors.joining(", ")));
        }

        for (RuntimeRegistration registration : registrations) {
            bindProperties(registration.state(), registration.properties());
        }

        for (RuntimeRegistration registration : registrations) {
            this.registerRuntimeDefinition(registration);
        }

        this.initialized = true;
    }

    public @Nullable BlockType getType(Identifier id) {
        SerializedStates states = this.blocks.get(id);
        return states == null ? null : states.type;
    }

    public Set<Identifier> getTypeIdentifiers(BlockType type) {
        return this.identifiersByType.getOrDefault(type, Set.of());
    }

    public @Nullable BlockState getState(Identifier id) {
        SerializedStates states = this.blocks.get(id);
        return states == null ? null : states.defaultState;
    }

    public Identifier getIdentifier(BlockState state) {
        Identifier identifier = this.identifiersByState.get(state);
        if (identifier == null) {
            throw new IllegalArgumentException("Unregistered block state: " + state);
        }
        return identifier;
    }

    public @Nullable BlockState getState(Identifier id, Map<String, Object> properties) {
        SerializedStates states = this.blocks.get(id);
        if (states == null) {
            return null;
        }

        NbtMapBuilder builder = NbtMap.builder();
        builder.putAll(properties);
        return states.states.get(builder.build());
    }

    public NbtMap getDefaultProperties(Identifier id) {
        return this.requireStates(id).defaultProperties;
    }

    public Map<NbtMap, BlockState> getStates(Identifier id) {
        return Collections.unmodifiableMap(this.requireStates(id).states);
    }

    public BlockState getBlockState(int runtimeId) {
        return this.getDefinition(runtimeId).getCloudState();
    }

    @Override
    public CloudBlockDefinition getDefinition(int runtimeId) {
        CloudBlockDefinition definition = this.runtimeDefinitions.get(runtimeId);
        if (definition == null) {
            throw new IllegalArgumentException("Invalid runtime ID: " + runtimeId);
        }
        return definition;
    }

    public @Nullable CloudBlockDefinition getFirstDefinition(Identifier id) {
        return this.firstDefinitions.get(id);
    }

    public @Nullable CloudBlockDefinition getDefinitionByStates(Identifier id, NbtMap properties) {
        BlockState state = this.getState(id, properties);
        return state == null ? null : this.stateDefinitions.get(state);
    }

    @Override
    public boolean isRegistered(BlockDefinition definition) {
        return definition instanceof CloudBlockDefinition cloudDefinition && this.runtimeDefinitions.get(cloudDefinition.getRuntimeId()) == cloudDefinition;
    }

    public @Nullable BlockState getBlockState(NbtMap tag) {
        return this.statesByTag.get(stateKey(tag));
    }

    public CloudBlockDefinition getDefinition(BlockState state) {
        CloudBlockDefinition definition = this.stateDefinitions.get(state);
        if (definition == null) {
            throw new IllegalArgumentException("Block state has no runtime definition: " + state);
        }
        return definition;
    }

    public @Nullable BlockGeometryData getGeometry(BlockState state) {
        return this.geometryByState.get(state);
    }

    public NbtMap getSerialized(BlockState state) {
        NbtMap tag = this.tagsByState.get(state);
        if (tag == null) {
            throw new IllegalArgumentException("Unregistered block state: " + state);
        }
        return tag;
    }

    public Map<NbtMap, BlockState> getSerializedPalette() {
        return Collections.unmodifiableMap(this.statesByTag);
    }

    public Map<Integer, CloudBlockDefinition> getRuntimeMap() {
        return Collections.unmodifiableMap(this.runtimeDefinitions);
    }

    private SerializedStates requireStates(Identifier id) {
        SerializedStates states = this.blocks.get(id);
        if (states == null) {
            throw new IllegalArgumentException("Unknown block identifier: " + id);
        }
        return states;
    }

    private static List<NbtMap> loadVanillaPalette() {
        try (InputStream stream = RegistryUtils.getOrAssertResource("data/block_palette.nbt");
             NBTInputStream reader = NbtUtils.createGZIPReader(stream)) {
            NbtMap root = (NbtMap) reader.readTag();
            List<NbtMap> palette = root.getList("blocks", NbtType.COMPOUND);
            if (palette.isEmpty()) {
                throw new IllegalStateException("Vanilla block palette is empty");
            }
            return palette;
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read vanilla block palette", exception);
        }
    }

    private static NbtMap serialize(BlockState state, BlockSerializer serializer) {
        NbtMapBuilder builder = NbtMap.builder();
        serializer.serialize(builder, state.getType(), state.getTraits());
        Object serializedName = builder.getOrDefault("name", state.getType().getId().toString());
        if (!(serializedName instanceof String name) || name.isBlank()) {
            throw new IllegalArgumentException("Block serializer produced an invalid name for " + state);
        }

        if (!(builder.get("states") instanceof NbtMap)) {
            throw new IllegalArgumentException("Block serializer must produce a states compound for " + state);
        }

        BlockStateUpdaters.serializeCommon(builder, name);
        return builder.build();
    }

    private static NbtMap stateKey(NbtMap tag) {
        return NbtMap.builder()
                .putString("name", tag.getString("name"))
                .putCompound("states", tag.getCompound("states", NbtMap.EMPTY))
                .build();
    }

    private static void bindProperties(BlockState state, BlockPropertyData.StateData properties) {
        BlockGeometryData geometry = properties.geometry();
        BlockRegistrationAccess.bindData(state, new BlockRegistrationAccess.Properties(
                geometry.collisionShape(),
                geometry.outlineShape(),
                properties.hardness(),
                properties.explosionResistance(),
                properties.friction(),
                properties.translucency(),
                properties.thickness(),
                properties.burnOdds(),
                properties.flameOdds(),
                properties.lightDampening(),
                properties.lightEmission(),
                properties.solid(),
                properties.requiresCorrectToolForDrops(),
                BlockMapColors.resolve(properties.mapColor(), properties.tintMethod(), state.getType()),
                properties.canContainLiquidSource(),
                properties.liquidReactionOnTouch()
        ));
    }

    private void registerRuntimeDefinition(RuntimeRegistration registration) {
        CloudBlockDefinition definition = new CloudBlockDefinition(
                registration.state(),
                registration.key(),
                registration.runtimeId(),
                registration.hash(),
                registration.properties().translationKey()
        );

        this.runtimeDefinitions.put(registration.runtimeId(), definition);
        this.stateDefinitions.put(registration.state(), definition);
        this.geometryByState.put(registration.state(), registration.properties().geometry());
        this.firstDefinitions.putIfAbsent(this.identifiersByState.get(registration.state()), definition);
    }

    @RequiredArgsConstructor
    private static class SerializedStates {

        private final BlockType type;
        private final Map<NbtMap, BlockState> states = new LinkedHashMap<>();
        private BlockState defaultState;
        private NbtMap defaultProperties;

        private void add(BlockState state, NbtMap properties) {
            this.states.put(properties, state);
            if (this.defaultState == null || state == this.type.getDefaultState()) {
                this.defaultState = state;
                this.defaultProperties = properties;
            }
        }
    }

    private record SerializedRegistration(Identifier identifier, BlockState state, NbtMap tag, NbtMap key) {
    }

    private record RuntimeRegistration(int runtimeId, NbtMap key, BlockState state, long hash, BlockPropertyData.StateData properties) {
    }
}
