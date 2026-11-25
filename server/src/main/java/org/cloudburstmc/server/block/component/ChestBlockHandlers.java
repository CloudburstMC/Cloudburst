package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.entity.passive.Cat;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.sound.SoundPlayback;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.BoundingBox;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.blockentity.BaseBlockEntity;
import org.cloudburstmc.server.blockentity.ChestBlockEntity;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.registry.VanillaChestBlocks;
import org.cloudburstmc.server.registry.VanillaCopperBlocks;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

@UtilityClass
public class ChestBlockHandlers {

    public static final ComplexBlockHandler CHEST_ADDED = ChestBlockHandlers::chestAdded;

    public static final PlacementStateHandler CHEST_PLACEMENT = ChestBlockHandlers::resolvePlacementState;

    public static final PlaceBlockHandler PLACE_CHEST = (state, player, position, face, _) -> placeChest(state, player, position, face);

    public static final ComplexBlockHandler CHEST_REMOVED = block -> {
        BlockType removed = block.getState().getType();
        BlockType replacement = block.refresh().getState().getType();
        if (!(VanillaChestBlocks.isCopper(removed) && VanillaChestBlocks.isCopper(replacement)) && blockEntity(block) instanceof ChestBlockEntity chest) {
            chest.unpair();
            chest.close();
        }
    };

    public static final ComplexBlockHandler ENDER_CHEST_ADDED = block -> {
        if (block.getLevel().getBlockEntity(block.getPosition()) == null) {
            CloudBlockEntityRegistry.get().newEntity(BlockEntityTypes.ENDER_CHEST, block);
        }
    };

    public static final BlockLootHandler ENDER_CHEST_LOOT = (_, context) -> List.of(
            context.enchantmentLevel(EnchantmentTypes.SILK_TOUCH) > 0
                    ? ItemStack.from(ItemTypes.ENDER_CHEST)
                    : ItemStack.from(ItemTypes.OBSIDIAN, 8)
    );

    public static final ComparatorOutputHandler GET_COMPARATOR_OUTPUT = ChestBlockHandlers::getComparatorOutput;

    public static final RedstoneSignalHandler GET_TRAPPED_SIGNAL = (block, _) -> blockEntity(block) instanceof ChestBlockEntity chest ? chest.getRedstoneSignal() : 0;

    public static final RedstoneSignalHandler GET_DIRECT_TRAPPED_SIGNAL = (block, face) -> face == Direction.UP ? GET_TRAPPED_SIGNAL.execute(block, face) : 0;

    public static final TickBlockHandler WEATHER_CHEST = ChestBlockHandlers::weatherChest;

    public static boolean canJoin(BlockState state, BlockState other) {
        if (!canJoinTypes(state.getType(), other.getType())
                || !state.getTraits().containsKey(BlockTraits.CARDINAL_DIRECTION)
                || !other.getTraits().containsKey(BlockTraits.CARDINAL_DIRECTION)) {
            return false;
        }

        return state.ensureTrait(BlockTraits.CARDINAL_DIRECTION) == other.ensureTrait(BlockTraits.CARDINAL_DIRECTION);
    }

    public static boolean isBlocked(Block block) {
        if (isBlockedByBlock(block)) {
            return true;
        }

        Vector3i pos = block.getPosition();
        BoundingBox above = new BoundingBox(pos.getX(), pos.getY() + 1, pos.getZ(),
                pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1);
        return !block.getLevel()
                .getNearbyEntities(above, entity -> entity instanceof Cat cat && cat.isSitting())
                .isEmpty();
    }

    public static boolean isBlockedByBlock(Block block) {
        Block above = block.getSide(Direction.UP);
        return above.requireComponent(BlockComponents.CONDUCTS_REDSTONE).execute(above.getState());
    }

    public static void playLidSound(BaseBlockEntity entity, boolean open) {
        BlockType type = entity.getBlockState().getType();
        Vector3f position = entity.getPosition().toFloat().add(0.5f, 0.5f, 0.5f);
        if (entity instanceof ChestBlockEntity chest) {
            ChestBlockEntity pair = chest.getPair();
            if (pair != null) {
                if (!chest.isPairLead()) {
                    return;
                }
                position = position.add(pair.getPosition().sub(entity.getPosition()).toFloat().mul(0.5f));
            }
        }

        float pitch = ThreadLocalRandom.current().nextFloat(0.9f, 1.0f);
        entity.getLevel().playSound(position, new SoundPlayback(VanillaChestBlocks.lidSound(type, open), 0.5f, pitch));
    }

    private static void chestAdded(Block block) {
        BlockEntity entity = blockEntity(block);
        if (entity == null) {
            entity = CloudBlockEntityRegistry.get().newEntity(BlockEntityTypes.CHEST, block);
        }

        if (!(entity instanceof ChestBlockEntity chest)) {
            return;
        }

        BlockType type = block.getState().getType();
        ChestBlockEntity pair = chest.getPair();
        if (pair != null && VanillaChestBlocks.isCopper(type) && block.getLevel() instanceof CloudLevel level) {
            applyType(pair, type);
            level.queueBlockEntitySync(chest);
            level.queueBlockEntitySync(pair);
        }
    }

