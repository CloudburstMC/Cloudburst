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
import lombok.val;
import org.cloudburstmc.api.registry.RegistryException;
import org.cloudburstmc.server.Bootstrap;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.BlockType;
import org.cloudburstmc.server.block.behavior.*;
import org.cloudburstmc.server.block.serializer.*;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers;
import org.cloudburstmc.server.block.util.BlockStateMetaMappings;
import org.cloudburstmc.server.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.utils.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.cloudburstmc.server.block.BlockTypes.*;

@Log4j2
public class BlockRegistry implements Registry {
    private static final BlockRegistry INSTANCE;
    private static final Map<Identifier, Integer> VANILLA_LEGACY_IDS;

    static {
        InputStream stream = RegistryUtils.getOrAssertResource("data/legacy_block_ids.json");

        try {
            VANILLA_LEGACY_IDS = Bootstrap.JSON_MAPPER.readValue(stream, new TypeReference<Map<Identifier, Integer>>() {
            });
        } catch (IOException e) {
            throw new AssertionError("Unable to load legacy IDs", e);
        }

        INSTANCE = new BlockRegistry(); // Needs to be initialized afterwards
    }

    private final Reference2ReferenceMap<BlockType, BlockBehavior> behaviorMap = new Reference2ReferenceOpenHashMap<>();
    private final HashBiMap<Identifier, Integer> idLegacyMap = HashBiMap.create();
    private final AtomicInteger customIdAllocator = new AtomicInteger(1000);
    private final BlockPalette palette = BlockPalette.INSTANCE;
    private NbtMap propertiesTag;
    private volatile boolean closed;
    private transient NbtList<NbtMap> serializedPalette;

    private BlockRegistry() {
        BlockTraitSerializers.init();
        this.registerVanillaBlocks();

        //register vanilla legacy IDs
        VANILLA_LEGACY_IDS.forEach((id, legacyId) -> {
            if (!this.idLegacyMap.containsKey(id)) {
                log.debug("Non-implemented block found: {}", id);
            }
        });
        this.idLegacyMap.putAll(VANILLA_LEGACY_IDS);
    }

    public static BlockRegistry get() {
        return INSTANCE;
    }

    public synchronized void register(BlockType type, BlockBehavior behavior) throws RegistryException {
        checkNotNull(type, "type");
        registerVanilla(type, behavior);

        // generate legacy ID (Not sure why we need to but it's a requirement)
        int legacyId = this.customIdAllocator.getAndIncrement();
        this.idLegacyMap.put(type.getId(), legacyId);
    }

    private void registerVanilla(BlockType type, BlockTrait<?>... traits) throws RegistryException {
        registerVanilla(type, NoopBlockBehavior.INSTANCE, traits);
    }

    private void registerVanilla(BlockType type, BlockBehavior behavior, BlockTrait<?>... traits) throws RegistryException {
        this.registerVanilla(type, behavior, DefaultBlockSerializer.INSTANCE, traits);
    }

