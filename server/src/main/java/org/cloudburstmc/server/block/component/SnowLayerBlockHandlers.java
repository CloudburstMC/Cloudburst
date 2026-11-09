package org.cloudburstmc.server.block.component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.event.block.BlockFadeEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.api.util.CollisionContext;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.VoxelShape;
import org.cloudburstmc.server.block.BlockLayerRules;
import org.cloudburstmc.server.block.BlockLayers;
import org.cloudburstmc.server.level.biome.CloudBiome;
import org.cloudburstmc.server.level.collision.CloudVoxelShapes;
import org.cloudburstmc.server.registry.CloudBiomeRegistry;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SnowLayerBlockHandlers {

    private static final int MAX_HEIGHT = 7;
    private static final int MELTING_BLOCK_LIGHT = 12;

    public static final ReplaceBlockHandler CAN_BE_REPLACED = (block, replacement, player, face, clickPosition) -> {
        BlockState state = block.getState();
        int height = state.ensureTrait(BlockTraits.HEIGHT);

        if (replacement.getType() != state.getType()) {
            return isSingleLayer(state);
        }

        return height < MAX_HEIGHT && (face == Direction.UP || face != Direction.DOWN && clickPosition.getY() > (height + 1) / 8f);
    };

    public static final BooleanBlockStateHandler CAN_BE_REPLACED_BY_FALLING_BLOCK = SnowLayerBlockHandlers::isSingleLayer;

    public static final SurviveBlockHandler CAN_SURVIVE = block -> {
        Block support = block.getSide(Direction.DOWN);
        Level level = block.getLevel();

        return canBeSupportedBy(
                level.getServer().getBlockRegistry(),
                support.getState(),
                BlockShapeContext.at(level, support.getPosition())
        );
    };

    public static final BooleanBlockHandler IS_FREE_TO_FALL = block -> !block.requireComponent(BlockComponents.CAN_SURVIVE).execute(block);

    public static final FallingBlockDropHandler FALLING_BREAK_DROP = state -> ItemStack.from(ItemTypes.SNOWBALL);

    public static final BlockLootHandler GET_LOOT = (block, context) -> {
        int height = block.getState().ensureTrait(BlockTraits.HEIGHT);
        int layers = height + 1;

        if (context.enchantmentLevel(EnchantmentTypes.SILK_TOUCH) > 0) {
            return List.of(height == MAX_HEIGHT
                    ? ItemStack.from(ItemTypes.SNOW)
                    : ItemStack.from(ItemTypes.SNOW_LAYER, layers));
        }

        return List.of(ItemStack.from(ItemTypes.SNOWBALL, Math.max(1, layers / 2)));
    };

    public static final TickBlockHandler ON_RANDOM_TICK = (block, random) -> {
        if (block.getBlockLight() < MELTING_BLOCK_LIGHT && !isSnowlessBiome(block)) {
            return;
        }

        BlockState state = block.getState();
        int height = state.ensureTrait(BlockTraits.HEIGHT);
        BlockState melted = height == 0 ? BlockStates.AIR : state.withTrait(BlockTraits.HEIGHT, height - 1);

        BlockFadeEvent event = new BlockFadeEvent(block, melted);
        block.getLevel().getServer().getEventManager().fire(event);

        if (!event.isCancelled() && block.refresh().getState() == state) {
            block.set(event.getNewState(), false, true);
        }
    };

    public static final FallingBlockPlacementHandler RESOLVE_FALLING_PLACEMENT = SnowLayerBlockHandlers::resolveFallingPlacement;

    public static final PlacementStateHandler RESOLVE_PLACEMENT_STATE =
            (state, block, player, face, clickPosition) -> {
                BlockState placed = block.getState().getType() == state.getType()
                        ? block.getState().incrementTrait(BlockTraits.HEIGHT)
                        : state.withTrait(BlockTraits.HEIGHT, 0);

                return BlockLayerRules.normalizeSnowCover(
                        new BlockLayers(placed, block.getSecondaryState()),
                        block.getRelativeState(0, -1, 0)
                ).primary();
            };

    public static boolean canBeSupportedBy(BlockRegistry registry, BlockState support, BlockShapeContext context) {
        if (support.is(BlockTags.CANNOT_SUPPORT_SNOW_LAYER)) {
            return false;
        }

        if (support.is(BlockTags.SUPPORT_OVERRIDE_SNOW_LAYER)
                || support.getType() == BlockTypes.SNOW_LAYER && support.ensureTrait(BlockTraits.HEIGHT) == MAX_HEIGHT) {
            return true;
        }

        VoxelShape shape = registry.requireComponent(support.getType(), BlockComponents.GET_COLLISION_SHAPE)
                .execute(support, context, CollisionContext.empty());
        return shape.getFaceShape(Direction.UP).covers(CloudVoxelShapes.block());
    }

    public static NeighborBlockHandler withSnowCoverUpdates(NeighborBlockHandler handler) {
        return (block, neighbor) -> {
            BlockState state = withCoveredState(block);
            if (state != block.getState()) {
                block.set(state, false, true);
                block = block.refresh();
            }

            handler.execute(block, neighbor);
        };
    }

    private static List<BlockChange> resolveFallingPlacement(BlockState falling, Block target) {
        BlockState existing = target.getState();
        if (existing.getType() != falling.getType()) {
            return FallingBlockHandlers.RESOLVE_PLACEMENT.execute(falling, target);
        }

        int height = existing.ensureTrait(BlockTraits.HEIGHT);
        if (height == MAX_HEIGHT) {
            if (target.getPosition().getY() + 1 >= target.getLevel().getMaxHeight()) {
                return List.of();
            }

            target = target.getSide(Direction.UP);
            existing = target.getState();
            if (existing.getType() != falling.getType()) {
                return FallingBlockHandlers.RESOLVE_PLACEMENT.execute(falling, target);
            }

            height = existing.ensureTrait(BlockTraits.HEIGHT);
            if (height == MAX_HEIGHT) {
                return List.of();
            }
        }

        int availableLayers = MAX_HEIGHT - height;
        int fallingLayers = falling.ensureTrait(BlockTraits.HEIGHT) + 1;
        BlockState combined = existing.withTrait(BlockTraits.HEIGHT, height + Math.min(availableLayers, fallingLayers));
        if (FallingBlockHandlers.isUnstableOnLanding(combined, target)) {
            return List.of();
        }

        BlockChange stack = new BlockChange(target, combined);
        if (fallingLayers <= availableLayers) {
            return List.of(stack);
        }

        if (target.getPosition().getY() + 1 >= target.getLevel().getMaxHeight()) {
            return List.of();
        }

        Block above = target.getSide(Direction.UP);
        if (above.getState() != BlockStates.AIR) {
            return List.of();
        }

        BlockState overflow = falling.withTrait(BlockTraits.HEIGHT, fallingLayers - availableLayers - 1);
        return List.of(stack, new BlockChange(above, overflow));
    }

    private static boolean isSingleLayer(BlockState state) {
        return state.ensureTrait(BlockTraits.HEIGHT) == 0;
    }

    private static boolean isSnowlessBiome(Block block) {
        Level level = block.getLevel();
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        CloudBiome biome = CloudBiomeRegistry.get().getBiome(level.getBiomeId(x, y, z));

        return biome != null && (!biome.hasPrecipitation() || !biome.coldEnoughToSnow(x, y, z, level.getSeaLevel()));
    }

    private static BlockState withCoveredState(Block block) {
        return BlockLayerRules.normalizeSnowCover(
                new BlockLayers(block.getState(), block.getSecondaryState()),
                block.getRelativeState(0, -1, 0)
        ).primary();
    }
}
