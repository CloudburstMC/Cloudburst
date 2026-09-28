package org.cloudburstmc.server.block.component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.event.block.BlockFadeEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.block.BlockLayerRules;
import org.cloudburstmc.server.block.BlockLayers;
import org.cloudburstmc.server.block.util.BlockSupport;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.biome.CloudBiome;
import org.cloudburstmc.server.registry.CloudBiomeRegistry;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SnowLayerBlockHandlers {

    private static final int MAX_HEIGHT = 7;

    public static final ReplaceBlockHandler CAN_BE_REPLACED = (block, replacement, player, face, clickPosition) -> {
        BlockState state = block.getState();
        int height = state.ensureTrait(BlockTraits.HEIGHT);
        if (replacement.getType() != state.getType()) {
            return height == 0;
        }

        return height < MAX_HEIGHT && (face == Direction.UP || face != Direction.DOWN && clickPosition.getY() > (height + 1) / 8f);
    };

    public static final SurviveBlockHandler CAN_SURVIVE = block -> {
        BlockState below = block.getSide(Direction.DOWN).getState();
        return (below.getType() == block.getState().getType()
                && below.ensureTrait(BlockTraits.HEIGHT) == MAX_HEIGHT)
                || BlockSupport.isFaceSturdy(block.getLevel(), block.getSide(Direction.DOWN).getPosition(), Direction.UP);
    };

    public static final TickBlockHandler ON_RANDOM_TICK = (block, random) -> {
        CloudLevel level = (CloudLevel) block.getLevel();
        CloudBiome biome = CloudBiomeRegistry.get().getBiome(level.getBiomeId(block.getX(), block.getY(), block.getZ()));

        int light = biome != null
                && biome.temperatureAt(block.getX(), block.getY(), block.getZ(), level.getSeaLevel()) <= 0.25
                ? level.getBlockLightAt(block.getX(), block.getY(), block.getZ())
                : level.getFullLight(block.getPosition());
        if (light <= 11) {
            return;
        }

        BlockFadeEvent event = new BlockFadeEvent(block, BlockStates.AIR);
        level.getServer().getEventManager().fire(event);
        if (!event.isCancelled()) {
            block.set(event.getNewState(), false, true);
        }
    };

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> {
        BlockState state = withCoveredState(block);
        if (state != block.getState()) {
            block.set(state, false, true);
            block = block.refresh();
        }

        FallingBlockHandlers.ON_NEIGHBOUR_CHANGED.execute(block, neighbor);
    };

    public static final PlacementStateHandler RESOLVE_PLACEMENT_STATE =
            (state, block, player, face, clickPosition) -> {
                BlockState placed = block.getState().getType() == state.getType()
                        ? block.getState().incrementTrait(BlockTraits.HEIGHT)
                        : state.withTrait(BlockTraits.HEIGHT, 0);
                return BlockLayerRules.normalizeSnowCover(new BlockLayers(placed, block.getSecondaryState()), block.getRelativeState(0, -1, 0)).primary();
            };

    public static ItemStack getResource(BlockState state) {
        return ItemStack.builder()
                .itemType(ItemTypes.SNOWBALL)
                .amount(Math.max(1, (state.ensureTrait(BlockTraits.HEIGHT) + 1) / 2))
                .build();
    }

    private static BlockState withCoveredState(Block block) {
        return BlockLayerRules.normalizeSnowCover(new BlockLayers(block.getState(), block.getSecondaryState()), block.getRelativeState(0, -1, 0)).primary();
    }
}
