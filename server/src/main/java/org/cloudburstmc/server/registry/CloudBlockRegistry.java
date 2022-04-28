package org.cloudburstmc.server.registry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.nukkitx.blockstateupdater.BlockStateUpdaters;
import com.nukkitx.nbt.NbtList;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtType;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.data.BehaviorKey;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.api.registry.GlobalRegistry;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.behavior.Behavior;
import org.cloudburstmc.api.util.behavior.BehaviorCollection;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.block.serializer.*;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers;
import org.cloudburstmc.server.block.util.BlockStateMetaMappings;
import org.cloudburstmc.server.registry.behavior.CloudBehaviorCollection;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.cloudburstmc.api.block.BlockTypes.*;

@Log4j2
public class CloudBlockRegistry extends CloudBehaviorRegistry<BlockType> implements BlockRegistry, Registry {
    public static CloudBlockRegistry REGISTRY;
    private static final HashBiMap<Identifier, Integer> VANILLA_LEGACY_IDS = HashBiMap.create();

    static {
        InputStream stream = RegistryUtils.getOrAssertResource("data/legacy_block_ids.json");

        try {
            VANILLA_LEGACY_IDS.putAll(Bootstrap.JSON_MAPPER.readValue(stream, new TypeReference<Map<Identifier, Integer>>() {}));
        } catch (IOException e) {
            throw new AssertionError("Unable to load legacy IDs", e);
        }
    }

    private final Reference2ReferenceMap<BlockType, BehaviorCollection> behaviorMap = new Reference2ReferenceOpenHashMap<>();
    //private final HashBiMap<Identifier, Integer> idLegacyMap = HashBiMap.create();
    private final AtomicInteger customIdAllocator = new AtomicInteger(1000);
    private final BlockPalette palette = BlockPalette.INSTANCE;
    private NbtMap propertiesTag;
    private volatile boolean closed;
    private transient NbtList<NbtMap> serializedPalette;
    private final CloudItemRegistry itemRegistry;

    public CloudBlockRegistry(CloudItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
        BlockTraitSerializers.init();
        this.registerVanillaBlocks();

        // Check legacy IDs
        behaviorMap.forEach((bt, b) -> {
            if (!VANILLA_LEGACY_IDS.containsKey(bt.getId())) {
                log.debug("Unable to map legacy id for blockType: {}", bt.getId());
            }
        });
        REGISTRY = this; // TODO: Remove at some point
    }

    @Override
    public synchronized CloudBehaviorCollection register(BlockType type) throws RegistryException {
        checkNotNull(type, "type");
        CloudBehaviorCollection behaviors = registerVanilla(type);

        // generate legacy ID (Not sure why we need to but it's a requirement)
        int legacyId = this.customIdAllocator.getAndIncrement();
        VANILLA_LEGACY_IDS.put(type.getId(), legacyId);
        return behaviors;
    }

    @Override
    public <F, E> void registerBehavior(BehaviorKey<F, E> key, F defaultBehavior, BiFunction<Behavior<E>, F, E> executorFactory) {
        checkArgument(this.isBehaviorRegistered(key), "Behaviour '%s' already registered", key);
        checkArgument(this.itemRegistry.isBehaviorRegistered(key), "Item Behaviour '%s' already registered", key);
        super.registerBehavior(key, defaultBehavior, executorFactory);
    }

    @Override
    public GlobalRegistry global() {
        return null;
    }

    @Override
    public BehaviorCollection getBehaviors(BlockType type) {
        return behaviorMap.get(type);
    }

    private CloudBehaviorCollection registerVanilla(BlockType type) throws RegistryException {
        return this.registerVanilla(type, DefaultBlockSerializer.INSTANCE);
    }

