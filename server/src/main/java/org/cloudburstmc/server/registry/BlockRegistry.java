package org.cloudburstmc.server.registry;

import com.google.common.collect.HashBiMap;
import com.nukkitx.blockstateupdater.BlockStateUpdaters;
import com.nukkitx.nbt.NbtList;
import com.nukkitx.nbt.NbtMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.server.block.BlockCategory;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.behavior.*;
import org.cloudburstmc.server.block.serializer.BlockSerializer;
import org.cloudburstmc.server.block.serializer.NoopBlockSerializer;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.block.util.BlockStateMetaMappings;
import org.cloudburstmc.server.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.utils.Identifier;

import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.cloudburstmc.server.block.BlockTypes.*;

@Log4j2
public class BlockRegistry implements Registry {
    private static final BlockRegistry INSTANCE = new BlockRegistry();

    private final Reference2ReferenceMap<Identifier, BlockBehavior> behaviorMap = new Reference2ReferenceOpenHashMap<>();
    private final Reference2ReferenceMap<Identifier, BlockSerializer> serializerMap = new Reference2ReferenceOpenHashMap<>();
    private final HashBiMap<Identifier, Integer> idLegacyMap = HashBiMap.create();
    private final EnumMap<BlockCategory, ReferenceSet<Identifier>> categoryMap = new EnumMap<>(BlockCategory.class);
    private final AtomicInteger customIdAllocator = new AtomicInteger(1000);
    private final BlockPalette palette = BlockPalette.INSTANCE;
    private NbtMap propertiesTag;
    private volatile boolean closed;

    private BlockRegistry() {
        for (BlockCategory category : BlockCategory.values()) {
            categoryMap.put(category, new ReferenceOpenHashSet<>());
        }

        this.registerVanillaBlocks();
    }

    public static BlockRegistry get() {
        return INSTANCE;
    }

    public synchronized void register(Identifier id, BlockBehavior behavior) throws RegistryException {
        registerVanilla(id, behavior, null);

        // generate legacy ID (Not sure why we need to but it's a requirement)
        int legacyId = this.customIdAllocator.getAndIncrement();
        this.idLegacyMap.put(id, legacyId);
    }

    private void registerVanilla(Identifier id, BlockBehavior behavior, BlockTrait<?>... traits) throws RegistryException {
        this.registerVanilla(id, behavior, NoopBlockSerializer.INSTANCE, traits);
    }

    private synchronized void registerVanilla(Identifier id, BlockBehavior behavior, BlockSerializer serializer,
                                              BlockTrait<?>... traits) throws RegistryException {
        checkNotNull(id, "id");
        checkNotNull(behavior, "behavior");
        checkNotNull(serializer, "serializer");
        checkNotNull(traits, "traits");
        checkClosed();
        if (this.behaviorMap.containsKey(id)) throw new RegistryException(id + " is already registered");

        this.registerVanilla(id, behavior);
        this.palette.addBlock(id, serializer, traits);
    }

    public boolean inCategory(Identifier type, BlockCategory category) {
        return categoryMap.get(category).add(type);
    }

