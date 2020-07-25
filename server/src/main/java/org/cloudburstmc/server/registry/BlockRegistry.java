package org.cloudburstmc.server.registry;

import com.google.common.collect.HashBiMap;
import com.nukkitx.blockstateupdater.BlockStateUpdaters;
import com.nukkitx.nbt.NbtList;
import com.nukkitx.nbt.NbtMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.server.block.BlockPalette;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.behavior.*;
import org.cloudburstmc.server.block.serializer.BlockSerializer;
import org.cloudburstmc.server.block.serializer.NoopBlockSerializer;
import org.cloudburstmc.server.block.trait.BlockTrait;
import org.cloudburstmc.server.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.cloudburstmc.server.block.BlockTypes.*;

@Log4j2
public class BlockRegistry implements Registry {
    private static final BlockRegistry INSTANCE = new BlockRegistry();

    private final Reference2ReferenceMap<Identifier, BlockBehavior> behaviorMap = new Reference2ReferenceOpenHashMap<>();
    private final Reference2ReferenceMap<Identifier, BlockSerializer> serializerMap = new Reference2ReferenceOpenHashMap<>();
    private final HashBiMap<Identifier, Integer> idLegacyMap = HashBiMap.create();
    private final AtomicInteger customIdAllocator = new AtomicInteger(1000);
    private final BlockPalette palette = BlockPalette.INSTANCE;
    private NbtMap propertiesTag;
    private volatile boolean closed;

