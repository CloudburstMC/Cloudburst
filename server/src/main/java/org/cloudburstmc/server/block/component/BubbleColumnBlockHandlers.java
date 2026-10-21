package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.EntityInsideBlockHandler;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.particle.ParticleTypes;
import org.cloudburstmc.api.util.CollisionContext;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.entity.vehicle.EntityBoat;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public class BubbleColumnBlockHandlers {

    private static final int CHECK_DELAY = 5;
    private static final int FORMATION_DELAY = 20;

    public static final EntityInsideBlockHandler ON_ENTITY_INSIDE = (block, entity, precise) -> {
        if (!precise) {
            return;
        }

        boolean dragDown = block.getState().ensureTrait(BlockTraits.HAS_DRAG_DOWN);
        Block above = block.getRelative(0, 1, 0);
        boolean atSurface = above.getCollisionShape(CollisionContext.of(entity)).isEmpty() && above.getLiquid().isEmpty();
        if (atSurface) {
            splash(block);
        }

        if (atSurface && entity instanceof EntityBoat boat) {
            boat.onAboveBubbleColumn(dragDown);
            return;
        }

        Vector3f motion = entity.getMotion();
        float y = dragDown ? Math.max(atSurface ? -0.9f : -0.3f, motion.getY() - 0.03f) : Math.min(atSurface ? 1.8f : 0.7f, motion.getY() + (atSurface ? 0.1f : 0.06f));
        Vector3f nextMotion = Vector3f.from(motion.getX(), y, motion.getZ());
        if (entity instanceof CloudPlayer player) {
            player.setPredictedMotion(nextMotion);
        } else {
            entity.setMotion(nextMotion);
        }

        if (!atSurface) {
            entity.resetFallDistance();
        }
    };

    public static void schedule(Block block) {
        block.getLevel().scheduleUpdate(block.getPosition(), CHECK_DELAY);
    }

    public static void scheduleFormation(Block block) {
        if (cannotOccupy(block)) {
            return;
        }

        Block below = block.getLevel().getLoadedBlock(Direction.DOWN.relative(block.getPosition()));
        if (below != null && direction(below.getState()) != null) {
            block.getLevel().scheduleUpdate(block.getPosition(), below.getState().getType() == BlockTypes.BUBBLE_COLUMN ? CHECK_DELAY : FORMATION_DELAY);
        }
    }

    public static void update(Block block) {
        update(block.getLevel(), block.getPosition());
    }

    private static void update(Level level, Vector3i start) {
        Block below = level.getLoadedBlock(Direction.DOWN.relative(start));
        if (below == null) {
            return;
        }

        Boolean dragDown = direction(below.getState());
        level.batchBlockUpdates(() -> {
            for (Vector3i position = start; position.getY() < level.getMaxHeight(); position = Direction.UP.relative(position)) {
                Block current = level.getLoadedBlock(position);
                if (current == null || cannotOccupy(current)) {
                    break;
                }

                if (dragDown == null && current.getState().getType() != BlockTypes.BUBBLE_COLUMN) {
                    break;
                }

                BlockState state = dragDown == null ? BlockStates.WATER : BlockTypes.BUBBLE_COLUMN.getDefaultState().withTrait(BlockTraits.HAS_DRAG_DOWN, dragDown);
                if (state != current.getState() && !level.setBlockState(position, state)) {
                    break;
                }
            }
        });
    }

    private static @Nullable Boolean direction(BlockState below) {
        if (below.getType() == BlockTypes.BUBBLE_COLUMN) {
            return below.ensureTrait(BlockTraits.HAS_DRAG_DOWN);
        }

        if (below.is(BlockTags.ENABLES_BUBBLE_COLUMN_DRAG_DOWN)) {
            return true;
        }

        return below.is(BlockTags.ENABLES_BUBBLE_COLUMN_PUSH_UP) ? false : null;
    }

    private static boolean cannotOccupy(Block block) {
        if (block.getState().getType() == BlockTypes.BUBBLE_COLUMN) {
            return false;
        }

        LiquidState water = block.getLiquid();
        return !block.getState().getType().isLiquid() || !water.isSource() || !water.getType().isSameFamily(LiquidTypes.WATER);
    }

    private static void splash(Block block) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 2; i++) {
            block.getLevel().spawnParticle(ParticleTypes.WATER_SPLASH, block.getPosition().toFloat().add(random.nextFloat(), 1, random.nextFloat()));
            block.getLevel().spawnParticle(ParticleTypes.BUBBLE, block.getPosition().toFloat().add(random.nextFloat(), 1, random.nextFloat()));
        }
    }
}