    private static BlockState resolvePlacementState(BlockState state, Block block, Player player, Direction face, Vector3f clickPosition) {
        BlockState placement = DefaultPlacementStateHandler.INSTANCE.execute(state, block, player, face, clickPosition);
        if (player == null || !player.isSneaking() || !face.getAxis().isHorizontal() || !(block.getLevel() instanceof CloudLevel level)) {
            return placement;
        }

        Vector3i neighborPosition = face.getOpposite().relative(block.getPosition());
        if (!(level.getLoadedBlockEntity(neighborPosition) instanceof ChestBlockEntity adjacent) || adjacent.isPaired()) {
            return placement;
        }

        BlockState neighbor = adjacent.getBlockState();
        Direction facing = neighbor.ensureTrait(BlockTraits.CARDINAL_DIRECTION).toDirection();
        if (facing.getAxis() != face.getAxis() && canJoinTypes(state.getType(), neighbor.getType())) {
            return placement.withTrait(BlockTraits.CARDINAL_DIRECTION, facing.getCardinalDirection());
        }

        return placement;
    }

    private static boolean placeChest(BlockState state, Player player, Vector3i position, Direction face) {
        if (player == null || !player.getLevel().setBlockState(position, state, true, true)) {
            return false;
        }

        if (player.getLevel() instanceof CloudLevel level && level.getLoadedBlockEntity(position) instanceof ChestBlockEntity chest) {
            Direction facing = state.ensureTrait(BlockTraits.CARDINAL_DIRECTION).toDirection();
            if (!player.isSneaking()) {
                if (!pairAt(level, chest, facing.rotateClockwise())) {
                    pairAt(level, chest, facing.rotateCounterClockwise());
                }
            } else if (face.getAxis().isHorizontal() && face.getAxis() != facing.getAxis()) {
                pairAt(level, chest, face.getOpposite());
            }
        }

        return true;
    }

    private static boolean pairAt(CloudLevel level, ChestBlockEntity chest, Direction direction) {
        if (!(level.getLoadedBlockEntity(direction.relative(chest.getPosition())) instanceof ChestBlockEntity adjacent) || !chest.pairWith(adjacent)) {
            return false;
        }

        BlockType type = chest.getBlockState().getType();
        if (VanillaChestBlocks.isCopper(type)) {
            BlockType preferred = preferredCopperType(type, adjacent.getBlockState().getType());
            applyType(chest, preferred);
            applyType(adjacent, preferred);
        }

        return true;
    }

    private static boolean canJoinTypes(BlockType type, BlockType other) {
        return type == other || (VanillaChestBlocks.isCopper(type) && VanillaChestBlocks.isCopper(other));
    }

    private static int getComparatorOutput(Block block) {
        if (!(blockEntity(block) instanceof ChestBlockEntity chest) || chest.isBlocked()) {
            return 0;
        }

        CloudContainer inventory = chest.getContainer();
        double fullness = 0;
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (!item.isEmpty()) {
                int stackLimit = CloudItemRegistry.get()
                        .requireComponent(item.getType(), ItemBehaviors.GET_MAX_STACK_SIZE)
                        .execute(item);
                int limit = Math.min(inventory.getMaxStackSize(slot), stackLimit);
                if (limit > 0) {
                    fullness += (double) item.getCount() / limit;
                }
            }
        }

        return fullness == 0 ? 0 : Math.min(15, 1 + (int) (14 * fullness / inventory.size()));
    }

    private static BlockEntity blockEntity(Block block) {
        return block.getLevel().getBlockEntity(block.getPosition());
    }

    private static void applyType(ChestBlockEntity chest, BlockType type) {
        BlockState state = chest.getBlockState();
        if (state.getType() != type) {
            chest.getBlock().set(type.getDefaultState().copyTraits(state), false, true);
        }
    }

    private static BlockType preferredCopperType(BlockType first, BlockType second) {
        BlockType firstUnwaxed = VanillaCopperBlocks.unwaxed(first);
        BlockType secondUnwaxed = VanillaCopperBlocks.unwaxed(second);
        BlockType firstBase = Objects.requireNonNullElse(firstUnwaxed, first);
        BlockType secondBase = Objects.requireNonNullElse(secondUnwaxed, second);

        boolean useFirst = VanillaCopperBlocks.weatheringStage(firstBase) <= VanillaCopperBlocks.weatheringStage(secondBase);
        if (firstUnwaxed != null && secondUnwaxed != null) {
            return useFirst ? first : second;
        }

        return useFirst ? firstBase : secondBase;
    }

    private static void weatherChest(Block block, RandomGenerator random) {
        if (!(blockEntity(block) instanceof ChestBlockEntity chest) || chest.hasViewers()) {
            return;
        }

        if (chest.isPaired()) {
            ChestBlockEntity pair = chest.getPair();
            if (chest.isPairLead() || pair == null || pair.hasViewers()) {
                return;
            }
        }

        CopperWeatheringBlockHandlers.ON_RANDOM_TICK.execute(block, random);
    }
}
