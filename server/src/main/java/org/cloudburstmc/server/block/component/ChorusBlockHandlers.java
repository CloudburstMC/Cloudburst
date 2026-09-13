package org.cloudburstmc.server.block.component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.entity.Projectile;
import org.cloudburstmc.api.event.block.BlockGrowEvent;
import org.cloudburstmc.api.event.block.BlockSpreadEvent;
import org.cloudburstmc.api.event.entity.EntityBlockChangeEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.Sound;

import java.util.List;
import java.util.random.RandomGenerator;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ChorusBlockHandlers {

    public static final SurviveBlockHandler PLANT_CAN_SURVIVE = ChorusBlockHandlers::canPlantSurvive;
    public static final SurviveBlockHandler FLOWER_CAN_SURVIVE = ChorusBlockHandlers::canFlowerSurvive;

    public static final BlockLootHandler PLANT_LOOT = (block, context) ->
            context.random().nextBoolean()
                    ? List.of(ItemStack.from(ItemTypes.CHORUS_FRUIT))
                    : List.of();

    public static final NeighborBlockHandler PLANT_NEIGHBOUR_CHANGED = (block, neighbor) ->
            block.getLevel().scheduleUpdate(block.getPosition(), 1);

    public static final NeighborBlockHandler FLOWER_NEIGHBOUR_CHANGED = (block, neighbor) -> {
        if (!neighbor.getPosition().equals(block.getPosition().add(0, 1, 0))) {
            block.getLevel().scheduleUpdate(block.getPosition(), 1);
        }
    };

    public static final TickBlockHandler ON_TICK = (block, random) -> {
        if (!block.requireComponent(BlockComponents.CAN_SURVIVE).execute(block)) {
            block.getLevel().breakBlock(block.getPosition(), ItemStack.EMPTY, null, true);
        }
    };

    public static final TickBlockHandler FLOWER_RANDOM_TICK = ChorusBlockHandlers::randomTickFlower;

    public static final EntityBlockHandler FLOWER_PROJECTILE_HIT = (block, entity) -> {
        if (!(entity instanceof Projectile projectile)) {
            return;
        }

        if (!(projectile.getShooter() instanceof Player)
                && !block.getLevel().getGameRules().get(GameRules.MOB_GRIEFING)) {
            return;
        }

        EntityBlockChangeEvent event = new EntityBlockChangeEvent(projectile, block, BlockStates.AIR);
        block.getLevel().getServer().getEventManager().fire(event);
        if (event.isCancelled()) {
            return;
        }

        block.getLevel().breakBlock(block.getPosition(), ItemStack.EMPTY, null, true);
    };

    private static boolean canPlantSurvive(Block block) {
        BlockState below = block.getSide(Direction.DOWN).getState();
        boolean blockedVertically = block.getSide(Direction.UP).getState().getType() != BlockTypes.AIR
                && below.getType() != BlockTypes.AIR;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            Block neighbor = block.getSide(direction);
            if (neighbor.getState().getType() != BlockTypes.CHORUS_PLANT) {
                continue;
            }

            if (blockedVertically) {
                return false;
            }

            BlockState neighborBelow = neighbor.getSide(Direction.DOWN).getState();
            if (neighborBelow.getType() == BlockTypes.CHORUS_PLANT || isPlantSupport(neighborBelow)) {
                return true;
            }
        }

        return below.getType() == BlockTypes.CHORUS_PLANT || isPlantSupport(below);
    }

    private static boolean canFlowerSurvive(Block block) {
        BlockState below = block.getSide(Direction.DOWN).getState();
        if (below.getType() == BlockTypes.CHORUS_PLANT || isFlowerSupport(below)) {
            return true;
        }

        if (below.getType() != BlockTypes.AIR) {
            return false;
        }

        boolean foundPlant = false;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState neighbor = block.getSide(direction).getState();
            if (neighbor.getType() == BlockTypes.CHORUS_PLANT) {
                if (foundPlant) {
                    return false;
                }
                foundPlant = true;
            } else if (neighbor.getType() != BlockTypes.AIR) {
                return false;
            }
        }

        return foundPlant;
    }

    private static boolean isFlowerSupport(BlockState state) {
        return state.is(BlockTags.SUPPORTS_CHORUS_FLOWER);
    }

    private static boolean isPlantSupport(BlockState state) {
        return state.is(BlockTags.SUPPORTS_CHORUS_PLANT);
    }

    private static void randomTickFlower(Block block, RandomGenerator random) {
        BlockState state = block.getState();
        int age = state.ensureTrait(BlockTraits.AGE);
        if (age >= 5 || block.getY() >= block.getLevel().getMaxHeight() - 1) {
            return;
        }

        Block above = block.getSide(Direction.UP);
        if (above.getState().getType() != BlockTypes.AIR) {
            return;
        }

        boolean growUp = false;
        boolean rootedPillar = false;
        BlockState below = block.getSide(Direction.DOWN).getState();
        if (isFlowerSupport(below)) {
            growUp = true;
        } else if (below.getType() == BlockTypes.CHORUS_PLANT) {
            int height = 1;
            while (height < 5 && block.getRelative(0, -height - 1, 0).getState().getType() == BlockTypes.CHORUS_PLANT) {
                height++;
            }

            rootedPillar = isFlowerSupport(block.getRelative(0, -height - 1, 0).getState());
            growUp = height < 2 || height <= random.nextInt(rootedPillar ? 5 : 4);
        } else if (below.getType() == BlockTypes.AIR) {
            growUp = true;
        }

        if (growUp && horizontalNeighborsAreAir(above, null)
                && above.getSide(Direction.UP).getState().getType() == BlockTypes.AIR) {
            if (spread(block, above, BlockStates.CHORUS_FLOWER.withTrait(BlockTraits.AGE, age))) {
                block.set(BlockStates.CHORUS_PLANT, false, true);
                ((CloudLevel) block.getLevel()).addSound(above.getPosition().toFloat(), Sound.BLOCK_CHORUSFLOWER_GROW);
            }
            return;
        }

        if (age < 4) {
            int attempts = random.nextInt(4) + (rootedPillar ? 1 : 0);
            boolean branched = false;
            for (int attempt = 0; attempt < attempts; attempt++) {
                Direction direction = Direction.Plane.HORIZONTAL.random(random);
                Block target = block.getSide(direction);
                if (target.getState().getType() == BlockTypes.AIR
                        && target.getSide(Direction.DOWN).getState().getType() == BlockTypes.AIR
                        && horizontalNeighborsAreAir(target, direction.getOpposite())
                        && spread(block, target, BlockStates.CHORUS_FLOWER.withTrait(BlockTraits.AGE, age + 1))) {
                    branched = true;
                }
            }

            if (branched) {
                block.set(BlockStates.CHORUS_PLANT, false, true);
                return;
            }
        }

        if (grow(block, state.withTrait(BlockTraits.AGE, 5))) {
            ((CloudLevel) block.getLevel()).addSound(block.getPosition().toFloat(), Sound.BLOCK_CHORUSFLOWER_DEATH);
        }
    }

    private static boolean horizontalNeighborsAreAir(Block block, Direction ignored) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (direction != ignored && block.getSide(direction).getState().getType() != BlockTypes.AIR) {
                return false;
            }
        }

        return true;
    }

    private static boolean grow(Block block, BlockState state) {
        BlockGrowEvent event = new BlockGrowEvent(block, state);
        block.getLevel().getServer().getEventManager().fire(event);
        if (event.isCancelled()) {
            return false;
        }

        block.set(event.getNewState(), false, true);
        return true;
    }

    private static boolean spread(Block source, Block target, BlockState state) {
        BlockSpreadEvent event = new BlockSpreadEvent(target, source, state);
        target.getLevel().getServer().getEventManager().fire(event);
        if (event.isCancelled()) {
            return false;
        }

        target.set(event.getNewState(), false, true);
        return true;
    }
}
