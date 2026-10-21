package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.Living;
import org.cloudburstmc.api.entity.passive.Turtle;
import org.cloudburstmc.api.event.Event;
import org.cloudburstmc.api.event.block.BlockFadeEvent;
import org.cloudburstmc.api.event.block.BlockGrowEvent;
import org.cloudburstmc.api.event.entity.CreatureSpawnEvent;
import org.cloudburstmc.api.event.entity.CreatureSpawnReason;
import org.cloudburstmc.api.event.entity.EntityInteractEvent;
import org.cloudburstmc.api.event.player.PlayerInteractEvent;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.level.particle.ParticleTypes;
import org.cloudburstmc.api.level.sound.SoundType;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.CrackedState;
import org.cloudburstmc.api.util.data.TurtleEggCount;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.server.block.util.BlockSupport;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.registry.CloudEntityRegistry;

import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

@UtilityClass
public class TurtleEggBlockHandlers {

    private static final TurtleEggCount[] COUNTS = TurtleEggCount.values();

    public static final ComplexBlockHandler ON_ADDED = block -> {
        Block below = block.getLevel().getLoadedBlock(block.getPosition().add(0, -1, 0));
        if (below == null || !below.getState().is(BlockTags.SAND)) {
            return;
        }

        RandomGenerator random = ThreadLocalRandom.current();
        float height = block.getState().getOutlineShape().bounds().getMaxY();
        for (int i = 0; i < 15; i++) {
            Vector3f position = block.getPosition().toFloat().add(random.nextFloat(), random.nextFloat() * height, random.nextFloat());
            block.getLevel().spawnParticle(ParticleTypes.VILLAGER_HAPPY, position);
        }
    };

    public static final ReplaceBlockHandler CAN_BE_REPLACED = (block, replacement, player, face, click) ->
            (player == null || !player.isSneaking())
                    && replacement.getType() == BlockTypes.TURTLE_EGG
                    && block.getState().ensureTrait(BlockTraits.TURTLE_EGG_COUNT) != TurtleEggCount.FOUR_EGG;

    public static final PlacementStateHandler RESOLVE_PLACEMENT_STATE = (state, block, player, face, click) -> {
        if (block.getState().getType() != BlockTypes.TURTLE_EGG) {
            if (!BlockSupport.isFaceSturdy(block.getLevel(), block.getPosition().add(0, -1, 0), Direction.UP)) {
                return null;
            }

            return state.withTrait(BlockTraits.TURTLE_EGG_COUNT, TurtleEggCount.ONE_EGG)
                    .withTrait(BlockTraits.CRACKED_STATE, CrackedState.NO_CRACKS);
        }

        BlockState current = block.getState();
        int count = current.ensureTrait(BlockTraits.TURTLE_EGG_COUNT).ordinal();
        return count == COUNTS.length - 1 ? null : current.withTrait(BlockTraits.TURTLE_EGG_COUNT, COUNTS[count + 1]);
    };

    public static final PlaceBlockHandler ON_PLACE = (state, player, position, face, click) -> {
        if (player == null) {
            return false;
        }

        Level level = player.getLevel();
        BlockState previous = level.getBlockState(position);
        if (!level.setBlockState(position, state, true, true)) {
            return false;
        }

        if (level.getBlockState(position) == state && previous.getType() == BlockTypes.TURTLE_EGG
                && state.ensureTrait(BlockTraits.TURTLE_EGG_COUNT).ordinal() > previous.ensureTrait(BlockTraits.TURTLE_EGG_COUNT).ordinal()) {
            Block below = level.getLoadedBlock(position.add(0, -1, 0));
            if (below != null && below.getState().is(BlockTags.SAND)) {
                level.playSound(position, SoundTypes.ITEM_BONE_MEAL_USE);
            }
        }

        return true;
    };

    public static final BlockDestroyHandler ON_DESTROY = (block, cause) -> {
        if (cause instanceof Player player && !player.isCreative()) {
            decreaseEggs(block, false);
        } else {
            if (block.getLevel().setBlockState(block.getPosition(), BlockStates.AIR) && cause instanceof Player) {
                playSound(block, SoundTypes.BLOCK_TURTLE_EGG_BREAK, ThreadLocalRandom.current());
            }
        }
    };

    public static final EntityBlockHandler ON_STEP_ON = (block, entity) -> {
        if (!(entity instanceof Player player && player.isSneaking())) {
            trample(block, entity, 100);
        }
    };

    public static final FallOnBlockHandler ON_FALL_ON = (block, entity, distance) -> {
        if (canTrampleOnLanding(entity)) {
            trample(block, entity, 3);
        }

        DefaultBlockHandlers.ON_FALL_ON.execute(block, entity, distance);
    };

    public static final TickBlockHandler ON_RANDOM_TICK = (block, random) -> grow(block, random == null ? ThreadLocalRandom.current() : random);