    private synchronized CloudBehaviorCollection registerVanilla(BlockType type, BlockSerializer serializer) throws RegistryException {
        checkNotNull(type, "type");
        checkNotNull(serializer, "serializer");
        checkClosed();

        CloudBehaviorCollection collection = new CloudBehaviorCollection(this);

        synchronized (this.behaviorMap) {
            if (this.behaviorMap.putIfAbsent(type, collection) != null) {
                throw new RegistryException(type + " is already registered");
            }
        }

        this.palette.addBlock(type, serializer);

        return collection;
    }

    @Override
    public boolean isBlock(Identifier id) {
        return VANILLA_LEGACY_IDS.containsKey(id);
    }

    public int getRuntimeId(BlockState blockState) {
        return this.palette.getRuntimeId(blockState);
    }

    public int getRuntimeId(Identifier identifier, int meta) {
        NbtMap tag = NbtMap.builder()
                .putString("name", identifier.toString())
                .putShort("val", (short) meta)
                .putInt("version", 0)
                .build();

        tag = BlockStateUpdaters.updateBlockState(tag, 0);

        return palette.getRuntimeId(palette.getBlockState(tag));
    }

    public int getRuntimeId(int id, int meta) {
        return getRuntimeId(VANILLA_LEGACY_IDS.inverse().get(id), meta);
    }

    public BlockState getBlock(BlockType type) {
        return palette.getDefaultState(type);
    }

    public BlockState getBlock(ItemStack item) {
        return BlockStateMetaMappings.getStateFromMeta(item);
    }

    public BlockState getBlock(Identifier identifier) {
        return getBlock(identifier, 0);
    }

    public BlockState getBlock(Identifier identifier, int meta) {
        return getBlock(getRuntimeId(identifier, meta));
    }

    public BlockState getBlock(int id, int meta) {
        return getBlock(getRuntimeId(id, meta));
    }

    public BlockState getBlock(int runtimeId) {
        BlockState blockBehavior = this.palette.getBlockState(runtimeId);
        if (blockBehavior == null) {
            throw new RegistryException("No block for runtime ID " + runtimeId + " registered");
        }
        return blockBehavior;
    }

    public BlockState getBlock(NbtMap tag) {
        BlockState state;

        if (!tag.containsKey("states", NbtType.COMPOUND)) {
            tag = tag.toBuilder().putCompound("states", NbtMap.EMPTY).build();
        }

        state = palette.getBlockState(tag);

        if (state == null) {
            tag = BlockStateUpdaters.updateBlockState(tag, tag.getInt("version"));
            state = palette.getBlockState(tag);
        }

        if (state == null/* && tag.containsKey("states", NbtType.COMPOUND)*/) { //TODO: fix unknown states
            var defaultState = getBlock(Identifier.fromString(tag.getString("name")));
            var serialized = palette.getSerialized(defaultState);

            if (serialized.containsKey("states", NbtType.COMPOUND)) {
                var builder = tag.toBuilder();

                var statesBuilder = ((NbtMap) builder.get("states")).toBuilder();
                serialized.getCompound("states").forEach(statesBuilder::putIfAbsent);
                builder.putCompound("states", statesBuilder.build());
                state = palette.getBlockState(builder.build());
            }
        }

        if (state == null) throw new IllegalStateException("Invalid block state\n" + tag);

        return state;
    }

    public int getLegacyId(String name) {
        return getLegacyId(Identifier.fromString(name));
    }

    public int getLegacyId(Identifier identifier) {
        int legacyId = VANILLA_LEGACY_IDS.getOrDefault(identifier, -1);
        if (legacyId == -1) {
            throw new RegistryException("No legacy ID found for " + identifier);
        }
        return legacyId;
    }

    public Identifier getNameFromLegacyId(int id) {
        Identifier identifier = VANILLA_LEGACY_IDS.inverse().get(id);
        if (identifier == null) {
            throw new RegistryException("No block found for ID " + id);
        }
        return identifier;
    }

    @Override
    public synchronized void close() throws RegistryException {
        checkClosed();
        this.closed = true;
        this.palette.generateRuntimeIds();
        // generate cache

        this.propertiesTag = NbtMap.EMPTY;
    }