    public boolean inCategories(Identifier type, BlockCategory... categories) {
        for (BlockCategory category : categories) {
            if (!inCategory(type, category)) {
                return false;
            }
        }

        return true;
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

    public BlockState getBlock(Identifier identifier) {
        return palette.getDefaultState(identifier);
    }

    public BlockState getBlock(Item item) {
        return BlockStateMetaMappings.getStateFromMeta(item);
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

        // generate cache

        this.propertiesTag = NbtMap.EMPTY;
    }

    private void checkClosed() throws RegistryException {
        if (this.closed) {
            throw new RegistryException("Registration has been closed");
        }
    }

    public NbtList<NbtMap> getPaletteTag() {
        return palette.getPalette();
    }

    public NbtMap getPropertiesTag() {
        return propertiesTag;
    }

    public BlockBehavior getBehavior(Identifier blockType) {
        return this.behaviorMap.get(blockType);
    }

    private void registerVanillaBlocks() {
        this.registerVanilla(AIR, new BlockBehaviorAir()); //0
        this.registerVanilla(STONE, new BlockBehaviorStone(), BlockTraits.STONE_TYPE); //1
        this.registerVanilla(GRASS, new BlockBehaviorGrass()); //2
        this.registerVanilla(DIRT, new BlockBehaviorDirt(), BlockTraits.DIRT_TYPE); //3
        this.registerVanilla(COBBLESTONE, new BlockBehaviorCobblestone()); //4
        this.registerVanilla(PLANKS, new BlockBehaviorPlanks(), BlockTraits.TREE_SPECIES); //5
        this.registerVanilla(SAPLING, new BlockBehaviorSapling(), BlockTraits.TREE_SPECIES, BlockTraits.HAS_AGE); //6
        this.registerVanilla(BEDROCK, new BlockBehaviorBedrock(), BlockTraits.HAS_INFINIBURN); //7
        this.registerVanilla(FLOWING_WATER, new BlockBehaviorWater(), BlockTraits.IS_FLOWING, BlockTraits.FLUID_LEVEL); //8
        this.registerVanilla(WATER, new BlockBehaviorWater(), BlockTraits.IS_FLOWING, BlockTraits.FLUID_LEVEL); //9
        this.registerVanilla(FLOWING_LAVA, new BlockBehaviorLava(), BlockTraits.IS_FLOWING, BlockTraits.FLUID_LEVEL); //10
        this.registerVanilla(LAVA, new BlockBehaviorLava(), BlockTraits.IS_FLOWING, BlockTraits.FLUID_LEVEL); //11
        this.registerVanilla(SAND, new BlockBehaviorSand(), BlockTraits.SAND_TYPE); //12
        this.registerVanilla(GRAVEL, new BlockBehaviorGravel()); //13
        this.registerVanilla(GOLD_ORE, new BlockBehaviorOreGold()); //14
        this.registerVanilla(IRON_ORE, new BlockBehaviorOreIron()); //15
        this.registerVanilla(COAL_ORE, new BlockBehaviorOreCoal()); //16
        this.registerVanilla(LOG, new BlockBehaviorLog(), BlockTraits.TREE_SPECIES, BlockTraits.AXIS); //17
        this.registerVanilla(LEAVES, new BlockBehaviorLeaves(), BlockTraits.TREE_SPECIES, BlockTraits.IS_PERSISTENT, BlockTraits.HAS_UPDATE); //18
        this.registerVanilla(SPONGE, new BlockBehaviorSponge(), BlockTraits.SPONGE_TYPE); //19
        this.registerVanilla(GLASS, new BlockBehaviorGlass()); //20
        this.registerVanilla(LAPIS_ORE, new BlockBehaviorOreLapis()); //21
        this.registerVanilla(LAPIS_BLOCK, new BlockBehaviorLapis()); //22
        this.registerVanilla(DISPENSER, new BlockBehaviorDispenser(), BlockTraits.IS_TRIGGERED, BlockTraits.DIRECTION); //23
        this.registerVanilla(SANDSTONE, new BlockBehaviorSandstone(), BlockTraits.SAND_STONE_TYPE); //24
        this.registerVanilla(NOTEBLOCK, new BlockBehaviorNoteblock()); //25
        this.registerVanilla(BED, new BlockBehaviorBed(), BlockTraits.IS_OCCUPIED, BlockTraits.IS_HEAD_PIECE, BlockTraits.DIRECTION); //26
        this.registerVanilla(GOLDEN_RAIL, new BlockBehaviorRailPowered(), BlockTraits.IS_POWERED, BlockTraits.SIMPLE_RAIL_DIRECTION); //27
        this.registerVanilla(DETECTOR_RAIL, new BlockBehaviorRailDetector(), BlockTraits.IS_TRIGGERED, BlockTraits.SIMPLE_RAIL_DIRECTION); //28
        this.registerVanilla(STICKY_PISTON, new BlockBehaviorPistonSticky(), BlockTraits.FACING_DIRECTION); //29
        this.registerVanilla(WEB, new BlockBehaviorCobweb()); //30
        this.registerVanilla(TALL_GRASS, new BlockBehaviorTallGrass(), BlockTraits.TALL_GRASS_TYPE); //31
        this.registerVanilla(DEADBUSH, new BlockBehaviorDeadBush()); //32
        this.registerVanilla(PISTON, new BlockBehaviorPiston(), BlockTraits.FACING_DIRECTION); //33
        this.registerVanilla(PISTON_ARM_COLLISION, new BlockBehaviorPistonHead(), BlockTraits.FACING_DIRECTION); //34
        this.registerVanilla(WOOL, new BlockBehaviorWool(), BlockTraits.COLOR); //35
        this.registerVanilla(YELLOW_FLOWER, new BlockBehaviorDandelion()); //37
        this.registerVanilla(RED_FLOWER, new BlockBehaviorFlower(), BlockTraits.FLOWER_TYPE); //38
        this.registerVanilla(BROWN_MUSHROOM, new BlockBehaviorMushroomBrown()); //39
        this.registerVanilla(RED_MUSHROOM, new BlockBehaviorMushroomRed()); //40
        this.registerVanilla(GOLD_BLOCK, new BlockBehaviorGold()); //41
        this.registerVanilla(IRON_BLOCK, new BlockBehaviorIron()); //42
        this.registerVanilla(DOUBLE_STONE_SLAB, new BlockBehaviorDoubleSlab(STONE_SLAB, BlockTraits.STONE_SLAB_TYPE), BlockTraits.IS_TOP_SLOT, BlockTraits.STONE_SLAB_TYPE); //43
        this.registerVanilla(STONE_SLAB, new BlockBehaviorSlab(STONE_SLAB, DOUBLE_STONE_SLAB), BlockTraits.IS_TOP_SLOT, BlockTraits.STONE_SLAB_TYPE); //44
        this.registerVanilla(BRICK_BLOCK, new BlockBehaviorBricks()); //45
        this.registerVanilla(TNT, new BlockBehaviorTNT()); //46
        this.registerVanilla(BOOKSHELF, new BlockBehaviorBookshelf()); //47
        this.registerVanilla(MOSSY_COBBLESTONE, new BlockBehaviorMossStone()); //48
        this.registerVanilla(OBSIDIAN, new BlockBehaviorObsidian()); //49
        this.registerVanilla(TORCH, new BlockBehaviorTorch(), BlockTraits.TORCH_DIRECTION); //50
        this.registerVanilla(FIRE, new BlockBehaviorFire(), BlockTraits.AGE); //51
        this.registerVanilla(MOB_SPAWNER, new BlockBehaviorMobSpawner()); //52
        this.registerVanilla(OAK_STAIRS, new BlockBehaviorStairsWood(), BlockTraits.DIRECTION, BlockTraits.IS_UPSIDE_DOWN); //53
        this.registerVanilla(CHEST, new BlockBehaviorChest(), BlockTraits.FACING_DIRECTION); //54
        this.registerVanilla(REDSTONE_WIRE, new BlockBehaviorRedstoneWire(), BlockTraits.REDSTONE_SIGNAL); //55
        this.registerVanilla(DIAMOND_ORE, new BlockBehaviorOreDiamond()); //56
        this.registerVanilla(DIAMOND_BLOCK, new BlockBehaviorDiamond()); //57
        this.registerVanilla(CRAFTING_TABLE, new BlockBehaviorCraftingTable()); //58
        this.registerVanilla(WHEAT, new BlockBehaviorWheat(), BlockTraits.GROWTH); //59
        this.registerVanilla(FARMLAND, new BlockBehaviorFarmland(), BlockTraits.MOISTURIZED_AMOUNT); //60
        this.registerVanilla(FURNACE, new BlockBehaviorFurnace(BlockEntityTypes.FURNACE), BlockTraits.FACING_DIRECTION); //61
        this.registerVanilla(LIT_FURNACE, new BlockBehaviorFurnaceBurning(BlockEntityTypes.FURNACE), BlockTraits.FACING_DIRECTION); //62
        this.registerVanilla(STANDING_SIGN, new BlockBehaviorSignPost(), BlockTraits.CARDINAL_DIRECTION); //63
        this.registerVanilla(WOODEN_DOOR, new BlockBehaviorDoorWood(), BlockTraits.IS_OPEN, BlockTraits.DIRECTION, BlockTraits.IS_DOOR_HINGE, BlockTraits.IS_UPPER_BLOCK); //64
        this.registerVanilla(LADDER, new BlockBehaviorLadder(), BlockTraits.FACING_DIRECTION); //65
        this.registerVanilla(RAIL, new BlockBehaviorRail(RAIL, BlockTraits.RAIL_DIRECTION), BlockTraits.RAIL_DIRECTION); //66
        this.registerVanilla(STONE_STAIRS, new BlockBehaviorStairsCobblestone(), BlockTraits.DIRECTION, BlockTraits.IS_UPSIDE_DOWN); //67
        this.registerVanilla(WALL_SIGN, new BlockBehaviorWallSign(), BlockTraits.FACING_DIRECTION); //68
        this.registerVanilla(LEVER, new BlockBehaviorLever(), BlockTraits.FACING_DIRECTION, BlockTraits.IS_OPEN); //69
        this.registerVanilla(STONE_PRESSURE_PLATE, new BlockBehaviorPressurePlateStone(), BlockTraits.REDSTONE_SIGNAL); //70
        this.registerVanilla(IRON_DOOR, new BlockBehaviorDoorIron(), BlockTraits.DIRECTION, BlockTraits.IS_OPEN, BlockTraits.IS_DOOR_HINGE, BlockTraits.IS_UPPER_BLOCK); //71
        this.registerVanilla(WOODEN_PRESSURE_PLATE, new BlockBehaviorPressurePlateWood(), BlockTraits.REDSTONE_SIGNAL); //72
        this.registerVanilla(REDSTONE_ORE, new BlockBehaviorOreRedstone()); //73
        this.registerVanilla(LIT_REDSTONE_ORE, new BlockBehaviorOreRedstoneGlowing()); //74
        this.registerVanilla(UNLIT_REDSTONE_TORCH, new BlockBehaviorRedstoneTorch(), BlockTraits.TORCH_DIRECTION);
        this.registerVanilla(REDSTONE_TORCH, new BlockBehaviorRedstoneTorch(), BlockTraits.TORCH_DIRECTION); //76
        this.registerVanilla(STONE_BUTTON, new BlockBehaviorButtonStone(), BlockTraits.FACING_DIRECTION, BlockTraits.IS_BUTTON_PRESSED); //77
        this.registerVanilla(SNOW_LAYER, new BlockBehaviorSnowLayer(), BlockTraits.HEIGHT, BlockTraits.IS_COVERED); //78
        this.registerVanilla(ICE, new BlockBehaviorIce()); //79
        this.registerVanilla(SNOW, new BlockBehaviorSnow()); //80
        this.registerVanilla(CACTUS, new BlockBehaviorCactus(), BlockTraits.AGE); //81
        this.registerVanilla(CLAY, new BlockBehaviorClay()); //82
        this.registerVanilla(REEDS, new ReedsBlockBehavior(), BlockTraits.AGE); //83
        this.registerVanilla(JUKEBOX, new BlockBehaviorJukebox()); //84
        this.registerVanilla(FENCE, new BlockBehaviorFenceWooden(), BlockTraits.TREE_SPECIES); //85
        this.registerVanilla(PUMPKIN, new BlockBehaviorPumpkin(PUMPKIN), BlockTraits.DIRECTION); //86
        this.registerVanilla(NETHERRACK, new BlockBehaviorNetherrack()); //87
        this.registerVanilla(SOUL_SAND, new BlockBehaviorSoulSand()); //88
        this.registerVanilla(GLOWSTONE, new BlockBehaviorGlowstone()); //89
        this.registerVanilla(PORTAL, new BlockBehaviorNetherPortal(), BlockTraits.PORTAL_AXIS); //90
        this.registerVanilla(LIT_PUMPKIN, new BlockBehaviorPumpkinLit(), BlockTraits.DIRECTION); //91
        this.registerVanilla(CAKE, new BlockBehaviorCake(), BlockTraits.BITE_COUNTER); //92
        this.registerVanilla(UNPOWERED_REPEATER, new BlockBehaviorRedstoneRepeater(UNPOWERED_REPEATER), BlockTraits.DIRECTION, BlockTraits.REPEATER_DELAY); //93
        this.registerVanilla(POWERED_REPEATER, new BlockBehaviorRedstoneRepeater(POWERED_REPEATER), BlockTraits.DIRECTION, BlockTraits.REPEATER_DELAY); //94
        this.registerVanilla(INVISIBLE_BEDROCK, new BlockBehaviorBedrockInvisible()); //95
        this.registerVanilla(TRAPDOOR, new BlockBehaviorTrapdoor(), BlockTraits.IS_OPEN, BlockTraits.DIRECTION, BlockTraits.IS_UPSIDE_DOWN); //96
        this.registerVanilla(MONSTER_EGG, new BlockBehaviorMonsterEgg()); //97
        this.registerVanilla(STONEBRICK, new BlockBehaviorBricksStone()); //98
        this.registerVanilla(BROWN_MUSHROOM_BLOCK, new BlockBehaviorHugeMushroomBrown(), BlockTraits.HUGE_MUSHROOM_BITS); //99
        this.registerVanilla(RED_MUSHROOM_BLOCK, new BlockBehaviorHugeMushroomRed(), BlockTraits.HUGE_MUSHROOM_BITS); //100
        this.registerVanilla(IRON_BARS, new BlockBehaviorIronBars()); //101
        this.registerVanilla(GLASS_PANE, new BlockBehaviorGlassPane()); //102
        this.registerVanilla(MELON_BLOCK, new BlockBehaviorMelon()); //103
        this.registerVanilla(PUMPKIN_STEM, new BlockBehaviorStemPumpkin(), BlockTraits.GROWTH, BlockTraits.FACING_DIRECTION); //104
        this.registerVanilla(MELON_STEM, new BlockBehaviorStemMelon(), BlockTraits.GROWTH, BlockTraits.FACING_DIRECTION); //105
        this.registerVanilla(VINE, new BlockBehaviorVine(), BlockTraits.VINE_DIRECTION_BITS); //106
        this.registerVanilla(FENCE_GATE, new BlockBehaviorFenceGate(), BlockTraits.IS_OPEN, BlockTraits.DIRECTION, BlockTraits.IS_IN_WALL); //107
        this.registerVanilla(BRICK_STAIRS, new BlockBehaviorStairsBrick(), BlockTraits.DIRECTION, BlockTraits.IS_UPSIDE_DOWN); //108
        this.registerVanilla(STONE_BRICK_STAIRS, new BlockBehaviorStairsStoneBrick(), BlockTraits.DIRECTION, BlockTraits.IS_UPSIDE_DOWN); //109
        this.registerVanilla(MYCELIUM, new BlockBehaviorMycelium()); //110
        this.registerVanilla(WATERLILY, new BlockBehaviorWaterLily()); //111
        this.registerVanilla(NETHER_BRICK, new BlockBehaviorBricksNether()); //112
        this.registerVanilla(NETHER_BRICK_FENCE, new BlockBehaviorFenceNetherBrick()); //113
        this.registerVanilla(NETHER_BRICK_STAIRS, new BlockBehaviorStairsNetherBrick(), BlockTraits.DIRECTION, BlockTraits.IS_UPSIDE_DOWN); //114
        this.registerVanilla(NETHER_WART, new BlockBehaviorNetherWart(), BlockTraits.WART_GROWTH); //115
        this.registerVanilla(ENCHANTING_TABLE, new BlockBehaviorEnchantingTable()); //116
        this.registerVanilla(BREWING_STAND, new BlockBehaviorBrewingStand(), BlockTraits.IS_BREWING_A, BlockTraits.IS_BREWING_B, BlockTraits.IS_BREWING_C); //117
        this.registerVanilla(CAULDRON, new BlockBehaviorCauldron(), BlockTraits.FLUID_TYPE, BlockTraits.FILL_LEVEL); //118
        this.registerVanilla(END_PORTAL, new BlockBehaviorEndPortal()); //119
        this.registerVanilla(END_PORTAL_FRAME, new BlockBehaviorEndPortalFrame(), BlockTraits.DIRECTION, BlockTraits.HAS_END_PORTAL_EYE); //120
        this.registerVanilla(END_STONE, new BlockBehaviorEndStone()); //121
        this.registerVanilla(DRAGON_EGG, new BlockBehaviorDragonEgg()); //122
        this.registerVanilla(REDSTONE_LAMP, new BlockBehaviorRedstoneLamp()); //123
        this.registerVanilla(LIT_REDSTONE_LAMP, new BlockBehaviorRedstoneLamp()); //124
        this.registerVanilla(DROPPER, NoopBlockBehavior.INSTANCE, BlockTraits.FACING_DIRECTION, BlockTraits.IS_TRIGGERED);
        this.registerVanilla(ACTIVATOR_RAIL, new BlockBehaviorRailActivator(), BlockTraits.IS_POWERED, BlockTraits.SIMPLE_RAIL_DIRECTION); //126
        this.registerVanilla(COCOA, new BlockBehaviorCocoa(), BlockTraits.COCOA_AGE, BlockTraits.DIRECTION); //127
        this.registerVanilla(SANDSTONE_STAIRS, new BlockBehaviorStairsSandstone()); //128
        this.registerVanilla(EMERALD_ORE, new BlockBehaviorOreEmerald()); //129
        this.registerVanilla(ENDER_CHEST, new BlockBehaviorEnderChest(), BlockTraits.FACING_DIRECTION); //130
        this.registerVanilla(TRIPWIRE_HOOK, new BlockBehaviorTripWireHook(), BlockTraits.DIRECTION, BlockTraits.IS_ATTACHED, BlockTraits.IS_POWERED); //131
        this.registerVanilla(TRIPWIRE, new BlockBehaviorTripWire(), BlockTraits.IS_POWERED, BlockTraits.IS_ATTACHED, BlockTraits.IS_DISARMED); //132
        this.registerVanilla(EMERALD_BLOCK, new BlockBehaviorEmerald()); //133
//        this.registerVanilla(SPRUCE_STAIRS, new BlockBehaviorStairsWood()); //134
//        this.registerVanilla(BIRCH_STAIRS, new BlockBehaviorStairsWood()); //135
//        this.registerVanilla(JUNGLE_STAIRS, new BlockBehaviorStairsWood()); //136
        this.registerVanilla(COMMAND_BLOCK, NoopBlockBehavior.INSTANCE, BlockTraits.FACING_DIRECTION, BlockTraits.IS_CONDITIONAL);
        this.registerVanilla(BEACON, new BlockBehaviorBeacon()); //138
        this.registerVanilla(COBBLESTONE_WALL, new BlockBehaviorWall(), BlockTraits.WALL_BLOCK_TYPE, BlockTraits.HAS_POST, BlockTraits.WALL_CONNECTION_NORTH, BlockTraits.WALL_CONNECTION_EAST, BlockTraits.WALL_CONNECTION_SOUTH, BlockTraits.WALL_CONNECTION_WEST); //139
        this.registerVanilla(FLOWER_POT, new BlockBehaviorFlowerPot(), BlockTraits.HAS_UPDATE); //140
        this.registerVanilla(CARROTS, new BlockBehaviorCarrot(), BlockTraits.GROWTH); //141
        this.registerVanilla(POTATOES, new BlockBehaviorPotato(), BlockTraits.GROWTH); //142
        this.registerVanilla(WOODEN_BUTTON, new BlockBehaviorButtonWooden(), BlockTraits.FACING_DIRECTION, BlockTraits.IS_BUTTON_PRESSED); //143
        this.registerVanilla(SKULL, new BlockBehaviorSkull(), BlockTraits.FACING_DIRECTION, BlockTraits.HAS_NO_DROP); //144
        this.registerVanilla(ANVIL, new BlockBehaviorAnvil(), BlockTraits.DIRECTION, BlockTraits.DAMAGE); //145
        this.registerVanilla(TRAPPED_CHEST, new BlockBehaviorTrappedChest(), BlockTraits.FACING_DIRECTION); //146
        this.registerVanilla(LIGHT_WEIGHTED_PRESSURE_PLATE, new BlockBehaviorWeightedPressurePlateLight(), BlockTraits.REDSTONE_SIGNAL); //147
        this.registerVanilla(HEAVY_WEIGHTED_PRESSURE_PLATE, new BlockBehaviorWeightedPressurePlateHeavy(), BlockTraits.REDSTONE_SIGNAL); //148
        this.registerVanilla(UNPOWERED_COMPARATOR, new BlockBehaviorRedstoneComparator(UNPOWERED_COMPARATOR), BlockTraits.DIRECTION, BlockTraits.IS_OUTPUT_LIT, BlockTraits.IS_OUTPUT_SUBTRACT); //149
        this.registerVanilla(POWERED_COMPARATOR, new BlockBehaviorRedstoneComparator(POWERED_COMPARATOR), BlockTraits.DIRECTION, BlockTraits.IS_OUTPUT_LIT, BlockTraits.IS_OUTPUT_SUBTRACT); //150
        this.registerVanilla(DAYLIGHT_DETECTOR, new BlockBehaviorDaylightDetector(), BlockTraits.REDSTONE_SIGNAL); //151
        this.registerVanilla(REDSTONE_BLOCK, new BlockBehaviorRedstone()); //152
        this.registerVanilla(QUARTZ_ORE, new BlockBehaviorOreQuartz()); //153
        this.registerVanilla(HOPPER, new BlockBehaviorHopper(), BlockTraits.FACING_DIRECTION, BlockTraits.IS_TOGGLED); //154
        this.registerVanilla(QUARTZ_BLOCK, new BlockBehaviorQuartz()); //155
        this.registerVanilla(QUARTZ_STAIRS, new BlockBehaviorStairsQuartz()); //156
        this.registerVanilla(DOUBLE_WOODEN_SLAB, new BlockBehaviorDoubleSlabWood(), BlockTraits.TREE_SPECIES, BlockTraits.IS_TOP_SLOT); //157
        this.registerVanilla(WOODEN_SLAB, new BlockBehaviorSlabWood(), BlockTraits.TREE_SPECIES, BlockTraits.IS_TOP_SLOT); //158
        this.registerVanilla(STAINED_HARDENED_CLAY, new BlockBehaviorTerracottaStained(), BlockTraits.COLOR); //159
        this.registerVanilla(STAINED_GLASS_PANE, new BlockBehaviorGlassPaneStained(), BlockTraits.COLOR); //160
//        this.registerVanilla(LEAVES2, new BlockBehaviorLeaves2()); //161
//        this.registerVanilla(LOG2, new BlockBehaviorLog2()); //162
//        this.registerVanilla(ACACIA_STAIRS, new BlockBehaviorStairsWood()); //163
//        this.registerVanilla(DARK_OAK_STAIRS, new BlockBehaviorStairsWood()); //164
        this.registerVanilla(SLIME, new BlockBehaviorSlime()); //165
        //166: glow_stick
        this.registerVanilla(IRON_TRAPDOOR, new BlockBehaviorTrapdoorIron(), BlockTraits.DIRECTION, BlockTraits.IS_UPSIDE_DOWN, BlockTraits.IS_OPEN); //167
        this.registerVanilla(PRISMARINE, new BlockBehaviorPrismarine(), BlockTraits.PRISMARINE_BLOCK_TYPE); //168
        this.registerVanilla(SEA_LANTERN, new BlockBehaviorSeaLantern()); //169
        this.registerVanilla(HAY_BLOCK, new BlockBehaviorHayBale(), BlockTraits.AXIS, BlockTraits.DEPRECATED); //170
        this.registerVanilla(CARPET, new BlockBehaviorCarpet(), BlockTraits.COLOR); //171
        this.registerVanilla(HARDENED_CLAY, new BlockBehaviorTerracotta()); //172
        this.registerVanilla(COAL_BLOCK, new BlockBehaviorCoal()); //173
        this.registerVanilla(PACKED_ICE, new BlockBehaviorIcePacked()); //174
        this.registerVanilla(DOUBLE_PLANT, new BlockBehaviorDoublePlant(), BlockTraits.DOUBLE_PLANT_TYPE, BlockTraits.IS_UPPER_BLOCK); //175
        this.registerVanilla(STANDING_BANNER, new BlockBehaviorBanner(), BlockTraits.CARDINAL_DIRECTION); //176
        this.registerVanilla(WALL_BANNER, new BlockBehaviorWallBanner(), BlockTraits.FACING_DIRECTION); //177
        this.registerVanilla(DAYLIGHT_DETECTOR_INVERTED, new BlockBehaviorDaylightDetectorInverted(), BlockTraits.REDSTONE_SIGNAL); //178
        this.registerVanilla(RED_SANDSTONE, new BlockBehaviorRedSandstone()); //179
        this.registerVanilla(RED_SANDSTONE_STAIRS, new BlockBehaviorStairsRedSandstone()); //180
//        this.registerVanilla(DOUBLE_STONE_SLAB2, BlockBehaviorDoubleSlab.factory(STONE_SLAB2, BlockBehaviorSlab.COLORS_2)); //181
//        this.registerVanilla(STONE_SLAB2, BlockBehaviorSlab.factory(DOUBLE_STONE_SLAB2, BlockBehaviorSlab.COLORS_2)); //182
        this.registerVanilla(SPRUCE_FENCE_GATE, new BlockBehaviorFenceGate(), BlockTraits.IS_OPEN, BlockTraits.DIRECTION, BlockTraits.IS_IN_WALL); //183
        this.registerVanilla(BIRCH_FENCE_GATE, new BlockBehaviorFenceGate(), BlockTraits.IS_OPEN, BlockTraits.DIRECTION, BlockTraits.IS_IN_WALL); //184
        this.registerVanilla(JUNGLE_FENCE_GATE, new BlockBehaviorFenceGate(), BlockTraits.IS_OPEN, BlockTraits.DIRECTION, BlockTraits.IS_IN_WALL); //185
        this.registerVanilla(DARK_OAK_FENCE_GATE, new BlockBehaviorFenceGate(), BlockTraits.IS_OPEN, BlockTraits.DIRECTION, BlockTraits.IS_IN_WALL); //186
        this.registerVanilla(ACACIA_FENCE_GATE, new BlockBehaviorFenceGate(), BlockTraits.IS_OPEN, BlockTraits.DIRECTION, BlockTraits.IS_IN_WALL); //187
        this.registerVanilla(REPEATING_COMMAND_BLOCK, NoopBlockBehavior.INSTANCE, BlockTraits.FACING_DIRECTION, BlockTraits.IS_CONDITIONAL); //188
        this.registerVanilla(CHAIN_COMMAND_BLOCK, NoopBlockBehavior.INSTANCE, BlockTraits.FACING_DIRECTION, BlockTraits.IS_CONDITIONAL); //189
        this.registerVanilla(HARD_GLASS_PANE, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(HARD_STAINED_GLASS_PANE, NoopBlockBehavior.INSTANCE, BlockTraits.COLOR);
        this.registerVanilla(CHEMICAL_HEAT, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(SPRUCE_DOOR, new BlockBehaviorDoorWood(), BlockTraits.DIRECTION, BlockTraits.IS_OPEN, BlockTraits.IS_DOOR_HINGE, BlockTraits.IS_UPPER_BLOCK); //193
        this.registerVanilla(BIRCH_DOOR, new BlockBehaviorDoorWood(), BlockTraits.DIRECTION, BlockTraits.IS_OPEN, BlockTraits.IS_DOOR_HINGE, BlockTraits.IS_UPPER_BLOCK); //194
        this.registerVanilla(JUNGLE_DOOR, new BlockBehaviorDoorWood(), BlockTraits.DIRECTION, BlockTraits.IS_OPEN, BlockTraits.IS_DOOR_HINGE, BlockTraits.IS_UPPER_BLOCK); //195
        this.registerVanilla(ACACIA_DOOR, new BlockBehaviorDoorWood(), BlockTraits.DIRECTION, BlockTraits.IS_OPEN, BlockTraits.IS_DOOR_HINGE, BlockTraits.IS_UPPER_BLOCK); //196
        this.registerVanilla(DARK_OAK_DOOR, new BlockBehaviorDoorWood(), BlockTraits.DIRECTION, BlockTraits.IS_OPEN, BlockTraits.IS_DOOR_HINGE, BlockTraits.IS_UPPER_BLOCK); //197
        this.registerVanilla(GRASS_PATH, new BlockBehaviorGrassPath()); //198
        this.registerVanilla(FRAME, new BlockBehaviorItemFrame(), BlockTraits.FACING_DIRECTION, BlockTraits.HAS_MAP); //199
        this.registerVanilla(CHORUS_FLOWER, new BlockBehaviorChorusFlower(), BlockTraits.CHORUS_AGE); //200
        this.registerVanilla(PURPUR_BLOCK, new BlockBehaviorPurpur(), BlockTraits.CHISEL_TYPE, BlockTraits.DIRECTION); //201
        this.registerVanilla(COLORED_TORCH_RG, new BlockBehaviorTorch(), BlockTraits.HAS_COLOR, BlockTraits.TORCH_DIRECTION);
        this.registerVanilla(PURPUR_STAIRS, new BlockBehaviorStairsPurpur(), BlockTraits.DIRECTION, BlockTraits.IS_UPSIDE_DOWN); //203
        this.registerVanilla(COLORED_TORCH_BP, new BlockBehaviorTorch(), BlockTraits.HAS_COLOR, BlockTraits.TORCH_DIRECTION);
        this.registerVanilla(UNDYED_SHULKER_BOX, new BlockBehaviorUndyedShulkerBox()); //205
        this.registerVanilla(END_BRICKS, new BlockBehaviorBricksEndStone()); //206
        this.registerVanilla(FROSTED_ICE, NoopBlockBehavior.INSTANCE, BlockTraits.ICE_AGE); //206
        this.registerVanilla(END_ROD, new BlockBehaviorEndRod(), BlockTraits.FACING_DIRECTION); //208
        this.registerVanilla(END_GATEWAY, new BlockBehaviorEndGateway()); //209
        this.registerVanilla(ALLOW, NoopBlockBehavior.INSTANCE); //210
        this.registerVanilla(DENY, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(BORDER_BLOCK, NoopBlockBehavior.INSTANCE, BlockTraits.WALL_BLOCK_TYPE, BlockTraits.HAS_POST, BlockTraits.WALL_CONNECTION_NORTH, BlockTraits.WALL_CONNECTION_EAST, BlockTraits.WALL_CONNECTION_SOUTH, BlockTraits.WALL_CONNECTION_WEST);
        this.registerVanilla(MAGMA, new BlockBehaviorMagma()); //213
        this.registerVanilla(NETHER_WART_BLOCK, new BlockNetherWartBlockBehavior()); //214
        this.registerVanilla(RED_NETHER_BRICK, new BlockBehaviorBricksRedNether()); //215
        this.registerVanilla(BONE_BLOCK, new BlockBehaviorBone(), BlockTraits.DIRECTION, BlockTraits.DEPRECATED); //216
        this.registerVanilla(STRUCTURE_VOID, NoopBlockBehavior.INSTANCE, BlockTraits.STRUCTURE_VOID_TYPE);
        this.registerVanilla(SHULKER_BOX, new BlockBehaviorShulkerBox(), BlockTraits.COLOR); //218
        this.registerVanilla(PURPLE_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed(), BlockTraits.FACING_DIRECTION); //219
        this.registerVanilla(WHITE_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed(), BlockTraits.FACING_DIRECTION); //220
        this.registerVanilla(ORANGE_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed(), BlockTraits.FACING_DIRECTION); //221
        this.registerVanilla(MAGENTA_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed(), BlockTraits.FACING_DIRECTION); //222
        this.registerVanilla(LIGHT_BLUE_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed(), BlockTraits.FACING_DIRECTION); //223
        this.registerVanilla(YELLOW_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed(), BlockTraits.FACING_DIRECTION); //224
        this.registerVanilla(LIME_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed(), BlockTraits.FACING_DIRECTION); //225
        this.registerVanilla(PINK_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed(), BlockTraits.FACING_DIRECTION); //226
        this.registerVanilla(GRAY_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed(), BlockTraits.FACING_DIRECTION); //227
        this.registerVanilla(SILVER_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed(), BlockTraits.FACING_DIRECTION); //228
        this.registerVanilla(CYAN_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed(), BlockTraits.FACING_DIRECTION); //229
        //230: chalkboard
        this.registerVanilla(BLUE_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed(), BlockTraits.FACING_DIRECTION); //231
        this.registerVanilla(BROWN_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed(), BlockTraits.FACING_DIRECTION); //232
        this.registerVanilla(GREEN_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed(), BlockTraits.FACING_DIRECTION); //233
        this.registerVanilla(RED_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed(), BlockTraits.FACING_DIRECTION); //234
        this.registerVanilla(BLACK_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed(), BlockTraits.FACING_DIRECTION); //235
        this.registerVanilla(CONCRETE, new BlockBehaviorConcrete(), BlockTraits.COLOR); //236
        this.registerVanilla(CONCRETE_POWDER, new BlockBehaviorConcretePowder(), BlockTraits.COLOR); //237
        this.registerVanilla(CHEMISTRY_TABLE, NoopBlockBehavior.INSTANCE, BlockTraits.CHEMISTRY_TABLE_TYPE, BlockTraits.DIRECTION);
        this.registerVanilla(UNDERWATER_TORCH, new BlockBehaviorTorch(), BlockTraits.TORCH_DIRECTION);
        this.registerVanilla(CHORUS_PLANT, new BlockBehaviorChorusPlant()); //240
        this.registerVanilla(STAINED_GLASS, new BlockBehaviorGlassStained(), BlockTraits.COLOR); //241
        this.registerVanilla(CAMERA, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(PODZOL, new BlockBehaviorPodzol()); //243
        this.registerVanilla(BEETROOT, new BlockBehaviorBeetroot(), BlockTraits.GROWTH); //244
        this.registerVanilla(STONECUTTER, new BlockBehaviorStonecutter()); //245
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
        this.registerVanilla(PRISMARINE_STAIRS, new BlockBehaviorStairsPrismarine(), BlockTraits.DIRECTION, BlockTraits.IS_UPSIDE_DOWN); //257
        this.registerVanilla(DARK_PRISMARINE_STAIRS, new BlockBehaviorStairsDarkPrismarine(), BlockTraits.DIRECTION, BlockTraits.IS_UPSIDE_DOWN); //258
        this.registerVanilla(PRISMARINE_BRICKS_STAIRS, new BlockBehaviorStairsPrismarineBricks(), BlockTraits.DIRECTION, BlockTraits.IS_UPSIDE_DOWN); //259
        this.registerVanilla(STRIPPED_SPRUCE_LOG, new BlockBehaviorStrippedLog(), BlockTraits.AXIS); //260
        this.registerVanilla(STRIPPED_BIRCH_LOG, new BlockBehaviorStrippedLog(), BlockTraits.AXIS); //261
        this.registerVanilla(STRIPPED_JUNGLE_LOG, new BlockBehaviorStrippedLog(), BlockTraits.AXIS); //262
        this.registerVanilla(STRIPPED_ACACIA_LOG, new BlockBehaviorStrippedLog(), BlockTraits.AXIS); //263
        this.registerVanilla(STRIPPED_DARK_OAK_LOG, new BlockBehaviorStrippedLog(), BlockTraits.AXIS); //264
        this.registerVanilla(STRIPPED_OAK_LOG, new BlockBehaviorStrippedLog(), BlockTraits.AXIS); //265
        this.registerVanilla(BLUE_ICE, new BlockBehaviorBlueIce()); //266
        for (int i = 1; i <= 118; i++) {
            Identifier element = Identifier.from("minecraft", "element_" + i);
            this.registerVanilla(element, NoopBlockBehavior.INSTANCE);
        }
        this.registerVanilla(SEAGRASS, NoopBlockBehavior.INSTANCE, BlockTraits.SEA_GRASS_TYPE);
        this.registerVanilla(CORAL, NoopBlockBehavior.INSTANCE, BlockTraits.CORAL_COLOR, BlockTraits.IS_DEAD);
        this.registerVanilla(CORAL_BLOCK, NoopBlockBehavior.INSTANCE, BlockTraits.CORAL_COLOR, BlockTraits.IS_DEAD);
        this.registerVanilla(CORAL_FAN, NoopBlockBehavior.INSTANCE, BlockTraits.CORAL_COLOR, BlockTraits.CORAL_FAN_DIRECTION);
        this.registerVanilla(CORAL_FAN_DEAD, NoopBlockBehavior.INSTANCE, BlockTraits.CORAL_COLOR, BlockTraits.CORAL_FAN_DIRECTION);
        this.registerVanilla(CORAL_FAN_HANG, NoopBlockBehavior.INSTANCE, BlockTraits.CORAL_HANG_COLOR, BlockTraits.DIRECTION, BlockTraits.IS_DEAD);
        this.registerVanilla(KELP, new BlockBehaviorKelp(), BlockTraits.KELP_AGE);//393
        this.registerVanilla(DRIED_KELP_BLOCK, new BlockBehaviorDriedKelp()); //394
        this.registerVanilla(ACACIA_BUTTON, new BlockBehaviorButtonWooden(), BlockTraits.FACING_DIRECTION, BlockTraits.IS_BUTTON_PRESSED);//395
        this.registerVanilla(BIRCH_BUTTON, new BlockBehaviorButtonWooden(), BlockTraits.FACING_DIRECTION, BlockTraits.IS_BUTTON_PRESSED);//396
        this.registerVanilla(DARK_OAK_BUTTON, new BlockBehaviorButtonWooden(), BlockTraits.FACING_DIRECTION, BlockTraits.IS_BUTTON_PRESSED);//397
        this.registerVanilla(JUNGLE_BUTTON, new BlockBehaviorButtonWooden(), BlockTraits.FACING_DIRECTION, BlockTraits.IS_BUTTON_PRESSED);//398
        this.registerVanilla(SPRUCE_BUTTON, new BlockBehaviorButtonWooden(), BlockTraits.FACING_DIRECTION, BlockTraits.IS_BUTTON_PRESSED);//399
        this.registerVanilla(ACACIA_TRAPDOOR, new BlockBehaviorTrapdoor(), BlockTraits.DIRECTION, BlockTraits.IS_UPSIDE_DOWN, BlockTraits.IS_OPEN); //400
        this.registerVanilla(BIRCH_TRAPDOOR, new BlockBehaviorTrapdoor(), BlockTraits.DIRECTION, BlockTraits.IS_UPSIDE_DOWN, BlockTraits.IS_OPEN); //401
        this.registerVanilla(DARK_OAK_TRAPDOOR, new BlockBehaviorTrapdoor(), BlockTraits.DIRECTION, BlockTraits.IS_UPSIDE_DOWN, BlockTraits.IS_OPEN); //402
        this.registerVanilla(JUNGLE_TRAPDOOR, new BlockBehaviorTrapdoor(), BlockTraits.DIRECTION, BlockTraits.IS_UPSIDE_DOWN, BlockTraits.IS_OPEN); //403
        this.registerVanilla(SPRUCE_TRAPDOOR, new BlockBehaviorTrapdoor(), BlockTraits.DIRECTION, BlockTraits.IS_UPSIDE_DOWN, BlockTraits.IS_OPEN); //404
        this.registerVanilla(ACACIA_PRESSURE_PLATE, new BlockBehaviorPressurePlateWood(), BlockTraits.REDSTONE_SIGNAL);//405
        this.registerVanilla(BIRCH_PRESSURE_PLATE, new BlockBehaviorPressurePlateWood(), BlockTraits.REDSTONE_SIGNAL);//406
        this.registerVanilla(DARK_OAK_PRESSURE_PLATE, new BlockBehaviorPressurePlateWood(), BlockTraits.REDSTONE_SIGNAL);//407
        this.registerVanilla(JUNGLE_PRESSURE_PLATE, new BlockBehaviorPressurePlateWood(), BlockTraits.REDSTONE_SIGNAL);//408
        this.registerVanilla(SPRUCE_PRESSURE_PLATE, new BlockBehaviorPressurePlateWood(), BlockTraits.REDSTONE_SIGNAL);//409
        this.registerVanilla(CARVED_PUMPKIN, NoopBlockBehavior.INSTANCE, BlockTraits.DIRECTION);
        this.registerVanilla(SEA_PICKLE, NoopBlockBehavior.INSTANCE, BlockTraits.CLUSTER_COUNT, BlockTraits.IS_DEAD);
        this.registerVanilla(CONDUIT, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(TURTLE_EGG, NoopBlockBehavior.INSTANCE, BlockTraits.CRACKED_STATE, BlockTraits.TURTLE_EGG_COUNT);
        this.registerVanilla(BUBBLE_COLUMN, NoopBlockBehavior.INSTANCE, BlockTraits.HAS_DRAG_DOWN);
        this.registerVanilla(BARRIER, new BlockBehaviorBarrier()); //415
//        this.registerVanilla(STONE_SLAB3, BlockBehaviorSlab.factory(DOUBLE_STONE_SLAB3, BlockBehaviorSlab.COLORS_3)); //416
        this.registerVanilla(BAMBOO, NoopBlockBehavior.INSTANCE, BlockTraits.BAMBOO_LEAF_SIZE, BlockTraits.BAMBOO_STALK_THICKNESS);
        this.registerVanilla(BAMBOO_SAPLING, NoopBlockBehavior.INSTANCE, BlockTraits.TREE_SPECIES, BlockTraits.HAS_AGE);
        this.registerVanilla(SCAFFOLDING, NoopBlockBehavior.INSTANCE, BlockTraits.STABILITY, BlockTraits.HAS_STABILITY_CHECK);
//        this.registerVanilla(STONE_SLAB4, BlockBehaviorSlab.factory(DOUBLE_STONE_SLAB4, BlockBehaviorSlab.COLORS_4)); //420
//        this.registerVanilla(DOUBLE_STONE_SLAB3, BlockBehaviorDoubleSlab.factory(STONE_SLAB3, BlockBehaviorSlab.COLORS_3)); //421
//        this.registerVanilla(DOUBLE_STONE_SLAB4, BlockBehaviorDoubleSlab.factory(DOUBLE_STONE_SLAB4, BlockBehaviorSlab.COLORS_4)); //422
        this.registerVanilla(GRANITE_STAIRS, new BlockBehaviorStairsGranite(), BlockTraits.DIRECTION, BlockTraits.IS_UPSIDE_DOWN); //423
        this.registerVanilla(DIORITE_STAIRS, new BlockBehaviorStairsDiorite(), BlockTraits.DIRECTION, BlockTraits.IS_UPSIDE_DOWN); //424
        this.registerVanilla(ANDESITE_STAIRS, new BlockBehaviorStairsAndesite(), BlockTraits.DIRECTION, BlockTraits.IS_UPSIDE_DOWN); //425
        this.registerVanilla(POLISHED_GRANITE_STAIRS, new BlockBehaviorStairsGranite(), BlockTraits.DIRECTION, BlockTraits.IS_UPSIDE_DOWN); //426
        this.registerVanilla(POLISHED_DIORITE_STAIRS, new BlockBehaviorStairsDiorite(), BlockTraits.DIRECTION, BlockTraits.IS_UPSIDE_DOWN); //427
        this.registerVanilla(POLISHED_ANDESITE_STAIRS, new BlockBehaviorStairsAndesite(), BlockTraits.DIRECTION, BlockTraits.IS_UPSIDE_DOWN); //428
        this.registerVanilla(MOSSY_STONE_BRICK_STAIRS, new BlockBehaviorStairsStoneBrick(), BlockTraits.DIRECTION, BlockTraits.IS_UPSIDE_DOWN); //429
        this.registerVanilla(SMOOTH_RED_SANDSTONE_STAIRS, new BlockBehaviorStairsSmoothRedSandstone(), BlockTraits.DIRECTION, BlockTraits.IS_UPSIDE_DOWN); //430
        this.registerVanilla(SMOOTH_SANDSTONE_STAIRS, new BlockBehaviorStairsSmoothSandstone(), BlockTraits.DIRECTION, BlockTraits.IS_UPSIDE_DOWN); //431
        this.registerVanilla(END_BRICK_STAIRS, new BlockBehaviorStairsEndStoneBrick(), BlockTraits.DIRECTION, BlockTraits.IS_UPSIDE_DOWN); //432
        this.registerVanilla(MOSSY_COBBLESTONE_STAIRS, new BlockBehaviorStairsCobblestone(), BlockTraits.DIRECTION, BlockTraits.IS_UPSIDE_DOWN); //433
        this.registerVanilla(NORMAL_STONE_STAIRS, new BlockBehaviorStairsStone(), BlockTraits.DIRECTION, BlockTraits.IS_UPSIDE_DOWN); //434
        this.registerVanilla(SPRUCE_STANDING_SIGN, new BlockBehaviorSignPost(), BlockTraits.CARDINAL_DIRECTION); //435
        this.registerVanilla(SPRUCE_WALL_SIGN, new BlockBehaviorWallSign(), BlockTraits.FACING_DIRECTION); // 436
        this.registerVanilla(SMOOTH_STONE, new BlockBehaviorSmoothStone()); // 437
        this.registerVanilla(RED_NETHER_BRICK_STAIRS, new BlockBehaviorStairsNetherBrick(), BlockTraits.DIRECTION, BlockTraits.IS_UPSIDE_DOWN); //438
        this.registerVanilla(SMOOTH_QUARTZ_STAIRS, new BlockBehaviorStairsQuartz(), BlockTraits.DIRECTION, BlockTraits.IS_UPSIDE_DOWN); //439
        this.registerVanilla(BIRCH_STANDING_SIGN, new BlockBehaviorSignPost(), BlockTraits.CARDINAL_DIRECTION); //440
        this.registerVanilla(BIRCH_WALL_SIGN, new BlockBehaviorWallSign(), BlockTraits.FACING_DIRECTION); //441
        this.registerVanilla(JUNGLE_STANDING_SIGN, new BlockBehaviorSignPost(), BlockTraits.CARDINAL_DIRECTION); //442
        this.registerVanilla(JUNGLE_WALL_SIGN, new BlockBehaviorWallSign(), BlockTraits.FACING_DIRECTION); //443
        this.registerVanilla(ACACIA_STANDING_SIGN, new BlockBehaviorSignPost(), BlockTraits.CARDINAL_DIRECTION); //444
        this.registerVanilla(ACACIA_WALL_SIGN, new BlockBehaviorWallSign(), BlockTraits.FACING_DIRECTION); //445
        this.registerVanilla(DARK_OAK_STANDING_SIGN, new BlockBehaviorSignPost(), BlockTraits.CARDINAL_DIRECTION); //446
        this.registerVanilla(DARK_OAK_WALL_SIGN, new BlockBehaviorWallSign(), BlockTraits.FACING_DIRECTION); //447
        this.registerVanilla(LECTERN, new BlockBehaviorLectern(), BlockTraits.DIRECTION, BlockTraits.IS_POWERED); //448
        this.registerVanilla(GRINDSTONE, NoopBlockBehavior.INSTANCE, BlockTraits.DIRECTION, BlockTraits.ATTACHMENT);
        this.registerVanilla(BLAST_FURNACE, new BlockBehaviorFurnace(BlockEntityTypes.BLAST_FURNACE), BlockTraits.FACING_DIRECTION); // 450
        this.registerVanilla(STONECUTTER_BLOCK, NoopBlockBehavior.INSTANCE, BlockTraits.FACING_DIRECTION); // 451
        this.registerVanilla(SMOKER, new BlockBehaviorFurnace(BlockEntityTypes.FURNACE), BlockTraits.FACING_DIRECTION); //452
        this.registerVanilla(LIT_SMOKER, new BlockBehaviorFurnaceBurning(BlockEntityTypes.FURNACE), BlockTraits.FACING_DIRECTION); //453
        this.registerVanilla(CARTOGRAPHY_TABLE, NoopBlockBehavior.INSTANCE); //454
        this.registerVanilla(FLETCHING_TABLE, NoopBlockBehavior.INSTANCE); //455
        this.registerVanilla(SMITHING_TABLE, NoopBlockBehavior.INSTANCE); //456
        this.registerVanilla(BARREL, new BlockBehaviorBarrel(), BlockTraits.FACING_DIRECTION, BlockTraits.IS_OPEN); // 457
        this.registerVanilla(LOOM, NoopBlockBehavior.INSTANCE, BlockTraits.DIRECTION);
        this.registerVanilla(BELL, NoopBlockBehavior.INSTANCE, BlockTraits.DIRECTION, BlockTraits.ATTACHMENT, BlockTraits.IS_TOGGLED);
        this.registerVanilla(SWEET_BERRY_BUSH, NoopBlockBehavior.INSTANCE, BlockTraits.GROWTH);
        this.registerVanilla(LANTERN, NoopBlockBehavior.INSTANCE, BlockTraits.IS_HANGING);
        this.registerVanilla(CAMPFIRE, new BlockBehaviorCampfire(), BlockTraits.DIRECTION, BlockTraits.IS_EXTINGUISHED); //462
        this.registerVanilla(LAVA_CAULDRON, NoopBlockBehavior.INSTANCE, BlockTraits.FILL_LEVEL, BlockTraits.FLUID_TYPE);
        this.registerVanilla(JIGSAW, NoopBlockBehavior.INSTANCE, BlockTraits.FACING_DIRECTION, BlockTraits.DIRECTION);
        this.registerVanilla(WOOD, new BlockBehaviorWood(), BlockTraits.TREE_SPECIES, BlockTraits.IS_STRIPPED); //465
        this.registerVanilla(COMPOSTER, NoopBlockBehavior.INSTANCE, BlockTraits.COMPOSTER_FILL_LEVEL);
        this.registerVanilla(LIT_BLAST_FURNACE, new BlockBehaviorFurnaceBurning(BlockEntityTypes.BLAST_FURNACE), BlockTraits.FACING_DIRECTION); //467
        this.registerVanilla(LIGHT_BLOCK, new BlockBehaviorLight(), BlockTraits.LIGHT_LEVEL); //468
        this.registerVanilla(WITHER_ROSE, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(STICKY_PISTON_ARM_COLLISION, NoopBlockBehavior.INSTANCE, BlockTraits.FACING_DIRECTION);
        this.registerVanilla(BEE_NEST, NoopBlockBehavior.INSTANCE, BlockTraits.HONEY_LEVEL, BlockTraits.DIRECTION);
        this.registerVanilla(BEEHIVE, NoopBlockBehavior.INSTANCE, BlockTraits.HONEY_LEVEL, BlockTraits.DIRECTION);
        this.registerVanilla(HONEY_BLOCK, NoopBlockBehavior.INSTANCE);
        this.registerVanilla(HONEYCOMB_BLOCK, new BlockHoneycombBlockBehavior()); //474
    }
}
