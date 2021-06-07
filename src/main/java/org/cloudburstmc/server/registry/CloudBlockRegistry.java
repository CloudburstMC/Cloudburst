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
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.block.behavior.BlockBehavior;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.registry.Registry;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.block.behavior.*;
import org.cloudburstmc.server.block.serializer.*;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers;
import org.cloudburstmc.server.block.util.BlockStateMetaMappings;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.cloudburstmc.api.block.BlockTypes.*;

@Log4j2
public class CloudBlockRegistry implements Registry {
    private static final CloudBlockRegistry INSTANCE;
    private static final HashBiMap<Identifier, Integer> VANILLA_LEGACY_IDS = HashBiMap.create();

    static {
        InputStream stream = RegistryUtils.getOrAssertResource("data/legacy_block_ids.json");

        try {
            VANILLA_LEGACY_IDS.putAll(Bootstrap.JSON_MAPPER.readValue(stream, new TypeReference<Map<Identifier, Integer>>() {
            }));
        } catch (IOException e) {
            throw new AssertionError("Unable to load legacy IDs", e);
        }

        INSTANCE = new CloudBlockRegistry(); // Needs to be initialized afterwards
    }

    private final Reference2ReferenceMap<BlockType, BlockBehavior> behaviorMap = new Reference2ReferenceOpenHashMap<>();
    //private final HashBiMap<Identifier, Integer> idLegacyMap = HashBiMap.create();
    private final AtomicInteger customIdAllocator = new AtomicInteger(1000);
    private final BlockPalette palette = BlockPalette.INSTANCE;
    private NbtMap propertiesTag;
    private volatile boolean closed;
    private transient NbtList<NbtMap> serializedPalette;

    private CloudBlockRegistry() {
        BlockTraitSerializers.init();
        this.registerVanillaBlocks();

        // Check legacy IDs
        behaviorMap.forEach((bt, b) -> {
            if (!VANILLA_LEGACY_IDS.containsKey(bt.getId())) {
                log.debug("Unable to map legacy id for block: {}", bt.getId());
            }
        });
    }

    public static CloudBlockRegistry get() {
        return INSTANCE;
    }

    public synchronized void register(BlockType type, BlockBehavior behavior) throws RegistryException {
        checkNotNull(type, "type");
        registerVanilla(type, behavior);

        // generate legacy ID (Not sure why we need to but it's a requirement)
        int legacyId = this.customIdAllocator.getAndIncrement();
        this.VANILLA_LEGACY_IDS.put(type.getId(), legacyId);
    }

    private void registerVanilla(BlockType type) throws RegistryException {
        registerVanilla(type, NoopBlockBehavior.INSTANCE);
    }

    private void registerVanilla(BlockType type, BlockBehavior behavior) throws RegistryException {
        this.registerVanilla(type, behavior, DefaultBlockSerializer.INSTANCE);
    }

    private synchronized void registerVanilla(BlockType type, BlockBehavior behavior, BlockSerializer serializer) throws RegistryException {
        checkNotNull(type, "type");
        checkNotNull(serializer, "serializer");
        checkClosed();

        synchronized (this.behaviorMap) {
            if (this.behaviorMap.putIfAbsent(type, behavior) != null)
                throw new RegistryException(type + " is already registered");
        }

        type.forEachPermutation(state -> state.setBehavior(behavior));

        this.palette.addBlock(type, serializer);
    }

    /**
     * Extends current behavior with delegated one provided by the factory.
     * All methods are delegated to the previous {@link BlockBehavior} instance by default.
     * <p>
     * Extending behavior must extend {@link BlockBehaviorDelegate} class accepting
     * parent behavior as a constructor parameter.
     * Parent behavior instance is supplied as an argument to the factory function.
     *
     * @param type    type to register the behavior for
     * @param factory factory providing BlockBehaviorDelegate instance
     * @throws RegistryException if there's no behavior registered for the specified type
     */
    public void extendBehavior(BlockType type, Function<BlockBehavior, BlockBehaviorDelegate> factory) {
        checkNotNull(type, "type");
        checkNotNull(factory, "factory");

        synchronized (this.behaviorMap) {
            if (this.behaviorMap.computeIfPresent(type, (key, existing) -> factory.apply(existing)) == null) {
                throw new RegistryException(type + " has not been registered");
            }
        }
    }