    private void checkClosed() throws RegistryException {
        if (this.closed) {
            throw new RegistryException("Registration has been closed");
        }
    }

    public NbtList<NbtMap> getPaletteTag() {
        if (this.serializedPalette != null) {
            return serializedPalette;
        }
        Map<NbtMap, BlockState> palette = this.palette.getSerializedPalette();
        List<NbtMap> serialized = new ArrayList<>(palette.size());
        palette.forEach((serializedState, state) -> {
            serialized.add(NbtMap.builder()
                    .putCompound("block", serializedState)
                    .putShort("id", (short) this.getLegacyId(state.getType().getId()))
                    .build());
        });

        this.serializedPalette = new NbtList<>(NbtType.COMPOUND, serialized);
        return this.serializedPalette;
    }

    public NbtMap getPropertiesTag() {
        return propertiesTag;
    }

    public ImmutableList<BlockState> getBlockStates() {
        return ImmutableList.copyOf(palette.getSerializedPalette().values());
    }

    private void registerVanillaBlocks() {
        this.registerVanilla(AIR); // 0
        this.registerVanilla(STONE); // 1
        this.registerVanilla(GRASS); // 2
        this.registerVanilla(DIRT); // 3
        this.registerVanilla(COBBLESTONE); // 4
        this.registerVanilla(PLANKS, MultiBlockSerializers.PLANKS); // 5
        this.registerVanilla(SAPLING); // 6
        this.registerVanilla(BEDROCK); // 7
        this.registerVanilla(FLOWING_WATER, FluidBlockSerializer.INSTANCE); // 8
        this.registerVanilla(WATER, FluidBlockSerializer.INSTANCE); // 9
        this.registerVanilla(FLOWING_LAVA, FluidBlockSerializer.INSTANCE); //10
        this.registerVanilla(LAVA, FluidBlockSerializer.INSTANCE); //11
        this.registerVanilla(SAND); //12
        this.registerVanilla(GRAVEL); //13
        this.registerVanilla(GOLD_ORE); //14
        this.registerVanilla(IRON_ORE); //15
        this.registerVanilla(COAL_ORE); //16
        this.registerVanilla(LOG, MultiBlockSerializers.LOG); //17
        this.registerVanilla(LEAVES, MultiBlockSerializers.LEAVES); //18
        this.registerVanilla(SPONGE); //19
        this.registerVanilla(GLASS); //20
        this.registerVanilla(LAPIS_ORE); //21
        this.registerVanilla(LAPIS_BLOCK); //22
        this.registerVanilla(DISPENSER); //23
        this.registerVanilla(SANDSTONE); //24
        this.registerVanilla(NOTEBLOCK); //25
        this.registerVanilla(BED); //26
        this.registerVanilla(GOLDEN_RAIL); //27
        this.registerVanilla(DETECTOR_RAIL); //28
        this.registerVanilla(WEB); //30
        this.registerVanilla(TALL_GRASS); //31
        this.registerVanilla(DEADBUSH); //32
        this.registerVanilla(PISTON, MultiBlockSerializers.PISTON); //33
        this.registerVanilla(PISTON_ARM_COLLISION, MultiBlockSerializers.PISTON_ARM_COLLISION); //34
        this.registerVanilla(WOOL); //35
        this.registerVanilla(FLOWER, MultiBlockSerializers.FLOWER); //37 - 38
        this.registerVanilla(BROWN_MUSHROOM); //39
        this.registerVanilla(RED_MUSHROOM); //40
        this.registerVanilla(GOLD_BLOCK); //41
        this.registerVanilla(IRON_BLOCK); //42
        this.registerVanilla(STONE_SLAB,SlabSerializer.INSTANCE); //44
        this.registerVanilla(BRICK_BLOCK); //45
        this.registerVanilla(TNT); //46
        this.registerVanilla(BOOKSHELF); //47
        this.registerVanilla(MOSSY_COBBLESTONE); //48
        this.registerVanilla(OBSIDIAN); //49
        this.registerVanilla(TORCH, MultiBlockSerializers.TORCH); //50
        this.registerVanilla(FIRE); //51
        this.registerVanilla(MOB_SPAWNER); //52
        this.registerVanilla(WOODEN_STAIRS, MultiBlockSerializers.WOOD_STAIRS); //53
        this.registerVanilla(CHEST); //54
        this.registerVanilla(REDSTONE_WIRE); //55
        this.registerVanilla(DIAMOND_ORE); //56
        this.registerVanilla(DIAMOND_BLOCK); //57
        this.registerVanilla(CRAFTING_TABLE); //58
        this.registerVanilla(WHEAT); //59
        this.registerVanilla(FARMLAND); //60
        this.registerVanilla(FURNACE, MultiBlockSerializers.FURNACE); //61
        this.registerVanilla(STANDING_SIGN, MultiBlockSerializers.WOOD_STANDING_SIGN); //63
        this.registerVanilla(WOODEN_DOOR, MultiBlockSerializers.WOOD_DOOR); //64
        this.registerVanilla(LADDER); //65
        this.registerVanilla(RAIL); //66
        this.registerVanilla(STONE_STAIRS, MultiBlockSerializers.STONE_STAIRS); //67
        this.registerVanilla(WALL_SIGN, MultiBlockSerializers.WOOD_WALL_SIGN); //68
        this.registerVanilla(LEVER); //69
        this.registerVanilla(STONE_PRESSURE_PLATE, MultiBlockSerializers.STONE_PRESSURE_PLATE); //70
        this.registerVanilla(IRON_DOOR); //71
        this.registerVanilla(WOODEN_PRESSURE_PLATE, MultiBlockSerializers.WOOD_PRESSURE_PLATE); //72
        this.registerVanilla(REDSTONE_ORE, MultiBlockSerializers.REDSTONE_ORE); //73, 74
        this.registerVanilla(REDSTONE_TORCH, MultiBlockSerializers.REDSTONE_TORCH); //75, 76
        this.registerVanilla(STONE_BUTTON, MultiBlockSerializers.STONE_BUTTON); //77
        this.registerVanilla(SNOW_LAYER); //78
        this.registerVanilla(ICE); //79
        this.registerVanilla(SNOW); //80
        this.registerVanilla(CACTUS); //81
        this.registerVanilla(CLAY); //82
        this.registerVanilla(REEDS); //83
        this.registerVanilla(JUKEBOX); //84
        this.registerVanilla(WOODEN_FENCE, MultiBlockSerializers.WOOD_FENCE); //85
        this.registerVanilla(PUMPKIN); //86
        this.registerVanilla(NETHERRACK); //87
        this.registerVanilla(SOUL_SAND); //88
        this.registerVanilla(GLOWSTONE); //89
        this.registerVanilla(PORTAL); //90
        this.registerVanilla(LIT_PUMPKIN); //91
        this.registerVanilla(CAKE); //92
        this.registerVanilla(REPEATER, MultiBlockSerializers.REPEATER); //93, 94
        this.registerVanilla(INVISIBLE_BEDROCK); //95
        this.registerVanilla(WOODEN_TRAPDOOR, MultiBlockSerializers.WOOD_TRAPDOOR); //96
        this.registerVanilla(MONSTER_EGG); //97
        this.registerVanilla(STONEBRICK); //98
        this.registerVanilla(BROWN_MUSHROOM_BLOCK); //99
        this.registerVanilla(RED_MUSHROOM_BLOCK); //100
        this.registerVanilla(IRON_BARS); //101
        this.registerVanilla(GLASS_PANE); //102
        this.registerVanilla(MELON_BLOCK); //103
        this.registerVanilla(PUMPKIN_STEM); //104
        this.registerVanilla(MELON_STEM); //105
        this.registerVanilla(VINE); //106
        this.registerVanilla(WOODEN_FENCE_GATE, MultiBlockSerializers.WOOD_FENCE_GATE); //107
        this.registerVanilla(MYCELIUM); //110
        this.registerVanilla(WATERLILY); // 111
        this.registerVanilla(NETHER_BRICK, MultiBlockSerializers.NETHER_BRICKS); //112
        this.registerVanilla(NETHER_BRICK_FENCE); //113
        this.registerVanilla(NETHER_WART); //115
        this.registerVanilla(ENCHANTING_TABLE); //116
        this.registerVanilla(BREWING_STAND); //117
        this.registerVanilla(CAULDRON, MultiBlockSerializers.CAULDRON); //118
        this.registerVanilla(END_PORTAL); //119
        this.registerVanilla(END_PORTAL_FRAME); //120
        this.registerVanilla(END_STONE); //121
        this.registerVanilla(DRAGON_EGG); //122
        this.registerVanilla(REDSTONE_LAMP, MultiBlockSerializers.REDSTONE_LAMP); //123
        this.registerVanilla(DROPPER);
        this.registerVanilla(ACTIVATOR_RAIL); //126
        this.registerVanilla(COCOA); //127
        this.registerVanilla(EMERALD_ORE); //129
        this.registerVanilla(ENDER_CHEST); //130
        this.registerVanilla(TRIPWIRE_HOOK); //131
        this.registerVanilla(TRIP_WIRE); //132
        this.registerVanilla(EMERALD_BLOCK); //133
        this.registerVanilla(COMMAND_BLOCK);
        this.registerVanilla(BEACON); //138
        this.registerVanilla(STONE_WALL, MultiBlockSerializers.WALL); //139
        this.registerVanilla(FLOWER_POT); //140
        this.registerVanilla(CARROTS); //141
        this.registerVanilla(POTATOES); //142
        this.registerVanilla(WOODEN_BUTTON, MultiBlockSerializers.WOOD_BUTTON); //143
        this.registerVanilla(SKULL); //144
        this.registerVanilla(ANVIL); //145
        this.registerVanilla(TRAPPED_CHEST); //146
        this.registerVanilla(LIGHT_WEIGHTED_PRESSURE_PLATE); //147
        this.registerVanilla(HEAVY_WEIGHTED_PRESSURE_PLATE); //148
        this.registerVanilla(COMPARATOR, MultiBlockSerializers.COMPARATOR); //150
        this.registerVanilla(DAYLIGHT_DETECTOR); //151
        this.registerVanilla(REDSTONE_BLOCK); //152
        this.registerVanilla(QUARTZ_ORE); //153
        this.registerVanilla(HOPPER); //154
        this.registerVanilla(QUARTZ_BLOCK); //155
        this.registerVanilla(WOODEN_SLAB, SlabSerializer.INSTANCE); //158
        this.registerVanilla(STAINED_HARDENED_CLAY); //159
        this.registerVanilla(STAINED_GLASS_PANE); //160
        this.registerVanilla(SLIME); //165
        //166: glow_stick
        this.registerVanilla(IRON_TRAPDOOR); //167
        this.registerVanilla(PRISMARINE); //168
        this.registerVanilla(SEA_LANTERN); //169
        this.registerVanilla(HAY_BLOCK); //170
        this.registerVanilla(CARPET); //171
        this.registerVanilla(HARDENED_CLAY); //172
        this.registerVanilla(COAL_BLOCK); //173
        this.registerVanilla(PACKED_ICE); //174
        this.registerVanilla(DOUBLE_PLANT); //175
        this.registerVanilla(STANDING_BANNER); //176
        this.registerVanilla(WALL_BANNER); //177
        this.registerVanilla(DAYLIGHT_DETECTOR_INVERTED); //178
        this.registerVanilla(RED_SANDSTONE); //179
        this.registerVanilla(REPEATING_COMMAND_BLOCK); //188
        this.registerVanilla(CHAIN_COMMAND_BLOCK); //189
        this.registerVanilla(HARD_GLASS_PANE);
        this.registerVanilla(HARD_STAINED_GLASS_PANE);
        this.registerVanilla(CHEMICAL_HEAT);
        this.registerVanilla(GRASS_PATH); //198
        this.registerVanilla(FRAME, MultiBlockSerializers.FRAME); //199
        this.registerVanilla(CHORUS_FLOWER); //200
        this.registerVanilla(PURPUR_BLOCK); //201
        this.registerVanilla(COLORED_TORCH_RG);
        this.registerVanilla(COLORED_TORCH_BP);
        this.registerVanilla(UNDYED_SHULKER_BOX); //205
        this.registerVanilla(END_BRICKS); //206
        this.registerVanilla(FROSTED_ICE); //206
        this.registerVanilla(END_ROD); //208
        this.registerVanilla(END_GATEWAY); //209
        this.registerVanilla(ALLOW); //210
        this.registerVanilla(DENY);
        this.registerVanilla(BORDER_BLOCK);
        this.registerVanilla(MAGMA); //213
        this.registerVanilla(NETHER_WART_BLOCK, MultiBlockSerializers.WART_BLOCK); //214
        this.registerVanilla(BONE_BLOCK); //216
        this.registerVanilla(STRUCTURE_VOID);
        this.registerVanilla(SHULKER_BOX); //218
        this.registerVanilla(GLAZED_TERRACOTTA, MultiBlockSerializers.TERRACOTTA); //219 - 235
        this.registerVanilla(CONCRETE); //236
        this.registerVanilla(CONCRETE_POWDER); //237
        this.registerVanilla(CHEMISTRY_TABLE);
        this.registerVanilla(UNDERWATER_TORCH);
        this.registerVanilla(CHORUS_PLANT); //240
        this.registerVanilla(STAINED_GLASS); //241
        this.registerVanilla(CAMERA);
        this.registerVanilla(PODZOL); //243
        this.registerVanilla(BEETROOT); //244
        this.registerVanilla(STONECUTTER); //245
        this.registerVanilla(GLOWING_OBSIDIAN); //246
        this.registerVanilla(NETHER_REACTOR);
        this.registerVanilla(INFO_UPDATE);
        this.registerVanilla(INFO_UPDATE2);
        this.registerVanilla(MOVING_BLOCK);
        this.registerVanilla(OBSERVER); //251
        this.registerVanilla(STRUCTURE_BLOCK); //252
        this.registerVanilla(HARD_GLASS);
        this.registerVanilla(HARD_STAINED_GLASS);
        this.registerVanilla(RESERVED6);
        this.registerVanilla(PACKED_MUD);
        this.registerVanilla(MUD_BRICKS);
        this.registerVanilla(MUD);
//        this.registerVanilla(MUD_BRICK_WALL); // Shouldn't this be in the StoneSlabType class?
        //256: unknown
        this.registerVanilla(BLUE_ICE); //266
        this.registerVanilla(ELEMENT, MultiBlockSerializers.ELEMENT);
        this.registerVanilla(SEAGRASS);
        this.registerVanilla(CORAL);
        this.registerVanilla(CORAL_BLOCK);
        this.registerVanilla(CORAL_FAN);
        this.registerVanilla(CORAL_FAN_DEAD);
        this.registerVanilla(CORAL_FAN_HANG, new CoralHangBlockSerializer());
        this.registerVanilla(KELP);//393
        this.registerVanilla(DRIED_KELP_BLOCK); //394
        this.registerVanilla(CARVED_PUMPKIN);
        this.registerVanilla(SEA_PICKLE);
        this.registerVanilla(CONDUIT);
        this.registerVanilla(TURTLE_EGG);
        this.registerVanilla(BUBBLE_COLUMN);
        this.registerVanilla(BARRIER); //415
        this.registerVanilla(BAMBOO);
        this.registerVanilla(BAMBOO_SAPLING);
        this.registerVanilla(SCAFFOLDING);
        this.registerVanilla(SMOOTH_STONE); // 437
        this.registerVanilla(LECTERN); //448
        this.registerVanilla(GRINDSTONE);
        this.registerVanilla(BLAST_FURNACE, MultiBlockSerializers.BLAST_FURNACE); // 450
        this.registerVanilla(STONECUTTER_BLOCK); // 451
        this.registerVanilla(SMOKER, MultiBlockSerializers.SMOKER); //452
        this.registerVanilla(CARTOGRAPHY_TABLE); //454
        this.registerVanilla(FLETCHING_TABLE); //455
        this.registerVanilla(SMITHING_TABLE); //456
        this.registerVanilla(BARREL); // 457
        this.registerVanilla(LOOM);
        this.registerVanilla(BELL);
        this.registerVanilla(SWEET_BERRY_BUSH);
        this.registerVanilla(LANTERN, MultiBlockSerializers.LANTERN);
        this.registerVanilla(CAMPFIRE, MultiBlockSerializers.CAMPFIRE);//464
        this.registerVanilla(JIGSAW, new JigsawSerializer());//466
        this.registerVanilla(WOOD, MultiBlockSerializers.WOOD); //467
        this.registerVanilla(COMPOSTER);//468
        this.registerVanilla(LIGHT_BLOCK);//470
        this.registerVanilla(WITHER_ROSE);//471
        this.registerVanilla(BEE_NEST);//473
        this.registerVanilla(BEEHIVE);//474
        this.registerVanilla(HONEY_BLOCK);//475
        this.registerVanilla(HONEYCOMB_BLOCK); // 476
        this.registerVanilla(LODESTONE);//477
        this.registerVanilla(NETHER_ROOTS, MultiBlockSerializers.ROOTS);//478
        this.registerVanilla(NETHER_FUNGUS, MultiBlockSerializers.FUNGUS);//482
        this.registerVanilla(SHROOMLIGHT);//484
        this.registerVanilla(WEEPING_VINES);//485
        this.registerVanilla(NETHER_NYLIUM, MultiBlockSerializers.NYLIUM);//486
        this.registerVanilla(BASALT);//490
        this.registerVanilla(POLISHED_BASALT);//590
        this.registerVanilla(SOUL_SOIL);//491
        this.registerVanilla(SOUL_FIRE);//492
        this.registerVanilla(NETHER_SPROUTS);//493
        this.registerVanilla(TARGET);//494
        this.registerVanilla(NETHERITE_BLOCK);//525
        this.registerVanilla(ANCIENT_DEBRIS);//526
        this.registerVanilla(RESPAWN_ANCHOR);//527
        this.registerVanilla(BLACKSTONE);//528
        this.registerVanilla(POLISHED_BLACKSTONE_BRICKS);//529
        this.registerVanilla(CHISELED_POLISHED_BLACKSTONE);//534
        this.registerVanilla(CRACKED_POLISHED_BLACKSTONE_BRICKS);//535
        this.registerVanilla(GILDED_BLACKSTONE);//536
        this.registerVanilla(CHAIN);//541
        this.registerVanilla(TWISTING_VINES);//542
        this.registerVanilla(NETHER_GOLD_ORE);//543
        this.registerVanilla(CRYING_OBSIDIAN);//544
        this.registerVanilla(POLISHED_BLACKSTONE);//546
        this.registerVanilla(QUARTZ_BRICKS);//559
        this.registerVanilla(UNKNOWN); //560

        this.registerVanilla(POWDER_SNOW);
        this.registerVanilla(SCULK_SENSOR);
        this.registerVanilla(POINTED_DRIPSTONE);
        this.registerVanilla(COPPER_ORE);
        this.registerVanilla(LIGHTNING_ROD);
        this.registerVanilla(DRIPSTONE_BLOCK);
        this.registerVanilla(DIRT_WITH_ROOTS);
        this.registerVanilla(HANGING_ROOTS);
        this.registerVanilla(MOSS_BLOCK);
        this.registerVanilla(SPORE_BLOSSOM);
        this.registerVanilla(BIG_DRIPLEAF);
        this.registerVanilla(AZALEA_LEAVES, MultiBlockSerializers.AZALEA_LEAVES);
        this.registerVanilla(CALCITE);
        this.registerVanilla(AMETHYST_BLOCK);
        this.registerVanilla(BUDDING_AMETHYST);
        this.registerVanilla(AMETHYST_CLUSTER, MultiBlockSerializers.AMETHYST_CLUSTER);
        this.registerVanilla(TUFF);
        this.registerVanilla(TINTED_GLASS);
        this.registerVanilla(MOSS_CARPET);
        this.registerVanilla(SMALL_DRIPLEAF);
        this.registerVanilla(AZALEA, MultiBlockSerializers.AZALEA);
        this.registerVanilla(COPPER, MultiBlockSerializers.COPPER_BLOCKS);
        this.registerVanilla(CUT_COPPER, MultiBlockSerializers.CUT_COPPER);
        this.registerVanilla(COPPER_SLAB, SlabSerializer.INSTANCE);
        this.registerVanilla(COPPER_STAIRS, MultiBlockSerializers.COPPER_STAIRS);
        this.registerVanilla(CAVE_VINES, MultiBlockSerializers.CAVE_VINES);
        this.registerVanilla(SMOOTH_BASALT);
        this.registerVanilla(DEEPSLATE, MultiBlockSerializers.DEEPSLATE);
        this.registerVanilla(COBBLED_DEEPSLATE);
        this.registerVanilla(POLISHED_DEEPSLATE);
        this.registerVanilla(DEEPSLATE_TILES);
        this.registerVanilla(DEEPSLATE_BRICKS);
        this.registerVanilla(CHISELED_DEEPSLATE);
        this.registerVanilla(DEEPSLATE_LAPIS_ORE);
        this.registerVanilla(DEEPSLATE_IRON_ORE);
        this.registerVanilla(DEEPSLATE_GOLD_ORE);
        this.registerVanilla(DEEPSLATE_REDSTONE_ORE, MultiBlockSerializers.DEEPSLATE_REDSTONE_ORE);
        this.registerVanilla(DEEPSLATE_DIAMOND_ORE);
        this.registerVanilla(DEEPSLATE_COAL_ORE);
        this.registerVanilla(DEEPSLATE_EMERALD_ORE);
        this.registerVanilla(DEEPSLATE_COPPER_ORE);
        this.registerVanilla(CRACKED_DEEPSLATE_TILES);
        this.registerVanilla(CRACKED_DEEPSLATE_BRICKS);
        this.registerVanilla(GLOW_LICHEN);
        this.registerVanilla(CANDLE, MultiBlockSerializers.CANDLES);
        this.registerVanilla(CANDLE_CAKE, MultiBlockSerializers.CANDLE_CAKES);
        this.registerVanilla(RAW_IRON_BLOCK);
        this.registerVanilla(RAW_COPPER_BLOCK);
        this.registerVanilla(RAW_GOLD_BLOCK);

        this.registerVanilla(CANDLE, MultiBlockSerializers.CANDLES);
        this.registerVanilla(CANDLE_CAKE, MultiBlockSerializers.CANDLE_CAKES);

        this.registerVanilla(SCULK);
        this.registerVanilla(SCULK_VEIN);
        this.registerVanilla(SCULK_CATALYST);
        this.registerVanilla(SCULK_SHRIEKER);
        this.registerVanilla(CLIENT_REQUEST_PLACEHOLDER_BLOCK);
//        this.registerVanilla(MYSTERIOUS_FRAME);
//        this.registerVanilla(MYSTERIOUS_FRAME_SLOT);
        this.registerVanilla(FROG_SPAWN);
        this.registerVanilla(PEARLESCENT_FROGLIGHT);
        this.registerVanilla(VERDANT_FROGLIGHT);
        this.registerVanilla(OCHRE_FROGLIGHT);
    }
}
