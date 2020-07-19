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
        this.behaviorMap.put(AIR, BlockBehaviorAir::new); //0
        this.behaviorMap.put(STONE, BlockBehaviorStone::new); //1
        this.behaviorMap.put(GRASS, BlockBehaviorGrass::new); //2
        this.behaviorMap.put(DIRT, BlockBehaviorDirt::new); //3
        this.behaviorMap.put(COBBLESTONE, BlockBehaviorCobblestone::new); //4
        this.behaviorMap.put(PLANKS, BlockBehaviorPlanks::new); //5
        this.behaviorMap.put(SAPLING, BlockBehaviorSapling::new); //6
        this.behaviorMap.put(BEDROCK, BlockBehaviorBedrock::new); //7
        this.behaviorMap.put(FLOWING_WATER, BlockBehaviorWater.factory(WATER)); //8
        this.behaviorMap.put(WATER, BlockBehaviorWaterStill.factory(FLOWING_WATER)); //9
        this.behaviorMap.put(FLOWING_LAVA, BlockBehaviorLava.factory(LAVA)); //10
        this.behaviorMap.put(LAVA, BlockBehaviorLavaStill.factory(FLOWING_LAVA)); //11
        this.behaviorMap.put(SAND, BlockBehaviorSand::new); //12
        this.behaviorMap.put(GRAVEL, BlockBehaviorGravel::new); //13
        this.behaviorMap.put(GOLD_ORE, BlockBehaviorOreGold::new); //14
        this.behaviorMap.put(IRON_ORE, BlockBehaviorOreIron::new); //15
        this.behaviorMap.put(COAL_ORE, BlockBehaviorOreCoal::new); //16
        this.behaviorMap.put(LOG, BlockBehaviorLog::new); //17
        this.behaviorMap.put(LEAVES, BlockBehaviorLeaves::new); //18
        this.behaviorMap.put(SPONGE, BlockBehaviorSponge::new); //19
        this.behaviorMap.put(GLASS, BlockBehaviorGlass::new); //20
        this.behaviorMap.put(LAPIS_ORE, BlockBehaviorOreLapis::new); //21
        this.behaviorMap.put(LAPIS_BLOCK, BlockBehaviorLapis::new); //22
        this.behaviorMap.put(DISPENSER, BlockBehaviorDispenser::new); //23
        this.behaviorMap.put(SANDSTONE, BlockBehaviorSandstone::new); //24
        this.behaviorMap.put(NOTEBLOCK, BlockBehaviorNoteblock::new); //25
        this.behaviorMap.put(BED, BlockBehaviorBed::new); //26
        this.behaviorMap.put(GOLDEN_RAIL, BlockBehaviorRailPowered::new); //27
        this.behaviorMap.put(DETECTOR_RAIL, BlockBehaviorRailDetector::new); //28
        this.behaviorMap.put(STICKY_PISTON, BlockBehaviorPistonSticky::new); //29
        this.behaviorMap.put(WEB, BlockBehaviorCobweb::new); //30
        this.behaviorMap.put(TALL_GRASS, BlockBehaviorTallGrass::new); //31
        this.behaviorMap.put(DEADBUSH, BlockBehaviorDeadBush::new); //32
        this.behaviorMap.put(PISTON, BlockBehaviorPiston::new); //33
        this.behaviorMap.put(PISTON_ARM_COLLISION, BlockBehaviorPistonHead::new); //34
        this.behaviorMap.put(WOOL, BlockBehaviorWool::new); //35
        this.behaviorMap.put(YELLOW_FLOWER, BlockBehaviorDandelion::new); //37
        this.behaviorMap.put(RED_FLOWER, BlockBehaviorFlower::new); //38
        this.behaviorMap.put(BROWN_MUSHROOM, BlockBehaviorMushroomBrown::new); //39
        this.behaviorMap.put(RED_MUSHROOM, BlockBehaviorMushroomRed::new); //40
        this.behaviorMap.put(GOLD_BLOCK, BlockBehaviorGold::new); //41
        this.behaviorMap.put(IRON_BLOCK, BlockBehaviorIron::new); //42
        this.behaviorMap.put(DOUBLE_STONE_SLAB, BlockBehaviorDoubleSlab.factory(STONE_SLAB, BlockBehaviorSlab.COLORS_1)); //43
        this.behaviorMap.put(STONE_SLAB, BlockBehaviorSlab.factory(DOUBLE_STONE_SLAB, BlockBehaviorSlab.COLORS_1)); //44
        this.behaviorMap.put(BRICK_BLOCK, BlockBehaviorBricks::new); //45
        this.behaviorMap.put(TNT, BlockBehaviorTNT::new); //46
        this.behaviorMap.put(BOOKSHELF, BlockBehaviorBookshelf::new); //47
        this.behaviorMap.put(MOSSY_COBBLESTONE, BlockBehaviorMossStone::new); //48
        this.behaviorMap.put(OBSIDIAN, BlockBehaviorObsidian::new); //49
        this.behaviorMap.put(TORCH, BlockBehaviorTorch::new); //50
        this.behaviorMap.put(FIRE, BlockBehaviorFire::new); //51
        this.behaviorMap.put(MOB_SPAWNER, BlockBehaviorMobSpawner::new); //52
        this.behaviorMap.put(OAK_STAIRS, BlockBehaviorStairsWood::new); //53
        this.behaviorMap.put(CHEST, BlockBehaviorChest::new); //54
        this.behaviorMap.put(REDSTONE_WIRE, BlockBehaviorRedstoneWire::new); //55
        this.behaviorMap.put(DIAMOND_ORE, BlockBehaviorOreDiamond::new); //56
        this.behaviorMap.put(DIAMOND_BLOCK, BlockBehaviorDiamond::new); //57
        this.behaviorMap.put(CRAFTING_TABLE, BlockBehaviorCraftingTable::new); //58
        this.behaviorMap.put(WHEAT, BlockBehaviorWheat::new); //59
        this.behaviorMap.put(FARMLAND, BlockBehaviorFarmland::new); //60
        this.behaviorMap.put(FURNACE, BlockBehaviorFurnace.factory(BlockEntityTypes.FURNACE)); //61
        this.behaviorMap.put(LIT_FURNACE, BlockBehaviorFurnaceBurning.factory(BlockEntityTypes.FURNACE)); //62
        this.behaviorMap.put(STANDING_SIGN, BlockBehaviorSignPost.factory(WALL_SIGN, ItemIds.SIGN)); //63
        this.behaviorMap.put(WOODEN_DOOR, BlockBehaviorDoorWood::new); //64
        this.behaviorMap.put(LADDER, BlockBehaviorLadder::new); //65
        this.behaviorMap.put(RAIL, BlockBehaviorRail::new); //66
        this.behaviorMap.put(STONE_STAIRS, BlockBehaviorStairsCobblestone::new); //67
        this.behaviorMap.put(WALL_SIGN, BlockBehaviorWallSign.factory(STANDING_SIGN, ItemIds.SIGN)); //68
        this.behaviorMap.put(LEVER, BlockBehaviorLever::new); //69
        this.behaviorMap.put(STONE_PRESSURE_PLATE, BlockBehaviorPressurePlateStone::new); //70
        this.behaviorMap.put(IRON_DOOR, BlockBehaviorDoorIron::new); //71
        this.behaviorMap.put(WOODEN_PRESSURE_PLATE, BlockBehaviorPressurePlateWood::new); //72
        this.behaviorMap.put(REDSTONE_ORE, BlockBehaviorOreRedstone::new); //73
        this.behaviorMap.put(LIT_REDSTONE_ORE, BlockBehaviorOreRedstoneGlowing::new); //74
        this.behaviorMap.put(UNLIT_REDSTONE_TORCH, BlockBehaviorRedstoneTorchUnlit::new);
        this.behaviorMap.put(REDSTONE_TORCH, BlockBehaviorRedstoneTorch::new); //76
        this.behaviorMap.put(STONE_BUTTON, BlockBehaviorButtonStone::new); //77
        this.behaviorMap.put(SNOW_LAYER, BlockBehaviorSnowLayer::new); //78
        this.behaviorMap.put(ICE, BlockBehaviorIce::new); //79
        this.behaviorMap.put(SNOW, BlockBehaviorSnow::new); //80
        this.behaviorMap.put(CACTUS, BlockBehaviorCactus::new); //81
        this.behaviorMap.put(CLAY, BlockBehaviorClay::new); //82
        this.behaviorMap.put(REEDS, ReedsBlockBehavior::new); //83
        this.behaviorMap.put(JUKEBOX, BlockBehaviorJukebox::new); //84
        this.behaviorMap.put(FENCE, BlockBehaviorFenceWooden::new); //85
        this.behaviorMap.put(PUMPKIN, BlockBehaviorPumpkin::new); //86
        this.behaviorMap.put(NETHERRACK, BlockBehaviorNetherrack::new); //87
        this.behaviorMap.put(SOUL_SAND, BlockBehaviorSoulSand::new); //88
        this.behaviorMap.put(GLOWSTONE, BlockBehaviorGlowstone::new); //89
        this.behaviorMap.put(PORTAL, BlockBehaviorNetherPortal::new); //90
        this.behaviorMap.put(LIT_PUMPKIN, BlockBehaviorPumpkinLit::new); //91
        this.behaviorMap.put(CAKE, BlockBehaviorCake::new); //92
        this.behaviorMap.put(UNPOWERED_REPEATER, BlockBehaviorRedstoneRepeaterUnpowered::new); //93
        this.behaviorMap.put(POWERED_REPEATER, BlockBehaviorRedstoneRepeaterPowered::new); //94
        this.behaviorMap.put(INVISIBLE_BEDROCK, BlockBehaviorBedrockInvisible::new); //95
        this.behaviorMap.put(TRAPDOOR, BlockBehaviorTrapdoor::new); //96
        this.behaviorMap.put(MONSTER_EGG, BlockBehaviorMonsterEgg::new); //97
        this.behaviorMap.put(STONEBRICK, BlockBehaviorBricksStone::new); //98
        this.behaviorMap.put(BROWN_MUSHROOM_BLOCK, BlockBehaviorHugeMushroomBrown::new); //99
        this.behaviorMap.put(RED_MUSHROOM_BLOCK, BlockBehaviorHugeMushroomRed::new); //100
        this.behaviorMap.put(IRON_BARS, BlockBehaviorIronBars::new); //101
        this.behaviorMap.put(GLASS_PANE, BlockBehaviorGlassPane::new); //102
        this.behaviorMap.put(MELON_BLOCK, BlockBehaviorMelon::new); //103
        this.behaviorMap.put(PUMPKIN_STEM, BlockBehaviorStemPumpkin::new); //104
        this.behaviorMap.put(MELON_STEM, BlockBehaviorStemMelon::new); //105
        this.behaviorMap.put(VINE, BlockBehaviorVine::new); //106
        this.behaviorMap.put(FENCE_GATE, BlockBehaviorFenceGate::new); //107
        this.behaviorMap.put(BRICK_STAIRS, BlockBehaviorStairsBrick::new); //108
        this.behaviorMap.put(STONE_BRICK_STAIRS, BlockBehaviorStairsStoneBrick::new); //109
        this.behaviorMap.put(MYCELIUM, BlockBehaviorMycelium::new); //110
        this.behaviorMap.put(WATERLILY, BlockBehaviorWaterLily::new); //111
        this.behaviorMap.put(NETHER_BRICK, BlockBehaviorBricksNether::new); //112
        this.behaviorMap.put(NETHER_BRICK_FENCE, BlockBehaviorFenceNetherBrick::new); //113
        this.behaviorMap.put(NETHER_BRICK_STAIRS, BlockBehaviorStairsNetherBrick::new); //114
        this.behaviorMap.put(NETHER_WART, BlockBehaviorNetherWart::new); //115
        this.behaviorMap.put(ENCHANTING_TABLE, BlockBehaviorEnchantingTable::new); //116
        this.behaviorMap.put(BREWING_STAND, BlockBehaviorBrewingStand::new); //117
        this.behaviorMap.put(CAULDRON, BlockBehaviorCauldron::new); //118
        this.behaviorMap.put(END_PORTAL, BlockBehaviorEndPortal::new); //119
        this.behaviorMap.put(END_PORTAL_FRAME, BlockBehaviorEndPortalFrame::new); //120
        this.behaviorMap.put(END_STONE, BlockBehaviorEndStone::new); //121
        this.behaviorMap.put(DRAGON_EGG, BlockBehaviorDragonEgg::new); //122
        this.behaviorMap.put(REDSTONE_LAMP, BlockBehaviorRedstoneLamp::new); //123
        this.behaviorMap.put(LIT_REDSTONE_LAMP, BlockBehaviorRedstoneLampLit::new); //124
        //TODO: list.put(DROPPER, BlockDropper::new); //125
        this.behaviorMap.put(ACTIVATOR_RAIL, BlockBehaviorRailActivator::new); //126
        this.behaviorMap.put(COCOA, BlockBehaviorCocoa::new); //127
        this.behaviorMap.put(SANDSTONE_STAIRS, BlockBehaviorStairsSandstone::new); //128
        this.behaviorMap.put(EMERALD_ORE, BlockBehaviorOreEmerald::new); //129
        this.behaviorMap.put(ENDER_CHEST, BlockBehaviorEnderChest::new); //130
        this.behaviorMap.put(TRIPWIRE_HOOK, BlockBehaviorTripWireHook::new); //131
        this.behaviorMap.put(TRIPWIRE, BlockBehaviorTripWire::new); //132
        this.behaviorMap.put(EMERALD_BLOCK, BlockBehaviorEmerald::new); //133
        this.behaviorMap.put(SPRUCE_STAIRS, BlockBehaviorStairsWood::new); //134
        this.behaviorMap.put(BIRCH_STAIRS, BlockBehaviorStairsWood::new); //135
        this.behaviorMap.put(JUNGLE_STAIRS, BlockBehaviorStairsWood::new); //136
        //137: impulse_command_block
        this.behaviorMap.put(BEACON, BlockBehaviorBeacon::new); //138
        this.behaviorMap.put(COBBLESTONE_WALL, BlockBehaviorWall::new); //139
        this.behaviorMap.put(FLOWER_POT, BlockBehaviorFlowerPot::new); //140
        this.behaviorMap.put(CARROTS, BlockBehaviorCarrot::new); //141
        this.behaviorMap.put(POTATOES, BlockBehaviorPotato::new); //142
        this.behaviorMap.put(WOODEN_BUTTON, BlockBehaviorButtonWooden::new); //143
        this.behaviorMap.put(SKULL, BlockBehaviorSkull::new); //144
        this.behaviorMap.put(ANVIL, BlockBehaviorAnvil::new); //145
        this.behaviorMap.put(TRAPPED_CHEST, BlockBehaviorTrappedChest::new); //146
        this.behaviorMap.put(LIGHT_WEIGHTED_PRESSURE_PLATE, BlockBehaviorWeightedPressurePlateLight::new); //147
        this.behaviorMap.put(HEAVY_WEIGHTED_PRESSURE_PLATE, BlockBehaviorWeightedPressurePlateHeavy::new); //148
        this.behaviorMap.put(UNPOWERED_COMPARATOR, BlockBehaviorRedstoneComparatorUnpowered::new); //149
        this.behaviorMap.put(POWERED_COMPARATOR, BlockBehaviorRedstoneComparatorPowered::new); //150
        this.behaviorMap.put(DAYLIGHT_DETECTOR, BlockBehaviorDaylightDetector::new); //151
        this.behaviorMap.put(REDSTONE_BLOCK, BlockBehaviorRedstone::new); //152
        this.behaviorMap.put(QUARTZ_ORE, BlockBehaviorOreQuartz::new); //153
        this.behaviorMap.put(HOPPER, BlockBehaviorHopper::new); //154
        this.behaviorMap.put(QUARTZ_BLOCK, BlockBehaviorQuartz::new); //155
        this.behaviorMap.put(QUARTZ_STAIRS, BlockBehaviorStairsQuartz::new); //156
        this.behaviorMap.put(DOUBLE_WOODEN_SLAB, BlockBehaviorDoubleSlabWood::new); //157
        this.behaviorMap.put(WOODEN_SLAB, BlockBehaviorSlabWood::new); //158
        this.behaviorMap.put(STAINED_HARDENED_CLAY, BlockBehaviorTerracottaStained::new); //159
        this.behaviorMap.put(STAINED_GLASS_PANE, BlockBehaviorGlassPaneStained::new); //160
        this.behaviorMap.put(LEAVES2, BlockBehaviorLeaves2::new); //161
        this.behaviorMap.put(LOG2, BlockBehaviorLog2::new); //162
        this.behaviorMap.put(ACACIA_STAIRS, BlockBehaviorStairsWood::new); //163
        this.behaviorMap.put(DARK_OAK_STAIRS, BlockBehaviorStairsWood::new); //164
        this.behaviorMap.put(SLIME, BlockBehaviorSlime::new); //165
        //166: glow_stick
        this.behaviorMap.put(IRON_TRAPDOOR, BlockBehaviorTrapdoorIron::new); //167
        this.behaviorMap.put(PRISMARINE, BlockBehaviorPrismarine::new); //168
        this.behaviorMap.put(SEA_LANTERN, BlockBehaviorSeaLantern::new); //169
        this.behaviorMap.put(HAY_BLOCK, BlockBehaviorHayBale::new); //170
        this.behaviorMap.put(CARPET, BlockBehaviorCarpet::new); //171
        this.behaviorMap.put(HARDENED_CLAY, BlockBehaviorTerracotta::new); //172
        this.behaviorMap.put(COAL_BLOCK, BlockBehaviorCoal::new); //173
        this.behaviorMap.put(PACKED_ICE, BlockBehaviorIcePacked::new); //174
        this.behaviorMap.put(DOUBLE_PLANT, BlockBehaviorDoublePlant::new); //175
        this.behaviorMap.put(STANDING_BANNER, BlockBehaviorBanner::new); //176
        this.behaviorMap.put(WALL_BANNER, BlockBehaviorWallBanner::new); //177
        this.behaviorMap.put(DAYLIGHT_DETECTOR_INVERTED, BlockBehaviorDaylightDetectorInverted::new); //178
        this.behaviorMap.put(RED_SANDSTONE, BlockBehaviorRedSandstone::new); //179
        this.behaviorMap.put(RED_SANDSTONE_STAIRS, BlockBehaviorStairsRedSandstone::new); //180
        this.behaviorMap.put(DOUBLE_STONE_SLAB2, BlockBehaviorDoubleSlab.factory(STONE_SLAB2, BlockBehaviorSlab.COLORS_2)); //181
        this.behaviorMap.put(STONE_SLAB2, BlockBehaviorSlab.factory(DOUBLE_STONE_SLAB2, BlockBehaviorSlab.COLORS_2)); //182
        this.behaviorMap.put(SPRUCE_FENCE_GATE, BlockBehaviorFenceGate::new); //183
        this.behaviorMap.put(BIRCH_FENCE_GATE, BlockBehaviorFenceGate::new); //184
        this.behaviorMap.put(JUNGLE_FENCE_GATE, BlockBehaviorFenceGate::new); //185
        this.behaviorMap.put(DARK_OAK_FENCE_GATE, BlockBehaviorFenceGate::new); //186
        this.behaviorMap.put(ACACIA_FENCE_GATE, BlockBehaviorFenceGate::new); //187
        //188: repeating_command_block
        //189: chain_command_block
        //190: hard_glass_pane
        //191: hard_stained_glass_pane
        //192: chemical_heat
        this.behaviorMap.put(SPRUCE_DOOR, BlockBehaviorDoorWood::new); //193
        this.behaviorMap.put(BIRCH_DOOR, BlockBehaviorDoorWood::new); //194
        this.behaviorMap.put(JUNGLE_DOOR, BlockBehaviorDoorWood::new); //195
        this.behaviorMap.put(ACACIA_DOOR, BlockBehaviorDoorWood::new); //196
        this.behaviorMap.put(DARK_OAK_DOOR, BlockBehaviorDoorWood::new); //197
        this.behaviorMap.put(GRASS_PATH, BlockBehaviorGrassPath::new); //198
        this.behaviorMap.put(FRAME, BlockBehaviorItemFrame::new); //199
        this.behaviorMap.put(CHORUS_FLOWER, BlockBehaviorChorusFlower::new); //200
        this.behaviorMap.put(PURPUR_BLOCK, BlockBehaviorPurpur::new); //201
        //202: chorus_flower
        this.behaviorMap.put(PURPUR_STAIRS, BlockBehaviorStairsPurpur::new); //203
        //204: colored_torch_bp
        this.behaviorMap.put(UNDYED_SHULKER_BOX, BlockBehaviorUndyedShulkerBox::new); //205
        this.behaviorMap.put(END_BRICKS, BlockBehaviorBricksEndStone::new); //206
        //207: frosted_ice
        this.behaviorMap.put(END_ROD, BlockBehaviorEndRod::new); //208
        this.behaviorMap.put(END_GATEWAY, BlockBehaviorEndGateway::new); //209
        //210: allow
        //211: deny
        //212: border
        this.behaviorMap.put(MAGMA, BlockBehaviorMagma::new); //213
        this.behaviorMap.put(NETHER_WART_BLOCK, BlockNetherWartBlockBehavior::new); //214
        this.behaviorMap.put(RED_NETHER_BRICK, BlockBehaviorBricksRedNether::new); //215
        this.behaviorMap.put(BONE_BLOCK, BlockBehaviorBone::new); //216
        //217: structure_void
        this.behaviorMap.put(SHULKER_BOX, BlockBehaviorShulkerBox::new); //218
        this.behaviorMap.put(PURPLE_GLAZED_TERRACOTTA, BlockBehaviorTerracottaGlazed::new); //219
        this.behaviorMap.put(WHITE_GLAZED_TERRACOTTA, BlockBehaviorTerracottaGlazed::new); //220
        this.behaviorMap.put(ORANGE_GLAZED_TERRACOTTA, BlockBehaviorTerracottaGlazed::new); //221
        this.behaviorMap.put(MAGENTA_GLAZED_TERRACOTTA, BlockBehaviorTerracottaGlazed::new); //222
        this.behaviorMap.put(LIGHT_BLUE_GLAZED_TERRACOTTA, BlockBehaviorTerracottaGlazed::new); //223
        this.behaviorMap.put(YELLOW_GLAZED_TERRACOTTA, BlockBehaviorTerracottaGlazed::new); //224
        this.behaviorMap.put(LIME_GLAZED_TERRACOTTA, BlockBehaviorTerracottaGlazed::new); //225
        this.behaviorMap.put(PINK_GLAZED_TERRACOTTA, BlockBehaviorTerracottaGlazed::new); //226
        this.behaviorMap.put(GRAY_GLAZED_TERRACOTTA, BlockBehaviorTerracottaGlazed::new); //227
        this.behaviorMap.put(SILVER_GLAZED_TERRACOTTA, BlockBehaviorTerracottaGlazed::new); //228
        this.behaviorMap.put(CYAN_GLAZED_TERRACOTTA, BlockBehaviorTerracottaGlazed::new); //229
        //230: chalkboard
        this.behaviorMap.put(BLUE_GLAZED_TERRACOTTA, BlockBehaviorTerracottaGlazed::new); //231
        this.behaviorMap.put(BROWN_GLAZED_TERRACOTTA, BlockBehaviorTerracottaGlazed::new); //232
        this.behaviorMap.put(GREEN_GLAZED_TERRACOTTA, BlockBehaviorTerracottaGlazed::new); //233
        this.behaviorMap.put(RED_GLAZED_TERRACOTTA, BlockBehaviorTerracottaGlazed::new); //234
        this.behaviorMap.put(BLACK_GLAZED_TERRACOTTA, BlockBehaviorTerracottaGlazed::new); //235
        this.behaviorMap.put(CONCRETE, BlockBehaviorConcrete::new); //236
        this.behaviorMap.put(CONCRETE_POWDER, BlockBehaviorConcretePowder::new); //237
        //238: chemistry_table
        //239: underwater_torch
        this.behaviorMap.put(CHORUS_PLANT, BlockBehaviorChorusPlant::new); //240
        this.behaviorMap.put(STAINED_GLASS, BlockBehaviorGlassStained::new); //241
        //242: camera
        this.behaviorMap.put(PODZOL, BlockBehaviorPodzol::new); //243
        this.behaviorMap.put(BEETROOT, BlockBehaviorBeetroot::new); //244
        this.behaviorMap.put(STONECUTTER, BlockBehaviorStonecutter::new); //245
        this.behaviorMap.put(GLOWING_OBSIDIAN, BlockBehaviorObsidianGlowing::new); //246
        //list.put(NETHER_REACTOR, BlockNetherReactor::new); //247 Should not be removed
        //248: info_update
        //249: info_update2
        //TODO: list.put(PISTON_EXTENSION, BlockPistonExtension::new); //250
        this.behaviorMap.put(OBSERVER, BlockBehaviorObserver::new); //251
        //252: structure_block
        //253: hard_glass
        //254: hard_stained_glass
        //255: reserved6
        //256: unknown
        this.behaviorMap.put(PRISMARINE_STAIRS, BlockBehaviorStairsPrismarine::new); //257
        this.behaviorMap.put(DARK_PRISMARINE_STAIRS, BlockBehaviorStairsDarkPrismarine::new); //258
        this.behaviorMap.put(PRISMARINE_BRICKS_STAIRS, BlockBehaviorStairsPrismarineBricks::new); //259
        this.behaviorMap.put(STRIPPED_SPRUCE_LOG, BlockBehaviorStrippedLog::new); //260
        this.behaviorMap.put(STRIPPED_BIRCH_LOG, BlockBehaviorStrippedLog::new); //261
        this.behaviorMap.put(STRIPPED_JUNGLE_LOG, BlockBehaviorStrippedLog::new); //262
        this.behaviorMap.put(STRIPPED_ACACIA_LOG, BlockBehaviorStrippedLog::new); //263
        this.behaviorMap.put(STRIPPED_DARK_OAK_LOG, BlockBehaviorStrippedLog::new); //264
        this.behaviorMap.put(STRIPPED_OAK_LOG, BlockBehaviorStrippedLog::new); //265
        this.behaviorMap.put(BLUE_ICE, BlockBehaviorBlueIce::new); //266
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
        this.behaviorMap.put(KELP, BlockBehaviorKelp::new);//393
        this.behaviorMap.put(DRIED_KELP_BLOCK, BlockBehaviorDriedKelp::new); //394
        this.behaviorMap.put(ACACIA_BUTTON, BlockBehaviorButtonWooden::new);//395
        this.behaviorMap.put(BIRCH_BUTTON, BlockBehaviorButtonWooden::new);//396
        this.behaviorMap.put(DARK_OAK_BUTTON, BlockBehaviorButtonWooden::new);//397
        this.behaviorMap.put(JUNGLE_BUTTON, BlockBehaviorButtonWooden::new);//398
        this.behaviorMap.put(SPRUCE_BUTTON, BlockBehaviorButtonWooden::new);//399
        this.behaviorMap.put(ACACIA_TRAPDOOR, BlockBehaviorTrapdoor.factory(BlockColor.ORANGE_BLOCK_COLOR)); //400
        this.behaviorMap.put(BIRCH_TRAPDOOR, BlockBehaviorTrapdoor.factory(BlockColor.SAND_BLOCK_COLOR)); //401
        this.behaviorMap.put(DARK_OAK_TRAPDOOR, BlockBehaviorTrapdoor.factory(BlockColor.BROWN_BLOCK_COLOR)); //402
        this.behaviorMap.put(JUNGLE_TRAPDOOR, BlockBehaviorTrapdoor.factory(BlockColor.DIRT_BLOCK_COLOR)); //403
        this.behaviorMap.put(SPRUCE_TRAPDOOR, BlockBehaviorTrapdoor.factory(BlockColor.SPRUCE_BLOCK_COLOR)); //404
        this.behaviorMap.put(ACACIA_PRESSURE_PLATE, BlockBehaviorPressurePlateWood::new);//405
        this.behaviorMap.put(BIRCH_PRESSURE_PLATE, BlockBehaviorPressurePlateWood::new);//406
        this.behaviorMap.put(DARK_OAK_PRESSURE_PLATE, BlockBehaviorPressurePlateWood::new);//407
        this.behaviorMap.put(JUNGLE_PRESSURE_PLATE, BlockBehaviorPressurePlateWood::new);//408
        this.behaviorMap.put(SPRUCE_PRESSURE_PLATE, BlockBehaviorPressurePlateWood::new);//409
        //410: carved_pumpkin
        //411: sea_pickle
        //412: conduit
        //413: turtle_egg
        //414: bubble_column
        this.behaviorMap.put(BARRIER, BlockBehaviorBarrier::new); //415
        this.behaviorMap.put(STONE_SLAB3, BlockBehaviorSlab.factory(DOUBLE_STONE_SLAB3, BlockBehaviorSlab.COLORS_3)); //416
        //416: stone_slab3
        //417: bamboo
        //418: bamboo_sapling
        //419: scaffolding
        this.behaviorMap.put(STONE_SLAB4, BlockBehaviorSlab.factory(DOUBLE_STONE_SLAB4, BlockBehaviorSlab.COLORS_4)); //420
        this.behaviorMap.put(DOUBLE_STONE_SLAB3, BlockBehaviorDoubleSlab.factory(STONE_SLAB3, BlockBehaviorSlab.COLORS_3)); //421
        this.behaviorMap.put(DOUBLE_STONE_SLAB4, BlockBehaviorDoubleSlab.factory(DOUBLE_STONE_SLAB4, BlockBehaviorSlab.COLORS_4)); //422
        this.behaviorMap.put(GRANITE_STAIRS, BlockBehaviorStairsGranite::new); //423
        this.behaviorMap.put(DIORITE_STAIRS, BlockBehaviorStairsDiorite::new); //424
        this.behaviorMap.put(ANDESITE_STAIRS, BlockBehaviorStairsAndesite::new); //425
        this.behaviorMap.put(POLISHED_GRANITE_STAIRS, BlockBehaviorStairsGranite::new); //426
        this.behaviorMap.put(POLISHED_DIORITE_STAIRS, BlockBehaviorStairsDiorite::new); //427
        this.behaviorMap.put(POLISHED_ANDESITE_STAIRS, BlockBehaviorStairsAndesite::new); //428
        this.behaviorMap.put(MOSSY_STONE_BRICK_STAIRS, BlockBehaviorStairsStoneBrick::new); //429
        this.behaviorMap.put(SMOOTH_RED_SANDSTONE_STAIRS, BlockBehaviorStairsSmoothRedSandstone::new); //430
        this.behaviorMap.put(SMOOTH_SANDSTONE_STAIRS, BlockBehaviorStairsSmoothSandstone::new); //431
        this.behaviorMap.put(END_BRICK_STAIRS, BlockBehaviorStairsEndStoneBrick::new); //432
        this.behaviorMap.put(MOSSY_COBBLESTONE_STAIRS, BlockBehaviorStairsCobblestone::new); //433
        this.behaviorMap.put(NORMAL_STONE_STAIRS, BlockBehaviorStairsStone::new); //434
        this.behaviorMap.put(SPRUCE_STANDING_SIGN, BlockBehaviorSignPost.factory(SPRUCE_WALL_SIGN, ItemIds.SPRUCE_SIGN)); //435
        this.behaviorMap.put(SPRUCE_WALL_SIGN, BlockBehaviorWallSign.factory(SPRUCE_STANDING_SIGN, ItemIds.SPRUCE_SIGN)); // 436
        this.behaviorMap.put(SMOOTH_STONE, BlockBehaviorSmoothStone::new); // 437
        this.behaviorMap.put(RED_NETHER_BRICK_STAIRS, BlockBehaviorStairsNetherBrick::new); //438
        this.behaviorMap.put(SMOOTH_QUARTZ_STAIRS, BlockBehaviorStairsQuartz::new); //439
        this.behaviorMap.put(BIRCH_STANDING_SIGN, BlockBehaviorSignPost.factory(BIRCH_WALL_SIGN, ItemIds.BIRCH_SIGN)); //440
        this.behaviorMap.put(BIRCH_WALL_SIGN, BlockBehaviorWallSign.factory(BIRCH_STANDING_SIGN, ItemIds.BIRCH_SIGN)); //441
        this.behaviorMap.put(JUNGLE_STANDING_SIGN, BlockBehaviorSignPost.factory(JUNGLE_WALL_SIGN, ItemIds.JUNGLE_SIGN)); //442
        this.behaviorMap.put(JUNGLE_WALL_SIGN, BlockBehaviorWallSign.factory(JUNGLE_STANDING_SIGN, ItemIds.JUNGLE_SIGN)); //443
        this.behaviorMap.put(ACACIA_STANDING_SIGN, BlockBehaviorSignPost.factory(ACACIA_WALL_SIGN, ItemIds.ACACIA_SIGN)); //444
        this.behaviorMap.put(ACACIA_WALL_SIGN, BlockBehaviorWallSign.factory(ACACIA_STANDING_SIGN, ItemIds.ACACIA_SIGN)); //445
        this.behaviorMap.put(DARK_OAK_STANDING_SIGN, BlockBehaviorSignPost.factory(DARK_OAK_WALL_SIGN, ItemIds.DARK_OAK_SIGN)); //446
        this.behaviorMap.put(DARK_OAK_WALL_SIGN, BlockBehaviorWallSign.factory(DARK_OAK_STANDING_SIGN, ItemIds.DARK_OAK_SIGN)); //447
        this.behaviorMap.put(LECTERN, BlockBehaviorLectern::new); //448
        //449: grindstone
        this.behaviorMap.put(BLAST_FURNACE, BlockBehaviorFurnace.factory(BlockEntityTypes.BLAST_FURNACE)); // 450
        //451: stonecutter_block
        this.behaviorMap.put(SMOKER, BlockBehaviorFurnace.factory(BlockEntityTypes.SMOKER)); //452
        this.behaviorMap.put(LIT_SMOKER, BlockBehaviorFurnaceBurning.factory(BlockEntityTypes.SMOKER)); //453
        //454: cartography_table
        //455: fletching_table
        //456: smithing_table
        this.behaviorMap.put(BARREL, BlockBehaviorBarrel::new); // 457
        //458: loom
        //459: bell
        //460: sweet_berry_bush
        //461: lantern
        this.behaviorMap.put(CAMPFIRE, BlockBehaviorCampfire::new); //462
        //463: lava_cauldron
        //464: jigsaw
        this.behaviorMap.put(WOOD, BlockBehaviorWood::new); //465
        //466: composter
        this.behaviorMap.put(LIT_BLAST_FURNACE, BlockBehaviorFurnaceBurning.factory(BlockEntityTypes.BLAST_FURNACE)); //467
        this.behaviorMap.put(LIGHT_BLOCK, BlockBehaviorLight::new); //468
        //469: wither_rose
        //470: stickypistonarmcollision
        //471: bee_nest
        //472: beehive
        //473: honey_block
        this.behaviorMap.put(HONEYCOMB_BLOCK, BlockHoneycombBlockBehavior::new); //474
    }
}
