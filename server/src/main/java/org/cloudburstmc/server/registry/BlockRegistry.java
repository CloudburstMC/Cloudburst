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
import org.cloudburstmc.server.block.behavior.*;
import org.cloudburstmc.server.block.serializer.BlockSerializer;
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
        register(id, behavior, null);

        // generate legacy ID (Not sure why we need to but it's a requirement)
        int legacyId = this.customIdAllocator.getAndIncrement();
        this.idLegacyMap.put(id, legacyId);
    }

    private synchronized void register(Identifier id, BlockBehavior behavior, BlockSerializer serializer,
                                       BlockTrait<?>... traits) throws RegistryException {
        checkNotNull(id, "id");
        checkNotNull(behavior, "behavior");
        checkNotNull(serializer, "serializer");
        checkNotNull(traits, "traits");
        checkClosed();
        if (this.behaviorMap.containsKey(id)) throw new RegistryException(id + " is already registered");

        this.behaviorMap.put(id, behavior);
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
        this.behaviorMap.put(AIR, new BlockBehaviorAir()); //0
        this.behaviorMap.put(STONE, new BlockBehaviorStone()); //1
        this.behaviorMap.put(GRASS, new BlockBehaviorGrass()); //2
        this.behaviorMap.put(DIRT, new BlockBehaviorDirt()); //3
        this.behaviorMap.put(COBBLESTONE, new BlockBehaviorCobblestone()); //4
        this.behaviorMap.put(PLANKS, new BlockBehaviorPlanks()); //5
        this.behaviorMap.put(SAPLING, new BlockBehaviorSapling()); //6
        this.behaviorMap.put(BEDROCK, new BlockBehaviorBedrock()); //7
        this.behaviorMap.put(FLOWING_WATER, BlockBehaviorWater.factory(WATER)); //8
        this.behaviorMap.put(WATER, BlockBehaviorWaterStill.factory(FLOWING_WATER)); //9
        this.behaviorMap.put(FLOWING_LAVA, BlockBehaviorLava.factory(LAVA)); //10
        this.behaviorMap.put(LAVA, BlockBehaviorLavaStill.factory(FLOWING_LAVA)); //11
        this.behaviorMap.put(SAND, new BlockBehaviorSand()); //12
        this.behaviorMap.put(GRAVEL, new BlockBehaviorGravel()); //13
        this.behaviorMap.put(GOLD_ORE, new BlockBehaviorOreGold()); //14
        this.behaviorMap.put(IRON_ORE, new BlockBehaviorOreIron()); //15
        this.behaviorMap.put(COAL_ORE, new BlockBehaviorOreCoal()); //16
        this.behaviorMap.put(LOG, new BlockBehaviorLog()); //17
        this.behaviorMap.put(LEAVES, new BlockBehaviorLeaves()); //18
        this.behaviorMap.put(SPONGE, new BlockBehaviorSponge()); //19
        this.behaviorMap.put(GLASS, new BlockBehaviorGlass()); //20
        this.behaviorMap.put(LAPIS_ORE, new BlockBehaviorOreLapis()); //21
        this.behaviorMap.put(LAPIS_BLOCK, new BlockBehaviorLapis()); //22
        this.behaviorMap.put(DISPENSER, new BlockBehaviorDispenser()); //23
        this.behaviorMap.put(SANDSTONE, new BlockBehaviorSandstone()); //24
        this.behaviorMap.put(NOTEBLOCK, new BlockBehaviorNoteblock()); //25
        this.behaviorMap.put(BED, new BlockBehaviorBed()); //26
        this.behaviorMap.put(GOLDEN_RAIL, new BlockBehaviorRailPowered()); //27
        this.behaviorMap.put(DETECTOR_RAIL, new BlockBehaviorRailDetector()); //28
        this.behaviorMap.put(STICKY_PISTON, new BlockBehaviorPistonSticky()); //29
        this.behaviorMap.put(WEB, new BlockBehaviorCobweb()); //30
        this.behaviorMap.put(TALL_GRASS, new BlockBehaviorTallGrass()); //31
        this.behaviorMap.put(DEADBUSH, new BlockBehaviorDeadBush()); //32
        this.behaviorMap.put(PISTON, new BlockBehaviorPiston()); //33
        this.behaviorMap.put(PISTON_ARM_COLLISION, new BlockBehaviorPistonHead()); //34
        this.behaviorMap.put(WOOL, new BlockBehaviorWool()); //35
        this.behaviorMap.put(YELLOW_FLOWER, new BlockBehaviorDandelion()); //37
        this.behaviorMap.put(RED_FLOWER, new BlockBehaviorFlower()); //38
        this.behaviorMap.put(BROWN_MUSHROOM, new BlockBehaviorMushroomBrown()); //39
        this.behaviorMap.put(RED_MUSHROOM, new BlockBehaviorMushroomRed()); //40
        this.behaviorMap.put(GOLD_BLOCK, new BlockBehaviorGold()); //41
        this.behaviorMap.put(IRON_BLOCK, new BlockBehaviorIron()); //42
        this.behaviorMap.put(DOUBLE_STONE_SLAB, BlockBehaviorDoubleSlab.factory(STONE_SLAB, BlockBehaviorSlab.COLORS_1)); //43
        this.behaviorMap.put(STONE_SLAB, BlockBehaviorSlab.factory(DOUBLE_STONE_SLAB, BlockBehaviorSlab.COLORS_1)); //44
        this.behaviorMap.put(BRICK_BLOCK, new BlockBehaviorBricks()); //45
        this.behaviorMap.put(TNT, new BlockBehaviorTNT()); //46
        this.behaviorMap.put(BOOKSHELF, new BlockBehaviorBookshelf()); //47
        this.behaviorMap.put(MOSSY_COBBLESTONE, new BlockBehaviorMossStone()); //48
        this.behaviorMap.put(OBSIDIAN, new BlockBehaviorObsidian()); //49
        this.behaviorMap.put(TORCH, new BlockBehaviorTorch()); //50
        this.behaviorMap.put(FIRE, new BlockBehaviorFire()); //51
        this.behaviorMap.put(MOB_SPAWNER, new BlockBehaviorMobSpawner()); //52
        this.behaviorMap.put(OAK_STAIRS, new BlockBehaviorStairsWood()); //53
        this.behaviorMap.put(CHEST, new BlockBehaviorChest()); //54
        this.behaviorMap.put(REDSTONE_WIRE, new BlockBehaviorRedstoneWire()); //55
        this.behaviorMap.put(DIAMOND_ORE, new BlockBehaviorOreDiamond()); //56
        this.behaviorMap.put(DIAMOND_BLOCK, new BlockBehaviorDiamond()); //57
        this.behaviorMap.put(CRAFTING_TABLE, new BlockBehaviorCraftingTable()); //58
        this.behaviorMap.put(WHEAT, new BlockBehaviorWheat()); //59
        this.behaviorMap.put(FARMLAND, new BlockBehaviorFarmland()); //60
        this.behaviorMap.put(FURNACE, BlockBehaviorFurnace.factory(BlockEntityTypes.FURNACE)); //61
        this.behaviorMap.put(LIT_FURNACE, BlockBehaviorFurnaceBurning.factory(BlockEntityTypes.FURNACE)); //62
        this.behaviorMap.put(STANDING_SIGN, BlockBehaviorSignPost.factory(WALL_SIGN, ItemIds.SIGN)); //63
        this.behaviorMap.put(WOODEN_DOOR, new BlockBehaviorDoorWood()); //64
        this.behaviorMap.put(LADDER, new BlockBehaviorLadder()); //65
        this.behaviorMap.put(RAIL, new BlockBehaviorRail()); //66
        this.behaviorMap.put(STONE_STAIRS, new BlockBehaviorStairsCobblestone()); //67
        this.behaviorMap.put(WALL_SIGN, BlockBehaviorWallSign.factory(STANDING_SIGN, ItemIds.SIGN)); //68
        this.behaviorMap.put(LEVER, new BlockBehaviorLever()); //69
        this.behaviorMap.put(STONE_PRESSURE_PLATE, new BlockBehaviorPressurePlateStone()); //70
        this.behaviorMap.put(IRON_DOOR, new BlockBehaviorDoorIron()); //71
        this.behaviorMap.put(WOODEN_PRESSURE_PLATE, new BlockBehaviorPressurePlateWood()); //72
        this.behaviorMap.put(REDSTONE_ORE, new BlockBehaviorOreRedstone()); //73
        this.behaviorMap.put(LIT_REDSTONE_ORE, new BlockBehaviorOreRedstoneGlowing()); //74
        this.behaviorMap.put(UNLIT_REDSTONE_TORCH, new BlockBehaviorRedstoneTorchUnlit());
        this.behaviorMap.put(REDSTONE_TORCH, new BlockBehaviorRedstoneTorch()); //76
        this.behaviorMap.put(STONE_BUTTON, new BlockBehaviorButtonStone()); //77
        this.behaviorMap.put(SNOW_LAYER, new BlockBehaviorSnowLayer()); //78
        this.behaviorMap.put(ICE, new BlockBehaviorIce()); //79
        this.behaviorMap.put(SNOW, new BlockBehaviorSnow()); //80
        this.behaviorMap.put(CACTUS, new BlockBehaviorCactus()); //81
        this.behaviorMap.put(CLAY, new BlockBehaviorClay()); //82
        this.behaviorMap.put(REEDS, new ReedsBlockBehavior()); //83
        this.behaviorMap.put(JUKEBOX, new BlockBehaviorJukebox()); //84
        this.behaviorMap.put(FENCE, new BlockBehaviorFenceWooden()); //85
        this.behaviorMap.put(PUMPKIN, new BlockBehaviorPumpkin()); //86
        this.behaviorMap.put(NETHERRACK, new BlockBehaviorNetherrack()); //87
        this.behaviorMap.put(SOUL_SAND, new BlockBehaviorSoulSand()); //88
        this.behaviorMap.put(GLOWSTONE, new BlockBehaviorGlowstone()); //89
        this.behaviorMap.put(PORTAL, new BlockBehaviorNetherPortal()); //90
        this.behaviorMap.put(LIT_PUMPKIN, new BlockBehaviorPumpkinLit()); //91
        this.behaviorMap.put(CAKE, new BlockBehaviorCake()); //92
        this.behaviorMap.put(UNPOWERED_REPEATER, new BlockBehaviorRedstoneRepeaterUnpowered()); //93
        this.behaviorMap.put(POWERED_REPEATER, new BlockBehaviorRedstoneRepeaterPowered()); //94
        this.behaviorMap.put(INVISIBLE_BEDROCK, new BlockBehaviorBedrockInvisible()); //95
        this.behaviorMap.put(TRAPDOOR, new BlockBehaviorTrapdoor()); //96
        this.behaviorMap.put(MONSTER_EGG, new BlockBehaviorMonsterEgg()); //97
        this.behaviorMap.put(STONEBRICK, new BlockBehaviorBricksStone()); //98
        this.behaviorMap.put(BROWN_MUSHROOM_BLOCK, new BlockBehaviorHugeMushroomBrown()); //99
        this.behaviorMap.put(RED_MUSHROOM_BLOCK, new BlockBehaviorHugeMushroomRed()); //100
        this.behaviorMap.put(IRON_BARS, new BlockBehaviorIronBars()); //101
        this.behaviorMap.put(GLASS_PANE, new BlockBehaviorGlassPane()); //102
        this.behaviorMap.put(MELON_BLOCK, new BlockBehaviorMelon()); //103
        this.behaviorMap.put(PUMPKIN_STEM, new BlockBehaviorStemPumpkin()); //104
        this.behaviorMap.put(MELON_STEM, new BlockBehaviorStemMelon()); //105
        this.behaviorMap.put(VINE, new BlockBehaviorVine()); //106
        this.behaviorMap.put(FENCE_GATE, new BlockBehaviorFenceGate()); //107
        this.behaviorMap.put(BRICK_STAIRS, new BlockBehaviorStairsBrick()); //108
        this.behaviorMap.put(STONE_BRICK_STAIRS, new BlockBehaviorStairsStoneBrick()); //109
        this.behaviorMap.put(MYCELIUM, new BlockBehaviorMycelium()); //110
        this.behaviorMap.put(WATERLILY, new BlockBehaviorWaterLily()); //111
        this.behaviorMap.put(NETHER_BRICK, new BlockBehaviorBricksNether()); //112
        this.behaviorMap.put(NETHER_BRICK_FENCE, new BlockBehaviorFenceNetherBrick()); //113
        this.behaviorMap.put(NETHER_BRICK_STAIRS, new BlockBehaviorStairsNetherBrick()); //114
        this.behaviorMap.put(NETHER_WART, new BlockBehaviorNetherWart()); //115
        this.behaviorMap.put(ENCHANTING_TABLE, new BlockBehaviorEnchantingTable()); //116
        this.behaviorMap.put(BREWING_STAND, new BlockBehaviorBrewingStand()); //117
        this.behaviorMap.put(CAULDRON, new BlockBehaviorCauldron()); //118
        this.behaviorMap.put(END_PORTAL, new BlockBehaviorEndPortal()); //119
        this.behaviorMap.put(END_PORTAL_FRAME, new BlockBehaviorEndPortalFrame()); //120
        this.behaviorMap.put(END_STONE, new BlockBehaviorEndStone()); //121
        this.behaviorMap.put(DRAGON_EGG, new BlockBehaviorDragonEgg()); //122
        this.behaviorMap.put(REDSTONE_LAMP, new BlockBehaviorRedstoneLamp()); //123
        this.behaviorMap.put(LIT_REDSTONE_LAMP, new BlockBehaviorRedstoneLampLit()); //124
        //TODO: list.put(DROPPER, new BlockDropper()); //125
        this.behaviorMap.put(ACTIVATOR_RAIL, new BlockBehaviorRailActivator()); //126
        this.behaviorMap.put(COCOA, new BlockBehaviorCocoa()); //127
        this.behaviorMap.put(SANDSTONE_STAIRS, new BlockBehaviorStairsSandstone()); //128
        this.behaviorMap.put(EMERALD_ORE, new BlockBehaviorOreEmerald()); //129
        this.behaviorMap.put(ENDER_CHEST, new BlockBehaviorEnderChest()); //130
        this.behaviorMap.put(TRIPWIRE_HOOK, new BlockBehaviorTripWireHook()); //131
        this.behaviorMap.put(TRIPWIRE, new BlockBehaviorTripWire()); //132
        this.behaviorMap.put(EMERALD_BLOCK, new BlockBehaviorEmerald()); //133
        this.behaviorMap.put(SPRUCE_STAIRS, new BlockBehaviorStairsWood()); //134
        this.behaviorMap.put(BIRCH_STAIRS, new BlockBehaviorStairsWood()); //135
        this.behaviorMap.put(JUNGLE_STAIRS, new BlockBehaviorStairsWood()); //136
        //137: impulse_command_block
        this.behaviorMap.put(BEACON, new BlockBehaviorBeacon()); //138
        this.behaviorMap.put(COBBLESTONE_WALL, new BlockBehaviorWall()); //139
        this.behaviorMap.put(FLOWER_POT, new BlockBehaviorFlowerPot()); //140
        this.behaviorMap.put(CARROTS, new BlockBehaviorCarrot()); //141
        this.behaviorMap.put(POTATOES, new BlockBehaviorPotato()); //142
        this.behaviorMap.put(WOODEN_BUTTON, new BlockBehaviorButtonWooden()); //143
        this.behaviorMap.put(SKULL, new BlockBehaviorSkull()); //144
        this.behaviorMap.put(ANVIL, new BlockBehaviorAnvil()); //145
        this.behaviorMap.put(TRAPPED_CHEST, new BlockBehaviorTrappedChest()); //146
        this.behaviorMap.put(LIGHT_WEIGHTED_PRESSURE_PLATE, new BlockBehaviorWeightedPressurePlateLight()); //147
        this.behaviorMap.put(HEAVY_WEIGHTED_PRESSURE_PLATE, new BlockBehaviorWeightedPressurePlateHeavy()); //148
        this.behaviorMap.put(UNPOWERED_COMPARATOR, new BlockBehaviorRedstoneComparatorUnpowered()); //149
        this.behaviorMap.put(POWERED_COMPARATOR, new BlockBehaviorRedstoneComparatorPowered()); //150
        this.behaviorMap.put(DAYLIGHT_DETECTOR, new BlockBehaviorDaylightDetector()); //151
        this.behaviorMap.put(REDSTONE_BLOCK, new BlockBehaviorRedstone()); //152
        this.behaviorMap.put(QUARTZ_ORE, new BlockBehaviorOreQuartz()); //153
        this.behaviorMap.put(HOPPER, new BlockBehaviorHopper()); //154
        this.behaviorMap.put(QUARTZ_BLOCK, new BlockBehaviorQuartz()); //155
        this.behaviorMap.put(QUARTZ_STAIRS, new BlockBehaviorStairsQuartz()); //156
        this.behaviorMap.put(DOUBLE_WOODEN_SLAB, new BlockBehaviorDoubleSlabWood()); //157
        this.behaviorMap.put(WOODEN_SLAB, new BlockBehaviorSlabWood()); //158
        this.behaviorMap.put(STAINED_HARDENED_CLAY, new BlockBehaviorTerracottaStained()); //159
        this.behaviorMap.put(STAINED_GLASS_PANE, new BlockBehaviorGlassPaneStained()); //160
        this.behaviorMap.put(LEAVES2, new BlockBehaviorLeaves2()); //161
        this.behaviorMap.put(LOG2, new BlockBehaviorLog2()); //162
        this.behaviorMap.put(ACACIA_STAIRS, new BlockBehaviorStairsWood()); //163
        this.behaviorMap.put(DARK_OAK_STAIRS, new BlockBehaviorStairsWood()); //164
        this.behaviorMap.put(SLIME, new BlockBehaviorSlime()); //165
        //166: glow_stick
        this.behaviorMap.put(IRON_TRAPDOOR, new BlockBehaviorTrapdoorIron()); //167
        this.behaviorMap.put(PRISMARINE, new BlockBehaviorPrismarine()); //168
        this.behaviorMap.put(SEA_LANTERN, new BlockBehaviorSeaLantern()); //169
        this.behaviorMap.put(HAY_BLOCK, new BlockBehaviorHayBale()); //170
        this.behaviorMap.put(CARPET, new BlockBehaviorCarpet()); //171
        this.behaviorMap.put(HARDENED_CLAY, new BlockBehaviorTerracotta()); //172
        this.behaviorMap.put(COAL_BLOCK, new BlockBehaviorCoal()); //173
        this.behaviorMap.put(PACKED_ICE, new BlockBehaviorIcePacked()); //174
        this.behaviorMap.put(DOUBLE_PLANT, new BlockBehaviorDoublePlant()); //175
        this.behaviorMap.put(STANDING_BANNER, new BlockBehaviorBanner()); //176
        this.behaviorMap.put(WALL_BANNER, new BlockBehaviorWallBanner()); //177
        this.behaviorMap.put(DAYLIGHT_DETECTOR_INVERTED, new BlockBehaviorDaylightDetectorInverted()); //178
        this.behaviorMap.put(RED_SANDSTONE, new BlockBehaviorRedSandstone()); //179
        this.behaviorMap.put(RED_SANDSTONE_STAIRS, new BlockBehaviorStairsRedSandstone()); //180
        this.behaviorMap.put(DOUBLE_STONE_SLAB2, BlockBehaviorDoubleSlab.factory(STONE_SLAB2, BlockBehaviorSlab.COLORS_2)); //181
        this.behaviorMap.put(STONE_SLAB2, BlockBehaviorSlab.factory(DOUBLE_STONE_SLAB2, BlockBehaviorSlab.COLORS_2)); //182
        this.behaviorMap.put(SPRUCE_FENCE_GATE, new BlockBehaviorFenceGate()); //183
        this.behaviorMap.put(BIRCH_FENCE_GATE, new BlockBehaviorFenceGate()); //184
        this.behaviorMap.put(JUNGLE_FENCE_GATE, new BlockBehaviorFenceGate()); //185
        this.behaviorMap.put(DARK_OAK_FENCE_GATE, new BlockBehaviorFenceGate()); //186
        this.behaviorMap.put(ACACIA_FENCE_GATE, new BlockBehaviorFenceGate()); //187
        //188: repeating_command_block
        //189: chain_command_block
        //190: hard_glass_pane
        //191: hard_stained_glass_pane
        //192: chemical_heat
        this.behaviorMap.put(SPRUCE_DOOR, new BlockBehaviorDoorWood()); //193
        this.behaviorMap.put(BIRCH_DOOR, new BlockBehaviorDoorWood()); //194
        this.behaviorMap.put(JUNGLE_DOOR, new BlockBehaviorDoorWood()); //195
        this.behaviorMap.put(ACACIA_DOOR, new BlockBehaviorDoorWood()); //196
        this.behaviorMap.put(DARK_OAK_DOOR, new BlockBehaviorDoorWood()); //197
        this.behaviorMap.put(GRASS_PATH, new BlockBehaviorGrassPath()); //198
        this.behaviorMap.put(FRAME, new BlockBehaviorItemFrame()); //199
        this.behaviorMap.put(CHORUS_FLOWER, new BlockBehaviorChorusFlower()); //200
        this.behaviorMap.put(PURPUR_BLOCK, new BlockBehaviorPurpur()); //201
        //202: chorus_flower
        this.behaviorMap.put(PURPUR_STAIRS, new BlockBehaviorStairsPurpur()); //203
        //204: colored_torch_bp
        this.behaviorMap.put(UNDYED_SHULKER_BOX, new BlockBehaviorUndyedShulkerBox()); //205
        this.behaviorMap.put(END_BRICKS, new BlockBehaviorBricksEndStone()); //206
        //207: frosted_ice
        this.behaviorMap.put(END_ROD, new BlockBehaviorEndRod()); //208
        this.behaviorMap.put(END_GATEWAY, new BlockBehaviorEndGateway()); //209
        //210: allow
        //211: deny
        //212: border
        this.behaviorMap.put(MAGMA, new BlockBehaviorMagma()); //213
        this.behaviorMap.put(NETHER_WART_BLOCK, new BlockNetherWartBlockBehavior()); //214
        this.behaviorMap.put(RED_NETHER_BRICK, new BlockBehaviorBricksRedNether()); //215
        this.behaviorMap.put(BONE_BLOCK, new BlockBehaviorBone()); //216
        //217: structure_void
        this.behaviorMap.put(SHULKER_BOX, new BlockBehaviorShulkerBox()); //218
        this.behaviorMap.put(PURPLE_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed()); //219
        this.behaviorMap.put(WHITE_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed()); //220
        this.behaviorMap.put(ORANGE_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed()); //221
        this.behaviorMap.put(MAGENTA_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed()); //222
        this.behaviorMap.put(LIGHT_BLUE_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed()); //223
        this.behaviorMap.put(YELLOW_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed()); //224
        this.behaviorMap.put(LIME_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed()); //225
        this.behaviorMap.put(PINK_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed()); //226
        this.behaviorMap.put(GRAY_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed()); //227
        this.behaviorMap.put(SILVER_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed()); //228
        this.behaviorMap.put(CYAN_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed()); //229
        //230: chalkboard
        this.behaviorMap.put(BLUE_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed()); //231
        this.behaviorMap.put(BROWN_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed()); //232
        this.behaviorMap.put(GREEN_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed()); //233
        this.behaviorMap.put(RED_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed()); //234
        this.behaviorMap.put(BLACK_GLAZED_TERRACOTTA, new BlockBehaviorTerracottaGlazed()); //235
        this.behaviorMap.put(CONCRETE, new BlockBehaviorConcrete()); //236
        this.behaviorMap.put(CONCRETE_POWDER, new BlockBehaviorConcretePowder()); //237
        //238: chemistry_table
        //239: underwater_torch
        this.behaviorMap.put(CHORUS_PLANT, new BlockBehaviorChorusPlant()); //240
        this.behaviorMap.put(STAINED_GLASS, new BlockBehaviorGlassStained()); //241
        //242: camera
        this.behaviorMap.put(PODZOL, new BlockBehaviorPodzol()); //243
        this.behaviorMap.put(BEETROOT, new BlockBehaviorBeetroot()); //244
        this.behaviorMap.put(STONECUTTER, new BlockBehaviorStonecutter()); //245
        this.behaviorMap.put(GLOWING_OBSIDIAN, new BlockBehaviorObsidianGlowing()); //246
        //list.put(NETHER_REACTOR, new BlockNetherReactor()); //247 Should not be removed
        //248: info_update
        //249: info_update2
        //TODO: list.put(PISTON_EXTENSION, new BlockPistonExtension()); //250
        this.behaviorMap.put(OBSERVER, new BlockBehaviorObserver()); //251
        //252: structure_block
        //253: hard_glass
        //254: hard_stained_glass
        //255: reserved6
        //256: unknown
        this.behaviorMap.put(PRISMARINE_STAIRS, new BlockBehaviorStairsPrismarine()); //257
        this.behaviorMap.put(DARK_PRISMARINE_STAIRS, new BlockBehaviorStairsDarkPrismarine()); //258
        this.behaviorMap.put(PRISMARINE_BRICKS_STAIRS, new BlockBehaviorStairsPrismarineBricks()); //259
        this.behaviorMap.put(STRIPPED_SPRUCE_LOG, new BlockBehaviorStrippedLog()); //260
        this.behaviorMap.put(STRIPPED_BIRCH_LOG, new BlockBehaviorStrippedLog()); //261
        this.behaviorMap.put(STRIPPED_JUNGLE_LOG, new BlockBehaviorStrippedLog()); //262
        this.behaviorMap.put(STRIPPED_ACACIA_LOG, new BlockBehaviorStrippedLog()); //263
        this.behaviorMap.put(STRIPPED_DARK_OAK_LOG, new BlockBehaviorStrippedLog()); //264
        this.behaviorMap.put(STRIPPED_OAK_LOG, new BlockBehaviorStrippedLog()); //265
        this.behaviorMap.put(BLUE_ICE, new BlockBehaviorBlueIce()); //266
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
        this.behaviorMap.put(KELP, new BlockBehaviorKelp());//393
        this.behaviorMap.put(DRIED_KELP_BLOCK, new BlockBehaviorDriedKelp()); //394
        this.behaviorMap.put(ACACIA_BUTTON, new BlockBehaviorButtonWooden());//395
        this.behaviorMap.put(BIRCH_BUTTON, new BlockBehaviorButtonWooden());//396
        this.behaviorMap.put(DARK_OAK_BUTTON, new BlockBehaviorButtonWooden());//397
        this.behaviorMap.put(JUNGLE_BUTTON, new BlockBehaviorButtonWooden());//398
        this.behaviorMap.put(SPRUCE_BUTTON, new BlockBehaviorButtonWooden());//399
        this.behaviorMap.put(ACACIA_TRAPDOOR, BlockBehaviorTrapdoor.factory(BlockColor.ORANGE_BLOCK_COLOR)); //400
        this.behaviorMap.put(BIRCH_TRAPDOOR, BlockBehaviorTrapdoor.factory(BlockColor.SAND_BLOCK_COLOR)); //401
        this.behaviorMap.put(DARK_OAK_TRAPDOOR, BlockBehaviorTrapdoor.factory(BlockColor.BROWN_BLOCK_COLOR)); //402
        this.behaviorMap.put(JUNGLE_TRAPDOOR, BlockBehaviorTrapdoor.factory(BlockColor.DIRT_BLOCK_COLOR)); //403
        this.behaviorMap.put(SPRUCE_TRAPDOOR, BlockBehaviorTrapdoor.factory(BlockColor.SPRUCE_BLOCK_COLOR)); //404
        this.behaviorMap.put(ACACIA_PRESSURE_PLATE, new BlockBehaviorPressurePlateWood());//405
        this.behaviorMap.put(BIRCH_PRESSURE_PLATE, new BlockBehaviorPressurePlateWood());//406
        this.behaviorMap.put(DARK_OAK_PRESSURE_PLATE, new BlockBehaviorPressurePlateWood());//407
        this.behaviorMap.put(JUNGLE_PRESSURE_PLATE, new BlockBehaviorPressurePlateWood());//408
        this.behaviorMap.put(SPRUCE_PRESSURE_PLATE, new BlockBehaviorPressurePlateWood());//409
        //410: carved_pumpkin
        //411: sea_pickle
        //412: conduit
        //413: turtle_egg
        //414: bubble_column
        this.behaviorMap.put(BARRIER, new BlockBehaviorBarrier()); //415
        this.behaviorMap.put(STONE_SLAB3, BlockBehaviorSlab.factory(DOUBLE_STONE_SLAB3, BlockBehaviorSlab.COLORS_3)); //416
        //416: stone_slab3
        //417: bamboo
        //418: bamboo_sapling
        //419: scaffolding
        this.behaviorMap.put(STONE_SLAB4, BlockBehaviorSlab.factory(DOUBLE_STONE_SLAB4, BlockBehaviorSlab.COLORS_4)); //420
        this.behaviorMap.put(DOUBLE_STONE_SLAB3, BlockBehaviorDoubleSlab.factory(STONE_SLAB3, BlockBehaviorSlab.COLORS_3)); //421
        this.behaviorMap.put(DOUBLE_STONE_SLAB4, BlockBehaviorDoubleSlab.factory(DOUBLE_STONE_SLAB4, BlockBehaviorSlab.COLORS_4)); //422
        this.behaviorMap.put(GRANITE_STAIRS, new BlockBehaviorStairsGranite()); //423
        this.behaviorMap.put(DIORITE_STAIRS, new BlockBehaviorStairsDiorite()); //424
        this.behaviorMap.put(ANDESITE_STAIRS, new BlockBehaviorStairsAndesite()); //425
        this.behaviorMap.put(POLISHED_GRANITE_STAIRS, new BlockBehaviorStairsGranite()); //426
        this.behaviorMap.put(POLISHED_DIORITE_STAIRS, new BlockBehaviorStairsDiorite()); //427
        this.behaviorMap.put(POLISHED_ANDESITE_STAIRS, new BlockBehaviorStairsAndesite()); //428
        this.behaviorMap.put(MOSSY_STONE_BRICK_STAIRS, new BlockBehaviorStairsStoneBrick()); //429
        this.behaviorMap.put(SMOOTH_RED_SANDSTONE_STAIRS, new BlockBehaviorStairsSmoothRedSandstone()); //430
        this.behaviorMap.put(SMOOTH_SANDSTONE_STAIRS, new BlockBehaviorStairsSmoothSandstone()); //431
        this.behaviorMap.put(END_BRICK_STAIRS, new BlockBehaviorStairsEndStoneBrick()); //432
        this.behaviorMap.put(MOSSY_COBBLESTONE_STAIRS, new BlockBehaviorStairsCobblestone()); //433
        this.behaviorMap.put(NORMAL_STONE_STAIRS, new BlockBehaviorStairsStone()); //434
        this.behaviorMap.put(SPRUCE_STANDING_SIGN, BlockBehaviorSignPost.factory(SPRUCE_WALL_SIGN, ItemIds.SPRUCE_SIGN)); //435
        this.behaviorMap.put(SPRUCE_WALL_SIGN, BlockBehaviorWallSign.factory(SPRUCE_STANDING_SIGN, ItemIds.SPRUCE_SIGN)); // 436
        this.behaviorMap.put(SMOOTH_STONE, new BlockBehaviorSmoothStone()); // 437
        this.behaviorMap.put(RED_NETHER_BRICK_STAIRS, new BlockBehaviorStairsNetherBrick()); //438
        this.behaviorMap.put(SMOOTH_QUARTZ_STAIRS, new BlockBehaviorStairsQuartz()); //439
        this.behaviorMap.put(BIRCH_STANDING_SIGN, BlockBehaviorSignPost.factory(BIRCH_WALL_SIGN, ItemIds.BIRCH_SIGN)); //440
        this.behaviorMap.put(BIRCH_WALL_SIGN, BlockBehaviorWallSign.factory(BIRCH_STANDING_SIGN, ItemIds.BIRCH_SIGN)); //441
        this.behaviorMap.put(JUNGLE_STANDING_SIGN, BlockBehaviorSignPost.factory(JUNGLE_WALL_SIGN, ItemIds.JUNGLE_SIGN)); //442
        this.behaviorMap.put(JUNGLE_WALL_SIGN, BlockBehaviorWallSign.factory(JUNGLE_STANDING_SIGN, ItemIds.JUNGLE_SIGN)); //443
        this.behaviorMap.put(ACACIA_STANDING_SIGN, BlockBehaviorSignPost.factory(ACACIA_WALL_SIGN, ItemIds.ACACIA_SIGN)); //444
        this.behaviorMap.put(ACACIA_WALL_SIGN, BlockBehaviorWallSign.factory(ACACIA_STANDING_SIGN, ItemIds.ACACIA_SIGN)); //445
        this.behaviorMap.put(DARK_OAK_STANDING_SIGN, BlockBehaviorSignPost.factory(DARK_OAK_WALL_SIGN, ItemIds.DARK_OAK_SIGN)); //446
        this.behaviorMap.put(DARK_OAK_WALL_SIGN, BlockBehaviorWallSign.factory(DARK_OAK_STANDING_SIGN, ItemIds.DARK_OAK_SIGN)); //447
        this.behaviorMap.put(LECTERN, new BlockBehaviorLectern()); //448
        //449: grindstone
        this.behaviorMap.put(BLAST_FURNACE, BlockBehaviorFurnace.factory(BlockEntityTypes.BLAST_FURNACE)); // 450
        //451: stonecutter_block
        this.behaviorMap.put(SMOKER, BlockBehaviorFurnace.factory(BlockEntityTypes.SMOKER)); //452
        this.behaviorMap.put(LIT_SMOKER, BlockBehaviorFurnaceBurning.factory(BlockEntityTypes.SMOKER)); //453
        //454: cartography_table
        //455: fletching_table
        //456: smithing_table
        this.behaviorMap.put(BARREL, new BlockBehaviorBarrel()); // 457
        //458: loom
        //459: bell
        //460: sweet_berry_bush
        //461: lantern
        this.behaviorMap.put(CAMPFIRE, new BlockBehaviorCampfire()); //462
        //463: lava_cauldron
        //464: jigsaw
        this.behaviorMap.put(WOOD, new BlockBehaviorWood()); //465
        //466: composter
        this.behaviorMap.put(LIT_BLAST_FURNACE, BlockBehaviorFurnaceBurning.factory(BlockEntityTypes.BLAST_FURNACE)); //467
        this.behaviorMap.put(LIGHT_BLOCK, new BlockBehaviorLight()); //468
        //469: wither_rose
        //470: stickypistonarmcollision
        //471: bee_nest
        //472: beehive
        //473: honey_block
        this.behaviorMap.put(HONEYCOMB_BLOCK, new BlockHoneycombBlockBehavior()); //474
    }
}
