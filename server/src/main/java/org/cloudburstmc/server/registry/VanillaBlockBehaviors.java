package org.cloudburstmc.server.registry;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.BlockLootHandler;
import org.cloudburstmc.api.block.component.SurviveBlockHandler;
import org.cloudburstmc.api.block.component.UseBlockHandler;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.sound.SoundType;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.component.ComponentBuilder;
import org.cloudburstmc.server.block.component.*;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;

import java.util.List;
import java.util.Map;

import static org.cloudburstmc.api.block.BlockTypes.*;

/**
 * Configures server behavior for vanilla block types.
 */
@UtilityClass
public class VanillaBlockBehaviors {

    private static final List<BlockType> DOUBLE_PLANTS = List.of(
            LARGE_FERN, LILAC, PEONY, PITCHER_PLANT, ROSE_BUSH, SUNFLOWER, TALL_GRASS
    );

    private static final int MAXIMUM_FALL_DAMAGE = 40;

    private static final List<CoralFamily> CORALS = List.of(
            new CoralFamily(BRAIN_CORAL_BLOCK, DEAD_BRAIN_CORAL_BLOCK, BRAIN_CORAL, DEAD_BRAIN_CORAL,
                    BRAIN_CORAL_FAN, DEAD_BRAIN_CORAL_FAN, BRAIN_CORAL_WALL_FAN, DEAD_BRAIN_CORAL_WALL_FAN),
            new CoralFamily(BUBBLE_CORAL_BLOCK, DEAD_BUBBLE_CORAL_BLOCK, BUBBLE_CORAL, DEAD_BUBBLE_CORAL,
                    BUBBLE_CORAL_FAN, DEAD_BUBBLE_CORAL_FAN, BUBBLE_CORAL_WALL_FAN, DEAD_BUBBLE_CORAL_WALL_FAN),
            new CoralFamily(FIRE_CORAL_BLOCK, DEAD_FIRE_CORAL_BLOCK, FIRE_CORAL, DEAD_FIRE_CORAL,
                    FIRE_CORAL_FAN, DEAD_FIRE_CORAL_FAN, FIRE_CORAL_WALL_FAN, DEAD_FIRE_CORAL_WALL_FAN),
            new CoralFamily(HORN_CORAL_BLOCK, DEAD_HORN_CORAL_BLOCK, HORN_CORAL, DEAD_HORN_CORAL,
                    HORN_CORAL_FAN, DEAD_HORN_CORAL_FAN, HORN_CORAL_WALL_FAN, DEAD_HORN_CORAL_WALL_FAN),
            new CoralFamily(TUBE_CORAL_BLOCK, DEAD_TUBE_CORAL_BLOCK, TUBE_CORAL, DEAD_TUBE_CORAL,
                    TUBE_CORAL_FAN, DEAD_TUBE_CORAL_FAN, TUBE_CORAL_WALL_FAN, DEAD_TUBE_CORAL_WALL_FAN)
    );

    private static final Map<ItemType, BlockType> CANDLE_CAKES = Map.ofEntries(
            Map.entry(ItemTypes.CANDLE, CANDLE_CAKE),
            Map.entry(ItemTypes.BLACK_CANDLE, BLACK_CANDLE_CAKE),
            Map.entry(ItemTypes.BLUE_CANDLE, BLUE_CANDLE_CAKE),
            Map.entry(ItemTypes.BROWN_CANDLE, BROWN_CANDLE_CAKE),
            Map.entry(ItemTypes.CYAN_CANDLE, CYAN_CANDLE_CAKE),
            Map.entry(ItemTypes.GRAY_CANDLE, GRAY_CANDLE_CAKE),
            Map.entry(ItemTypes.GREEN_CANDLE, GREEN_CANDLE_CAKE),
            Map.entry(ItemTypes.LIGHT_BLUE_CANDLE, LIGHT_BLUE_CANDLE_CAKE),
            Map.entry(ItemTypes.LIGHT_GRAY_CANDLE, LIGHT_GRAY_CANDLE_CAKE),
            Map.entry(ItemTypes.LIME_CANDLE, LIME_CANDLE_CAKE),
            Map.entry(ItemTypes.MAGENTA_CANDLE, MAGENTA_CANDLE_CAKE),
            Map.entry(ItemTypes.ORANGE_CANDLE, ORANGE_CANDLE_CAKE),
            Map.entry(ItemTypes.PINK_CANDLE, PINK_CANDLE_CAKE),
            Map.entry(ItemTypes.PURPLE_CANDLE, PURPLE_CANDLE_CAKE),
            Map.entry(ItemTypes.RED_CANDLE, RED_CANDLE_CAKE),
            Map.entry(ItemTypes.WHITE_CANDLE, WHITE_CANDLE_CAKE),
            Map.entry(ItemTypes.YELLOW_CANDLE, YELLOW_CANDLE_CAKE)
    );

    private static final List<BlockType> NO_LOOT_BLOCKS = List.of(
            AIR, BARRIER, BEDROCK, BUDDING_AMETHYST, CAKE, CHAIN_COMMAND_BLOCK,
            COMMAND_BLOCK, END_GATEWAY, END_PORTAL, END_PORTAL_FRAME, FIRE, FROG_SPAWN,
            FROSTED_ICE, INVISIBLE_BEDROCK, JIGSAW,
            LIGHT_BLOCK_0, LIGHT_BLOCK_1, LIGHT_BLOCK_2, LIGHT_BLOCK_3,
            LIGHT_BLOCK_4, LIGHT_BLOCK_5, LIGHT_BLOCK_6, LIGHT_BLOCK_7,
            LIGHT_BLOCK_8, LIGHT_BLOCK_9, LIGHT_BLOCK_10, LIGHT_BLOCK_11,
            LIGHT_BLOCK_12, LIGHT_BLOCK_13, LIGHT_BLOCK_14, LIGHT_BLOCK_15,
            MOB_SPAWNER, MOVING_BLOCK, PISTON_ARM_COLLISION, PORTAL, POWDER_SNOW,
            REINFORCED_DEEPSLATE, REPEATING_COMMAND_BLOCK, SOUL_FIRE,
            STICKY_PISTON_ARM_COLLISION, STRUCTURE_BLOCK, STRUCTURE_VOID,
            SUSPICIOUS_GRAVEL, SUSPICIOUS_SAND, TRIAL_SPAWNER, VAULT
    );

    private static final List<BlockType> RENEWABLE_TALL_FLOWERS = List.of(
            LILAC, PEONY, ROSE_BUSH, SUNFLOWER
    );

    private static final List<BlockType> SINGLE_PLANTS = List.of(
            ALLIUM, AZURE_BLUET, BLUE_ORCHID, CLOSED_EYEBLOSSOM, CORNFLOWER, DANDELION,
            FERN, GOLDEN_DANDELION, LILY_OF_THE_VALLEY, OPEN_EYEBLOSSOM, ORANGE_TULIP,
            OXEYE_DAISY, PINK_TULIP, POPPY, RED_SHRUB, RED_TULIP, SHORT_DRY_GRASS, SHORT_GRASS,
            SWEET_BERRY_BUSH, TORCHFLOWER, WHITE_TULIP
    );