    private synchronized void registerVanilla(BlockType type, BlockBehavior behavior, BlockSerializer serializer,
                                              BlockTrait<?>... traits) throws RegistryException {
        checkNotNull(type, "type");
        checkNotNull(behavior, "behavior");
        checkNotNull(serializer, "serializer");
        if (traits == null) {
            traits = new BlockTrait[0];
        }
        checkClosed();

        synchronized (this.behaviorMap) {
            if (this.behaviorMap.putIfAbsent(type, behavior) != null)
                throw new RegistryException(type + " is already registered");
        }

        this.palette.addBlock(type, serializer, traits);
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
        return this.idLegacyMap.containsKey(id);
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
        return getRuntimeId(this.idLegacyMap.inverse().get(id), meta);
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
            val defaultState = getBlock(Identifier.fromString(tag.getString("name")));
            val serialized = palette.getSerialized(defaultState);

            if (serialized.containsKey("states", NbtType.COMPOUND)) {
                val builder = tag.toBuilder();

                val statesBuilder = ((NbtMap) builder.get("states")).toBuilder();
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
        int legacyId = this.idLegacyMap.getOrDefault(identifier, -1);
        if (legacyId == -1) {
            throw new RegistryException("No legacy ID found for " + identifier);
        }
        return legacyId;
    }

    public Identifier getNameFromLegacyId(int id) {
        Identifier identifier = idLegacyMap.inverse().get(id);
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
                    .putShort("id", (short) this.getLegacyId(state.getId()))
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
        return ImmutableList.copyOf(palette.getStateMap().keySet());
    }

    private void registerVanillaBlocks() {
        this.registerVanilla(AIR, new BlockBehaviorAir()); //0
        this.registerVanilla(STONE, new BlockBehaviorStone(), BlockTraits.STONE_TYPE); //1
        this.registerVanilla(GRASS, new BlockBehaviorGrass()); //2
        this.registerVanilla(DIRT, new BlockBehaviorDirt(), BlockTraits.DIRT_TYPE); //3
        this.registerVanilla(COBBLESTONE, new BlockBehaviorCobblestone()); //4
        this.registerVanilla(PLANKS, new BlockBehaviorPlanks(), MultiBlockSerializers.PLANKS, BlockTraits.TREE_SPECIES); //5
        this.registerVanilla(SAPLING, new BlockBehaviorSapling(), BlockTraits.TREE_SPECIES_OVERWORLD, BlockTraits.HAS_AGE); //6
        this.registerVanilla(BEDROCK, new BlockBehaviorBedrock(), BlockTraits.HAS_INFINIBURN); //7
        this.registerVanilla(FLOWING_WATER, new BlockBehaviorWater(), FluidBlockSerializer.INSTANCE, BlockTraits.IS_FLOWING, BlockTraits.FLUID_LEVEL); //8
        this.registerVanilla(WATER, new BlockBehaviorWater(), FluidBlockSerializer.INSTANCE, BlockTraits.IS_FLOWING, BlockTraits.FLUID_LEVEL); //9
        this.registerVanilla(FLOWING_LAVA, new BlockBehaviorLava(), FluidBlockSerializer.INSTANCE, BlockTraits.IS_FLOWING, BlockTraits.FLUID_LEVEL); //10
        this.registerVanilla(LAVA, new BlockBehaviorLava(), FluidBlockSerializer.INSTANCE, BlockTraits.IS_FLOWING, BlockTraits.FLUID_LEVEL); //11
        this.registerVanilla(SAND, new BlockBehaviorSand(), BlockTraits.SAND_TYPE); //12
        this.registerVanilla(GRAVEL, new BlockBehaviorGravel()); //13
        this.registerVanilla(GOLD_ORE); //14
        this.registerVanilla(IRON_ORE); //15
        this.registerVanilla(COAL_ORE, new BlockBehaviorOreCoal()); //16
        this.registerVanilla(LOG, new BlockBehaviorLog(), MultiBlockSerializers.LOG, BlockTraits.DEPRECATED, BlockTraits.AXIS, BlockTraits.TREE_SPECIES, BlockTraits.IS_STRIPPED); //17
        this.registerVanilla(LEAVES, new BlockBehaviorLeaves(), MultiBlockSerializers.LEAVES, BlockTraits.IS_PERSISTENT, BlockTraits.HAS_UPDATE, BlockTraits.TREE_SPECIES_OVERWORLD); //18
        this.registerVanilla(SPONGE, new BlockBehaviorSponge(), BlockTraits.SPONGE_TYPE); //19
        this.registerVanilla(GLASS, new BlockBehaviorGlass()); //20
        this.registerVanilla(LAPIS_ORE, new BlockBehaviorOreLapis()); //21
        this.registerVanilla(LAPIS_BLOCK, new BlockBehaviorLapis()); //22
        this.registerVanilla(DISPENSER, new BlockBehaviorDispenser(), BlockTraits.FACING_DIRECTION, BlockTraits.IS_TRIGGERED); //23
        this.registerVanilla(SANDSTONE, new BlockBehaviorSandstone(), BlockTraits.SAND_STONE_TYPE); //24
        this.registerVanilla(NOTEBLOCK, new BlockBehaviorNoteblock()); //25
        this.registerVanilla(BED, new BlockBehaviorBed(), BlockTraits.IS_HEAD_PIECE, BlockTraits.IS_OCCUPIED, BlockTraits.DIRECTION); //26
        this.registerVanilla(GOLDEN_RAIL, new BlockBehaviorRailPowered(), BlockTraits.SIMPLE_RAIL_DIRECTION, BlockTraits.IS_POWERED); //27
        this.registerVanilla(DETECTOR_RAIL, new BlockBehaviorRailDetector(), BlockTraits.SIMPLE_RAIL_DIRECTION, BlockTraits.IS_TRIGGERED); //28
        this.registerVanilla(WEB, new BlockBehaviorCobweb()); //30
        this.registerVanilla(TALL_GRASS, new BlockBehaviorTallGrass(), BlockTraits.TALL_GRASS_TYPE); //31
        this.registerVanilla(DEADBUSH, new BlockBehaviorDeadBush()); //32
        this.registerVanilla(PISTON, new BlockBehaviorPiston(), MultiBlockSerializers.PISTON, BlockTraits.FACING_DIRECTION, BlockTraits.IS_STICKY); //33
        this.registerVanilla(PISTON_ARM_COLLISION, new BlockBehaviorPistonHead(), MultiBlockSerializers.PISTON_ARM_COLLISION, BlockTraits.FACING_DIRECTION, BlockTraits.IS_STICKY); //34
        this.registerVanilla(WOOL, new BlockBehaviorWool(), BlockTraits.COLOR); //35
        this.registerVanilla(FLOWER, new BlockBehaviorFlower(), MultiBlockSerializers.FLOWER, BlockTraits.FLOWER_TYPE); //37 - 38
        this.registerVanilla(BROWN_MUSHROOM, new BlockBehaviorMushroomBrown()); //39
        this.registerVanilla(RED_MUSHROOM, new BlockBehaviorMushroomRed()); //40
        this.registerVanilla(GOLD_BLOCK, new BlockBehaviorGold()); //41
        this.registerVanilla(IRON_BLOCK, new BlockBehaviorIron()); //42
        this.registerVanilla(STONE_SLAB, new BlockBehaviorSlab(), SlabSerializer.INSTANCE, BlockTraits.SLAB_SLOT, BlockTraits.STONE_SLAB_TYPE); //44
        this.registerVanilla(BRICK_BLOCK, new BlockBehaviorBricks()); //45
        this.registerVanilla(TNT, new BlockBehaviorTNT(), BlockTraits.EXPLODE, BlockTraits.IS_ALLOWED_UNDERWATER); //46
        this.registerVanilla(BOOKSHELF, new BlockBehaviorBookshelf()); //47
        this.registerVanilla(MOSSY_COBBLESTONE); //48
        this.registerVanilla(OBSIDIAN, new BlockBehaviorObsidian()); //49
        this.registerVanilla(TORCH, new BlockBehaviorTorch(), MultiBlockSerializers.TORCH, BlockTraits.TORCH_DIRECTION, BlockTraits.IS_SOUL); //50
        this.registerVanilla(FIRE, new BlockBehaviorFire(), BlockTraits.AGE); //51
        this.registerVanilla(MOB_SPAWNER, new BlockBehaviorMobSpawner()); //52
        this.registerVanilla(WOODEN_STAIRS, new BlockBehaviorStairsWood(), MultiBlockSerializers.WOOD_STAIRS, BlockTraits.IS_UPSIDE_DOWN, BlockTraits.DIRECTION, BlockTraits.TREE_SPECIES); //53
        this.registerVanilla(CHEST, new BlockBehaviorChest(), BlockTraits.FACING_DIRECTION); //54
        this.registerVanilla(REDSTONE_WIRE, new BlockBehaviorRedstoneWire(), BlockTraits.REDSTONE_SIGNAL); //55
        this.registerVanilla(DIAMOND_ORE, new BlockBehaviorOreDiamond()); //56
        this.registerVanilla(DIAMOND_BLOCK, new BlockBehaviorDiamond()); //57
        this.registerVanilla(CRAFTING_TABLE, new BlockBehaviorCraftingTable()); //58
        this.registerVanilla(WHEAT, new BlockBehaviorWheat(), BlockTraits.GROWTH); //59
        this.registerVanilla(FARMLAND, new BlockBehaviorFarmland(), BlockTraits.MOISTURIZED_AMOUNT); //60
        this.registerVanilla(FURNACE, new BlockBehaviorFurnace(BlockEntityTypes.FURNACE), MultiBlockSerializers.FURNACE, BlockTraits.FACING_DIRECTION, BlockTraits.IS_EXTINGUISHED); //61
        this.registerVanilla(STANDING_SIGN, new BlockBehaviorSignPost(), MultiBlockSerializers.WOOD_STANDING_SIGN, BlockTraits.CARDINAL_DIRECTION, BlockTraits.TREE_SPECIES); //63
        this.registerVanilla(WOODEN_DOOR, new BlockBehaviorDoorWood(), MultiBlockSerializers.WOOD_DOOR, BlockTraits.IS_OPEN, BlockTraits.IS_UPPER_BLOCK, BlockTraits.IS_DOOR_HINGE, BlockTraits.DIRECTION, BlockTraits.TREE_SPECIES); //64
        this.registerVanilla(LADDER, new BlockBehaviorLadder(), BlockTraits.FACING_DIRECTION); //65
        this.registerVanilla(RAIL, new BlockBehaviorRail(RAIL, BlockTraits.RAIL_DIRECTION), BlockTraits.RAIL_DIRECTION); //66
        this.registerVanilla(STONE_STAIRS, new BlockBehaviorStairsCobblestone(), MultiBlockSerializers.STONE_STAIRS, BlockTraits.IS_UPSIDE_DOWN, BlockTraits.DIRECTION, BlockTraits.STONE_STAIRS_TYPE); //67
        this.registerVanilla(WALL_SIGN, new BlockBehaviorWallSign(), MultiBlockSerializers.WOOD_WALL_SIGN, BlockTraits.FACING_DIRECTION, BlockTraits.TREE_SPECIES); //68
        this.registerVanilla(LEVER, new BlockBehaviorLever(), BlockTraits.IS_OPEN, BlockTraits.LEVER_DIRECTION); //69
        this.registerVanilla(STONE_PRESSURE_PLATE, new BlockBehaviorPressurePlateStone(), MultiBlockSerializers.STONE_PRESSURE_PLATE, BlockTraits.REDSTONE_SIGNAL, BlockTraits.STONE_PRESSURE_PLATE_TYPE); //70
        this.registerVanilla(IRON_DOOR, new BlockBehaviorDoorIron(), BlockTraits.DIRECTION, BlockTraits.IS_OPEN, BlockTraits.IS_DOOR_HINGE, BlockTraits.IS_UPPER_BLOCK); //71
        this.registerVanilla(WOODEN_PRESSURE_PLATE, new BlockBehaviorPressurePlateWood(), MultiBlockSerializers.WOOD_PRESSURE_PLATE, BlockTraits.REDSTONE_SIGNAL, BlockTraits.TREE_SPECIES); //72
        this.registerVanilla(REDSTONE_ORE, new BlockBehaviorOreRedstone(), MultiBlockSerializers.REDSTONE_ORE, BlockTraits.IS_EXTINGUISHED); //73, 74
        this.registerVanilla(REDSTONE_TORCH, new BlockBehaviorRedstoneTorch(), MultiBlockSerializers.REDSTONE_TORCH, BlockTraits.TORCH_DIRECTION, BlockTraits.IS_POWERED); //75, 76
        this.registerVanilla(STONE_BUTTON, new BlockBehaviorButton(), MultiBlockSerializers.STONE_BUTTON, BlockTraits.FACING_DIRECTION, BlockTraits.IS_BUTTON_PRESSED, BlockTraits.STONE_BUTTON_TYPE); //77
        this.registerVanilla(SNOW_LAYER, new BlockBehaviorSnowLayer(), BlockTraits.IS_COVERED, BlockTraits.HEIGHT); //78
        this.registerVanilla(ICE, new BlockBehaviorIce()); //79
        this.registerVanilla(SNOW, new BlockBehaviorSnow()); //80
        this.registerVanilla(CACTUS, new BlockBehaviorCactus(), BlockTraits.AGE); //81
        this.registerVanilla(CLAY, new BlockBehaviorClay()); //82
        this.registerVanilla(REEDS, new ReedsBlockBehavior(), BlockTraits.AGE); //83
        this.registerVanilla(JUKEBOX, new BlockBehaviorJukebox()); //84
        this.registerVanilla(WOODEN_FENCE, new BlockBehaviorFenceWooden(), MultiBlockSerializers.WOOD_FENCE, BlockTraits.TREE_SPECIES); //85
        this.registerVanilla(PUMPKIN, new BlockBehaviorPumpkin(), BlockTraits.DIRECTION); //86
        this.registerVanilla(NETHERRACK, new BlockBehaviorNetherrack()); //87
        this.registerVanilla(SOUL_SAND, new BlockBehaviorSoulSand()); //88
        this.registerVanilla(GLOWSTONE, new BlockBehaviorGlowstone()); //89
        this.registerVanilla(PORTAL, new BlockBehaviorNetherPortal(), BlockTraits.PORTAL_AXIS); //90
        this.registerVanilla(LIT_PUMPKIN, new BlockBehaviorPumpkin(), BlockTraits.DIRECTION); //91
        this.registerVanilla(CAKE, new BlockBehaviorCake(), BlockTraits.BITE_COUNTER); //92
        this.registerVanilla(REPEATER, new BlockBehaviorRedstoneRepeater(), MultiBlockSerializers.REPEATER, BlockTraits.REPEATER_DELAY, BlockTraits.DIRECTION, BlockTraits.IS_POWERED); //93, 94
        this.registerVanilla(INVISIBLE_BEDROCK, new BlockBehaviorBedrockInvisible()); //95
        this.registerVanilla(WOODEN_TRAPDOOR, new BlockBehaviorTrapdoor(), MultiBlockSerializers.WOOD_TRAPDOOR, BlockTraits.IS_OPEN, BlockTraits.IS_UPSIDE_DOWN, BlockTraits.DIRECTION, BlockTraits.TREE_SPECIES); //96
        this.registerVanilla(MONSTER_EGG, new BlockBehaviorMonsterEgg(), BlockTraits.MONSTER_EGG_STONE_TYPE); //97
        this.registerVanilla(STONEBRICK, BlockTraits.STONE_BRICK_TYPE); //98
        this.registerVanilla(BROWN_MUSHROOM_BLOCK, new BlockBehaviorHugeMushroomBrown(), BlockTraits.HUGE_MUSHROOM_BITS); //99
        this.registerVanilla(RED_MUSHROOM_BLOCK, new BlockBehaviorHugeMushroomRed(), BlockTraits.HUGE_MUSHROOM_BITS); //100
        this.registerVanilla(IRON_BARS, new BlockBehaviorIronBars()); //101
        this.registerVanilla(GLASS_PANE, new BlockBehaviorGlassPane()); //102
        this.registerVanilla(MELON_BLOCK, new BlockBehaviorMelon()); //103
        this.registerVanilla(PUMPKIN_STEM, new BlockBehaviorStemPumpkin(), BlockTraits.FACING_DIRECTION, BlockTraits.GROWTH); //104
        this.registerVanilla(MELON_STEM, new BlockBehaviorStemMelon(), BlockTraits.FACING_DIRECTION, BlockTraits.GROWTH); //105
        this.registerVanilla(VINE, new BlockBehaviorVine(), BlockTraits.VINE_DIRECTION_BITS); //106
        this.registerVanilla(WOODEN_FENCE_GATE, new BlockBehaviorFenceGate(), MultiBlockSerializers.WOOD_FENCE_GATE, BlockTraits.IS_IN_WALL, BlockTraits.IS_OPEN, BlockTraits.DIRECTION, BlockTraits.TREE_SPECIES); //107
        this.registerVanilla(MYCELIUM, new BlockBehaviorMycelium()); //110
        this.registerVanilla(WATERLILY, new BlockBehaviorWaterLily()); //111
        this.registerVanilla(NETHER_BRICK, new BlockBehaviorBricksNether(), MultiBlockSerializers.NETHER_BRICKS, BlockTraits.NETHER_BRICK_TYPE); //112
        this.registerVanilla(NETHER_BRICK_FENCE, new BlockBehaviorFenceNetherBrick()); //113
        this.registerVanilla(NETHER_WART, new BlockBehaviorNetherWart(), BlockTraits.WART_GROWTH); //115
        this.registerVanilla(ENCHANTING_TABLE, new BlockBehaviorEnchantingTable()); //116
        this.registerVanilla(BREWING_STAND, new BlockBehaviorBrewingStand(), BlockTraits.IS_BREWING_C, BlockTraits.IS_BREWING_A, BlockTraits.IS_BREWING_B); //117
        this.registerVanilla(CAULDRON, new BlockBehaviorCauldron(), MultiBlockSerializers.CAULDRON, BlockTraits.FILL_LEVEL, BlockTraits.FLUID_TYPE, BlockTraits.CAULDRON_TYPE); //118
        this.registerVanilla(END_PORTAL, new BlockBehaviorEndPortal()); //119
        this.registerVanilla(END_PORTAL_FRAME, new BlockBehaviorEndPortalFrame(), BlockTraits.HAS_END_PORTAL_EYE, BlockTraits.DIRECTION); //120
        this.registerVanilla(END_STONE, new BlockBehaviorEndStone()); //121
        this.registerVanilla(DRAGON_EGG, new BlockBehaviorDragonEgg()); //122
        this.registerVanilla(REDSTONE_LAMP, new BlockBehaviorRedstoneLamp(), MultiBlockSerializers.REDSTONE_LAMP, BlockTraits.IS_POWERED); //123
        this.registerVanilla(DROPPER, NoopBlockBehavior.INSTANCE, BlockTraits.FACING_DIRECTION, BlockTraits.IS_TRIGGERED);
        this.registerVanilla(ACTIVATOR_RAIL, new BlockBehaviorRailActivator(), BlockTraits.SIMPLE_RAIL_DIRECTION, BlockTraits.IS_POWERED); //126
        this.registerVanilla(COCOA, new BlockBehaviorCocoa(), BlockTraits.COCOA_AGE, BlockTraits.DIRECTION); //127
        this.registerVanilla(EMERALD_ORE, new BlockBehaviorOreEmerald()); //129
        this.registerVanilla(ENDER_CHEST, new BlockBehaviorEnderChest(), BlockTraits.FACING_DIRECTION); //130
        this.registerVanilla(TRIPWIRE_HOOK, new BlockBehaviorTripWireHook(), BlockTraits.IS_POWERED, BlockTraits.IS_ATTACHED, BlockTraits.DIRECTION); //131
        this.registerVanilla(TRIPWIRE, new BlockBehaviorTripWire(), BlockTraits.IS_POWERED, BlockTraits.IS_SUSPENDED, BlockTraits.IS_DISARMED, BlockTraits.IS_ATTACHED); //132
        this.registerVanilla(EMERALD_BLOCK, new BlockBehaviorEmerald()); //133
        this.registerVanilla(COMMAND_BLOCK, NoopBlockBehavior.INSTANCE, BlockTraits.IS_CONDITIONAL, BlockTraits.FACING_DIRECTION);
        this.registerVanilla(BEACON, new BlockBehaviorBeacon()); //138
        this.registerVanilla(STONE_WALL, new BlockBehaviorWall(), MultiBlockSerializers.WALL, BlockTraits.WALL_CONNECTION_EAST, BlockTraits.HAS_POST, BlockTraits.WALL_CONNECTION_SOUTH, BlockTraits.WALL_CONNECTION_WEST, BlockTraits.WALL_CONNECTION_NORTH, BlockTraits.WALL_BLOCK_TYPE); //139
        this.registerVanilla(FLOWER_POT, new BlockBehaviorFlowerPot(), BlockTraits.HAS_UPDATE); //140
        this.registerVanilla(CARROTS, new BlockBehaviorCarrot(), BlockTraits.GROWTH); //141
        this.registerVanilla(POTATOES, new BlockBehaviorPotato(), BlockTraits.GROWTH); //142
        this.registerVanilla(WOODEN_BUTTON, new BlockBehaviorButton(), MultiBlockSerializers.WOOD_BUTTON, BlockTraits.IS_BUTTON_PRESSED, BlockTraits.FACING_DIRECTION, BlockTraits.TREE_SPECIES); //143
        this.registerVanilla(SKULL, new BlockBehaviorSkull(), BlockTraits.FACING_DIRECTION, BlockTraits.HAS_NO_DROP); //144
        this.registerVanilla(ANVIL, new BlockBehaviorAnvil(), BlockTraits.DAMAGE, BlockTraits.DIRECTION); //145
        this.registerVanilla(TRAPPED_CHEST, new BlockBehaviorTrappedChest(), BlockTraits.FACING_DIRECTION); //146
        this.registerVanilla(LIGHT_WEIGHTED_PRESSURE_PLATE, new BlockBehaviorWeightedPressurePlateLight(), BlockTraits.REDSTONE_SIGNAL); //147
        this.registerVanilla(HEAVY_WEIGHTED_PRESSURE_PLATE, new BlockBehaviorWeightedPressurePlateHeavy(), BlockTraits.REDSTONE_SIGNAL); //148
        this.registerVanilla(COMPARATOR, new BlockBehaviorRedstoneComparator(), MultiBlockSerializers.COMPARATOR, BlockTraits.IS_OUTPUT_LIT, BlockTraits.IS_OUTPUT_SUBTRACT, BlockTraits.DIRECTION, BlockTraits.IS_POWERED); //150
        this.registerVanilla(DAYLIGHT_DETECTOR, new BlockBehaviorDaylightDetector(), BlockTraits.REDSTONE_SIGNAL); //151
        this.registerVanilla(REDSTONE_BLOCK, new BlockBehaviorRedstone()); //152
        this.registerVanilla(QUARTZ_ORE, new BlockBehaviorOreQuartz()); //153
        this.registerVanilla(HOPPER, new BlockBehaviorHopper(), BlockTraits.FACING_DIRECTION, BlockTraits.IS_TOGGLED); //154
        this.registerVanilla(QUARTZ_BLOCK, new BlockBehaviorQuartz(), BlockTraits.CHISEL_TYPE, BlockTraits.AXIS); //155
        this.registerVanilla(WOODEN_SLAB, new BlockBehaviorSlabWood(), SlabSerializer.INSTANCE, BlockTraits.TREE_SPECIES, BlockTraits.SLAB_SLOT); //158
        this.registerVanilla(STAINED_HARDENED_CLAY, new BlockBehaviorTerracottaStained(), BlockTraits.COLOR); //159
        this.registerVanilla(STAINED_GLASS_PANE, new BlockBehaviorGlassPaneStained(), BlockTraits.COLOR); //160
        this.registerVanilla(SLIME, new BlockBehaviorSlime()); //165
        //166: glow_stick
        this.registerVanilla(IRON_TRAPDOOR, new BlockBehaviorTrapdoorIron(), BlockTraits.DIRECTION, BlockTraits.IS_UPSIDE_DOWN, BlockTraits.IS_OPEN); //167
        this.registerVanilla(PRISMARINE, new BlockBehaviorPrismarine(), BlockTraits.PRISMARINE_BLOCK_TYPE); //168
        this.registerVanilla(SEA_LANTERN, new BlockBehaviorSeaLantern()); //169
        this.registerVanilla(HAY_BLOCK, new BlockBehaviorHayBale(), BlockTraits.DEPRECATED, BlockTraits.AXIS); //170
        this.registerVanilla(CARPET, new BlockBehaviorCarpet(), BlockTraits.COLOR); //171
        this.registerVanilla(HARDENED_CLAY, new BlockBehaviorTerracotta()); //172
        this.registerVanilla(COAL_BLOCK, new BlockBehaviorCoal()); //173
        this.registerVanilla(PACKED_ICE, new BlockBehaviorIcePacked()); //174
        this.registerVanilla(DOUBLE_PLANT, new BlockBehaviorDoublePlant(), BlockTraits.IS_UPPER_BLOCK, BlockTraits.DOUBLE_PLANT_TYPE); //175
        this.registerVanilla(STANDING_BANNER, new BlockBehaviorBanner(), BlockTraits.CARDINAL_DIRECTION); //176
        this.registerVanilla(WALL_BANNER, new BlockBehaviorWallBanner(), BlockTraits.FACING_DIRECTION); //177
        this.registerVanilla(DAYLIGHT_DETECTOR_INVERTED, new BlockBehaviorDaylightDetectorInverted(), BlockTraits.REDSTONE_SIGNAL); //178
        this.registerVanilla(RED_SANDSTONE, new BlockBehaviorRedSandstone(), BlockTraits.SAND_STONE_TYPE); //179
        this.registerVanilla(REPEATING_COMMAND_BLOCK, NoopBlockBehavior.INSTANCE, BlockTraits.IS_CONDITIONAL, BlockTraits.FACING_DIRECTION); //188
        this.registerVanilla(CHAIN_COMMAND_BLOCK, NoopBlockBehavior.INSTANCE, BlockTraits.IS_CONDITIONAL, BlockTraits.FACING_DIRECTION); //189
        this.registerVanilla(HARD_GLASS_PANE, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(HARD_STAINED_GLASS_PANE, NoopBlockBehavior.INSTANCE, BlockTraits.COLOR);
        this.registerVanilla(CHEMICAL_HEAT, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(GRASS_PATH, new BlockBehaviorGrassPath()); //198
        this.registerVanilla(FRAME, new BlockBehaviorItemFrame(), BlockTraits.FACING_DIRECTION, BlockTraits.HAS_MAP); //199
        this.registerVanilla(CHORUS_FLOWER, new BlockBehaviorChorusFlower(), BlockTraits.CHORUS_AGE); //200
        this.registerVanilla(PURPUR_BLOCK, new BlockBehaviorPurpur(), BlockTraits.CHISEL_TYPE, BlockTraits.AXIS); //201
        this.registerVanilla(COLORED_TORCH_RG, new BlockBehaviorTorch(), BlockTraits.HAS_COLOR, BlockTraits.TORCH_DIRECTION);
        this.registerVanilla(COLORED_TORCH_BP, new BlockBehaviorTorch(), BlockTraits.HAS_COLOR, BlockTraits.TORCH_DIRECTION);
        this.registerVanilla(UNDYED_SHULKER_BOX, new BlockBehaviorUndyedShulkerBox()); //205
        this.registerVanilla(END_BRICKS, new BlockBehaviorBricksEndStone()); //206
        this.registerVanilla(FROSTED_ICE, NoopBlockBehavior.INSTANCE, BlockTraits.ICE_AGE); //206
        this.registerVanilla(END_ROD, new BlockBehaviorEndRod(), BlockTraits.FACING_DIRECTION); //208
        this.registerVanilla(END_GATEWAY, new BlockBehaviorEndGateway()); //209
        this.registerVanilla(ALLOW, NoopBlockBehavior.INSTANCE); //210
        this.registerVanilla(DENY, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(BORDER_BLOCK, NoopBlockBehavior.INSTANCE, BlockTraits.WALL_CONNECTION_EAST, BlockTraits.HAS_POST, BlockTraits.WALL_CONNECTION_SOUTH, BlockTraits.WALL_CONNECTION_WEST, BlockTraits.WALL_CONNECTION_NORTH);
        this.registerVanilla(MAGMA, new BlockBehaviorMagma()); //213
        this.registerVanilla(NETHER_WART_BLOCK, new BlockNetherWartBlockBehavior(), MultiBlockSerializers.WART_BLOCK, BlockTraits.TREE_SPECIES_NETHER); //214
        this.registerVanilla(BONE_BLOCK, new BlockBehaviorBone(), BlockTraits.DEPRECATED, BlockTraits.AXIS); //216
        this.registerVanilla(STRUCTURE_VOID, NoopBlockBehavior.INSTANCE, BlockTraits.STRUCTURE_VOID_TYPE);
        this.registerVanilla(SHULKER_BOX, new BlockBehaviorShulkerBox(), BlockTraits.COLOR); //218
        this.registerVanilla(GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed(), MultiBlockSerializers.TERRACOTTA, BlockTraits.FACING_DIRECTION, BlockTraits.COLOR); //219 - 235
        this.registerVanilla(CONCRETE, new BlockBehaviorConcrete(), BlockTraits.COLOR); //236
        this.registerVanilla(CONCRETE_POWDER, new BlockBehaviorConcretePowder(), BlockTraits.COLOR); //237
        this.registerVanilla(CHEMISTRY_TABLE, NoopBlockBehavior.INSTANCE, BlockTraits.CHEMISTRY_TABLE_TYPE, BlockTraits.DIRECTION);
        this.registerVanilla(UNDERWATER_TORCH, new BlockBehaviorTorch(), BlockTraits.TORCH_DIRECTION);
        this.registerVanilla(CHORUS_PLANT, new BlockBehaviorChorusPlant()); //240
        this.registerVanilla(STAINED_GLASS, new BlockBehaviorGlassStained(), BlockTraits.COLOR); //241
        this.registerVanilla(CAMERA, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(PODZOL, new BlockBehaviorPodzol()); //243
        this.registerVanilla(BEETROOT, new BlockBehaviorBeetroot(), BlockTraits.GROWTH); //244
        this.registerVanilla(STONECUTTER); //245
        this.registerVanilla(GLOWING_OBSIDIAN, new BlockBehaviorObsidianGlowing()); //246
        this.registerVanilla(NETHER_REACTOR, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(INFO_UPDATE, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(INFO_UPDATE2, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(MOVING_BLOCK, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(OBSERVER, new BlockBehaviorObserver(), BlockTraits.FACING_DIRECTION, BlockTraits.IS_POWERED); //251
        this.registerVanilla(STRUCTURE_BLOCK, NoopBlockBehavior.INSTANCE, BlockTraits.STRUCTURE_BLOCK_TYPE); //252
        this.registerVanilla(HARD_GLASS, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(HARD_STAINED_GLASS, NoopBlockBehavior.INSTANCE, BlockTraits.COLOR);
        this.registerVanilla(RESERVED6, NoopBlockBehavior.INSTANCE);
        //256: unknown
        this.registerVanilla(BLUE_ICE, new BlockBehaviorBlueIce()); //266
        this.registerVanilla(ELEMENT, NoopBlockBehavior.INSTANCE, MultiBlockSerializers.ELEMENT, BlockTraits.ELEMENT_TYPE);
        this.registerVanilla(SEAGRASS, NoopBlockBehavior.INSTANCE, BlockTraits.SEA_GRASS_TYPE);
        this.registerVanilla(CORAL, NoopBlockBehavior.INSTANCE, BlockTraits.CORAL_COLOR, BlockTraits.IS_DEAD);
        this.registerVanilla(CORAL_BLOCK, NoopBlockBehavior.INSTANCE, BlockTraits.CORAL_COLOR, BlockTraits.IS_DEAD);
        this.registerVanilla(CORAL_FAN, NoopBlockBehavior.INSTANCE, BlockTraits.CORAL_COLOR, BlockTraits.CORAL_FAN_DIRECTION);
        this.registerVanilla(CORAL_FAN_DEAD, NoopBlockBehavior.INSTANCE, BlockTraits.CORAL_FAN_DIRECTION, BlockTraits.CORAL_COLOR);
        this.registerVanilla(CORAL_FAN_HANG, NoopBlockBehavior.INSTANCE, new CoralHangBlockSerializer(), BlockTraits.DIRECTION, BlockTraits.IS_DEAD, BlockTraits.CORAL_HANG_COLOR);
        this.registerVanilla(KELP, new BlockBehaviorKelp(), BlockTraits.KELP_AGE);//393
        this.registerVanilla(DRIED_KELP_BLOCK, new BlockBehaviorDriedKelp()); //394
        this.registerVanilla(CARVED_PUMPKIN, NoopBlockBehavior.INSTANCE, BlockTraits.DIRECTION);
        this.registerVanilla(SEA_PICKLE, NoopBlockBehavior.INSTANCE, BlockTraits.CLUSTER_COUNT, BlockTraits.IS_DEAD);
        this.registerVanilla(CONDUIT, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(TURTLE_EGG, NoopBlockBehavior.INSTANCE, BlockTraits.CRACKED_STATE, BlockTraits.TURTLE_EGG_COUNT);
        this.registerVanilla(BUBBLE_COLUMN, NoopBlockBehavior.INSTANCE, BlockTraits.HAS_DRAG_DOWN);
        this.registerVanilla(BARRIER, new BlockBehaviorBarrier()); //415
        this.registerVanilla(BAMBOO, NoopBlockBehavior.INSTANCE, BlockTraits.BAMBOO_LEAF_SIZE, BlockTraits.HAS_AGE, BlockTraits.BAMBOO_STALK_THICKNESS);
        this.registerVanilla(BAMBOO_SAPLING, NoopBlockBehavior.INSTANCE, BlockTraits.TREE_SPECIES_OVERWORLD, BlockTraits.HAS_AGE);
        this.registerVanilla(SCAFFOLDING, NoopBlockBehavior.INSTANCE, BlockTraits.STABILITY, BlockTraits.HAS_STABILITY_CHECK);
        this.registerVanilla(SMOOTH_STONE); // 437
        this.registerVanilla(LECTERN, new BlockBehaviorLectern(), BlockTraits.IS_POWERED, BlockTraits.DIRECTION); //448
        this.registerVanilla(GRINDSTONE, NoopBlockBehavior.INSTANCE, BlockTraits.DIRECTION, BlockTraits.ATTACHMENT);
        this.registerVanilla(BLAST_FURNACE, new BlockBehaviorFurnace(BlockEntityTypes.BLAST_FURNACE), MultiBlockSerializers.BLAST_FURNACE, BlockTraits.FACING_DIRECTION, BlockTraits.IS_EXTINGUISHED); // 450
        this.registerVanilla(STONECUTTER_BLOCK, NoopBlockBehavior.INSTANCE, BlockTraits.FACING_DIRECTION); // 451
        this.registerVanilla(SMOKER, new BlockBehaviorFurnace(BlockEntityTypes.FURNACE), MultiBlockSerializers.SMOKER, BlockTraits.FACING_DIRECTION, BlockTraits.IS_EXTINGUISHED); //452
        this.registerVanilla(CARTOGRAPHY_TABLE, NoopBlockBehavior.INSTANCE); //454
        this.registerVanilla(FLETCHING_TABLE, NoopBlockBehavior.INSTANCE); //455
        this.registerVanilla(SMITHING_TABLE, NoopBlockBehavior.INSTANCE); //456
        this.registerVanilla(BARREL, new BlockBehaviorBarrel(), BlockTraits.FACING_DIRECTION, BlockTraits.IS_OPEN); // 457
        this.registerVanilla(LOOM, NoopBlockBehavior.INSTANCE, BlockTraits.DIRECTION);
        this.registerVanilla(BELL, NoopBlockBehavior.INSTANCE, BlockTraits.ATTACHMENT, BlockTraits.IS_TOGGLED, BlockTraits.DIRECTION);
        this.registerVanilla(SWEET_BERRY_BUSH, NoopBlockBehavior.INSTANCE, BlockTraits.GROWTH);
        this.registerVanilla(LANTERN, NoopBlockBehavior.INSTANCE, MultiBlockSerializers.LANTERN, BlockTraits.IS_HANGING, BlockTraits.IS_SOUL);
        this.registerVanilla(CAMPFIRE, new BlockBehaviorCampfire(), MultiBlockSerializers.CAMPFIRE, BlockTraits.IS_EXTINGUISHED, BlockTraits.DIRECTION, BlockTraits.IS_SOUL);//464
        this.registerVanilla(JIGSAW, NoopBlockBehavior.INSTANCE, new JigsawSerializer(), BlockTraits.FACING_DIRECTION, BlockTraits.DIRECTION);//466
        this.registerVanilla(WOOD, new BlockBehaviorWood(), MultiBlockSerializers.WOOD, BlockTraits.TREE_SPECIES, BlockTraits.IS_STRIPPED, BlockTraits.AXIS, BlockTraits.DEPRECATED); //467
        this.registerVanilla(COMPOSTER, NoopBlockBehavior.INSTANCE, BlockTraits.COMPOSTER_FILL_LEVEL);//468
        this.registerVanilla(LIGHT_BLOCK, new BlockBehaviorLight(), BlockTraits.LIGHT_LEVEL);//470
        this.registerVanilla(WITHER_ROSE, NoopBlockBehavior.INSTANCE);//471
        this.registerVanilla(BEE_NEST, NoopBlockBehavior.INSTANCE, BlockTraits.DIRECTION, BlockTraits.HONEY_LEVEL);//473
        this.registerVanilla(BEEHIVE, NoopBlockBehavior.INSTANCE, BlockTraits.DIRECTION, BlockTraits.HONEY_LEVEL);//474
        this.registerVanilla(HONEY_BLOCK, NoopBlockBehavior.INSTANCE);//475
        this.registerVanilla(HONEYCOMB_BLOCK, new BlockHoneycombBlockBehavior()); //476
        this.registerVanilla(LODESTONE, NoopBlockBehavior.INSTANCE);//477
        this.registerVanilla(NETHER_ROOTS, NoopBlockBehavior.INSTANCE, MultiBlockSerializers.ROOTS, BlockTraits.TREE_SPECIES_NETHER);//478
        this.registerVanilla(NETHER_FUNGUS, NoopBlockBehavior.INSTANCE, MultiBlockSerializers.FUNGUS, BlockTraits.TREE_SPECIES_NETHER);//482
        this.registerVanilla(SHROOMLIGHT, NoopBlockBehavior.INSTANCE);//484
        this.registerVanilla(WEEPING_VINES, NoopBlockBehavior.INSTANCE, BlockTraits.WEEPING_VINES_AGE);//485
        this.registerVanilla(NETHER_NYLIUM, NoopBlockBehavior.INSTANCE, MultiBlockSerializers.NYLIUM, BlockTraits.TREE_SPECIES_NETHER);//486
        this.registerVanilla(BASALT, NoopBlockBehavior.INSTANCE, BlockTraits.AXIS);//490
        this.registerVanilla(POLISHED_BASALT, NoopBlockBehavior.INSTANCE, BlockTraits.AXIS);//590
        this.registerVanilla(SOUL_SOIL, NoopBlockBehavior.INSTANCE);//491
        this.registerVanilla(SOUL_FIRE, NoopBlockBehavior.INSTANCE, BlockTraits.AGE);//492
        this.registerVanilla(NETHER_SPROUTS, NoopBlockBehavior.INSTANCE);//493
        this.registerVanilla(TARGET, NoopBlockBehavior.INSTANCE);//494
        this.registerVanilla(NETHERITE_BLOCK, NoopBlockBehavior.INSTANCE);//525
        this.registerVanilla(ANCIENT_DEBRIS, NoopBlockBehavior.INSTANCE);//526
        this.registerVanilla(RESPAWN_ANCHOR, NoopBlockBehavior.INSTANCE, BlockTraits.RESPAWN_ANCHOR_CHARGE);//527
        this.registerVanilla(BLACKSTONE, NoopBlockBehavior.INSTANCE);//528
        this.registerVanilla(POLISHED_BLACKSTONE_BRICKS, NoopBlockBehavior.INSTANCE);//529
        this.registerVanilla(CHISELED_POLISHED_BLACKSTONE, NoopBlockBehavior.INSTANCE);//534
        this.registerVanilla(CRACKED_POLISHED_BLACKSTONE_BRICKS, NoopBlockBehavior.INSTANCE);//535
        this.registerVanilla(GILDED_BLACKSTONE, NoopBlockBehavior.INSTANCE);//536
        this.registerVanilla(CHAIN, NoopBlockBehavior.INSTANCE, BlockTraits.AXIS);//541
        this.registerVanilla(TWISTING_VINES, NoopBlockBehavior.INSTANCE, BlockTraits.TWISTING_VINES_AGE);//542
        this.registerVanilla(NETHER_GOLD_ORE, NoopBlockBehavior.INSTANCE);//543
        this.registerVanilla(CRYING_OBSIDIAN, NoopBlockBehavior.INSTANCE);//544
        this.registerVanilla(POLISHED_BLACKSTONE, NoopBlockBehavior.INSTANCE);//546
        this.registerVanilla(QUARTZ_BRICKS, NoopBlockBehavior.INSTANCE);//559

        this.registerVanilla(UNKNOWN, NoopBlockBehavior.INSTANCE);
    }
}