    /**
     * Overwrites existing or registers new {@link BlockBehavior} instance if absent
     *
     * @param type     type to register the behavior for
     * @param behavior overwriting behavior instance
     */
    public void overwriteBehavior(BlockType type, BlockBehavior behavior) {
        checkNotNull(type, "type");
        checkNotNull(behavior, behavior);

        synchronized (this.behaviorMap) {
            this.behaviorMap.put(type, behavior);
        }
    }

    boolean isBlock(Identifier id) {
        return this.VANILLA_LEGACY_IDS.containsKey(id);
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
        return getRuntimeId(this.VANILLA_LEGACY_IDS.inverse().get(id), meta);
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
        int legacyId = this.VANILLA_LEGACY_IDS.getOrDefault(identifier, -1);
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

    public BlockBehavior getBehavior(BlockType blockType) {
        return this.behaviorMap.get(blockType);
    }

    public ImmutableList<BlockState> getBlockStates() {
        return ImmutableList.copyOf(palette.getState2identifierMap().keySet());
    }

    private void registerVanillaBlocks() {
        this.registerVanilla(AIR, new BlockBehaviorAir()); //0
        this.registerVanilla(STONE, new BlockBehaviorStone()); //1
        this.registerVanilla(GRASS, new BlockBehaviorGrass()); //2
        this.registerVanilla(DIRT, new BlockBehaviorDirt()); //3
        this.registerVanilla(COBBLESTONE, new BlockBehaviorCobblestone()); //4
        this.registerVanilla(PLANKS, new BlockBehaviorPlanks(), MultiBlockSerializers.PLANKS); //5
        this.registerVanilla(SAPLING, new BlockBehaviorSapling()); //6
        this.registerVanilla(BEDROCK, new BlockBehaviorBedrock()); //7
        this.registerVanilla(FLOWING_WATER, new BlockBehaviorWater(), FluidBlockSerializer.INSTANCE); //8
        this.registerVanilla(WATER, new BlockBehaviorWater(), FluidBlockSerializer.INSTANCE); //9
        this.registerVanilla(FLOWING_LAVA, new BlockBehaviorLava(), FluidBlockSerializer.INSTANCE); //10
        this.registerVanilla(LAVA, new BlockBehaviorLava(), FluidBlockSerializer.INSTANCE); //11
        this.registerVanilla(SAND, new BlockBehaviorSand()); //12
        this.registerVanilla(GRAVEL, new BlockBehaviorGravel()); //13
        this.registerVanilla(GOLD_ORE); //14
        this.registerVanilla(IRON_ORE); //15
        this.registerVanilla(COAL_ORE, new BlockBehaviorOreCoal()); //16
        this.registerVanilla(LOG, new BlockBehaviorLog(), MultiBlockSerializers.LOG); //17
        this.registerVanilla(LEAVES, new BlockBehaviorLeaves(), MultiBlockSerializers.LEAVES); //18
        this.registerVanilla(SPONGE, new BlockBehaviorSponge()); //19
        this.registerVanilla(GLASS, new BlockBehaviorGlass()); //20
        this.registerVanilla(LAPIS_ORE, new BlockBehaviorOreLapis()); //21
        this.registerVanilla(LAPIS_BLOCK, new BlockBehaviorLapis()); //22
        this.registerVanilla(DISPENSER, new BlockBehaviorDispenser()); //23
        this.registerVanilla(SANDSTONE, new BlockBehaviorSandstone()); //24
        this.registerVanilla(NOTEBLOCK, new BlockBehaviorNoteblock()); //25
        this.registerVanilla(BED, new BlockBehaviorBed()); //26
        this.registerVanilla(GOLDEN_RAIL, new BlockBehaviorRailPowered()); //27
        this.registerVanilla(DETECTOR_RAIL, new BlockBehaviorRailDetector()); //28
        this.registerVanilla(WEB, new BlockBehaviorCobweb()); //30
        this.registerVanilla(TALL_GRASS, new BlockBehaviorTallGrass()); //31
        this.registerVanilla(DEADBUSH, new BlockBehaviorDeadBush()); //32
        this.registerVanilla(PISTON, new BlockBehaviorPiston(), MultiBlockSerializers.PISTON); //33
        this.registerVanilla(PISTON_ARM_COLLISION, new BlockBehaviorPistonHead(), MultiBlockSerializers.PISTON_ARM_COLLISION); //34
        this.registerVanilla(WOOL, new BlockBehaviorWool()); //35
        this.registerVanilla(FLOWER, new BlockBehaviorFlower(), MultiBlockSerializers.FLOWER); //37 - 38
        this.registerVanilla(BROWN_MUSHROOM, new BlockBehaviorMushroomBrown()); //39
        this.registerVanilla(RED_MUSHROOM, new BlockBehaviorMushroomRed()); //40
        this.registerVanilla(GOLD_BLOCK, new BlockBehaviorGold()); //41
        this.registerVanilla(IRON_BLOCK, new BlockBehaviorIron()); //42
        this.registerVanilla(STONE_SLAB, new BlockBehaviorSlab(), SlabSerializer.INSTANCE); //44
        this.registerVanilla(BRICK_BLOCK, new BlockBehaviorBricks()); //45
        this.registerVanilla(TNT, new BlockBehaviorTNT()); //46
        this.registerVanilla(BOOKSHELF, new BlockBehaviorBookshelf()); //47
        this.registerVanilla(MOSSY_COBBLESTONE, new BlockBehaviorCobblestone()); //48
        this.registerVanilla(OBSIDIAN, new BlockBehaviorObsidian()); //49
        this.registerVanilla(TORCH, new BlockBehaviorTorch(), MultiBlockSerializers.TORCH); //50
        this.registerVanilla(FIRE, new BlockBehaviorFire()); //51
        this.registerVanilla(MOB_SPAWNER, new BlockBehaviorMobSpawner()); //52
        this.registerVanilla(WOODEN_STAIRS, new BlockBehaviorStairsWood(), MultiBlockSerializers.WOOD_STAIRS); //53
        this.registerVanilla(CHEST, new BlockBehaviorChest()); //54
        this.registerVanilla(REDSTONE_WIRE, new BlockBehaviorRedstoneWire()); //55
        this.registerVanilla(DIAMOND_ORE, new BlockBehaviorOreDiamond()); //56
        this.registerVanilla(DIAMOND_BLOCK, new BlockBehaviorDiamond()); //57
        this.registerVanilla(CRAFTING_TABLE, new BlockBehaviorCraftingTable()); //58
        this.registerVanilla(WHEAT, new BlockBehaviorWheat()); //59
        this.registerVanilla(FARMLAND, new BlockBehaviorFarmland()); //60
        this.registerVanilla(FURNACE, new BlockBehaviorFurnace(BlockEntityTypes.FURNACE), MultiBlockSerializers.FURNACE); //61
        this.registerVanilla(STANDING_SIGN, new BlockBehaviorSignPost(), MultiBlockSerializers.WOOD_STANDING_SIGN); //63
        this.registerVanilla(WOODEN_DOOR, new BlockBehaviorDoorWood(), MultiBlockSerializers.WOOD_DOOR); //64
        this.registerVanilla(LADDER, new BlockBehaviorLadder()); //65
        this.registerVanilla(RAIL, new BlockBehaviorRail(RAIL, BlockTraits.RAIL_DIRECTION)); //66
        this.registerVanilla(STONE_STAIRS, new BlockBehaviorStairsCobblestone(), MultiBlockSerializers.STONE_STAIRS); //67
        this.registerVanilla(WALL_SIGN, new BlockBehaviorWallSign(), MultiBlockSerializers.WOOD_WALL_SIGN); //68
        this.registerVanilla(LEVER, new BlockBehaviorLever()); //69
        this.registerVanilla(STONE_PRESSURE_PLATE, new BlockBehaviorPressurePlateStone(), MultiBlockSerializers.STONE_PRESSURE_PLATE); //70
        this.registerVanilla(IRON_DOOR, new BlockBehaviorDoorIron()); //71
        this.registerVanilla(WOODEN_PRESSURE_PLATE, new BlockBehaviorPressurePlateWood(), MultiBlockSerializers.WOOD_PRESSURE_PLATE); //72
        this.registerVanilla(REDSTONE_ORE, new BlockBehaviorOreRedstone(), MultiBlockSerializers.REDSTONE_ORE); //73, 74
        this.registerVanilla(REDSTONE_TORCH, new BlockBehaviorRedstoneTorch(), MultiBlockSerializers.REDSTONE_TORCH); //75, 76
        this.registerVanilla(STONE_BUTTON, new BlockBehaviorButton(), MultiBlockSerializers.STONE_BUTTON); //77
        this.registerVanilla(SNOW_LAYER, new BlockBehaviorSnowLayer()); //78
        this.registerVanilla(ICE, new BlockBehaviorIce()); //79
        this.registerVanilla(SNOW, new BlockBehaviorSnow()); //80
        this.registerVanilla(CACTUS, new BlockBehaviorCactus()); //81
        this.registerVanilla(CLAY, new BlockBehaviorClay()); //82
        this.registerVanilla(REEDS, new ReedsBlockBehavior()); //83
        this.registerVanilla(JUKEBOX, new BlockBehaviorJukebox()); //84
        this.registerVanilla(WOODEN_FENCE, new BlockBehaviorFenceWooden(), MultiBlockSerializers.WOOD_FENCE); //85
        this.registerVanilla(PUMPKIN, new BlockBehaviorPumpkin()); //86
        this.registerVanilla(NETHERRACK, new BlockBehaviorNetherrack()); //87
        this.registerVanilla(SOUL_SAND, new BlockBehaviorSoulSand()); //88
        this.registerVanilla(GLOWSTONE, new BlockBehaviorGlowstone()); //89
        this.registerVanilla(PORTAL, new BlockBehaviorNetherPortal()); //90
        this.registerVanilla(LIT_PUMPKIN, new BlockBehaviorPumpkin()); //91
        this.registerVanilla(CAKE, new BlockBehaviorCake()); //92
        this.registerVanilla(REPEATER, new BlockBehaviorRedstoneRepeater(), MultiBlockSerializers.REPEATER); //93, 94
        this.registerVanilla(INVISIBLE_BEDROCK, new BlockBehaviorBedrockInvisible()); //95
        this.registerVanilla(WOODEN_TRAPDOOR, new BlockBehaviorTrapdoor(), MultiBlockSerializers.WOOD_TRAPDOOR); //96
        this.registerVanilla(MONSTER_EGG, new BlockBehaviorMonsterEgg()); //97
        this.registerVanilla(STONEBRICK); //98
        this.registerVanilla(BROWN_MUSHROOM_BLOCK, new BlockBehaviorHugeMushroomBrown()); //99
        this.registerVanilla(RED_MUSHROOM_BLOCK, new BlockBehaviorHugeMushroomRed()); //100
        this.registerVanilla(IRON_BARS, new BlockBehaviorIronBars()); //101
        this.registerVanilla(GLASS_PANE, new BlockBehaviorGlassPane()); //102
        this.registerVanilla(MELON_BLOCK, new BlockBehaviorMelon()); //103
        this.registerVanilla(PUMPKIN_STEM, new BlockBehaviorStemPumpkin()); //104
        this.registerVanilla(MELON_STEM, new BlockBehaviorStemMelon()); //105
        this.registerVanilla(VINE, new BlockBehaviorVine()); //106
        this.registerVanilla(WOODEN_FENCE_GATE, new BlockBehaviorFenceGate(), MultiBlockSerializers.WOOD_FENCE_GATE); //107
        this.registerVanilla(MYCELIUM, new BlockBehaviorMycelium()); //110
        this.registerVanilla(WATERLILY, new BlockBehaviorWaterLily()); //111
        this.registerVanilla(NETHER_BRICK, new BlockBehaviorBricksNether(), MultiBlockSerializers.NETHER_BRICKS); //112
        this.registerVanilla(NETHER_BRICK_FENCE, new BlockBehaviorFenceNetherBrick()); //113
        this.registerVanilla(NETHER_WART, new BlockBehaviorNetherWart()); //115
        this.registerVanilla(ENCHANTING_TABLE, new BlockBehaviorEnchantingTable()); //116
        this.registerVanilla(BREWING_STAND, new BlockBehaviorBrewingStand()); //117
        this.registerVanilla(CAULDRON, new BlockBehaviorCauldron(), MultiBlockSerializers.CAULDRON); //118
        this.registerVanilla(END_PORTAL, new BlockBehaviorEndPortal()); //119
        this.registerVanilla(END_PORTAL_FRAME, new BlockBehaviorEndPortalFrame()); //120
        this.registerVanilla(END_STONE, new BlockBehaviorEndStone()); //121
        this.registerVanilla(DRAGON_EGG, new BlockBehaviorDragonEgg()); //122
        this.registerVanilla(REDSTONE_LAMP, new BlockBehaviorRedstoneLamp(), MultiBlockSerializers.REDSTONE_LAMP); //123
        this.registerVanilla(DROPPER, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(ACTIVATOR_RAIL, new BlockBehaviorRailActivator()); //126
        this.registerVanilla(COCOA, new BlockBehaviorCocoa()); //127
        this.registerVanilla(EMERALD_ORE, new BlockBehaviorOreEmerald()); //129
        this.registerVanilla(ENDER_CHEST, new BlockBehaviorEnderChest()); //130
        this.registerVanilla(TRIPWIRE_HOOK, new BlockBehaviorTripWireHook()); //131
        this.registerVanilla(TRIPWIRE, new BlockBehaviorTripWire()); //132
        this.registerVanilla(EMERALD_BLOCK, new BlockBehaviorEmerald()); //133
        this.registerVanilla(COMMAND_BLOCK, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(BEACON, new BlockBehaviorBeacon()); //138
        this.registerVanilla(STONE_WALL, new BlockBehaviorWall(), MultiBlockSerializers.WALL); //139
        this.registerVanilla(FLOWER_POT, new BlockBehaviorFlowerPot()); //140
        this.registerVanilla(CARROTS, new BlockBehaviorCarrot()); //141
        this.registerVanilla(POTATOES, new BlockBehaviorPotato()); //142
        this.registerVanilla(WOODEN_BUTTON, new BlockBehaviorButton(), MultiBlockSerializers.WOOD_BUTTON); //143
        this.registerVanilla(SKULL, new BlockBehaviorSkull()); //144
        this.registerVanilla(ANVIL, new BlockBehaviorAnvil()); //145
        this.registerVanilla(TRAPPED_CHEST, new BlockBehaviorTrappedChest()); //146
        this.registerVanilla(LIGHT_WEIGHTED_PRESSURE_PLATE, new BlockBehaviorWeightedPressurePlateLight()); //147
        this.registerVanilla(HEAVY_WEIGHTED_PRESSURE_PLATE, new BlockBehaviorWeightedPressurePlateHeavy()); //148
        this.registerVanilla(COMPARATOR, new BlockBehaviorRedstoneComparator(), MultiBlockSerializers.COMPARATOR); //150
        this.registerVanilla(DAYLIGHT_DETECTOR, new BlockBehaviorDaylightDetector()); //151
        this.registerVanilla(REDSTONE_BLOCK, new BlockBehaviorRedstone()); //152
        this.registerVanilla(QUARTZ_ORE, new BlockBehaviorOreQuartz()); //153
        this.registerVanilla(HOPPER, new BlockBehaviorHopper()); //154
        this.registerVanilla(QUARTZ_BLOCK, new BlockBehaviorQuartz()); //155
        this.registerVanilla(WOODEN_SLAB, new BlockBehaviorSlabWood(), SlabSerializer.INSTANCE); //158
        this.registerVanilla(STAINED_HARDENED_CLAY, new BlockBehaviorTerracottaStained()); //159
        this.registerVanilla(STAINED_GLASS_PANE, new BlockBehaviorGlassPaneStained()); //160
        this.registerVanilla(SLIME, new BlockBehaviorSlime()); //165
        //166: glow_stick
        this.registerVanilla(IRON_TRAPDOOR, new BlockBehaviorTrapdoorIron()); //167
        this.registerVanilla(PRISMARINE, new BlockBehaviorPrismarine()); //168
        this.registerVanilla(SEA_LANTERN, new BlockBehaviorSeaLantern()); //169
        this.registerVanilla(HAY_BLOCK, new BlockBehaviorHayBale()); //170
        this.registerVanilla(CARPET, new BlockBehaviorCarpet()); //171
        this.registerVanilla(HARDENED_CLAY, new BlockBehaviorTerracotta()); //172
        this.registerVanilla(COAL_BLOCK, new BlockBehaviorCoal()); //173
        this.registerVanilla(PACKED_ICE, new BlockBehaviorIcePacked()); //174
        this.registerVanilla(DOUBLE_PLANT, new BlockBehaviorDoublePlant()); //175
        this.registerVanilla(STANDING_BANNER, new BlockBehaviorBanner()); //176
        this.registerVanilla(WALL_BANNER, new BlockBehaviorWallBanner()); //177
        this.registerVanilla(DAYLIGHT_DETECTOR_INVERTED, new BlockBehaviorDaylightDetectorInverted()); //178
        this.registerVanilla(RED_SANDSTONE, new BlockBehaviorRedSandstone()); //179
        this.registerVanilla(REPEATING_COMMAND_BLOCK, NoopBlockBehavior.INSTANCE); //188
        this.registerVanilla(CHAIN_COMMAND_BLOCK, NoopBlockBehavior.INSTANCE); //189
        this.registerVanilla(HARD_GLASS_PANE, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(HARD_STAINED_GLASS_PANE, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(CHEMICAL_HEAT, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(GRASS_PATH, new BlockBehaviorGrassPath()); //198
        this.registerVanilla(FRAME, new BlockBehaviorItemFrame(), MultiBlockSerializers.FRAME); //199
        this.registerVanilla(CHORUS_FLOWER, new BlockBehaviorChorusFlower()); //200
        this.registerVanilla(PURPUR_BLOCK, new BlockBehaviorPurpur()); //201
        this.registerVanilla(COLORED_TORCH_RG, new BlockBehaviorTorch());
        this.registerVanilla(COLORED_TORCH_BP, new BlockBehaviorTorch());
        this.registerVanilla(UNDYED_SHULKER_BOX, new BlockBehaviorUndyedShulkerBox()); //205
        this.registerVanilla(END_BRICKS, new BlockBehaviorBricksEndStone()); //206
        this.registerVanilla(FROSTED_ICE, NoopBlockBehavior.INSTANCE); //206
        this.registerVanilla(END_ROD, new BlockBehaviorEndRod()); //208
        this.registerVanilla(END_GATEWAY, new BlockBehaviorEndGateway()); //209
        this.registerVanilla(ALLOW, NoopBlockBehavior.INSTANCE); //210
        this.registerVanilla(DENY, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(BORDER_BLOCK, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(MAGMA, new BlockBehaviorMagma()); //213
        this.registerVanilla(NETHER_WART_BLOCK, new BlockNetherWartBlockBehavior(), MultiBlockSerializers.WART_BLOCK); //214
        this.registerVanilla(BONE_BLOCK, new BlockBehaviorBone()); //216
        this.registerVanilla(STRUCTURE_VOID, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(SHULKER_BOX, new BlockBehaviorShulkerBox()); //218
        this.registerVanilla(GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed(), MultiBlockSerializers.TERRACOTTA); //219 - 235
        this.registerVanilla(CONCRETE, new BlockBehaviorConcrete()); //236
        this.registerVanilla(CONCRETE_POWDER, new BlockBehaviorConcretePowder()); //237
        this.registerVanilla(CHEMISTRY_TABLE, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(UNDERWATER_TORCH, new BlockBehaviorTorch());
        this.registerVanilla(CHORUS_PLANT, new BlockBehaviorChorusPlant()); //240
        this.registerVanilla(STAINED_GLASS, new BlockBehaviorGlassStained()); //241
        this.registerVanilla(CAMERA, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(PODZOL, new BlockBehaviorPodzol()); //243
        this.registerVanilla(BEETROOT, new BlockBehaviorBeetroot()); //244
        this.registerVanilla(STONECUTTER); //245
        this.registerVanilla(GLOWING_OBSIDIAN, new BlockBehaviorObsidianGlowing()); //246
        this.registerVanilla(NETHER_REACTOR, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(INFO_UPDATE, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(INFO_UPDATE2, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(MOVING_BLOCK, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(OBSERVER, new BlockBehaviorObserver()); //251
        this.registerVanilla(STRUCTURE_BLOCK, NoopBlockBehavior.INSTANCE); //252
        this.registerVanilla(HARD_GLASS, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(HARD_STAINED_GLASS, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(RESERVED6, NoopBlockBehavior.INSTANCE);
        //256: unknown
        this.registerVanilla(BLUE_ICE, new BlockBehaviorBlueIce()); //266
        this.registerVanilla(ELEMENT, NoopBlockBehavior.INSTANCE, MultiBlockSerializers.ELEMENT);
        this.registerVanilla(SEAGRASS, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(CORAL, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(CORAL_BLOCK, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(CORAL_FAN, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(CORAL_FAN_DEAD, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(CORAL_FAN_HANG, NoopBlockBehavior.INSTANCE, new CoralHangBlockSerializer());
        this.registerVanilla(KELP, new BlockBehaviorKelp());//393
        this.registerVanilla(DRIED_KELP_BLOCK, new BlockBehaviorDriedKelp()); //394
        this.registerVanilla(CARVED_PUMPKIN, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(SEA_PICKLE, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(CONDUIT, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(TURTLE_EGG, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(BUBBLE_COLUMN, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(BARRIER, new BlockBehaviorBarrier()); //415
        this.registerVanilla(BAMBOO, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(BAMBOO_SAPLING, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(SCAFFOLDING, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(SMOOTH_STONE); // 437
        this.registerVanilla(LECTERN, new BlockBehaviorLectern()); //448
        this.registerVanilla(GRINDSTONE, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(BLAST_FURNACE, new BlockBehaviorFurnace(BlockEntityTypes.BLAST_FURNACE), MultiBlockSerializers.BLAST_FURNACE); // 450
        this.registerVanilla(STONECUTTER_BLOCK, NoopBlockBehavior.INSTANCE); // 451
        this.registerVanilla(SMOKER, new BlockBehaviorFurnace(BlockEntityTypes.FURNACE), MultiBlockSerializers.SMOKER); //452
        this.registerVanilla(CARTOGRAPHY_TABLE, NoopBlockBehavior.INSTANCE); //454
        this.registerVanilla(FLETCHING_TABLE, NoopBlockBehavior.INSTANCE); //455
        this.registerVanilla(SMITHING_TABLE, NoopBlockBehavior.INSTANCE); //456
        this.registerVanilla(BARREL, new BlockBehaviorBarrel()); // 457
        this.registerVanilla(LOOM, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(BELL, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(SWEET_BERRY_BUSH, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(LANTERN, NoopBlockBehavior.INSTANCE, MultiBlockSerializers.LANTERN);
        this.registerVanilla(CAMPFIRE, new BlockBehaviorCampfire(), MultiBlockSerializers.CAMPFIRE);//464
        this.registerVanilla(JIGSAW, NoopBlockBehavior.INSTANCE, new JigsawSerializer());//466
        this.registerVanilla(WOOD, new BlockBehaviorWood(), MultiBlockSerializers.WOOD); //467
        this.registerVanilla(COMPOSTER, NoopBlockBehavior.INSTANCE);//468
        this.registerVanilla(LIGHT_BLOCK, new BlockBehaviorLight());//470
        this.registerVanilla(WITHER_ROSE, NoopBlockBehavior.INSTANCE);//471
        this.registerVanilla(BEE_NEST, NoopBlockBehavior.INSTANCE);//473
        this.registerVanilla(BEEHIVE, NoopBlockBehavior.INSTANCE);//474
        this.registerVanilla(HONEY_BLOCK, NoopBlockBehavior.INSTANCE);//475
        this.registerVanilla(HONEYCOMB_BLOCK, new BlockHoneycombBlockBehavior()); //476
        this.registerVanilla(LODESTONE, NoopBlockBehavior.INSTANCE);//477
        this.registerVanilla(NETHER_ROOTS, NoopBlockBehavior.INSTANCE, MultiBlockSerializers.ROOTS);//478
        this.registerVanilla(NETHER_FUNGUS, NoopBlockBehavior.INSTANCE, MultiBlockSerializers.FUNGUS);//482
        this.registerVanilla(SHROOMLIGHT, NoopBlockBehavior.INSTANCE);//484
        this.registerVanilla(WEEPING_VINES, NoopBlockBehavior.INSTANCE);//485
        this.registerVanilla(NETHER_NYLIUM, NoopBlockBehavior.INSTANCE, MultiBlockSerializers.NYLIUM);//486
        this.registerVanilla(BASALT, NoopBlockBehavior.INSTANCE);//490
        this.registerVanilla(POLISHED_BASALT, NoopBlockBehavior.INSTANCE);//590
        this.registerVanilla(SOUL_SOIL, NoopBlockBehavior.INSTANCE);//491
        this.registerVanilla(SOUL_FIRE, NoopBlockBehavior.INSTANCE);//492
        this.registerVanilla(NETHER_SPROUTS, NoopBlockBehavior.INSTANCE);//493
        this.registerVanilla(TARGET, NoopBlockBehavior.INSTANCE);//494
        this.registerVanilla(NETHERITE_BLOCK, NoopBlockBehavior.INSTANCE);//525
        this.registerVanilla(ANCIENT_DEBRIS, NoopBlockBehavior.INSTANCE);//526
        this.registerVanilla(RESPAWN_ANCHOR, NoopBlockBehavior.INSTANCE);//527
        this.registerVanilla(BLACKSTONE, NoopBlockBehavior.INSTANCE);//528
        this.registerVanilla(POLISHED_BLACKSTONE_BRICKS, NoopBlockBehavior.INSTANCE);//529
        this.registerVanilla(CHISELED_POLISHED_BLACKSTONE, NoopBlockBehavior.INSTANCE);//534
        this.registerVanilla(CRACKED_POLISHED_BLACKSTONE_BRICKS, NoopBlockBehavior.INSTANCE);//535
        this.registerVanilla(GILDED_BLACKSTONE, NoopBlockBehavior.INSTANCE);//536
        this.registerVanilla(CHAIN, NoopBlockBehavior.INSTANCE);//541
        this.registerVanilla(TWISTING_VINES, NoopBlockBehavior.INSTANCE);//542
        this.registerVanilla(NETHER_GOLD_ORE, NoopBlockBehavior.INSTANCE);//543
        this.registerVanilla(CRYING_OBSIDIAN, NoopBlockBehavior.INSTANCE);//544
        this.registerVanilla(POLISHED_BLACKSTONE, NoopBlockBehavior.INSTANCE);//546
        this.registerVanilla(QUARTZ_BRICKS, NoopBlockBehavior.INSTANCE);//559
        this.registerVanilla(UNKNOWN, NoopBlockBehavior.INSTANCE); //560

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
        this.registerVanilla(AZALEA_LEAVES, NoopBlockBehavior.INSTANCE, MultiBlockSerializers.AZALEA_LEAVES);
        this.registerVanilla(CALCITE);
        this.registerVanilla(AMETHYST_BLOCK);
        this.registerVanilla(BUDDING_AMETHYST);
        this.registerVanilla(AMETHYST_CLUSTER, NoopBlockBehavior.INSTANCE, MultiBlockSerializers.AMETHYST_CLUSTER);
        this.registerVanilla(TUFF);
        this.registerVanilla(TINTED_GLASS);
        this.registerVanilla(MOSS_CARPET);
        this.registerVanilla(SMALL_DRIPLEAF);
        this.registerVanilla(AZALEA, NoopBlockBehavior.INSTANCE, MultiBlockSerializers.AZALEA);
        this.registerVanilla(COPPER, NoopBlockBehavior.INSTANCE, MultiBlockSerializers.COPPER_BLOCKS);
        this.registerVanilla(CUT_COPPER, NoopBlockBehavior.INSTANCE, MultiBlockSerializers.CUT_COPPER);
        this.registerVanilla(COPPER_SLAB, NoopBlockBehavior.INSTANCE, SlabSerializer.INSTANCE);
        this.registerVanilla(COPPER_STAIRS, NoopBlockBehavior.INSTANCE, MultiBlockSerializers.COPPER_STAIRS);
        this.registerVanilla(CAVE_VINES, NoopBlockBehavior.INSTANCE, MultiBlockSerializers.CAVE_VINES);
        this.registerVanilla(SMOOTH_BASALT);
        this.registerVanilla(DEEPSLATE, NoopBlockBehavior.INSTANCE, MultiBlockSerializers.DEEPSLATE);
        this.registerVanilla(COBBLED_DEEPSLATE);
        this.registerVanilla(POLISHED_DEEPSLATE);
        this.registerVanilla(DEEPSLATE_TILES);
        this.registerVanilla(DEEPSLATE_BRICKS);
        this.registerVanilla(CHISELED_DEEPSLATE);
        this.registerVanilla(DEEPSLATE_LAPIS_ORE);
        this.registerVanilla(DEEPSLATE_IRON_ORE);
        this.registerVanilla(DEEPSLATE_GOLD_ORE);
        this.registerVanilla(DEEPSLATE_REDSTONE_ORE, NoopBlockBehavior.INSTANCE, MultiBlockSerializers.DEEPSLATE_REDSTONE_ORE);
        this.registerVanilla(DEEPSLATE_DIAMOND_ORE);
        this.registerVanilla(DEEPSLATE_COAL_ORE);
        this.registerVanilla(DEEPSLATE_EMERALD_ORE);
        this.registerVanilla(DEEPSLATE_COPPER_ORE);
        this.registerVanilla(CRACKED_DEEPSLATE_TILES);
        this.registerVanilla(CRACKED_DEEPSLATE_BRICKS);
        this.registerVanilla(GLOW_LICHEN);
        this.registerVanilla(RAW_COPPER_BLOCK);
        this.registerVanilla(RAW_IRON_BLOCK);
        this.registerVanilla(RAW_GOLD_BLOCK);


    }
}