    public static void configure(CloudBlockRegistry registry) {
        configureFenceGates(registry);
        configureFences(registry);
        configureBars(registry);
        configureColoredBuildingBlockSlabs(registry);
        configureAquaticBlocks(registry);
        configureStairs(registry);
        configureVegetation(registry);
        configureWalls(registry);
        configureWoodenButton(registry, ACACIA_BUTTON);
        configureDoor(registry, ACACIA_DOOR);
        configureHangingSign(registry, ACACIA_HANGING_SIGN);
        configureLeaves(registry, ACACIA_LEAVES, VanillaBlockLoot.leaves(ACACIA_LEAVES, ACACIA_SAPLING, false));
        configureSlab(registry, ACACIA_SLAB, ACACIA_DOUBLE_SLAB);
        configureTrapdoor(registry, ACACIA_TRAPDOOR);
        configurePoweredRail(registry, ACTIVATOR_RAIL);
        configureSlab(registry, ANDESITE_SLAB, ANDESITE_DOUBLE_SLAB);
        configureAnvil(registry, ANVIL);
        configureLeaves(registry, AZALEA_LEAVES, VanillaBlockLoot.leaves(AZALEA_LEAVES, AZALEA, false));
        configureLeaves(registry, AZALEA_LEAVES_FLOWERED, VanillaBlockLoot.leaves(AZALEA_LEAVES_FLOWERED, FLOWERING_AZALEA, false));
        configureWoodenButton(registry, BAMBOO_BUTTON);
        configureDoor(registry, BAMBOO_DOOR);
        configureHangingSign(registry, BAMBOO_HANGING_SIGN);
        configureSlab(registry, BAMBOO_MOSAIC_SLAB, BAMBOO_MOSAIC_DOUBLE_SLAB);
        configureSlab(registry, BAMBOO_SLAB, BAMBOO_DOUBLE_SLAB);
        configureTrapdoor(registry, BAMBOO_TRAPDOOR);
        configureUsable(registry, BARREL, ContainerBlockHandlers.BARREL);
        configureUsable(registry, BEACON, ContainerBlockHandlers.BEACON);
        configureBed(registry, BED);
        registry.configure(BIG_DRIPLEAF)
                .set(BlockComponents.RESOLVE_PLACEMENT_STATE, BigDripleafBlockHandlers.RESOLVE_PLACEMENT_STATE)
                .set(BlockComponents.CAN_SURVIVE, BigDripleafBlockHandlers.CAN_SURVIVE)
                .set(BlockComponents.ON_STATE_CHANGED, BigDripleafBlockHandlers.ON_STATE_CHANGED)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, BigDripleafBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_ENTITY_INSIDE, BigDripleafBlockHandlers.ON_ENTITY_INSIDE)
                .set(BlockComponents.ON_PROJECTILE_HIT, BigDripleafBlockHandlers.ON_PROJECTILE_HIT)
                .set(BlockComponents.ON_TICK, BigDripleafBlockHandlers.ON_TICK)
                .set(BlockComponents.FERTILIZE, BigDripleafBlockHandlers.FERTILIZE)
                .set(BlockComponents.GET_LOOT, BigDripleafBlockHandlers.GET_LOOT)
                .set(BlockComponents.GET_PICK_BLOCK, BigDripleafBlockHandlers.GET_PICK_BLOCK);
        configureWoodenButton(registry, BIRCH_BUTTON);
        configureDoor(registry, BIRCH_DOOR);
        configureHangingSign(registry, BIRCH_HANGING_SIGN);
        configureLeaves(registry, BIRCH_LEAVES, VanillaBlockLoot.leaves(BIRCH_LEAVES, BIRCH_SAPLING, false));
        configureSlab(registry, BIRCH_SLAB, BIRCH_DOUBLE_SLAB);
        configureTrapdoor(registry, BIRCH_TRAPDOOR);
        configureConcretePowder(registry, BLACK_CONCRETE_POWDER, BLACK_CONCRETE);
        configureShulkerBox(registry, BLACK_SHULKER_BOX);
        configureSlab(registry, BLACKSTONE_SLAB, BLACKSTONE_DOUBLE_SLAB);
        configureUsable(registry, BLAST_FURNACE, ContainerBlockHandlers.BLAST_FURNACE);
        configureConcretePowder(registry, BLUE_CONCRETE_POWDER, BLUE_CONCRETE);
        registry.configure(BLUE_ICE).set(BlockComponents.GET_LOOT, VanillaBlockLoot.silkTouchOnly(BLUE_ICE));
        configureShulkerBox(registry, BLUE_SHULKER_BOX);
        configureUsable(registry, BREWING_STAND, ContainerBlockHandlers.BREWING_STAND);
        configureSlab(registry, BRICK_SLAB, BRICK_DOUBLE_SLAB);
        configureConcretePowder(registry, BROWN_CONCRETE_POWDER, BROWN_CONCRETE);
        configureShulkerBox(registry, BROWN_SHULKER_BOX);
        registry.configure(BUBBLE_COLUMN)
                .set(BlockComponents.CAN_BE_REPLACED, (block, replacement, player, face, click) -> true)
                .set(BlockComponents.ON_STATE_CHANGED, BubbleColumnBlockHandlers::schedule)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, (block, neighbor) -> BubbleColumnBlockHandlers.schedule(block))
                .set(BlockComponents.ON_TICK, (block, random) -> BubbleColumnBlockHandlers.update(block))
                .set(BlockComponents.ON_ENTITY_INSIDE, BubbleColumnBlockHandlers.ON_ENTITY_INSIDE);
        registry.configure(BUSH).set(BlockComponents.GET_LOOT, VanillaBlockLoot.shearsOrSilkTouchOnly(BUSH));
        registry.configure(CACTUS).set(BlockComponents.ON_ENTITY_INSIDE, DefaultBlockHandlers.CACTUS_ENTITY_INSIDE);
        configureUsable(registry, CAKE, CakeBlockHandlers.cake(CANDLE_CAKES));
        configureCakeSupport(registry, CAKE);
        CANDLE_CAKES.forEach((candle, cake) -> configureCandleCake(registry, cake, candle));
        registry.configure(CAMPFIRE).set(BlockComponents.GET_IGNITED_STATE,
                BlockIgnitionHandlers.litState(BlockTraits.IS_EXTINGUISHED, false));
        configureUsable(registry, CARTOGRAPHY_TABLE, ContainerBlockHandlers.CARTOGRAPHY_TABLE);
        registry.configure(CARVED_PUMPKIN).set(BlockComponents.ON_PLACE, new CarvedPumpkinPlaceHandler());
        registry.configure(CAULDRON)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, CauldronBlockHandlers.USE)
                .set(BlockComponents.ON_STATE_CHANGED, CauldronBlockHandlers.ON_STATE_CHANGED)
                .set(BlockComponents.ON_TICK, CauldronBlockHandlers.ON_TICK)
                .set(BlockComponents.ON_ENTITY_INSIDE, CauldronBlockHandlers.ON_ENTITY_INSIDE);
        configureWoodenButton(registry, CHERRY_BUTTON);
        configureDoor(registry, CHERRY_DOOR);
        configureHangingSign(registry, CHERRY_HANGING_SIGN);
        configureLeaves(registry, CHERRY_LEAVES, VanillaBlockLoot.leaves(CHERRY_LEAVES, CHERRY_SAPLING, false));
        configureSlab(registry, CHERRY_SLAB, CHERRY_DOUBLE_SLAB);
        configureTrapdoor(registry, CHERRY_TRAPDOOR);
        configureUsable(registry, CHEST, ContainerBlockHandlers.CHEST);
        configureAnvil(registry, CHIPPED_ANVIL);
        registry.configure(CHORUS_FLOWER)
                .set(BlockComponents.CAN_RANDOM_TICK, true)
                .set(BlockComponents.CAN_SURVIVE, ChorusBlockHandlers.FLOWER_CAN_SURVIVE)
                .set(BlockComponents.GET_BLOCK_SUPPORT_SHAPE, DefaultBlockHandlers.CHORUS_FLOWER_BLOCK_SUPPORT_SHAPE)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, ChorusBlockHandlers.FLOWER_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_PROJECTILE_HIT, ChorusBlockHandlers.FLOWER_PROJECTILE_HIT)
                .set(BlockComponents.ON_RANDOM_TICK, ChorusBlockHandlers.FLOWER_RANDOM_TICK)
                .set(BlockComponents.ON_TICK, ChorusBlockHandlers.ON_TICK);
        registry.configure(CHORUS_PLANT)
                .set(BlockComponents.RESOLVE_PLACEMENT_STATE, ChorusBlockHandlers.PLANT_PLACEMENT)
                .set(BlockComponents.ON_ADDED, ChorusBlockHandlers.PLANT_ADDED)
                .set(BlockComponents.CAN_SURVIVE, ChorusBlockHandlers.PLANT_CAN_SURVIVE)
                .set(BlockComponents.GET_LOOT, ChorusBlockHandlers.PLANT_LOOT)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, ChorusBlockHandlers.PLANT_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_TICK, ChorusBlockHandlers.ON_TICK);
        configureSlab(registry, CINNABAR_BRICK_SLAB, CINNABAR_BRICK_DOUBLE_SLAB);
        configureSlab(registry, CINNABAR_SLAB, CINNABAR_DOUBLE_SLAB);
        configureOre(registry, COAL_ORE, OreLoot.coal(COAL_ORE.getDefaultState()));
        configureSlab(registry, COBBLED_DEEPSLATE_SLAB, COBBLED_DEEPSLATE_DOUBLE_SLAB);
        configureSlab(registry, COBBLESTONE_SLAB, COBBLESTONE_DOUBLE_SLAB);
        configureTorch(registry, COLORED_TORCH_BLUE);
        configureTorch(registry, COLORED_TORCH_GREEN);
        configureTorch(registry, COLORED_TORCH_PURPLE);
        configureTorch(registry, COLORED_TORCH_RED);
        registry.configure(CONDUIT).set(BlockComponents.ON_STATE_CHANGED, ConduitBlockHandlers.ON_STATE_CHANGED);
        configureDoor(registry, COPPER_DOOR);
        configureOre(registry, COPPER_ORE, OreLoot.copper(COPPER_ORE.getDefaultState()));
        configureTorch(registry, COPPER_TORCH);
        configureTrapdoor(registry, COPPER_TRAPDOOR);
        configureUsable(registry, CRAFTER, ContainerBlockHandlers.CRAFTER);
        configureUsable(registry, CRAFTING_TABLE, ContainerBlockHandlers.CRAFTING_TABLE);
        configureWoodenButton(registry, CRIMSON_BUTTON);
        configureDoor(registry, CRIMSON_DOOR);
        configureHangingSign(registry, CRIMSON_HANGING_SIGN);
        configureSlab(registry, CRIMSON_SLAB, CRIMSON_DOUBLE_SLAB);
        configureTrapdoor(registry, CRIMSON_TRAPDOOR);
        configureSlab(registry, CUT_COPPER_SLAB, DOUBLE_CUT_COPPER_SLAB);
        configureSlab(registry, CUT_RED_SANDSTONE_SLAB, CUT_RED_SANDSTONE_DOUBLE_SLAB);
        configureSlab(registry, CUT_SANDSTONE_SLAB, CUT_SANDSTONE_DOUBLE_SLAB);
        configureConcretePowder(registry, CYAN_CONCRETE_POWDER, CYAN_CONCRETE);
        configureShulkerBox(registry, CYAN_SHULKER_BOX);
        configureAnvil(registry, DAMAGED_ANVIL);
        configureWoodenButton(registry, DARK_OAK_BUTTON);
        configureDoor(registry, DARK_OAK_DOOR);
        configureHangingSign(registry, DARK_OAK_HANGING_SIGN);
        configureLeaves(registry, DARK_OAK_LEAVES, VanillaBlockLoot.leaves(DARK_OAK_LEAVES, DARK_OAK_SAPLING, true));
        configureSlab(registry, DARK_OAK_SLAB, DARK_OAK_DOUBLE_SLAB);
        configureTrapdoor(registry, DARK_OAK_TRAPDOOR);
        configureSlab(registry, DARK_PRISMARINE_SLAB, DARK_PRISMARINE_DOUBLE_SLAB);
        registry.configure(DEEPSLATE)
                .set(BlockComponents.GET_LOOT, VanillaBlockLoot.silkTouchAlternative(DEEPSLATE, COBBLED_DEEPSLATE));
        configureSlab(registry, DEEPSLATE_BRICK_SLAB, DEEPSLATE_BRICK_DOUBLE_SLAB);
        configureOre(registry, DEEPSLATE_COAL_ORE, OreLoot.coal(DEEPSLATE_COAL_ORE.getDefaultState()));
        configureOre(registry, DEEPSLATE_COPPER_ORE, OreLoot.copper(DEEPSLATE_COPPER_ORE.getDefaultState()));
        configureOre(registry, DEEPSLATE_DIAMOND_ORE, OreLoot.diamond(DEEPSLATE_DIAMOND_ORE.getDefaultState()));
        configureOre(registry, DEEPSLATE_EMERALD_ORE, OreLoot.emerald(DEEPSLATE_EMERALD_ORE.getDefaultState()));
        configureOre(registry, DEEPSLATE_GOLD_ORE, OreLoot.gold(DEEPSLATE_GOLD_ORE.getDefaultState()));
        configureOre(registry, DEEPSLATE_IRON_ORE, OreLoot.iron(DEEPSLATE_IRON_ORE.getDefaultState()));
        configureOre(registry, DEEPSLATE_LAPIS_ORE, OreLoot.lapis(DEEPSLATE_LAPIS_ORE.getDefaultState()));
        configureOre(registry, DEEPSLATE_REDSTONE_ORE, OreLoot.redstone(DEEPSLATE_REDSTONE_ORE.getDefaultState()));
        configureSlab(registry, DEEPSLATE_TILE_SLAB, DEEPSLATE_TILE_DOUBLE_SLAB);
        configurePoweredRail(registry, DETECTOR_RAIL);
        configureOre(registry, DIAMOND_ORE, OreLoot.diamond(DIAMOND_ORE.getDefaultState()));
        configureSlab(registry, DIORITE_SLAB, DIORITE_DOUBLE_SLAB);
        configureUsable(registry, DISPENSER, ContainerBlockHandlers.DISPENSER);
        configureFalling(registry, DRAGON_EGG, SoundTypes.LAND_STONE, SoundTypes.DIG_STONE)
                .set(BlockComponents.ATTACK, DragonEggBlockHandlers.ATTACK)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, DragonEggBlockHandlers.USE);
        registry.configure(DRIED_GHAST)
                .set(BlockComponents.CAN_RANDOM_TICK, true)
                .set(BlockComponents.ON_ADDED, DriedGhastBlockHandlers.ON_ADDED)
                .set(BlockComponents.ON_LIQUID_CHANGED, DriedGhastBlockHandlers.ON_LIQUID_CHANGED)
                .set(BlockComponents.ON_RANDOM_TICK, DriedGhastBlockHandlers.ON_RANDOM_TICK)
                .set(BlockComponents.ON_TICK, DriedGhastBlockHandlers.ON_TICK);
        configureUsable(registry, DROPPER, ContainerBlockHandlers.DROPPER);
        configureOre(registry, EMERALD_ORE, OreLoot.emerald(EMERALD_ORE.getDefaultState()));
        configureUsable(registry, ENCHANTING_TABLE, ContainerBlockHandlers.ENCHANTING_TABLE);
        registry.configure(END_GATEWAY)
                .set(BlockComponents.GET_COLLISION_SHAPE, EndGatewayBlockHandlers.COLLISION_SHAPE)
                .set(BlockComponents.GET_ENTITY_INSIDE_COLLISION_SHAPE, EndGatewayBlockHandlers.ENTITY_INSIDE_SHAPE)
                .set(BlockComponents.ON_ENTITY_INSIDE, EndGatewayBlockHandlers.ON_ENTITY_INSIDE);
        registry.configure(END_PORTAL)
                .set(BlockComponents.GET_COLLISION_SHAPE, EndPortalBlockHandlers.COLLISION_SHAPE)
                .set(BlockComponents.GET_ENTITY_INSIDE_COLLISION_SHAPE, EndPortalBlockHandlers.ENTITY_INSIDE_SHAPE)
                .set(BlockComponents.ON_ENTITY_INSIDE, EndPortalBlockHandlers.ON_ENTITY_INSIDE);
        registry.configure(END_PORTAL_FRAME)
                .set(BlockComponents.ON_DESTROY, EndPortalBlockHandlers.DESTROY_FRAME);
        configureSlab(registry, END_STONE_BRICK_SLAB, END_STONE_BRICK_DOUBLE_SLAB);
        configureUsable(registry, ENDER_CHEST, ContainerBlockHandlers.ENDER_CHEST);
        configureDoor(registry, EXPOSED_COPPER_DOOR);
        configureTrapdoor(registry, EXPOSED_COPPER_TRAPDOOR);
        configureSlab(registry, EXPOSED_CUT_COPPER_SLAB, EXPOSED_DOUBLE_CUT_COPPER_SLAB);
        registry.configure(FARMLAND).set(BlockComponents.ON_FALL_ON, FarmlandBlockHandlers.FALL_ON);
        registry.configure(FIRE)
                .set(BlockComponents.RESOLVE_PLACEMENT_STATE, FireBlockHandlers.RESOLVE_PLACEMENT_STATE)
                .set(BlockComponents.ON_ADDED, FireBlockHandlers.ON_ADDED)
                .set(BlockComponents.CAN_SURVIVE, FireBlockHandlers.CAN_SURVIVE)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, FireBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_ENTITY_INSIDE, DefaultBlockHandlers.FIRE_ENTITY_INSIDE);
        configureLiquid(registry, FLOWING_LAVA, LiquidTypes.FLOWING_LAVA)
                .set(BlockComponents.CAN_RANDOM_TICK, true)
                .set(BlockComponents.ON_RANDOM_TICK, LiquidBlockHandlers::randomTick)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, (block, neighbor) -> LiquidBlockHandlers.schedule(block))
                .set(BlockComponents.ON_ENTITY_INSIDE, DefaultBlockHandlers.LAVA_ENTITY_INSIDE);
        configureLiquid(registry, FLOWING_WATER, LiquidTypes.FLOWING_WATER)
                .set(BlockComponents.ON_STATE_CHANGED, BubbleColumnBlockHandlers::scheduleFormation)
                .set(BlockComponents.ON_TICK, (block, random) -> BubbleColumnBlockHandlers.update(block))
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, (block, neighbor) -> {
                    LiquidBlockHandlers.schedule(block);
                    BubbleColumnBlockHandlers.scheduleFormation(block);
                });
        configureItemFrame(registry, FRAME);
        registry.configure(FROSTED_ICE)
                .set(BlockComponents.AFTER_BREAK, IceBlockHandlers.AFTER_BREAK)
                .set(BlockComponents.ON_ADDED, FrostedIceBlockHandlers.ON_ADDED)
                .set(BlockComponents.ON_TICK, FrostedIceBlockHandlers.ON_TICK)
                .set(BlockComponents.ON_REMOVE, FrostedIceBlockHandlers.ON_REMOVE)
                .set(BlockComponents.GET_PICK_BLOCK, FrostedIceBlockHandlers.GET_PICK_BLOCK);
        configureUsable(registry, FURNACE, ContainerBlockHandlers.FURNACE);
        registry.configure(GLASS).set(BlockComponents.GET_LOOT, VanillaBlockLoot.silkTouchOnly(GLASS));
        configureItemFrame(registry, GLOW_FRAME);
        registry.configure(GLOWSTONE)
                .set(BlockComponents.GET_LOOT, VanillaBlockLoot.silkTouchWithUniformBonus(GLOWSTONE, ItemTypes.GLOWSTONE_DUST, 2, 4, 4));
        configureOre(registry, GOLD_ORE, OreLoot.gold(GOLD_ORE.getDefaultState()));
        configurePoweredRail(registry, GOLDEN_RAIL);
        configureSlab(registry, GRANITE_SLAB, GRANITE_DOUBLE_SLAB);
        registry.configure(GRASS_BLOCK)
                .set(BlockComponents.GET_LOOT, VanillaBlockLoot.silkTouchAlternative(GRASS_BLOCK, DIRT));
        registry.configure(GRASS_PATH)
                .set(BlockComponents.GET_LOOT, (block, context) -> List.of(ItemStack.from(DIRT.getDefaultState())));
        configureFalling(registry, GRAVEL, SoundTypes.LAND_GRAVEL, SoundTypes.DIG_GRAVEL);
        configureConcretePowder(registry, GRAY_CONCRETE_POWDER, GRAY_CONCRETE);
        configureShulkerBox(registry, GRAY_SHULKER_BOX);
        configureConcretePowder(registry, GREEN_CONCRETE_POWDER, GREEN_CONCRETE);
        configureShulkerBox(registry, GREEN_SHULKER_BOX);
        configureUsable(registry, GRINDSTONE, ContainerBlockHandlers.GRINDSTONE);
        configureUsable(registry, HOPPER, ContainerBlockHandlers.HOPPER);
        registry.configure(ICE)
                .set(BlockComponents.GET_LOOT, VanillaBlockLoot.silkTouchOnly(ICE))
                .set(BlockComponents.AFTER_BREAK, IceBlockHandlers.AFTER_BREAK)
                .set(BlockComponents.CAN_RANDOM_TICK, true)
                .set(BlockComponents.ON_RANDOM_TICK, IceBlockHandlers.ON_RANDOM_TICK);
        registry.configure(ICE_CRYSTAL)
                .set(BlockComponents.RESOLVE_PLACEMENT_STATE, IceCrystalBlockHandlers.RESOLVE_PLACEMENT_STATE)
                .set(BlockComponents.CAN_SURVIVE, IceCrystalBlockHandlers.CAN_SURVIVE)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, IceCrystalBlockHandlers.ON_NEIGHBOUR_CHANGED);
        configureSpeleothem(registry, ICICLE)
                .set(BlockComponents.RESOLVE_PLACEMENT_STATE, IcicleBlockHandlers.RESOLVE_PLACEMENT_STATE)
                .set(BlockComponents.CAN_RANDOM_TICK, true)
                .set(BlockComponents.ON_RANDOM_TICK, IcicleBlockHandlers.ON_RANDOM_TICK)
                .set(BlockComponents.ON_FALL_ON, SpeleothemBlockHandlers.ON_FALL_ON);
        configureDoor(registry, IRON_DOOR);
        configureOre(registry, IRON_ORE, OreLoot.iron(IRON_ORE.getDefaultState()));
        configureTrapdoor(registry, IRON_TRAPDOOR);
        configureWoodenButton(registry, JUNGLE_BUTTON);
        configureDoor(registry, JUNGLE_DOOR);
        configureHangingSign(registry, JUNGLE_HANGING_SIGN);
        configureLeaves(registry, JUNGLE_LEAVES, VanillaBlockLoot.jungleLeaves(JUNGLE_LEAVES, JUNGLE_SAPLING));
        configureSlab(registry, JUNGLE_SLAB, JUNGLE_DOUBLE_SLAB);
        configureTrapdoor(registry, JUNGLE_TRAPDOOR);
        configureOre(registry, LAPIS_ORE, OreLoot.lapis(LAPIS_ORE.getDefaultState()));
        configureLiquid(registry, LAVA, LiquidTypes.LAVA)
                .set(BlockComponents.CAN_RANDOM_TICK, true)
                .set(BlockComponents.ON_RANDOM_TICK, LiquidBlockHandlers::randomTick)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, (block, neighbor) -> LiquidBlockHandlers.schedule(block))
                .set(BlockComponents.ON_ENTITY_INSIDE, DefaultBlockHandlers.LAVA_ENTITY_INSIDE);
        configureUsable(registry, LECTERN, LecternBlockHandlers.USE);
        registry.configure(LEVER)
                .set(BlockComponents.GET_REDSTONE_SIGNAL, LeverBlockHandlers.GET_REDSTONE_SIGNAL)
                .set(BlockComponents.GET_DIRECT_REDSTONE_SIGNAL, LeverBlockHandlers.GET_DIRECT_REDSTONE_SIGNAL)
                .set(BlockComponents.ON_PLACE, new LeverPlaceHandler())
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, LeverBlockHandlers.USE)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, LeverBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_DESTROY, LeverBlockHandlers.ON_DESTROY);
        configureConcretePowder(registry, LIGHT_BLUE_CONCRETE_POWDER, LIGHT_BLUE_CONCRETE);
        configureShulkerBox(registry, LIGHT_BLUE_SHULKER_BOX);
        configureConcretePowder(registry, LIGHT_GRAY_CONCRETE_POWDER, LIGHT_GRAY_CONCRETE);
        configureShulkerBox(registry, LIGHT_GRAY_SHULKER_BOX);
        configureConcretePowder(registry, LIME_CONCRETE_POWDER, LIME_CONCRETE);
        configureShulkerBox(registry, LIME_SHULKER_BOX);
        configureUsable(registry, LIT_BLAST_FURNACE, ContainerBlockHandlers.BLAST_FURNACE);
        configureOre(registry, LIT_DEEPSLATE_REDSTONE_ORE, OreLoot.redstone(DEEPSLATE_REDSTONE_ORE.getDefaultState()));
        configureUsable(registry, LIT_FURNACE, ContainerBlockHandlers.FURNACE);
        registry.configure(LIT_PUMPKIN).set(BlockComponents.ON_PLACE, new CarvedPumpkinPlaceHandler());
        configureOre(registry, LIT_REDSTONE_ORE, OreLoot.redstone(REDSTONE_ORE.getDefaultState()));
        configureUsable(registry, LIT_SMOKER, ContainerBlockHandlers.SMOKER);
        configureUsable(registry, LOOM, ContainerBlockHandlers.LOOM);
        configureConcretePowder(registry, MAGENTA_CONCRETE_POWDER, MAGENTA_CONCRETE);
        configureShulkerBox(registry, MAGENTA_SHULKER_BOX);
        configureWoodenButton(registry, MANGROVE_BUTTON);
        configureDoor(registry, MANGROVE_DOOR);
        configureHangingSign(registry, MANGROVE_HANGING_SIGN);
        configureLeaves(registry, MANGROVE_LEAVES, VanillaBlockLoot.mangroveLeaves(MANGROVE_LEAVES));
        registry.configure(MANGROVE_LEAVES).set(BlockComponents.FERTILIZE, MangrovePropaguleBlockHandlers.FERTILIZE_LEAVES);
        registry.configure(MANGROVE_PROPAGULE)
                .set(BlockComponents.RESOLVE_PLACEMENT_STATE, MangrovePropaguleBlockHandlers.RESOLVE_PLACEMENT_STATE)
                .set(BlockComponents.CAN_SURVIVE, MangrovePropaguleBlockHandlers.CAN_SURVIVE)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, MangrovePropaguleBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.CAN_RANDOM_TICK, true)
                .set(BlockComponents.ON_RANDOM_TICK, MangrovePropaguleBlockHandlers.ON_RANDOM_TICK)
                .set(BlockComponents.FERTILIZE, MangrovePropaguleBlockHandlers.FERTILIZE)
                .set(BlockComponents.GET_LOOT, MangrovePropaguleBlockHandlers.GET_LOOT)
                .set(BlockComponents.GET_PICK_BLOCK, MangrovePropaguleBlockHandlers.GET_PICK_BLOCK);
        configureSlab(registry, MANGROVE_SLAB, MANGROVE_DOUBLE_SLAB);
        configureTrapdoor(registry, MANGROVE_TRAPDOOR);
        configureSlab(registry, MOSSY_COBBLESTONE_SLAB, MOSSY_COBBLESTONE_DOUBLE_SLAB);
        configureSlab(registry, MOSSY_STONE_BRICK_SLAB, MOSSY_STONE_BRICK_DOUBLE_SLAB);
        registry.configure(MUD)
                .set(BlockComponents.GET_BLOCK_SUPPORT_SHAPE, DefaultBlockHandlers.FULL_BLOCK_SUPPORT_SHAPE);
        configureSlab(registry, MUD_BRICK_SLAB, MUD_BRICK_DOUBLE_SLAB);
        registry.configure(MYCELIUM)
                .set(BlockComponents.GET_LOOT, VanillaBlockLoot.silkTouchAlternative(MYCELIUM, DIRT));
        configureSlab(registry, NETHER_BRICK_SLAB, NETHER_BRICK_DOUBLE_SLAB);
        configureOre(registry, NETHER_GOLD_ORE, OreLoot.netherGold(NETHER_GOLD_ORE.getDefaultState()));
        configureWoodenButton(registry, OAK_BUTTON);
        configureDoor(registry, OAK_DOOR);
        configureHangingSign(registry, OAK_HANGING_SIGN);
        configureLeaves(registry, OAK_LEAVES, VanillaBlockLoot.leaves(OAK_LEAVES, OAK_SAPLING, true));
        configureSlab(registry, OAK_SLAB, OAK_DOUBLE_SLAB);
        configureTrapdoor(registry, OAK_TRAPDOOR);
        configureConcretePowder(registry, ORANGE_CONCRETE_POWDER, ORANGE_CONCRETE);
        configureLeaves(registry, ORANGE_POPLAR_LEAVES, VanillaBlockLoot.leaves(ORANGE_POPLAR_LEAVES, POPLAR_SAPLING, false));
        configureShulkerBox(registry, ORANGE_SHULKER_BOX);
        configureDoor(registry, OXIDIZED_COPPER_DOOR);
        configureTrapdoor(registry, OXIDIZED_COPPER_TRAPDOOR);
        configureSlab(registry, OXIDIZED_CUT_COPPER_SLAB, OXIDIZED_DOUBLE_CUT_COPPER_SLAB);
        configureWoodenButton(registry, PALE_OAK_BUTTON);
        configureDoor(registry, PALE_OAK_DOOR);
        configureHangingSign(registry, PALE_OAK_HANGING_SIGN);
        configureLeaves(registry, PALE_OAK_LEAVES, VanillaBlockLoot.leaves(PALE_OAK_LEAVES, PALE_OAK_SAPLING, false));
        configureSlab(registry, PALE_OAK_SLAB, PALE_OAK_DOUBLE_SLAB);
        configureTrapdoor(registry, PALE_OAK_TRAPDOOR);
        configureSlab(registry, PETRIFIED_OAK_SLAB, PETRIFIED_OAK_DOUBLE_SLAB);
        configureConcretePowder(registry, PINK_CONCRETE_POWDER, PINK_CONCRETE);
        configureShulkerBox(registry, PINK_SHULKER_BOX);
        registry.configure(PODZOL).set(BlockComponents.GET_LOOT, VanillaBlockLoot.silkTouchAlternative(PODZOL, DIRT));
        configureSpeleothem(registry, POINTED_DRIPSTONE)
                .set(BlockComponents.CAN_RANDOM_TICK, true)
                .set(BlockComponents.ON_RANDOM_TICK, PointedDripstoneBlockHandlers.ON_RANDOM_TICK)
                .set(BlockComponents.ON_FALL_ON, SpeleothemBlockHandlers.ON_FALL_ON);
        configureSlab(registry, POLISHED_ANDESITE_SLAB, POLISHED_ANDESITE_DOUBLE_SLAB);
        configureSlab(registry, POLISHED_BLACKSTONE_BRICK_SLAB, POLISHED_BLACKSTONE_BRICK_DOUBLE_SLAB);
        configureStoneButton(registry, POLISHED_BLACKSTONE_BUTTON);
        configureSlab(registry, POLISHED_BLACKSTONE_SLAB, POLISHED_BLACKSTONE_DOUBLE_SLAB);
        configureSlab(registry, POLISHED_CINNABAR_SLAB, POLISHED_CINNABAR_DOUBLE_SLAB);
        configureSlab(registry, POLISHED_DEEPSLATE_SLAB, POLISHED_DEEPSLATE_DOUBLE_SLAB);
        configureSlab(registry, POLISHED_DIORITE_SLAB, POLISHED_DIORITE_DOUBLE_SLAB);
        configureSlab(registry, POLISHED_GRANITE_SLAB, POLISHED_GRANITE_DOUBLE_SLAB);
        configureSlab(registry, POLISHED_SULFUR_SLAB, POLISHED_SULFUR_DOUBLE_SLAB);
        configureSlab(registry, POLISHED_TUFF_SLAB, POLISHED_TUFF_DOUBLE_SLAB);
        configureWoodenButton(registry, POPLAR_BUTTON);
        registry.configure(PACKED_ICE).set(BlockComponents.GET_LOOT, VanillaBlockLoot.silkTouchOnly(PACKED_ICE));
        configureDoor(registry, POPLAR_DOOR);
        configureHangingSign(registry, POPLAR_HANGING_SIGN);
        configureSlab(registry, POPLAR_SLAB, POPLAR_DOUBLE_SLAB);
        configureTrapdoor(registry, POPLAR_TRAPDOOR);
        registry.configure(PORTAL)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, PortalBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.CAN_RANDOM_TICK, true)
                .set(BlockComponents.ON_RANDOM_TICK, PortalBlockHandlers.ON_RANDOM_TICK);
        registry.configure(POWDER_SNOW)
                .set(BlockComponents.BUCKET_PICKUP, PowderSnowBlockHandlers.BUCKET_PICKUP)
                .set(BlockComponents.GET_COLLISION_SHAPE, PowderSnowBlockHandlers.COLLISION_SHAPE)
                .set(BlockComponents.ON_FALL_ON, PowderSnowBlockHandlers.FALL_ON)
                .set(BlockComponents.ON_ENTITY_INSIDE, PowderSnowBlockHandlers.ENTITY_INSIDE);
        configureSlab(registry, PRISMARINE_BRICK_SLAB, PRISMARINE_BRICK_DOUBLE_SLAB);
        configureSlab(registry, PRISMARINE_SLAB, PRISMARINE_DOUBLE_SLAB);
        configureConcretePowder(registry, PURPLE_CONCRETE_POWDER, PURPLE_CONCRETE);
        configureShulkerBox(registry, PURPLE_SHULKER_BOX);
        configureSlab(registry, PURPUR_SLAB, PURPUR_DOUBLE_SLAB);
        configureOre(registry, QUARTZ_ORE, OreLoot.quartz(QUARTZ_ORE.getDefaultState()));
        configureSlab(registry, QUARTZ_SLAB, QUARTZ_DOUBLE_SLAB);
        registry.configure(RAIL)
                .set(BlockComponents.ON_PLACE, RailPlaceHandler.INSTANCE)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, RailBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_REMOVE, RailBlockHandlers.ON_REMOVE);
        configureConcretePowder(registry, RED_CONCRETE_POWDER, RED_CONCRETE);
        configureSlab(registry, RED_NETHER_BRICK_SLAB, RED_NETHER_BRICK_DOUBLE_SLAB);
        configureLeaves(registry, RED_POPLAR_LEAVES, VanillaBlockLoot.leaves(RED_POPLAR_LEAVES, POPLAR_SAPLING, false));
        configureFalling(registry, RED_SAND, SoundTypes.LAND_SAND, SoundTypes.DIG_SAND);
        configureSlab(registry, RED_SANDSTONE_SLAB, RED_SANDSTONE_DOUBLE_SLAB);
        configureShulkerBox(registry, RED_SHULKER_BOX);
        registry.configure(REDSTONE_WIRE)
                .set(BlockComponents.RESOLVE_PLACEMENT_STATE, RedstoneWireBlockHandlers.RESOLVE_PLACEMENT_STATE)
                .set(BlockComponents.CAN_SURVIVE, RedstoneWireBlockHandlers.CAN_SURVIVE)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, RedstoneWireBlockHandlers.USE)
                .set(BlockComponents.ON_ADDED, RedstoneWireBlockHandlers.ON_ADDED)
                .set(BlockComponents.ON_REMOVE, RedstoneWireBlockHandlers.ON_REMOVE)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, RedstoneWireBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_REDSTONE_UPDATE, RedstoneWireBlockHandlers.ON_REDSTONE_UPDATE)
                .set(BlockComponents.GET_REDSTONE_SIGNAL, RedstoneWireBlockHandlers.GET_REDSTONE_SIGNAL)
                .set(BlockComponents.GET_DIRECT_REDSTONE_SIGNAL, RedstoneWireBlockHandlers.GET_DIRECT_REDSTONE_SIGNAL);
        configureOre(registry, REDSTONE_ORE, OreLoot.redstone(REDSTONE_ORE.getDefaultState()));
        configureTorch(registry, REDSTONE_TORCH);
        registry.configure(REDSTONE_TORCH)
                .set(BlockComponents.GET_REDSTONE_SIGNAL, (block, face) -> face != block.getState().ensureTrait(BlockTraits.TORCH_DIRECTION) ? 15 : 0)
                .set(BlockComponents.GET_DIRECT_REDSTONE_SIGNAL, (block, face) -> face == Direction.UP ? 15 : 0);
        registry.configure(REDSTONE_BLOCK).set(BlockComponents.GET_REDSTONE_SIGNAL, (block, face) -> 15);
        configureSlab(registry, RESIN_BRICK_SLAB, RESIN_BRICK_DOUBLE_SLAB);
        configureUsable(registry, RESPAWN_ANCHOR, RespawnAnchorBlockHandlers.RESPAWN_ANCHOR);
        configureFalling(registry, SAND, SoundTypes.LAND_SAND, SoundTypes.DIG_SAND);
        configureSlab(registry, SANDSTONE_SLAB, SANDSTONE_DOUBLE_SLAB);
        registry.configure(SEA_LANTERN)
                .set(BlockComponents.GET_LOOT, VanillaBlockLoot.silkTouchWithUniformBonus(SEA_LANTERN, ItemTypes.PRISMARINE_CRYSTALS, 2, 3, 5));
        registry.configure(SEA_PICKLE)
                .set(BlockComponents.CAN_BE_REPLACED, SeaPickleBlockHandlers.CAN_BE_REPLACED)
                .set(BlockComponents.RESOLVE_PLACEMENT_STATE, SeaPickleBlockHandlers.RESOLVE_PLACEMENT_STATE)
                .set(BlockComponents.CAN_SURVIVE, SeaPickleBlockHandlers.CAN_SURVIVE)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, VegetationBlockHandlers.checkSurvival(SeaPickleBlockHandlers.CAN_SURVIVE))
                .set(BlockComponents.FERTILIZE, SeaPickleBlockHandlers.FERTILIZE)
                .set(BlockComponents.GET_LOOT, SeaPickleBlockHandlers.GET_LOOT);
        configureUsable(registry, SMITHING_TABLE, ContainerBlockHandlers.SMITHING_TABLE);
        configureUsable(registry, SMOKER, ContainerBlockHandlers.SMOKER);
        configureSlab(registry, SMOOTH_QUARTZ_SLAB, SMOOTH_QUARTZ_DOUBLE_SLAB);
        configureSlab(registry, SMOOTH_RED_SANDSTONE_SLAB, SMOOTH_RED_SANDSTONE_DOUBLE_SLAB);
        configureSlab(registry, SMOOTH_SANDSTONE_SLAB, SMOOTH_SANDSTONE_DOUBLE_SLAB);
        configureSlab(registry, SMOOTH_STONE_SLAB, SMOOTH_STONE_DOUBLE_SLAB);
        registry.configure(SNOW)
                .set(BlockComponents.GET_LOOT, (block, context) -> List.of(ItemStack.from(ItemTypes.SNOWBALL).withCount(4)));
        configureFalling(registry, SNOW_LAYER, SoundTypes.LAND_SNOW, SoundTypes.DIG_SNOW)
                .set(BlockComponents.CAN_BE_REPLACED, SnowLayerBlockHandlers.CAN_BE_REPLACED)
                .set(BlockComponents.CAN_RANDOM_TICK, true)
                .set(BlockComponents.CAN_SURVIVE, SnowLayerBlockHandlers.CAN_SURVIVE)
                .set(BlockComponents.GET_BLOCK_SUPPORT_SHAPE, DefaultBlockHandlers.SNOW_LAYER_BLOCK_SUPPORT_SHAPE)
                .set(BlockComponents.GET_LOOT, (block, context) -> List.of(SnowLayerBlockHandlers.getResource(block.getState())))
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, SnowLayerBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.RESOLVE_PLACEMENT_STATE, SnowLayerBlockHandlers.RESOLVE_PLACEMENT_STATE)
                .set(BlockComponents.ON_RANDOM_TICK, SnowLayerBlockHandlers.ON_RANDOM_TICK);
        registry.configure(SOUL_CAMPFIRE).set(BlockComponents.GET_IGNITED_STATE,
                BlockIgnitionHandlers.litState(BlockTraits.IS_EXTINGUISHED, false));
        registry.configure(SOUL_FIRE)
                .set(BlockComponents.CAN_SURVIVE, FireBlockHandlers.SOUL_CAN_SURVIVE)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, FireBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_ENTITY_INSIDE, DefaultBlockHandlers.FIRE_ENTITY_INSIDE);
        registry.configure(SOUL_SAND)
                .set(BlockComponents.GET_BLOCK_SUPPORT_SHAPE, DefaultBlockHandlers.FULL_BLOCK_SUPPORT_SHAPE);
        configureTorch(registry, SOUL_TORCH);
        registry.configure(SPONGE)
                .set(BlockComponents.ON_STATE_CHANGED, SpongeBlockHandlers::absorb)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, (block, neighbor) -> SpongeBlockHandlers.absorb(block));
        registry.configure(SMALL_DRIPLEAF_BLOCK)
                .set(BlockComponents.RESOLVE_PLACEMENT_STATE, SmallDripleafBlockHandlers.RESOLVE_PLACEMENT_STATE)
                .set(BlockComponents.CAN_BE_PLACED, SmallDripleafBlockHandlers.CAN_BE_PLACED)
                .set(BlockComponents.CAN_SURVIVE, SmallDripleafBlockHandlers.CAN_SURVIVE)
                .set(BlockComponents.ON_STATE_CHANGED, SmallDripleafBlockHandlers.ON_STATE_CHANGED)
                .set(BlockComponents.ON_PLACE, SmallDripleafBlockHandlers.ON_PLACE)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, SmallDripleafBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_TICK, SmallDripleafBlockHandlers.ON_TICK)
                .set(BlockComponents.ON_DESTROY, SmallDripleafBlockHandlers.ON_DESTROY)
                .set(BlockComponents.FERTILIZE, SmallDripleafBlockHandlers.FERTILIZE)
                .set(BlockComponents.GET_LOOT, VanillaBlockLoot.shearsOnly(SMALL_DRIPLEAF_BLOCK));
        configureWoodenButton(registry, SPRUCE_BUTTON);
        configureDoor(registry, SPRUCE_DOOR);
        configureHangingSign(registry, SPRUCE_HANGING_SIGN);
        configureLeaves(registry, SPRUCE_LEAVES, VanillaBlockLoot.leaves(SPRUCE_LEAVES, SPRUCE_SAPLING, false));
        configureSlab(registry, SPRUCE_SLAB, SPRUCE_DOUBLE_SLAB);
        configureTrapdoor(registry, SPRUCE_TRAPDOOR);
        registry.configure(STONE)
                .set(BlockComponents.GET_LOOT,
                        (block, context) -> List.of(ItemStack.from(
                                context.enchantmentLevel(EnchantmentTypes.SILK_TOUCH) > 0
                                        ? BlockTypes.STONE.getDefaultState()
                                        : BlockTypes.COBBLESTONE.getDefaultState())));
        configureSlab(registry, STONE_BRICK_SLAB, STONE_BRICK_DOUBLE_SLAB);
        configureStoneButton(registry, STONE_BUTTON);
        configureSlab(registry, STONE_SLAB, STONE_DOUBLE_SLAB);
        configureUsable(registry, STONECUTTER, ContainerBlockHandlers.STONECUTTER);
        configureUsable(registry, STONECUTTER_BLOCK, ContainerBlockHandlers.STONECUTTER);
        configureBed(registry, STRAW_BED);
        configureSlab(registry, SULFUR_BRICK_SLAB, SULFUR_BRICK_DOUBLE_SLAB);
        configureSlab(registry, SULFUR_SLAB, SULFUR_DOUBLE_SLAB);
        configureSpeleothem(registry, SULFUR_SPIKE)
                .set(BlockComponents.CAN_RANDOM_TICK, true)
                .set(BlockComponents.ON_RANDOM_TICK, SpeleothemBlockHandlers.naturalGrowth(SULFUR, 2));
        registry.configure(SWEET_BERRY_BUSH)
                .set(BlockComponents.ON_ENTITY_INSIDE, VegetationBlockHandlers.SWEET_BERRY_BUSH_ENTITY_INSIDE)
                .set(BlockComponents.GET_LOOT, VanillaBlockLoot.sweetBerryBush())
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, VegetationBlockHandlers.SWEET_BERRY_BUSH_USE);
        BlockTypes.values().stream()
                .filter(type -> type.getTraits().contains(BlockTraits.EXPLODE))
                .forEach(type -> registry.configure(type)
                        .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                        .set(BlockComponents.USE, TntBlockHandlers.USE)
                        .set(BlockComponents.GET_EXPLOSION_LOOT, DefaultBlockHandlers.NO_LOOT)
                        .set(BlockComponents.ON_EXPLOSION_HIT, TntBlockHandlers.ON_EXPLOSION_HIT)
                        .set(BlockComponents.ON_PROJECTILE_HIT, TntBlockHandlers.ON_PROJECTILE_HIT));
        configureTorch(registry, TORCH);
        configureUsable(registry, TRAPPED_CHEST, ContainerBlockHandlers.TRAPPED_CHEST);
        registry.configure(TRIP_WIRE)
                .set(BlockComponents.ON_PLACE, new TripwireBlockPlaceHandler())
                .set(BlockComponents.GET_LOOT, TripwireBlockHandlers.GET_LOOT)
                .set(BlockComponents.GET_PICK_BLOCK, TripwireBlockHandlers.GET_PICK_BLOCK)
                .set(BlockComponents.ON_ENTITY_INSIDE, TripwireBlockHandlers.ON_ENTITY_INSIDE)
                .set(BlockComponents.GET_ENTITY_INSIDE_COLLISION_SHAPE, (state, context) -> state.getOutlineShape())
                .set(BlockComponents.ON_TICK, TripwireBlockHandlers.ON_TICK)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, TripwireBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_DESTROY, TripwireBlockHandlers.ON_DESTROY);
        registry.configure(TRIPWIRE_HOOK)
                .set(BlockComponents.ON_PLACE, new TripwireHookPlaceHandler())
                .set(BlockComponents.ON_TICK, TripwireHookBlockHandlers.ON_TICK)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, TripwireHookBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_DESTROY, TripwireHookBlockHandlers.ON_DESTROY);
        configureSlab(registry, TUFF_BRICK_SLAB, TUFF_BRICK_DOUBLE_SLAB);
        configureSlab(registry, TUFF_SLAB, TUFF_DOUBLE_SLAB);
        registry.configure(TURTLE_EGG)
                .set(BlockComponents.CAN_RANDOM_TICK, true)
                .set(BlockComponents.ON_ADDED, TurtleEggBlockHandlers.ON_ADDED)
                .set(BlockComponents.CAN_BE_REPLACED, TurtleEggBlockHandlers.CAN_BE_REPLACED)
                .set(BlockComponents.RESOLVE_PLACEMENT_STATE, TurtleEggBlockHandlers.RESOLVE_PLACEMENT_STATE)
                .set(BlockComponents.ON_PLACE, TurtleEggBlockHandlers.ON_PLACE)
                .set(BlockComponents.ON_DESTROY, TurtleEggBlockHandlers.ON_DESTROY)
                .set(BlockComponents.ON_STEP_ON, TurtleEggBlockHandlers.ON_STEP_ON)
                .set(BlockComponents.ON_FALL_ON, TurtleEggBlockHandlers.ON_FALL_ON)
                .set(BlockComponents.ON_RANDOM_TICK, TurtleEggBlockHandlers.ON_RANDOM_TICK)
                .set(BlockComponents.GET_LOOT, VanillaBlockLoot.silkTouchOnly(TURTLE_EGG));
        configureTorch(registry, UNDERWATER_TORCH);
        configureShulkerBox(registry, UNDYED_SHULKER_BOX);
        configureTorch(registry, UNLIT_REDSTONE_TORCH);
        registry.configure(VINE).set(BlockComponents.GET_LOOT, VanillaBlockLoot.shearsOnly(VINE));
        configureWoodenButton(registry, WARPED_BUTTON);
        configureDoor(registry, WARPED_DOOR);
        configureHangingSign(registry, WARPED_HANGING_SIGN);
        configureSlab(registry, WARPED_SLAB, WARPED_DOUBLE_SLAB);
        configureTrapdoor(registry, WARPED_TRAPDOOR);
        configureLiquid(registry, WATER, LiquidTypes.WATER)
                .set(BlockComponents.ON_STATE_CHANGED, BubbleColumnBlockHandlers::scheduleFormation)
                .set(BlockComponents.ON_TICK, (block, random) -> BubbleColumnBlockHandlers.update(block))
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, (block, neighbor) -> {
                    LiquidBlockHandlers.schedule(block);
                    BubbleColumnBlockHandlers.scheduleFormation(block);
                });
        registry.configure(WATERLILY)
                .set(BlockComponents.CAN_SURVIVE, LilyPadBlockHandlers.CAN_SURVIVE)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, VegetationBlockHandlers.checkSurvival(LilyPadBlockHandlers.CAN_SURVIVE))
                .set(BlockComponents.ON_ENTITY_INSIDE, LilyPadBlockHandlers.ON_ENTITY_INSIDE);
        configureDoor(registry, WAXED_COPPER_DOOR);
        configureTrapdoor(registry, WAXED_COPPER_TRAPDOOR);
        configureSlab(registry, WAXED_CUT_COPPER_SLAB, WAXED_DOUBLE_CUT_COPPER_SLAB);
        configureDoor(registry, WAXED_EXPOSED_COPPER_DOOR);
        configureTrapdoor(registry, WAXED_EXPOSED_COPPER_TRAPDOOR);
        configureSlab(registry, WAXED_EXPOSED_CUT_COPPER_SLAB, WAXED_EXPOSED_DOUBLE_CUT_COPPER_SLAB);
        configureDoor(registry, WAXED_OXIDIZED_COPPER_DOOR);
        configureTrapdoor(registry, WAXED_OXIDIZED_COPPER_TRAPDOOR);
        configureSlab(registry, WAXED_OXIDIZED_CUT_COPPER_SLAB, WAXED_OXIDIZED_DOUBLE_CUT_COPPER_SLAB);
        configureDoor(registry, WAXED_WEATHERED_COPPER_DOOR);
        configureTrapdoor(registry, WAXED_WEATHERED_COPPER_TRAPDOOR);
        configureSlab(registry, WAXED_WEATHERED_CUT_COPPER_SLAB, WAXED_WEATHERED_DOUBLE_CUT_COPPER_SLAB);
        configureDoor(registry, WEATHERED_COPPER_DOOR);
        configureTrapdoor(registry, WEATHERED_COPPER_TRAPDOOR);
        configureSlab(registry, WEATHERED_CUT_COPPER_SLAB, WEATHERED_DOUBLE_CUT_COPPER_SLAB);
        registry.configure(WEB)
                .set(BlockComponents.ON_ENTITY_INSIDE, DefaultBlockHandlers.WEB_ENTITY_INSIDE);
        registry.configure(WET_SPONGE).set(BlockComponents.ON_STATE_CHANGED,
                block -> SpongeBlockHandlers.dry(block, ((CloudLevel) block.getLevel()).getDimension() == CloudLevel.DIMENSION_NETHER));
        configureConcretePowder(registry, WHITE_CONCRETE_POWDER, WHITE_CONCRETE);
        configureShulkerBox(registry, WHITE_SHULKER_BOX);
        configureConcretePowder(registry, YELLOW_CONCRETE_POWDER, YELLOW_CONCRETE);
        configureLeaves(registry, YELLOW_POPLAR_LEAVES, VanillaBlockLoot.leaves(YELLOW_POPLAR_LEAVES, POPLAR_SAPLING, false));
        configureShulkerBox(registry, YELLOW_SHULKER_BOX);

        for (BlockType type : BlockTypes.values()) {
            if (type.getTraits().contains(BlockTraits.CANDLES)) {
                registry.configure(type)
                        .set(BlockComponents.GET_IGNITED_STATE, BlockIgnitionHandlers.litState(BlockTraits.IS_LIT, true))
                        .set(BlockComponents.CAN_BE_REPLACED, CandleBlockHandlers.CAN_BE_REPLACED)
                        .set(BlockComponents.RESOLVE_PLACEMENT_STATE, CandleBlockHandlers.RESOLVE_PLACEMENT_STATE)
                        .set(BlockComponents.CAN_SURVIVE, CandleBlockHandlers.CAN_SURVIVE)
                        .set(BlockComponents.GET_LOOT, (block, context) -> List.of(ItemStack.from(type.getDefaultState())
                                .withCount(block.getState().ensureTrait(BlockTraits.CANDLES) + 1)));
            }
        }

        for (BlockType type : NO_LOOT_BLOCKS) {
            registry.configure(type).set(BlockComponents.GET_LOOT, DefaultBlockHandlers.NO_LOOT);
        }
        configureRedstoneConnections(registry);
        configureLightOcclusion(registry);
    }

    private static void configureRedstoneConnections(CloudBlockRegistry registry) {
        for (BlockType type : BlockTypes.values()) {
            if (type.is(BlockTags.BUTTON) || type.is(BlockTags.PRESSURE_PLATE) || type.is(BlockTags.LIGHTNING_RODS)) {
                registry.configure(type).set(BlockComponents.CONNECTS_TO_REDSTONE_WIRE, RedstoneConnectionHandlers.ALL_SIDES);
            }
        }

        for (BlockType type : List.of(LEVER, REDSTONE_TORCH, UNLIT_REDSTONE_TORCH, REDSTONE_BLOCK,
                POWERED_COMPARATOR, UNPOWERED_COMPARATOR, DAYLIGHT_DETECTOR, DAYLIGHT_DETECTOR_INVERTED,
                DETECTOR_RAIL, TRIPWIRE_HOOK, TRAPPED_CHEST, LECTERN, TARGET, JUKEBOX, SCULK_SENSOR, CALIBRATED_SCULK_SENSOR)) {
            registry.configure(type).set(BlockComponents.CONNECTS_TO_REDSTONE_WIRE, RedstoneConnectionHandlers.ALL_SIDES);
        }
        for (BlockType type : List.of(POWERED_REPEATER, UNPOWERED_REPEATER)) {
            registry.configure(type).set(BlockComponents.CONNECTS_TO_REDSTONE_WIRE, RedstoneConnectionHandlers.REPEATER);
        }
        registry.configure(OBSERVER).set(BlockComponents.CONNECTS_TO_REDSTONE_WIRE, RedstoneConnectionHandlers.OBSERVER);
        registry.configure(REDSTONE_WIRE).set(BlockComponents.CONNECTS_TO_REDSTONE_WIRE, RedstoneConnectionHandlers.WIRE);
    }

    private static ComponentBuilder configureSpeleothem(CloudBlockRegistry registry, BlockType type) {
        return registry.configure(type)
                .set(BlockComponents.RESOLVE_PLACEMENT_STATE, SpeleothemBlockHandlers.RESOLVE_PLACEMENT_STATE)
                .set(BlockComponents.CAN_SURVIVE, SpeleothemBlockHandlers.CAN_SURVIVE)
                .set(BlockComponents.START_FALLING, SpeleothemBlockHandlers.START_FALLING)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, SpeleothemBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_TICK, SpeleothemBlockHandlers.ON_TICK)
                .set(BlockComponents.ON_PROJECTILE_HIT, SpeleothemBlockHandlers.ON_PROJECTILE_HIT)
                .set(BlockComponents.BREAKS_ON_LANDING, true)
                .set(BlockComponents.GET_FALLING_DAMAGE_SOURCE, entity -> DamageSource.of(DamageTypes.FALLING_STALACTITE, entity));
    }

    private static void configureLightOcclusion(CloudBlockRegistry registry) {
        for (BlockType type : VanillaBlockTags.values(BlockTags.SLAB)) {
            registry.configure(type).set(BlockComponents.GET_LIGHT_OCCLUSION_SHAPE, LightOcclusionBlockHandlers.OUTLINE);
        }

        for (BlockType type : VanillaBlockTags.values(BlockTags.STAIRS)) {
            registry.configure(type).set(BlockComponents.GET_LIGHT_OCCLUSION_SHAPE, LightOcclusionBlockHandlers.STAIRS);
        }

        for (BlockType type : List.of(DAYLIGHT_DETECTOR, DAYLIGHT_DETECTOR_INVERTED, ENCHANTING_TABLE, FARMLAND, GRASS_PATH, SNOW_LAYER)) {
            registry.configure(type).set(BlockComponents.GET_LIGHT_OCCLUSION_SHAPE, LightOcclusionBlockHandlers.OUTLINE);
        }

        for (BlockType type : List.of(SCULK_SENSOR, CALIBRATED_SCULK_SENSOR, SCULK_SHRIEKER)) {
            registry.configure(type).set(BlockComponents.GET_LIGHT_OCCLUSION_SHAPE, state -> CloudVoxelShapes.box(0, 0, 0, 1, 0.5f, 1));
        }

        for (BlockType type : List.of(ACACIA_SHELF, BAMBOO_SHELF, BIRCH_SHELF, CHERRY_SHELF, CRIMSON_SHELF,
                DARK_OAK_SHELF, JUNGLE_SHELF, MANGROVE_SHELF, OAK_SHELF, PALE_OAK_SHELF, POPLAR_SHELF,
                SPRUCE_SHELF, WARPED_SHELF)) {
            registry.configure(type).set(BlockComponents.GET_LIGHT_OCCLUSION_SHAPE, LightOcclusionBlockHandlers.SHELF);
        }

        registry.configure(LECTERN).set(BlockComponents.GET_LIGHT_OCCLUSION_SHAPE, LightOcclusionBlockHandlers.LECTERN);
        registry.configure(END_PORTAL_FRAME).set(BlockComponents.GET_LIGHT_OCCLUSION_SHAPE, LightOcclusionBlockHandlers.END_PORTAL_FRAME);
        registry.configure(STONECUTTER_BLOCK).set(BlockComponents.GET_LIGHT_OCCLUSION_SHAPE,
                state -> CloudVoxelShapes.box(0, 0, 0, 1, 9 / 16f, 1));
        for (BlockType type : List.of(PISTON, STICKY_PISTON, PISTON_ARM_COLLISION, STICKY_PISTON_ARM_COLLISION)) {
            registry.configure(type).set(BlockComponents.GET_LIGHT_OCCLUSION_SHAPE, BlockState::getCollisionShape);
        }
    }

    private void configureAnvil(CloudBlockRegistry registry, BlockType type) {
        configureFalling(registry, type, SoundTypes.RANDOM_ANVIL_LAND, SoundTypes.RANDOM_ANVIL_BREAK, 2)
                .set(BlockComponents.GET_FALLING_DAMAGE_SOURCE, entity -> DamageSource.of(DamageTypes.FALLING_ANVIL, entity))
                .set(BlockComponents.ON_FALLING_LAND, FallingBlockHandlers.ANVIL_LAND)
                .set(BlockComponents.RESOLVE_PLACEMENT_STATE, AnvilBlockHandlers.RESOLVE_PLACEMENT_STATE)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.ANVIL);
    }

    private void configureBed(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, BedBlockHandlers.BED)
                .set(BlockComponents.ON_PLACE, BedBlockHandlers.PLACE)
                .set(BlockComponents.ON_DESTROY, BedBlockHandlers.ON_DESTROY)
                .set(BlockComponents.GET_LOOT, BedBlockHandlers.GET_LOOT)
                .set(BlockComponents.GET_PICK_BLOCK, BedBlockHandlers.GET_PICK_BLOCK);
    }

    private void configureColoredBuildingBlockSlabs(CloudBlockRegistry registry) {
        for (VanillaSlabAndStairFamily family : VanillaBlockFamilies.COLORED_BUILDING_BLOCKS) {
            configureSlab(registry, family.slab(), family.doubleSlab());
        }
    }

    private void configureConcretePowder(CloudBlockRegistry registry, BlockType powderType, BlockType concreteType) {
        configureFalling(registry, powderType, SoundTypes.LAND_SAND, SoundTypes.DIG_SAND)
                .set(BlockComponents.ON_FALLING_LAND, FallingBlockHandlers.solidifyConcretePowder(concreteType));
    }

    private void configureDoor(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.ON_PLACE, new DoorPlaceHandler())
                .set(BlockComponents.CAN_BE_USED, DoorBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.GET_LOOT, DoorBlockHandlers.GET_LOOT)
                .set(BlockComponents.USE, DoorBlockHandlers.USE)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, DoorBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_DESTROY, DoorBlockHandlers.ON_DESTROY);
    }

    private ComponentBuilder configureFalling(CloudBlockRegistry registry, BlockType type, SoundType landingSound, SoundType breakSound) {
        return configureFalling(registry, type, landingSound, breakSound, 0);
    }

    private ComponentBuilder configureFalling(CloudBlockRegistry registry, BlockType type, SoundType landingSound, SoundType breakSound, float damagePerBlock) {
        return registry.configure(type)
                .set(BlockComponents.IS_FREE_TO_FALL, FallingBlockHandlers.IS_FREE_TO_FALL)
                .set(
                        BlockComponents.START_FALLING,
                        FallingBlockHandlers.startFalling(
                                landingSound, breakSound, damagePerBlock, MAXIMUM_FALL_DAMAGE))
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, FallingBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_TICK, FallingBlockHandlers.ON_TICK);
    }

    private void configureBars(CloudBlockRegistry registry) {
        for (BlockType type : VanillaBlockTags.values(BlockTags.BARS)) {
            registry.configure(type)
                    .set(BlockComponents.RESOLVE_PLACEMENT_STATE, BarBlockHandlers.RESOLVE_PLACEMENT_STATE)
                    .set(BlockComponents.ON_NEIGHBOUR_CHANGED, BarBlockHandlers.ON_NEIGHBOUR_CHANGED);
        }

        for (BlockType type : VanillaBlockTags.values(BlockTags.GLASS_PANES)) {
            registry.configure(type)
                    .set(BlockComponents.RESOLVE_PLACEMENT_STATE, BarBlockHandlers.RESOLVE_PLACEMENT_STATE)
                    .set(BlockComponents.ON_NEIGHBOUR_CHANGED, BarBlockHandlers.ON_NEIGHBOUR_CHANGED);
        }
    }

    private void configureFenceGates(CloudBlockRegistry registry) {
        for (BlockType type : VanillaBlockTags.values(BlockTags.FENCE_GATE)) {
            registry.configure(type)
                    .set(BlockComponents.GET_BLOCK_SUPPORT_SHAPE, DefaultBlockHandlers.FENCE_GATE_BLOCK_SUPPORT_SHAPE)
                    .set(BlockComponents.RESOLVE_PLACEMENT_STATE, FenceGateBlockHandlers.RESOLVE_PLACEMENT_STATE)
                    .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                    .set(BlockComponents.USE, FenceGateBlockHandlers.USE)
                    .set(BlockComponents.ON_NEIGHBOUR_CHANGED, FenceGateBlockHandlers.ON_NEIGHBOUR_CHANGED);
        }
    }

    private void configureFences(CloudBlockRegistry registry) {
        for (BlockType type : VanillaBlockTags.values(BlockTags.FENCE)) {
            registry.configure(type)
                    .set(BlockComponents.RESOLVE_PLACEMENT_STATE, FenceBlockHandlers.RESOLVE_PLACEMENT_STATE)
                    .set(BlockComponents.ON_NEIGHBOUR_CHANGED, FenceBlockHandlers.ON_NEIGHBOUR_CHANGED);
        }
    }

    private void configureHangingSign(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.GET_BLOCK_SUPPORT_SHAPE, DefaultBlockHandlers.OUTLINE_BLOCK_SUPPORT_SHAPE);
    }

    private void configureLeaves(CloudBlockRegistry registry, BlockType type, BlockLootHandler loot) {
        registry.configure(type)
                .set(BlockComponents.GET_BLOCK_SUPPORT_SHAPE, DefaultBlockHandlers.EMPTY_BLOCK_SUPPORT_SHAPE)
                .set(BlockComponents.GET_LOOT, loot);
    }

    private ComponentBuilder configureLiquid(CloudBlockRegistry registry, BlockType blockType, LiquidType liquidType) {
        BlockRegistrationAccess.bindLiquidType(blockType, liquidType);
        return registry.configure(blockType).set(BlockComponents.ON_LIQUID_TICK, LiquidBlockHandlers::tick);
    }

    private void configureOre(CloudBlockRegistry registry, BlockType type, OreLoot loot) {
        registry.configure(type)
                .set(BlockComponents.GET_LOOT, (block, context) -> loot.drops(context))
                .set(BlockComponents.GET_EXPERIENCE, (block, context) -> loot.experience(context));
    }

    private void configurePoweredRail(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.ON_PLACE, RailPlaceHandler.INSTANCE)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, PoweredRailBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_REMOVE, RailBlockHandlers.ON_REMOVE);
    }

    private void configureShulkerBox(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.GET_BLOCK_SUPPORT_SHAPE, DefaultBlockHandlers.SHULKER_BOX_BLOCK_SUPPORT_SHAPE)
                .set(BlockComponents.GET_COLLISION_SHAPE, DefaultBlockHandlers.SHULKER_BOX_COLLISION_SHAPE)
                .set(BlockComponents.GET_OUTLINE_SHAPE, DefaultBlockHandlers.SHULKER_BOX_OUTLINE_SHAPE)
                .set(BlockComponents.ON_PLACE, new ShulkerBoxPlaceHandler())
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ContainerBlockHandlers.SHULKER_BOX);
    }

    private void configureSlab(CloudBlockRegistry registry, BlockType slabType, BlockType doubleSlabType) {
        registry.configure(slabType).set(BlockComponents.ON_PLACE, new SlabPlaceHandler(doubleSlabType));
    }

    private void configureStairs(CloudBlockRegistry registry) {
        for (BlockType type : VanillaBlockTags.values(BlockTags.STAIRS)) {
            registry.configure(type)
                    .set(BlockComponents.RESOLVE_PLACEMENT_STATE, StairBlockHandlers.RESOLVE_PLACEMENT_STATE)
                    .set(BlockComponents.ON_NEIGHBOUR_CHANGED, StairBlockHandlers.ON_NEIGHBOUR_CHANGED);
        }
    }

    private void configureStoneButton(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.GET_REDSTONE_SIGNAL, ButtonBlockHandlers.GET_REDSTONE_SIGNAL)
                .set(BlockComponents.GET_DIRECT_REDSTONE_SIGNAL, ButtonBlockHandlers.GET_DIRECT_REDSTONE_SIGNAL)
                .set(BlockComponents.BUTTON_PRESS_DURATION_TICKS, ButtonBlockHandlers.STONE_PRESS_TICKS)
                .set(BlockComponents.ON_PLACE, new ButtonPlaceHandler())
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ButtonBlockHandlers.USE)
                .set(BlockComponents.ON_TICK, ButtonBlockHandlers.ON_TICK)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, ButtonBlockHandlers.ON_NEIGHBOUR_CHANGED);
    }

    private void configureTorch(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.ON_PLACE, new TorchPlaceHandler())
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, TorchBlockHandlers.ON_NEIGHBOUR_CHANGED);
    }

    private void configureTrapdoor(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.RESOLVE_PLACEMENT_STATE, TrapdoorBlockHandlers.RESOLVE_PLACEMENT_STATE)
                .set(BlockComponents.CAN_BE_USED, TrapdoorBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, TrapdoorBlockHandlers.USE);
    }

    private void configureAquaticBlocks(CloudBlockRegistry registry) {
        registry.configure(FROG_SPAWN)
                .set(BlockComponents.CAN_SURVIVE, FrogspawnBlockHandlers.CAN_SURVIVE)
                .set(BlockComponents.ON_STATE_CHANGED, FrogspawnBlockHandlers.ON_STATE_CHANGED)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, FrogspawnBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_TICK, FrogspawnBlockHandlers.ON_TICK)
                .set(BlockComponents.ON_ENTITY_INSIDE, FrogspawnBlockHandlers.ON_ENTITY_INSIDE);

        registry.configure(KELP)
                .set(BlockComponents.RESOLVE_PLACEMENT_STATE, KelpBlockHandlers.RESOLVE_PLACEMENT_STATE)
                .set(BlockComponents.CAN_SURVIVE, KelpBlockHandlers.CAN_SURVIVE)
                .set(BlockComponents.CAN_RANDOM_TICK, true)
                .set(BlockComponents.ON_RANDOM_TICK, KelpBlockHandlers.ON_RANDOM_TICK)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, KelpBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_TICK, KelpBlockHandlers.ON_TICK)
                .set(BlockComponents.ON_REMOVE, KelpBlockHandlers.ON_REMOVE)
                .set(BlockComponents.FERTILIZE, KelpBlockHandlers.FERTILIZE)
                .set(BlockComponents.GET_LOOT, KelpBlockHandlers.GET_LOOT)
                .set(BlockComponents.GET_PICK_BLOCK, KelpBlockHandlers.GET_PICK_BLOCK);

        registry.configure(REEDS)
                .set(BlockComponents.CAN_SURVIVE, SugarCaneBlockHandlers.CAN_SURVIVE)
                .set(BlockComponents.CAN_RANDOM_TICK, true)
                .set(BlockComponents.ON_RANDOM_TICK, SugarCaneBlockHandlers.ON_RANDOM_TICK)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, SugarCaneBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_TICK, SugarCaneBlockHandlers.ON_TICK)
                .set(BlockComponents.FERTILIZE, SugarCaneBlockHandlers.FERTILIZE)
                .set(BlockComponents.GET_LOOT, SugarCaneBlockHandlers.GET_LOOT)
                .set(BlockComponents.GET_PICK_BLOCK, SugarCaneBlockHandlers.GET_PICK_BLOCK);

        registry.configure(SEAGRASS)
                .set(BlockComponents.CAN_SURVIVE, UnderwaterPlantBlockHandlers.CAN_SURVIVE)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, UnderwaterPlantBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_DESTROY, UnderwaterPlantBlockHandlers.ON_DESTROY)
                .set(BlockComponents.GET_LOOT, UnderwaterPlantBlockHandlers.GET_LOOT)
                .set(BlockComponents.GET_PICK_BLOCK, block -> ItemStack.from(SEAGRASS.getDefaultState()))
                .set(BlockComponents.FERTILIZE, UnderwaterPlantBlockHandlers.FERTILIZE);

        for (CoralFamily family : CORALS) {
            configureCoralDeath(registry, family.block(), family.deadBlock())
                    .set(BlockComponents.GET_LOOT, VanillaBlockLoot.silkTouchAlternative(family.block(), family.deadBlock()));

            for (BlockType type : List.of(family.plant(), family.deadPlant(), family.fan(), family.deadFan(), family.wallFan(), family.deadWallFan())) {
                BlockType itemBlock = type == family.wallFan() ? family.fan() : type == family.deadWallFan() ? family.deadFan() : type;
                registry.configure(type)
                        .set(BlockComponents.CAN_SURVIVE, CoralBlockHandlers.CAN_SURVIVE)
                        .set(BlockComponents.ON_NEIGHBOUR_CHANGED, VegetationBlockHandlers.checkSurvival(CoralBlockHandlers.CAN_SURVIVE))
                        .set(BlockComponents.GET_LOOT, VanillaBlockLoot.silkTouchOnly(itemBlock))
                        .set(BlockComponents.GET_PICK_BLOCK, block -> ItemStack.from(itemBlock.getDefaultState()));
            }

            configureCoralDeath(registry, family.plant(), family.deadPlant());
            configureCoralDeath(registry, family.fan(), family.deadFan());
            configureCoralDeath(registry, family.wallFan(), family.deadWallFan());

            for (BlockType type : List.of(family.fan(), family.wallFan())) {
                registry.configure(type).set(BlockComponents.RESOLVE_PLACEMENT_STATE, CoralBlockHandlers.fanPlacement(family.fan(), family.wallFan(), family.deadFan(), family.deadWallFan()));
            }

            for (BlockType type : List.of(family.deadFan(), family.deadWallFan())) {
                registry.configure(type).set(BlockComponents.RESOLVE_PLACEMENT_STATE, CoralBlockHandlers.fanPlacement(family.deadFan(), family.deadWallFan(), family.deadFan(), family.deadWallFan()));
            }
        }
    }

    private ComponentBuilder configureCoralDeath(CloudBlockRegistry registry, BlockType living, BlockType dead) {
        return registry.configure(living)
                .set(BlockComponents.ON_STATE_CHANGED, CoralBlockHandlers.ON_STATE_CHANGED)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, CoralBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.ON_TICK, CoralBlockHandlers.die(dead));
    }

    private void configureVegetation(CloudBlockRegistry registry) {
        for (BlockType type : SINGLE_PLANTS) {
            configureSinglePlant(registry, type);
        }

        registry.configure(CACTUS_FLOWER)
                .set(BlockComponents.CAN_SURVIVE, VegetationBlockHandlers.CACTUS_FLOWER_CAN_SURVIVE)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, VegetationBlockHandlers.checkSurvival(VegetationBlockHandlers.CACTUS_FLOWER_CAN_SURVIVE));

        registry.configure(SHORT_GRASS)
                .set(BlockComponents.FERTILIZE, VegetationBlockHandlers.growInto(TALL_GRASS))
                .set(BlockComponents.GET_LOOT, VanillaBlockLoot.grass(SHORT_GRASS));

        registry.configure(FERN)
                .set(BlockComponents.FERTILIZE, VegetationBlockHandlers.growInto(LARGE_FERN))
                .set(BlockComponents.GET_LOOT, VanillaBlockLoot.grass(FERN));

        registry.configure(SHORT_DRY_GRASS)
                .set(BlockComponents.FERTILIZE, VegetationBlockHandlers.replaceWith(TALL_DRY_GRASS));

        registry.configure(SWEET_BERRY_BUSH)
                .set(BlockComponents.FERTILIZE, VegetationBlockHandlers.growToAge(3));

        SurviveBlockHandler dryVegetationSurvival = VegetationBlockHandlers.supportedBy(BlockTags.SUPPORTS_DRY_VEGETATION);
        registry.configure(SHORT_DRY_GRASS)
                .set(BlockComponents.CAN_SURVIVE, dryVegetationSurvival)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, VegetationBlockHandlers.checkSurvival(dryVegetationSurvival));
        registry.configure(TALL_DRY_GRASS)
                .set(BlockComponents.CAN_SURVIVE, dryVegetationSurvival)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, VegetationBlockHandlers.checkSurvival(dryVegetationSurvival));
        registry.configure(TALL_DRY_GRASS)
                .set(BlockComponents.FERTILIZE, VegetationBlockHandlers.spread(SHORT_DRY_GRASS, BlockTags.SUPPORTS_DRY_VEGETATION));

        SurviveBlockHandler witherRoseSurvival = VegetationBlockHandlers.supportedBy(BlockTags.SUPPORTS_WITHER_ROSE);
        registry.configure(WITHER_ROSE)
                .set(BlockComponents.CAN_SURVIVE, witherRoseSurvival)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, VegetationBlockHandlers.checkSurvival(witherRoseSurvival))
                .set(BlockComponents.ON_ENTITY_INSIDE, VegetationBlockHandlers.WITHER_ROSE_ENTITY_INSIDE);
        registry.configure(OPEN_EYEBLOSSOM)
                .set(BlockComponents.ON_ENTITY_INSIDE, VegetationBlockHandlers.EYEBLOSSOM_ENTITY_INSIDE);
        registry.configure(CLOSED_EYEBLOSSOM)
                .set(BlockComponents.ON_ENTITY_INSIDE, VegetationBlockHandlers.EYEBLOSSOM_ENTITY_INSIDE);
        configureFlowerBed(registry, PINK_PETALS);
        configureFlowerBed(registry, WILDFLOWERS);
        registry.configure(GRASS_BLOCK)
                .set(BlockComponents.FERTILIZE, VegetationBlockHandlers.spreadGroundCover());

        for (BlockType type : DOUBLE_PLANTS) {
            configureDoublePlant(registry, type);
        }

        registry.configure(TALL_GRASS).set(BlockComponents.GET_LOOT, VanillaBlockLoot.tallGrass());
        registry.configure(LARGE_FERN).set(BlockComponents.GET_LOOT, VanillaBlockLoot.largeFern());

        for (BlockType type : RENEWABLE_TALL_FLOWERS) {
            registry.configure(type).set(BlockComponents.FERTILIZE, VegetationBlockHandlers.duplicate(type));
        }
    }

    private void configureWalls(CloudBlockRegistry registry) {
        for (BlockType type : VanillaBlockTags.values(BlockTags.WALLS)) {
            registry.configure(type)
                    .set(BlockComponents.RESOLVE_PLACEMENT_STATE, WallBlockHandlers.RESOLVE_PLACEMENT_STATE)
                    .set(BlockComponents.ON_NEIGHBOUR_CHANGED, WallBlockHandlers.ON_NEIGHBOUR_CHANGED);
        }
    }

    private void configureDoublePlant(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.CAN_SURVIVE, VegetationBlockHandlers.CAN_SURVIVE)
                .set(BlockComponents.ON_DESTROY, VegetationBlockHandlers.DOUBLE_PLANT_DESTROY)
                .set(BlockComponents.ON_PLACE, new DoublePlantPlaceHandler())
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, VegetationBlockHandlers.DOUBLE_PLANT_NEIGHBOUR_CHANGED);
    }

    private void configureFlowerBed(CloudBlockRegistry registry, BlockType type) {
        configureSinglePlant(registry, type);
        registry.configure(type)
                .set(BlockComponents.CAN_BE_REPLACED, VegetationBlockHandlers.FLOWER_BED_REPLACEABLE)
                .set(BlockComponents.RESOLVE_PLACEMENT_STATE, VegetationBlockHandlers.FLOWER_BED_PLACEMENT)
                .set(BlockComponents.FERTILIZE, VegetationBlockHandlers.growFlowerBed(type))
                .set(BlockComponents.GET_LOOT, VanillaBlockLoot.flowerBed(type));
    }

    private void configureSinglePlant(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.CAN_SURVIVE, VegetationBlockHandlers.CAN_SURVIVE)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, VegetationBlockHandlers.NEIGHBOUR_CHANGED);
    }

    private void configureUsable(CloudBlockRegistry registry, BlockType type, UseBlockHandler handler) {
        registry.configure(type)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, handler);
    }

    private void configureItemFrame(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.RESOLVE_PLACEMENT_STATE, ItemFrameBlockHandlers.RESOLVE_PLACEMENT_STATE)
                .set(BlockComponents.CAN_SURVIVE, ItemFrameBlockHandlers.CAN_SURVIVE)
                .set(BlockComponents.ON_PLACE, ItemFrameBlockHandlers.ON_PLACE)
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ItemFrameBlockHandlers.USE)
                .set(BlockComponents.ATTACK, ItemFrameBlockHandlers.ATTACK)
                .set(BlockComponents.ON_PROJECTILE_HIT, ItemFrameBlockHandlers.ON_PROJECTILE_HIT)
                .set(BlockComponents.ON_EXPLOSION_HIT, ItemFrameBlockHandlers.ON_EXPLOSION_HIT)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, ItemFrameBlockHandlers.ON_NEIGHBOUR_CHANGED)
                .set(BlockComponents.GET_LOOT, ItemFrameBlockHandlers.GET_LOOT)
                .set(BlockComponents.ON_REMOVE, ItemFrameBlockHandlers.ON_REMOVE)
                .set(BlockComponents.ON_DESTROY, ItemFrameBlockHandlers.ON_DESTROY);
    }

    private void configureCandleCake(CloudBlockRegistry registry, BlockType type, ItemType candle) {
        configureUsable(registry, type, CakeBlockHandlers.candleCake(candle));
        configureCakeSupport(registry, type);
        registry.configure(type)
                .set(BlockComponents.GET_IGNITED_STATE, BlockIgnitionHandlers.litState(BlockTraits.IS_LIT, true))
                .set(BlockComponents.GET_PICK_BLOCK, block -> ItemStack.from(ItemTypes.CAKE))
                .set(BlockComponents.GET_LOOT, (block, context) -> List.of(ItemStack.from(candle)));
    }

    private void configureCakeSupport(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.IS_SOLID_SUPPORT, state -> true)
                .set(BlockComponents.CAN_SURVIVE, CakeBlockHandlers.CAN_SURVIVE)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, VegetationBlockHandlers.checkSurvival(CakeBlockHandlers.CAN_SURVIVE));
    }

    private void configureWoodenButton(CloudBlockRegistry registry, BlockType type) {
        registry.configure(type)
                .set(BlockComponents.GET_REDSTONE_SIGNAL, ButtonBlockHandlers.GET_REDSTONE_SIGNAL)
                .set(BlockComponents.GET_DIRECT_REDSTONE_SIGNAL, ButtonBlockHandlers.GET_DIRECT_REDSTONE_SIGNAL)
                .set(BlockComponents.BUTTON_PRESS_DURATION_TICKS, ButtonBlockHandlers.WOODEN_PRESS_TICKS)
                .set(BlockComponents.ON_PLACE, new ButtonPlaceHandler())
                .set(BlockComponents.CAN_BE_USED, DefaultBlockHandlers.CAN_BE_USED)
                .set(BlockComponents.USE, ButtonBlockHandlers.USE)
                .set(BlockComponents.ON_TICK, ButtonBlockHandlers.ON_TICK)
                .set(BlockComponents.ON_NEIGHBOUR_CHANGED, ButtonBlockHandlers.ON_NEIGHBOUR_CHANGED);
    }

    private record CoralFamily(BlockType block, BlockType deadBlock, BlockType plant, BlockType deadPlant,
                               BlockType fan, BlockType deadFan, BlockType wallFan, BlockType deadWallFan) {
    }
}
