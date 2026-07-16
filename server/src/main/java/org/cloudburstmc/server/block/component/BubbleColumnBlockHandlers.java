package org.cloudburstmc.server.block.component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.EntityInsideBlockHandler;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BubbleColumnBlockHandlers {

    public static final EntityInsideBlockHandler ON_ENTITY_INSIDE = (block, entity, precise) -> {
        if (!precise) {
            return;
        }

        boolean dragDown = block.getState().ensureTrait(BlockTraits.HAS_DRAG_DOWN);
        Block above = block.up();
        boolean atSurface = above.getState().getCollisionShape().isEmpty() && above.getLiquid().isEmpty();
        Vector3f motion = entity.getMotion();
        float y = dragDown
                ? Math.max(atSurface ? -0.9f : -0.3f, motion.getY() - 0.03f)
                : Math.min(atSurface ? 1.8f : 0.7f, motion.getY() + (atSurface ? 0.1f : 0.06f));
        entity.setMotion(Vector3f.from(motion.getX(), y, motion.getZ()));

        if (!atSurface) {
            entity.resetFallDistance();
        }
    };

    public static void updateAbove(Block block) {
        update((CloudLevel) block.getLevel(), Direction.UP.relative(block.getPosition()));
    }

    public static DefaultBlockPlaceHandler supportPlacement(CloudBlockRegistry registry) {
        return new SupportPlaceHandler(registry);
    }

    public static void update(Block block) {
        update((CloudLevel) block.getLevel(), block.getPosition());
    }

    private static void update(CloudLevel level, Vector3i start) {
        Vector3i position = start;
        Boolean dragDown = direction(level.getBlockState(Direction.DOWN.relative(position)));
        while (!level.isOutsideBuildHeight(position.getY())) {
            BlockState current = level.getBlockState(position);
            if (dragDown == null) {
                if (current.getType() != BlockTypes.BUBBLE_COLUMN || restoreWaterFailed(level, position)) {
                    return;
                }
            } else if (current.getType() == BlockTypes.BUBBLE_COLUMN) {
                LiquidState water = level.getBlock(position).getLiquid();
                if (!isSourceWater(water)) {
                    if (restoreWaterFailed(level, position)) {
                        return;
                    }

                    dragDown = null;
                } else if (current.ensureTrait(BlockTraits.HAS_DRAG_DOWN) != dragDown) {
                    if (!level.setBlockState(position, current.withTrait(BlockTraits.HAS_DRAG_DOWN, dragDown))) {
                        return;
                    }
                }
            } else {
                LiquidState water = level.getBlock(position).getLiquid();
                if (!isSourceWater(water)) {
                    return;
                }

                BlockState bubble = BlockTypes.BUBBLE_COLUMN.getDefaultState().withTrait(BlockTraits.HAS_DRAG_DOWN, dragDown);
                if (!level.setBlockState(position, bubble)) {
                    return;
                }
            }

            position = Direction.UP.relative(position);
        }
    }

    private static Boolean direction(BlockState below) {
        if (below.getType() == BlockTypes.BUBBLE_COLUMN) {
            return below.ensureTrait(BlockTraits.HAS_DRAG_DOWN);
        }

        if (below.getType() == BlockTypes.MAGMA) {
            return true;
        }

        if (below.getType() == BlockTypes.SOUL_SAND) {
            return false;
        }

        return null;
    }

    private static boolean isSourceWater(LiquidState liquid) {
        return liquid.isSource() && liquid.getType().isSameFamily(LiquidTypes.WATER);
    }

    private static boolean restoreWaterFailed(CloudLevel level, Vector3i position) {
        LiquidState water = level.getBlock(position).getLiquid();
        BlockState state = isSourceWater(water) ? LiquidStateAccess.blockState(water) : BlockStates.WATER;
        return !level.setBlockState(position, state);
    }

    private static final class SupportPlaceHandler extends DefaultBlockPlaceHandler {

        private SupportPlaceHandler(CloudBlockRegistry registry) {
            super(registry);
        }

        @Override
        public boolean execute(BlockState state, Player player, Vector3i position, Direction face, Vector3f clickPosition) {
            if (!super.execute(state, player, position, face, clickPosition)) {
                return false;
            }

            updateAbove(player.getLevel().getBlock(position));
            return true;
        }
    }
}