    private static void trample(Block block, Entity entity, int randomness) {
        if (entity.getType() == EntityTypes.TURTLE || entity.getType() == EntityTypes.BAT) {
            return;
        }

        Level level = block.getLevel();
        Event interaction;
        if (entity instanceof Player player) {
            if (player.isSpectator() || level instanceof CloudLevel cloud && !player.isOp() && cloud.isInSpawnRadius(block.getPosition())) {
                return;
            }

            interaction = new PlayerInteractEvent(player, null, block, null, PlayerInteractEvent.Action.PHYSICAL);
        } else {
            if (!(entity instanceof Living) || !level.getGameRules().get(GameRules.MOB_GRIEFING)) {
                return;
            }

            interaction = new EntityInteractEvent(entity, block);
        }

        RandomGenerator random = level instanceof CloudLevel cloud ? cloud.getRandom() : ThreadLocalRandom.current();
        if (random.nextInt(randomness) != 0) {
            return;
        }

        level.getServer().getEventManager().fire(interaction);
        if (!interaction.isCancelled() && level.getBlockState(block.getPosition()) == block.getState()) {
            decreaseEggs(block, true);
        }
    }

    private static boolean canTrampleOnLanding(Entity entity) {
        return entity.getType() != EntityTypes.ZOMBIE && entity.getType() != EntityTypes.HUSK
                && entity.getType() != EntityTypes.DROWNED && entity.getType() != EntityTypes.ZOMBIE_VILLAGER
                && entity.getType() != EntityTypes.DEPRECATED_ZOMBIE_VILLAGER && entity.getType() != EntityTypes.ZOMBIE_PIGMAN;
    }

    private static void decreaseEggs(Block block, boolean particles) {
        Level level = block.getLevel();
        BlockState state = block.getState();
        int count = state.ensureTrait(BlockTraits.TURTLE_EGG_COUNT).ordinal();
        BlockState replacement = count == 0 ? BlockStates.AIR : state.withTrait(BlockTraits.TURTLE_EGG_COUNT, COUNTS[count - 1]);
        if (level.getBlockState(block.getPosition()) != state || !level.setBlockState(block.getPosition(), replacement)) {
            return;
        }

        playSound(block, SoundTypes.BLOCK_TURTLE_EGG_BREAK, ThreadLocalRandom.current());
        if (particles) {
            level.playBlockBreakEffect(block.getPosition(), state);
        }
    }

    private static void grow(Block block, RandomGenerator random) {
        Level level = block.getLevel();
        Block below = level.getLoadedBlock(block.getPosition().add(0, -1, 0));
        if (below == null || !below.getState().is(BlockTags.SAND)) {
            return;
        }

        long time = level.getTime();
        boolean dawn = level.hasDaylightCycle() && time >= 21062 && time < 21905;
        if (random.nextFloat() >= (dawn ? 1f : 0.002f)) {
            return;
        }

        BlockState state = block.getState();
        CrackedState cracks = state.ensureTrait(BlockTraits.CRACKED_STATE);
        if (cracks != CrackedState.MAX_CRACKED) {
            CrackedState next = cracks == CrackedState.NO_CRACKS ? CrackedState.CRACKED : CrackedState.MAX_CRACKED;
            BlockGrowEvent event = new BlockGrowEvent(block, state.withTrait(BlockTraits.CRACKED_STATE, next));
            level.getServer().getEventManager().fire(event);
            if (!event.isCancelled() && level.getBlockState(block.getPosition()) == state && level.setBlockState(block.getPosition(), event.getNewState())) {
                playSound(block, SoundTypes.BLOCK_TURTLE_EGG_CRACK, random);
            }
            return;
        }

        hatch(block, random);
    }

    private static void hatch(Block block, RandomGenerator random) {
        Level level = block.getLevel();
        BlockFadeEvent event = new BlockFadeEvent(block, BlockStates.AIR);
        level.getServer().getEventManager().fire(event);
        if (event.isCancelled() || level.getBlockState(block.getPosition()) != block.getState()
                || !level.setBlockState(block.getPosition(), event.getNewState()) || event.getNewState() != BlockStates.AIR) {
            return;
        }

        playSound(block, SoundTypes.MOB_TURTLE_BABY_BORN, random);
        int count = block.getState().ensureTrait(BlockTraits.TURTLE_EGG_COUNT).ordinal() + 1;
        for (int i = 0; i < count; i++) {
            level.playBlockBreakEffect(block.getPosition(), block.getState());
            Location location = Location.from(block.getPosition().getX() + 0.3f + i * 0.2f,
                    block.getPosition().getY(), block.getPosition().getZ() + 0.3f, 0, 0, level);
            Turtle turtle = CloudEntityRegistry.get().newEntity(EntityTypes.TURTLE, location);
            turtle.setAge(-24000);
            turtle.setHome(Location.from(block.getPosition().toFloat(), 0, 0, level));
            ((CloudEntity) turtle).spawn(new CreatureSpawnEvent(turtle, CreatureSpawnReason.EGG));
        }
    }

    private static void playSound(Block block, SoundType sound, RandomGenerator random) {
        block.getLevel().playSound(block.getPosition(), sound, 0.7f, 0.9f + random.nextFloat() * 0.2f);
    }
}