    private BlockRegistry() {
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
        this.registerVanilla(SAPLING, new BlockBehaviorSapling(), BlockTraits.TREE_SPECIES); //6
        this.registerVanilla(BEDROCK, new BlockBehaviorBedrock()); //7
        this.registerVanilla(FLOWING_WATER, new BlockBehaviorWater(), BlockTraits.IS_FLOWING, BlockTraits.FLUID_LEVEL); //8
        this.registerVanilla(WATER, new BlockBehaviorWaterStill(), BlockTraits.IS_FLOWING, BlockTraits.FLUID_LEVEL); //9
        this.registerVanilla(FLOWING_LAVA, new BlockBehaviorLava(), BlockTraits.IS_FLOWING, BlockTraits.FLUID_LEVEL); //10
        this.registerVanilla(LAVA, new BlockBehaviorLavaStill(), BlockTraits.IS_FLOWING, BlockTraits.FLUID_LEVEL); //11
        this.registerVanilla(SAND, new BlockBehaviorSand(), BlockTraits.SAND_TYPE); //12
        this.registerVanilla(GRAVEL, new BlockBehaviorGravel()); //13
        this.registerVanilla(GOLD_ORE, new BlockBehaviorOreGold()); //14
        this.registerVanilla(IRON_ORE, new BlockBehaviorOreIron()); //15
        this.registerVanilla(COAL_ORE, new BlockBehaviorOreCoal()); //16
        this.registerVanilla(LOG, new BlockBehaviorLog(), BlockTraits.TREE_SPECIES/*, BlockTraits.PILLAR_DIRECTION*/); //17
        this.registerVanilla(LEAVES, new BlockBehaviorLeaves(), BlockTraits.TREE_SPECIES); //18
        this.registerVanilla(SPONGE, new BlockBehaviorSponge(), BlockTraits.SPONGE_TYPE); //19
        this.registerVanilla(GLASS, new BlockBehaviorGlass()); //20
        this.registerVanilla(LAPIS_ORE, new BlockBehaviorOreLapis()); //21
        this.registerVanilla(LAPIS_BLOCK, new BlockBehaviorLapis()); //22
        this.registerVanilla(DISPENSER, new BlockBehaviorDispenser(), BlockTraits.IS_TRIGGERED, BlockTraits.DIRECTION); //23
        this.registerVanilla(SANDSTONE, new BlockBehaviorSandstone(), BlockTraits.SAND_STONE_TYPE); //24
        this.registerVanilla(NOTEBLOCK, new BlockBehaviorNoteblock()); //25
        this.registerVanilla(BED, new BlockBehaviorBed(), BlockTraits.IS_OCCUPIED, BlockTraits.IS_HEAD_PIECE, BlockTraits.DIRECTION); //26
        this.registerVanilla(GOLDEN_RAIL, new BlockBehaviorRailPowered()); //27
        this.registerVanilla(DETECTOR_RAIL, new BlockBehaviorRailDetector()); //28
        this.registerVanilla(STICKY_PISTON, new BlockBehaviorPistonSticky()); //29
        this.registerVanilla(WEB, new BlockBehaviorCobweb()); //30
        this.registerVanilla(TALL_GRASS, new BlockBehaviorTallGrass()); //31
        this.registerVanilla(DEADBUSH, new BlockBehaviorDeadBush()); //32
        this.registerVanilla(PISTON, new BlockBehaviorPiston()); //33
        this.registerVanilla(PISTON_ARM_COLLISION, new BlockBehaviorPistonHead()); //34
        this.registerVanilla(WOOL, new BlockBehaviorWool()); //35
        this.registerVanilla(YELLOW_FLOWER, new BlockBehaviorDandelion()); //37
        this.registerVanilla(RED_FLOWER, new BlockBehaviorFlower()); //38
        this.registerVanilla(BROWN_MUSHROOM, new BlockBehaviorMushroomBrown()); //39
        this.registerVanilla(RED_MUSHROOM, new BlockBehaviorMushroomRed()); //40
        this.registerVanilla(GOLD_BLOCK, new BlockBehaviorGold()); //41
        this.registerVanilla(IRON_BLOCK, new BlockBehaviorIron()); //42
        this.registerVanilla(DOUBLE_STONE_SLAB, new BlockBehaviorDoubleSlab(), BlockTraits.IS_TOP_SLOT, BlockTraits.STONE_SLAB_TYPE); //43
        this.registerVanilla(STONE_SLAB, new BlockBehaviorSlab(), BlockTraits.IS_TOP_SLOT, BlockTraits.STONE_SLAB_TYPE); //44
        this.registerVanilla(BRICK_BLOCK, new BlockBehaviorBricks()); //45
        this.registerVanilla(TNT, new BlockBehaviorTNT()); //46
        this.registerVanilla(BOOKSHELF, new BlockBehaviorBookshelf()); //47
        this.registerVanilla(MOSSY_COBBLESTONE, new BlockBehaviorMossStone()); //48
        this.registerVanilla(OBSIDIAN, new BlockBehaviorObsidian()); //49
        this.registerVanilla(TORCH, new BlockBehaviorTorch()); //50
        this.registerVanilla(FIRE, new BlockBehaviorFire(), BlockTraits.AGE); //51
        this.registerVanilla(MOB_SPAWNER, new BlockBehaviorMobSpawner()); //52
        this.registerVanilla(OAK_STAIRS, new BlockBehaviorStairsWood(), BlockTraits.IS_UPSIDE_DOWN); //53
        this.registerVanilla(CHEST, new BlockBehaviorChest()); //54
        this.registerVanilla(REDSTONE_WIRE, new BlockBehaviorRedstoneWire(), BlockTraits.REDSTONE_SIGNAL); //55
        this.registerVanilla(DIAMOND_ORE, new BlockBehaviorOreDiamond()); //56
        this.registerVanilla(DIAMOND_BLOCK, new BlockBehaviorDiamond()); //57
        this.registerVanilla(CRAFTING_TABLE, new BlockBehaviorCraftingTable()); //58
        this.registerVanilla(WHEAT, new BlockBehaviorWheat(), BlockTraits.GROWTH); //59
        this.registerVanilla(FARMLAND, new BlockBehaviorFarmland(), BlockTraits.MOISTURIZED_AMOUNT); //60
        this.registerVanilla(FURNACE, BlockBehaviorFurnace.factory(BlockEntityTypes.FURNACE)); //61
        this.registerVanilla(LIT_FURNACE, BlockBehaviorFurnaceBurning.factory(BlockEntityTypes.FURNACE)); //62
        this.registerVanilla(STANDING_SIGN, BlockBehaviorSignPost.factory(WALL_SIGN, ItemIds.SIGN)); //63
        this.registerVanilla(WOODEN_DOOR, new BlockBehaviorDoorWood()); //64
        this.registerVanilla(LADDER, new BlockBehaviorLadder()); //65
        this.registerVanilla(RAIL, new BlockBehaviorRail()); //66
        this.registerVanilla(STONE_STAIRS, new BlockBehaviorStairsCobblestone()); //67
        this.registerVanilla(WALL_SIGN, BlockBehaviorWallSign.factory(STANDING_SIGN, ItemIds.SIGN)); //68
        this.registerVanilla(LEVER, new BlockBehaviorLever()); //69
        this.registerVanilla(STONE_PRESSURE_PLATE, new BlockBehaviorPressurePlateStone()); //70
        this.registerVanilla(IRON_DOOR, new BlockBehaviorDoorIron()); //71
        this.registerVanilla(WOODEN_PRESSURE_PLATE, new BlockBehaviorPressurePlateWood()); //72
        this.registerVanilla(REDSTONE_ORE, new BlockBehaviorOreRedstone()); //73
        this.registerVanilla(LIT_REDSTONE_ORE, new BlockBehaviorOreRedstoneGlowing()); //74
        this.registerVanilla(UNLIT_REDSTONE_TORCH, new BlockBehaviorRedstoneTorchUnlit());
        this.registerVanilla(REDSTONE_TORCH, new BlockBehaviorRedstoneTorch()); //76
        this.registerVanilla(STONE_BUTTON, new BlockBehaviorButtonStone()); //77
        this.registerVanilla(SNOW_LAYER, new BlockBehaviorSnowLayer()); //78
        this.registerVanilla(ICE, new BlockBehaviorIce()); //79
        this.registerVanilla(SNOW, new BlockBehaviorSnow()); //80
        this.registerVanilla(CACTUS, new BlockBehaviorCactus()); //81
        this.registerVanilla(CLAY, new BlockBehaviorClay()); //82
        this.registerVanilla(REEDS, new ReedsBlockBehavior()); //83
        this.registerVanilla(JUKEBOX, new BlockBehaviorJukebox()); //84
        this.registerVanilla(FENCE, new BlockBehaviorFenceWooden()); //85
        this.registerVanilla(PUMPKIN, new BlockBehaviorPumpkin()); //86
        this.registerVanilla(NETHERRACK, new BlockBehaviorNetherrack()); //87
        this.registerVanilla(SOUL_SAND, new BlockBehaviorSoulSand()); //88
        this.registerVanilla(GLOWSTONE, new BlockBehaviorGlowstone()); //89
        this.registerVanilla(PORTAL, new BlockBehaviorNetherPortal()); //90
        this.registerVanilla(LIT_PUMPKIN, new BlockBehaviorPumpkinLit()); //91
        this.registerVanilla(CAKE, new BlockBehaviorCake()); //92
        this.registerVanilla(UNPOWERED_REPEATER, new BlockBehaviorRedstoneRepeaterUnpowered()); //93
        this.registerVanilla(POWERED_REPEATER, new BlockBehaviorRedstoneRepeaterPowered()); //94
        this.registerVanilla(INVISIBLE_BEDROCK, new BlockBehaviorBedrockInvisible()); //95
        this.registerVanilla(TRAPDOOR, new BlockBehaviorTrapdoor()); //96
        this.registerVanilla(MONSTER_EGG, new BlockBehaviorMonsterEgg()); //97
        this.registerVanilla(STONEBRICK, new BlockBehaviorBricksStone()); //98
        this.registerVanilla(BROWN_MUSHROOM_BLOCK, new BlockBehaviorHugeMushroomBrown()); //99
        this.registerVanilla(RED_MUSHROOM_BLOCK, new BlockBehaviorHugeMushroomRed()); //100
        this.registerVanilla(IRON_BARS, new BlockBehaviorIronBars()); //101
        this.registerVanilla(GLASS_PANE, new BlockBehaviorGlassPane()); //102
        this.registerVanilla(MELON_BLOCK, new BlockBehaviorMelon()); //103
        this.registerVanilla(PUMPKIN_STEM, new BlockBehaviorStemPumpkin()); //104
        this.registerVanilla(MELON_STEM, new BlockBehaviorStemMelon()); //105
        this.registerVanilla(VINE, new BlockBehaviorVine()); //106
        this.registerVanilla(FENCE_GATE, new BlockBehaviorFenceGate()); //107
        this.registerVanilla(BRICK_STAIRS, new BlockBehaviorStairsBrick()); //108
        this.registerVanilla(STONE_BRICK_STAIRS, new BlockBehaviorStairsStoneBrick()); //109
        this.registerVanilla(MYCELIUM, new BlockBehaviorMycelium()); //110
        this.registerVanilla(WATERLILY, new BlockBehaviorWaterLily()); //111
        this.registerVanilla(NETHER_BRICK, new BlockBehaviorBricksNether()); //112
        this.registerVanilla(NETHER_BRICK_FENCE, new BlockBehaviorFenceNetherBrick()); //113
        this.registerVanilla(NETHER_BRICK_STAIRS, new BlockBehaviorStairsNetherBrick()); //114
        this.registerVanilla(NETHER_WART, new BlockBehaviorNetherWart()); //115
        this.registerVanilla(ENCHANTING_TABLE, new BlockBehaviorEnchantingTable()); //116
        this.registerVanilla(BREWING_STAND, new BlockBehaviorBrewingStand()); //117
        this.registerVanilla(CAULDRON, new BlockBehaviorCauldron()); //118
        this.registerVanilla(END_PORTAL, new BlockBehaviorEndPortal()); //119
        this.registerVanilla(END_PORTAL_FRAME, new BlockBehaviorEndPortalFrame()); //120
        this.registerVanilla(END_STONE, new BlockBehaviorEndStone()); //121
        this.registerVanilla(DRAGON_EGG, new BlockBehaviorDragonEgg()); //122
        this.registerVanilla(REDSTONE_LAMP, new BlockBehaviorRedstoneLamp()); //123
        this.registerVanilla(LIT_REDSTONE_LAMP, new BlockBehaviorRedstoneLampLit()); //124
        //TODO: list.put(DROPPER, new BlockDropper()); //125
        this.registerVanilla(ACTIVATOR_RAIL, new BlockBehaviorRailActivator()); //126
        this.registerVanilla(COCOA, new BlockBehaviorCocoa()); //127
        this.registerVanilla(SANDSTONE_STAIRS, new BlockBehaviorStairsSandstone()); //128
        this.registerVanilla(EMERALD_ORE, new BlockBehaviorOreEmerald()); //129
        this.registerVanilla(ENDER_CHEST, new BlockBehaviorEnderChest()); //130
        this.registerVanilla(TRIPWIRE_HOOK, new BlockBehaviorTripWireHook()); //131
        this.registerVanilla(TRIPWIRE, new BlockBehaviorTripWire()); //132
        this.registerVanilla(EMERALD_BLOCK, new BlockBehaviorEmerald()); //133
        this.registerVanilla(SPRUCE_STAIRS, new BlockBehaviorStairsWood()); //134
        this.registerVanilla(BIRCH_STAIRS, new BlockBehaviorStairsWood()); //135
        this.registerVanilla(JUNGLE_STAIRS, new BlockBehaviorStairsWood()); //136
        //137: impulse_command_block
        this.registerVanilla(BEACON, new BlockBehaviorBeacon()); //138
        this.registerVanilla(COBBLESTONE_WALL, new BlockBehaviorWall()); //139
        this.registerVanilla(FLOWER_POT, new BlockBehaviorFlowerPot()); //140
        this.registerVanilla(CARROTS, new BlockBehaviorCarrot()); //141
        this.registerVanilla(POTATOES, new BlockBehaviorPotato()); //142
        this.registerVanilla(WOODEN_BUTTON, new BlockBehaviorButtonWooden()); //143
        this.registerVanilla(SKULL, new BlockBehaviorSkull()); //144
        this.registerVanilla(ANVIL, new BlockBehaviorAnvil()); //145
        this.registerVanilla(TRAPPED_CHEST, new BlockBehaviorTrappedChest()); //146
        this.registerVanilla(LIGHT_WEIGHTED_PRESSURE_PLATE, new BlockBehaviorWeightedPressurePlateLight()); //147
        this.registerVanilla(HEAVY_WEIGHTED_PRESSURE_PLATE, new BlockBehaviorWeightedPressurePlateHeavy()); //148
        this.registerVanilla(UNPOWERED_COMPARATOR, new BlockBehaviorRedstoneComparatorUnpowered()); //149
        this.registerVanilla(POWERED_COMPARATOR, new BlockBehaviorRedstoneComparatorPowered()); //150
        this.registerVanilla(DAYLIGHT_DETECTOR, new BlockBehaviorDaylightDetector()); //151
        this.registerVanilla(REDSTONE_BLOCK, new BlockBehaviorRedstone()); //152
        this.registerVanilla(QUARTZ_ORE, new BlockBehaviorOreQuartz()); //153
        this.registerVanilla(HOPPER, new BlockBehaviorHopper()); //154
        this.registerVanilla(QUARTZ_BLOCK, new BlockBehaviorQuartz()); //155
        this.registerVanilla(QUARTZ_STAIRS, new BlockBehaviorStairsQuartz()); //156
        this.registerVanilla(DOUBLE_WOODEN_SLAB, new BlockBehaviorDoubleSlabWood()); //157
        this.registerVanilla(WOODEN_SLAB, new BlockBehaviorSlabWood()); //158
        this.registerVanilla(STAINED_HARDENED_CLAY, new BlockBehaviorTerracottaStained()); //159
        this.registerVanilla(STAINED_GLASS_PANE, new BlockBehaviorGlassPaneStained()); //160
        this.registerVanilla(LEAVES2, new BlockBehaviorLeaves2()); //161
        this.registerVanilla(LOG2, new BlockBehaviorLog2()); //162
        this.registerVanilla(ACACIA_STAIRS, new BlockBehaviorStairsWood()); //163
        this.registerVanilla(DARK_OAK_STAIRS, new BlockBehaviorStairsWood()); //164
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
        this.registerVanilla(RED_SANDSTONE_STAIRS, new BlockBehaviorStairsRedSandstone()); //180
        this.registerVanilla(DOUBLE_STONE_SLAB2, BlockBehaviorDoubleSlab.factory(STONE_SLAB2, BlockBehaviorSlab.COLORS_2)); //181
        this.registerVanilla(STONE_SLAB2, BlockBehaviorSlab.factory(DOUBLE_STONE_SLAB2, BlockBehaviorSlab.COLORS_2)); //182
        this.registerVanilla(SPRUCE_FENCE_GATE, new BlockBehaviorFenceGate()); //183
        this.registerVanilla(BIRCH_FENCE_GATE, new BlockBehaviorFenceGate()); //184
        this.registerVanilla(JUNGLE_FENCE_GATE, new BlockBehaviorFenceGate()); //185
        this.registerVanilla(DARK_OAK_FENCE_GATE, new BlockBehaviorFenceGate()); //186
        this.registerVanilla(ACACIA_FENCE_GATE, new BlockBehaviorFenceGate()); //187
        //188: repeating_command_block
        //189: chain_command_block
        //190: hard_glass_pane
        //191: hard_stained_glass_pane
        //192: chemical_heat
        this.registerVanilla(SPRUCE_DOOR, new BlockBehaviorDoorWood()); //193
        this.registerVanilla(BIRCH_DOOR, new BlockBehaviorDoorWood()); //194
        this.registerVanilla(JUNGLE_DOOR, new BlockBehaviorDoorWood()); //195
        this.registerVanilla(ACACIA_DOOR, new BlockBehaviorDoorWood()); //196
        this.registerVanilla(DARK_OAK_DOOR, new BlockBehaviorDoorWood()); //197
        this.registerVanilla(GRASS_PATH, new BlockBehaviorGrassPath()); //198
        this.registerVanilla(FRAME, new BlockBehaviorItemFrame()); //199
        this.registerVanilla(CHORUS_FLOWER, new BlockBehaviorChorusFlower()); //200
        this.registerVanilla(PURPUR_BLOCK, new BlockBehaviorPurpur()); //201
        //202: chorus_flower
        this.registerVanilla(PURPUR_STAIRS, new BlockBehaviorStairsPurpur()); //203
        //204: colored_torch_bp
        this.registerVanilla(UNDYED_SHULKER_BOX, new BlockBehaviorUndyedShulkerBox()); //205
        this.registerVanilla(END_BRICKS, new BlockBehaviorBricksEndStone()); //206
        //207: frosted_ice
        this.registerVanilla(END_ROD, new BlockBehaviorEndRod()); //208
        this.registerVanilla(END_GATEWAY, new BlockBehaviorEndGateway()); //209
        //210: allow
        //211: deny
        //212: border
        this.registerVanilla(MAGMA, new BlockBehaviorMagma()); //213
        this.registerVanilla(NETHER_WART_BLOCK, new BlockNetherWartBlockBehavior()); //214
        this.registerVanilla(RED_NETHER_BRICK, new BlockBehaviorBricksRedNether()); //215
        this.registerVanilla(BONE_BLOCK, new BlockBehaviorBone()); //216
        //217: structure_void
        this.registerVanilla(SHULKER_BOX, new BlockBehaviorShulkerBox()); //218
        this.registerVanilla(PURPLE_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed()); //219
        this.registerVanilla(WHITE_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed()); //220
        this.registerVanilla(ORANGE_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed()); //221
        this.registerVanilla(MAGENTA_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed()); //222
        this.registerVanilla(LIGHT_BLUE_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed()); //223
        this.registerVanilla(YELLOW_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed()); //224
        this.registerVanilla(LIME_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed()); //225
        this.registerVanilla(PINK_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed()); //226
        this.registerVanilla(GRAY_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed()); //227
        this.registerVanilla(SILVER_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed()); //228
        this.registerVanilla(CYAN_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed()); //229
        //230: chalkboard
        this.registerVanilla(BLUE_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed()); //231
        this.registerVanilla(BROWN_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed()); //232
        this.registerVanilla(GREEN_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed()); //233
        this.registerVanilla(RED_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed()); //234
        this.registerVanilla(BLACK_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed()); //235
        this.registerVanilla(CONCRETE, new BlockBehaviorConcrete()); //236
        this.registerVanilla(CONCRETE_POWDER, new BlockBehaviorConcretePowder()); //237
        //238: chemistry_table
        //239: underwater_torch
        this.registerVanilla(CHORUS_PLANT, new BlockBehaviorChorusPlant()); //240
        this.registerVanilla(STAINED_GLASS, new BlockBehaviorGlassStained()); //241
        //242: camera
        this.registerVanilla(PODZOL, new BlockBehaviorPodzol()); //243
        this.registerVanilla(BEETROOT, new BlockBehaviorBeetroot()); //244
        this.registerVanilla(STONECUTTER, new BlockBehaviorStonecutter()); //245
        this.registerVanilla(GLOWING_OBSIDIAN, new BlockBehaviorObsidianGlowing()); //246
        //list.put(NETHER_REACTOR, new BlockNetherReactor()); //247 Should not be removed
        //248: info_update
        //249: info_update2
        //TODO: list.put(PISTON_EXTENSION, new BlockPistonExtension()); //250
        this.registerVanilla(OBSERVER, new BlockBehaviorObserver()); //251
        //252: structure_block
        //253: hard_glass
        //254: hard_stained_glass
        //255: reserved6
        //256: unknown
        this.registerVanilla(PRISMARINE_STAIRS, new BlockBehaviorStairsPrismarine()); //257
        this.registerVanilla(DARK_PRISMARINE_STAIRS, new BlockBehaviorStairsDarkPrismarine()); //258
        this.registerVanilla(PRISMARINE_BRICKS_STAIRS, new BlockBehaviorStairsPrismarineBricks()); //259
        this.registerVanilla(STRIPPED_SPRUCE_LOG, new BlockBehaviorStrippedLog()); //260
        this.registerVanilla(STRIPPED_BIRCH_LOG, new BlockBehaviorStrippedLog()); //261
        this.registerVanilla(STRIPPED_JUNGLE_LOG, new BlockBehaviorStrippedLog()); //262
        this.registerVanilla(STRIPPED_ACACIA_LOG, new BlockBehaviorStrippedLog()); //263
        this.registerVanilla(STRIPPED_DARK_OAK_LOG, new BlockBehaviorStrippedLog()); //264
        this.registerVanilla(STRIPPED_OAK_LOG, new BlockBehaviorStrippedLog()); //265
        this.registerVanilla(BLUE_ICE, new BlockBehaviorBlueIce()); //266
        //267: element_1
        // ...
        //384: element_118
        //385: seagrass
        //386: coral
        //387: coral_block
        //388: coral_fan
        //389: coral_fan_dead
        //390: coral_fan_hang
        //391: coral_fan_hang2
        //392: coral_fan_hang3
        this.registerVanilla(KELP, new BlockBehaviorKelp());//393
        this.registerVanilla(DRIED_KELP_BLOCK, new BlockBehaviorDriedKelp()); //394
        this.registerVanilla(ACACIA_BUTTON, new BlockBehaviorButtonWooden());//395
        this.registerVanilla(BIRCH_BUTTON, new BlockBehaviorButtonWooden());//396
        this.registerVanilla(DARK_OAK_BUTTON, new BlockBehaviorButtonWooden());//397
        this.registerVanilla(JUNGLE_BUTTON, new BlockBehaviorButtonWooden());//398
        this.registerVanilla(SPRUCE_BUTTON, new BlockBehaviorButtonWooden());//399
        this.registerVanilla(ACACIA_TRAPDOOR, BlockBehaviorTrapdoor.factory(BlockColor.ORANGE_BLOCK_COLOR)); //400
        this.registerVanilla(BIRCH_TRAPDOOR, BlockBehaviorTrapdoor.factory(BlockColor.SAND_BLOCK_COLOR)); //401
        this.registerVanilla(DARK_OAK_TRAPDOOR, BlockBehaviorTrapdoor.factory(BlockColor.BROWN_BLOCK_COLOR)); //402
        this.registerVanilla(JUNGLE_TRAPDOOR, BlockBehaviorTrapdoor.factory(BlockColor.DIRT_BLOCK_COLOR)); //403
        this.registerVanilla(SPRUCE_TRAPDOOR, BlockBehaviorTrapdoor.factory(BlockColor.SPRUCE_BLOCK_COLOR)); //404
        this.registerVanilla(ACACIA_PRESSURE_PLATE, new BlockBehaviorPressurePlateWood());//405
        this.registerVanilla(BIRCH_PRESSURE_PLATE, new BlockBehaviorPressurePlateWood());//406
        this.registerVanilla(DARK_OAK_PRESSURE_PLATE, new BlockBehaviorPressurePlateWood());//407
        this.registerVanilla(JUNGLE_PRESSURE_PLATE, new BlockBehaviorPressurePlateWood());//408
        this.registerVanilla(SPRUCE_PRESSURE_PLATE, new BlockBehaviorPressurePlateWood());//409
        //410: carved_pumpkin
        //411: sea_pickle
        //412: conduit
        //413: turtle_egg
        //414: bubble_column
        this.registerVanilla(BARRIER, new BlockBehaviorBarrier()); //415
        this.registerVanilla(STONE_SLAB3, BlockBehaviorSlab.factory(DOUBLE_STONE_SLAB3, BlockBehaviorSlab.COLORS_3)); //416
        //416: stone_slab3
        //417: bamboo
        //418: bamboo_sapling
        //419: scaffolding
        this.registerVanilla(STONE_SLAB4, BlockBehaviorSlab.factory(DOUBLE_STONE_SLAB4, BlockBehaviorSlab.COLORS_4)); //420
        this.registerVanilla(DOUBLE_STONE_SLAB3, BlockBehaviorDoubleSlab.factory(STONE_SLAB3, BlockBehaviorSlab.COLORS_3)); //421
        this.registerVanilla(DOUBLE_STONE_SLAB4, BlockBehaviorDoubleSlab.factory(DOUBLE_STONE_SLAB4, BlockBehaviorSlab.COLORS_4)); //422
        this.registerVanilla(GRANITE_STAIRS, new BlockBehaviorStairsGranite()); //423
        this.registerVanilla(DIORITE_STAIRS, new BlockBehaviorStairsDiorite()); //424
        this.registerVanilla(ANDESITE_STAIRS, new BlockBehaviorStairsAndesite()); //425
        this.registerVanilla(POLISHED_GRANITE_STAIRS, new BlockBehaviorStairsGranite()); //426
        this.registerVanilla(POLISHED_DIORITE_STAIRS, new BlockBehaviorStairsDiorite()); //427
        this.registerVanilla(POLISHED_ANDESITE_STAIRS, new BlockBehaviorStairsAndesite()); //428
        this.registerVanilla(MOSSY_STONE_BRICK_STAIRS, new BlockBehaviorStairsStoneBrick()); //429
        this.registerVanilla(SMOOTH_RED_SANDSTONE_STAIRS, new BlockBehaviorStairsSmoothRedSandstone()); //430
        this.registerVanilla(SMOOTH_SANDSTONE_STAIRS, new BlockBehaviorStairsSmoothSandstone()); //431
        this.registerVanilla(END_BRICK_STAIRS, new BlockBehaviorStairsEndStoneBrick()); //432
        this.registerVanilla(MOSSY_COBBLESTONE_STAIRS, new BlockBehaviorStairsCobblestone()); //433
        this.registerVanilla(NORMAL_STONE_STAIRS, new BlockBehaviorStairsStone()); //434
        this.registerVanilla(SPRUCE_STANDING_SIGN, BlockBehaviorSignPost.factory(SPRUCE_WALL_SIGN, ItemIds.SPRUCE_SIGN)); //435
        this.registerVanilla(SPRUCE_WALL_SIGN, BlockBehaviorWallSign.factory(SPRUCE_STANDING_SIGN, ItemIds.SPRUCE_SIGN)); // 436
        this.registerVanilla(SMOOTH_STONE, new BlockBehaviorSmoothStone()); // 437
        this.registerVanilla(RED_NETHER_BRICK_STAIRS, new BlockBehaviorStairsNetherBrick()); //438
        this.registerVanilla(SMOOTH_QUARTZ_STAIRS, new BlockBehaviorStairsQuartz()); //439
        this.registerVanilla(BIRCH_STANDING_SIGN, BlockBehaviorSignPost.factory(BIRCH_WALL_SIGN, ItemIds.BIRCH_SIGN)); //440
        this.registerVanilla(BIRCH_WALL_SIGN, BlockBehaviorWallSign.factory(BIRCH_STANDING_SIGN, ItemIds.BIRCH_SIGN)); //441
        this.registerVanilla(JUNGLE_STANDING_SIGN, BlockBehaviorSignPost.factory(JUNGLE_WALL_SIGN, ItemIds.JUNGLE_SIGN)); //442
        this.registerVanilla(JUNGLE_WALL_SIGN, BlockBehaviorWallSign.factory(JUNGLE_STANDING_SIGN, ItemIds.JUNGLE_SIGN)); //443
        this.registerVanilla(ACACIA_STANDING_SIGN, BlockBehaviorSignPost.factory(ACACIA_WALL_SIGN, ItemIds.ACACIA_SIGN)); //444
        this.registerVanilla(ACACIA_WALL_SIGN, BlockBehaviorWallSign.factory(ACACIA_STANDING_SIGN, ItemIds.ACACIA_SIGN)); //445
        this.registerVanilla(DARK_OAK_STANDING_SIGN, BlockBehaviorSignPost.factory(DARK_OAK_WALL_SIGN, ItemIds.DARK_OAK_SIGN)); //446
        this.registerVanilla(DARK_OAK_WALL_SIGN, BlockBehaviorWallSign.factory(DARK_OAK_STANDING_SIGN, ItemIds.DARK_OAK_SIGN)); //447
        this.registerVanilla(LECTERN, new BlockBehaviorLectern()); //448
        //449: grindstone
        this.registerVanilla(BLAST_FURNACE, BlockBehaviorFurnace.factory(BlockEntityTypes.BLAST_FURNACE)); // 450
        //451: stonecutter_block
        this.registerVanilla(SMOKER, BlockBehaviorFurnace.factory(BlockEntityTypes.SMOKER)); //452
        this.registerVanilla(LIT_SMOKER, BlockBehaviorFurnaceBurning.factory(BlockEntityTypes.SMOKER)); //453
        //454: cartography_table
        //455: fletching_table
        //456: smithing_table
        this.registerVanilla(BARREL, new BlockBehaviorBarrel()); // 457
        //458: loom
        //459: bell
        //460: sweet_berry_bush
        //461: lantern
        this.registerVanilla(CAMPFIRE, new BlockBehaviorCampfire()); //462
        //463: lava_cauldron
        //464: jigsaw
        this.registerVanilla(WOOD, new BlockBehaviorWood()); //465
        //466: composter
        this.registerVanilla(LIT_BLAST_FURNACE, BlockBehaviorFurnaceBurning.factory(BlockEntityTypes.BLAST_FURNACE)); //467
        this.registerVanilla(LIGHT_BLOCK, new BlockBehaviorLight()); //468
        //469: wither_rose
        //470: stickypistonarmcollision
        //471: bee_nest
        //472: beehive
        //473: honey_block
        this.registerVanilla(HONEYCOMB_BLOCK, new BlockHoneycombBlockBehavior()); //474
    }
}
