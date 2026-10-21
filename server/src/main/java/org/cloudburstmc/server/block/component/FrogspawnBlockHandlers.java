package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.misc.FallingBlock;
import org.cloudburstmc.api.entity.passive.Tadpole;
import org.cloudburstmc.api.event.block.BlockFadeEvent;
import org.cloudburstmc.api.event.entity.CreatureSpawnEvent;
import org.cloudburstmc.api.event.entity.CreatureSpawnReason;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

@UtilityClass
public class FrogspawnBlockHandlers {

    private static final int MIN_HATCH_DELAY = 3600;
    private static final int MAX_HATCH_DELAY = 12000;

    public static final SurviveBlockHandler CAN_SURVIVE = block -> {
        if (!block.getLiquid().isEmpty()) {
            return false;
        }

        Block below = block.getLevel().getLoadedBlock(block.getPosition().add(0, -1, 0));
        if (below == null) {
            return false;
        }

        LiquidState liquid = below.getLiquid();
        return liquid.isSource() && liquid.getType().isSameFamily(LiquidTypes.WATER);
    };

    public static final ComplexBlockHandler ON_STATE_CHANGED = block -> {
        if (!CAN_SURVIVE.execute(block)) {
            destroy(block);
            return;
        }

        block.getLevel().scheduleUpdate(block.getPosition(), ThreadLocalRandom.current().nextInt(MIN_HATCH_DELAY, MAX_HATCH_DELAY));
    };

    public static final NeighborBlockHandler ON_NEIGHBOUR_CHANGED = (block, neighbor) -> {
        if (!CAN_SURVIVE.execute(block)) {
            destroy(block);
        }
    };

    public static final TickBlockHandler ON_TICK = (block, random) -> {
        if (!CAN_SURVIVE.execute(block)) {
            destroy(block);
            return;
        }

        hatch(block, random == null ? ThreadLocalRandom.current() : random);
    };

    public static final EntityInsideBlockHandler ON_ENTITY_INSIDE = (block, entity, precise) -> {
        if (entity instanceof FallingBlock && block.getLevel().getBlockState(block.getPosition()) == block.getState()) {
            destroy(block);
        }
    };

    private static void hatch(Block block, RandomGenerator random) {
        Level level = block.getLevel();
        BlockFadeEvent event = new BlockFadeEvent(block, BlockStates.AIR);
        level.getServer().getEventManager().fire(event);
        if (event.isCancelled() || level.getBlockState(block.getPosition()) != block.getState() || !CAN_SURVIVE.execute(block.refresh())) {
            return;
        }

        BlockState replacement = event.getNewState();
        if (!level.setBlockState(block.getPosition(), replacement) || replacement != BlockStates.AIR) {
            return;
        }

        level.playBlockBreakEffect(block.getPosition(), block.getState());
        level.playSound(block.getPosition(), SoundTypes.HATCH_FROG_SPAWN);
        spawnTadpoles(level, block.getPosition(), random);
    }

    private static void spawnTadpoles(Level level, Vector3i position, RandomGenerator random) {
        int count = random.nextInt(2, 6);
        for (int i = 0; i < count; i++) {
            float x = position.getX() + (float) Math.clamp(random.nextDouble(), 0.2, 0.8);
            float z = position.getZ() + (float) Math.clamp(random.nextDouble(), 0.2, 0.8);
            Location location = Location.from(x, position.getY() - 0.5f, z, random.nextInt(1, 361), 0, level);
            Tadpole tadpole = CloudEntityRegistry.get().newEntity(EntityTypes.TADPOLE, location);
            ((CloudEntity) tadpole).spawn(new CreatureSpawnEvent(tadpole, CreatureSpawnReason.EGG));
        }
    }

    private static void destroy(Block block) {
        if (block.getLevel().setBlockState(block.getPosition(), BlockStates.AIR)) {
            block.getLevel().playBlockBreakEffect(block.getPosition(), block.getState());
        }
    }
}
