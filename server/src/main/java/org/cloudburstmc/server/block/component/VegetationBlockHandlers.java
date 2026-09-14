package org.cloudburstmc.server.block.component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.EntityTypes;
import org.cloudburstmc.api.entity.Living;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.passive.Bee;
import org.cloudburstmc.api.event.entity.EntityDamageEvent;
import org.cloudburstmc.api.event.player.PlayerHarvestBlockEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Difficulty;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.entity.CloudEntity;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.level.particle.DestroyBlockParticle;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.potion.CloudEffect;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VegetationBlockHandlers {

    public static final SurviveBlockHandler CACTUS_FLOWER_CAN_SURVIVE = block -> {
        Block below = block.getRelative(0, -1, 0);
        return below.getState().getType() == BlockTypes.CACTUS || below.isFaceSturdy(Direction.UP, SupportType.CENTER);
    };

    public static final SurviveBlockHandler CAN_SURVIVE = block -> {
        if (isUpperHalf(block.getState())) {
            BlockState below = block.getRelativeState(0, -1, 0);
            return below.getType() == block.getState().getType() && !isUpperHalf(below);
        }

        return block.getRelativeState(0, -1, 0).is(BlockTags.SUPPORTS_VEGETATION);
    };

    public static final PlayerBlockHandler DOUBLE_PLANT_DESTROY = (block, player) -> {
        BlockState state = block.getState();
        boolean upper = isUpperHalf(state);
        Block partner = block.getRelative(0, upper ? -1 : 1, 0);
        BlockState partnerState = partner.getState();

        block.set(BlockStates.AIR, false, false);
        if (partnerState.getType() == state.getType() && isUpperHalf(partnerState) != upper) {
            partner.set(BlockStates.AIR, false, true);
            ((CloudLevel) block.getLevel()).addParticle(new DestroyBlockParticle(partner.getPosition().toFloat().add(0.5f, 0.5f, 0.5f), partnerState));
        }
    };

    public static final NeighborBlockHandler DOUBLE_PLANT_NEIGHBOUR_CHANGED = (block, neighbor) -> {
        BlockState state = block.getState();
        boolean upper = isUpperHalf(state);
        BlockState partner = block.getRelativeState(0, upper ? -1 : 1, 0);
        boolean validPartner = partner.getType() == state.getType() && isUpperHalf(partner) != upper;
        if (!validPartner) {
            if (upper) {
                block.set(BlockStates.AIR);
            } else {
                breakNaturally(block);
            }
        } else if (!CAN_SURVIVE.execute(block)) {
            breakNaturally(block);
        }
    };

    public static final PlacementStateHandler FLOWER_BED_PLACEMENT = (state, block, player, face, click) -> {
        BlockState current = block.getState();
        if (current.getType() == state.getType()) {
            return current.withTrait(BlockTraits.GROWTH, current.ensureTrait(BlockTraits.GROWTH) + 1);
        }

        return state;
    };

    public static final ReplaceBlockHandler FLOWER_BED_REPLACEABLE = (block, replacement, player, face, click) ->
            block.getState().getType() == replacement.getType() && block.getState().ensureTrait(BlockTraits.GROWTH) < 3;

    public static final NeighborBlockHandler NEIGHBOUR_CHANGED = checkSurvival(CAN_SURVIVE);

    public static final EntityInsideBlockHandler WITHER_ROSE_ENTITY_INSIDE = (block, entity, precise) -> {
        if (block.getLevel().getDifficulty() != Difficulty.PEACEFUL
                && entity instanceof Living
                && canReceiveWither(entity)) {
            entity.addEffect(new CloudEffect(EffectTypes.WITHER).setDuration(40));
        }
    };

    public static final EntityInsideBlockHandler EYEBLOSSOM_ENTITY_INSIDE = (block, entity, precise) -> {
        if (block.getLevel().getDifficulty() != Difficulty.PEACEFUL
                && entity instanceof Bee
                && !entity.hasEffect(EffectTypes.POISON)) {
            entity.addEffect(new CloudEffect(EffectTypes.POISON).setDuration(25));
        }
    };

    public static final EntityInsideBlockHandler SWEET_BERRY_BUSH_ENTITY_INSIDE = (block, entity, precise) -> {
        if (!(entity instanceof Living)
                || entity.getType() == EntityTypes.FOX
                || entity.getType() == EntityTypes.BEE) {
            return;
        }

        entity.makeStuckInBlock(block.getState(), Vector3f.from(0.8f, 0.75f, 0.8f));
        Vector3f movement = entity instanceof CloudPlayer player ? player.getKnownMovement() : entity.getMotion();
        if (block.getState().ensureTrait(BlockTraits.GROWTH) != 0
                && (Math.abs(movement.getX()) >= 0.003f || Math.abs(movement.getZ()) >= 0.003f)) {
            DamageSource source = DamageSource.builder(DamageTypes.SWEET_BERRY_BUSH).block(block).build();
            entity.attack(new EntityDamageEvent(entity, source, 1));
        }
    };

    public static final UseBlockHandler SWEET_BERRY_BUSH_USE = (block, player, direction, item) -> {
        int age = block.getState().ensureTrait(BlockTraits.GROWTH);
        if (player == null || age < 2 || (item.getType() == ItemTypes.BONE_MEAL && age < 3)) {
            return false;
        }

        int count = ThreadLocalRandom.current().nextInt(1, 3) + (age == 3 ? 1 : 0);
        PlayerHarvestBlockEvent event = new PlayerHarvestBlockEvent(
                player, block, List.of(ItemStack.from(ItemTypes.SWEET_BERRIES, count)));
        ((CloudLevel) block.getLevel()).getServer().getEventManager().fire(event);
        if (event.isCancelled()) {
            return true;
        }

        block.set(block.getState().withTrait(BlockTraits.GROWTH, 1));
        for (ItemStack harvested : event.getItemsHarvested()) {
            block.getLevel().dropItem(block.getPosition().toFloat().add(0.5f, 0.5f, 0.5f), harvested);
        }
        ((CloudLevel) block.getLevel()).addSound(block.getPosition(), Sound.BLOCK_SWEET_BERRY_BUSH_PICK);
        return true;
    };

    public static NeighborBlockHandler checkSurvival(SurviveBlockHandler survival) {
        return (block, neighbor) -> {
            if (!survival.execute(block)) {
                breakNaturally(block);
            }
        };
    }

    public static FertilizeBlockHandler duplicate(BlockType type) {
        return (block, player, random) -> new FertilizationResult(List.of(), List.of(ItemStack.from(type.getDefaultState())));
    }

    public static FertilizeBlockHandler growFlowerBed(BlockType type) {
        return (block, player, random) -> {
            BlockState state = block.getState();
            int amount = state.ensureTrait(BlockTraits.GROWTH);
            if (amount < 3) {
                return new FertilizationResult(
                        List.of(new BlockChange(block, state.withTrait(BlockTraits.GROWTH, amount + 1))),
                        List.of());
            }
            return new FertilizationResult(List.of(), List.of(ItemStack.from(type.getDefaultState())));
        };
    }

    public static FertilizeBlockHandler growToAge(int maximumAge) {
        return (block, player, random) -> {
            BlockState state = block.getState();
            int age = state.ensureTrait(BlockTraits.GROWTH);
            if (age >= maximumAge) {
                return FertilizationResult.none();
            }

            return new FertilizationResult(
                    List.of(new BlockChange(block, state.withTrait(BlockTraits.GROWTH, age + 1))),
                    List.of());
        };
    }

    public static FertilizeBlockHandler growInto(BlockType grownType) {
        return (block, player, random) -> {
            Block above = block.up();
            BlockState grown = grownType.getDefaultState();
            if (!above.getState().isReplaceable()) {
                return FertilizationResult.none();
            }

            return new FertilizationResult(List.of(
                    new BlockChange(block, lowerHalf(grown)),
                    new BlockChange(above, upperHalf(grown))
            ), List.of());
        };
    }

    public static FertilizeBlockHandler replaceWith(BlockType type) {
        return (block, player, random) -> new FertilizationResult(
                List.of(new BlockChange(block, type.getDefaultState())), List.of());
    }

    public static FertilizeBlockHandler spreadGroundCover() {
        return (block, player, random) -> {
            Map<Vector3i, BlockChange> changes = new LinkedHashMap<>();
            Vector3i origin = block.getPosition().add(0, 1, 0);
            CloudLevel level = (CloudLevel) block.getLevel();

            attempt:
            for (int attempt = 0; attempt < 128; attempt++) {
                Vector3i targetPosition = origin;
                for (int step = 0; step < attempt / 16; step++) {
                    targetPosition = targetPosition.add(
                            random.nextInt(3) - 1,
                            (random.nextInt(3) - 1) * random.nextInt(3) / 2,
                            random.nextInt(3) - 1
                    );

                    if (proposedState(level, changes, targetPosition.add(0, -1, 0)).getType() != BlockTypes.GRASS_BLOCK) {
                        continue attempt;
                    }
                }

                Block target = level.getBlock(targetPosition);
                BlockState targetState = proposedState(level, changes, targetPosition);
                if (targetState.getType() == BlockTypes.SHORT_GRASS && random.nextInt(10) == 0) {
                    Vector3i abovePosition = targetPosition.add(0, 1, 0);
                    if (proposedState(level, changes, abovePosition).isReplaceable()) {
                        BlockState tallGrass = BlockStates.TALL_GRASS;
                        changes.put(targetPosition, new BlockChange(target, lowerHalf(tallGrass)));
                        changes.put(abovePosition, new BlockChange(level.getBlock(abovePosition), upperHalf(tallGrass)));
                    }
                } else if (targetState.getType() == BlockTypes.AIR) {
                    BlockState vegetation = random.nextInt(8) == 0
                            ? BiomeBoneMealVegetation.select(level, targetPosition, random).getDefaultState()
                            : BlockStates.SHORT_GRASS;
                    changes.put(targetPosition, new BlockChange(target, vegetation));
                }
            }

            return changes.isEmpty() ? FertilizationResult.none() : new FertilizationResult(List.copyOf(changes.values()), List.of());
        };
    }

    public static FertilizeBlockHandler spread(BlockType type, BlockTagKey supportTag) {
        return (block, player, random) -> {
            Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
            int start = random.nextInt(directions.length);
            for (int offset = 0; offset < directions.length; offset++) {
                Block target = block.getSide(directions[(start + offset) % directions.length]);
                if (target.getState().isReplaceable() && target.getRelativeState(0, -1, 0).is(supportTag)) {
                    return new FertilizationResult(List.of(new BlockChange(target, type.getDefaultState())), List.of());
                }
            }

            return FertilizationResult.none();
        };
    }

    public static SurviveBlockHandler supportedBy(BlockTagKey supportTag) {
        return block -> block.getRelativeState(0, -1, 0).is(supportTag);
    }

    public static BlockState lowerHalf(BlockState state) {
        return state.withTrait(BlockTraits.IS_UPPER_BLOCK, false);
    }

    public static BlockState upperHalf(BlockState state) {
        return state.withTrait(BlockTraits.IS_UPPER_BLOCK, true);
    }

    private static boolean isUpperHalf(BlockState state) {
        return state.getTraits().containsKey(BlockTraits.IS_UPPER_BLOCK) && state.ensureTrait(BlockTraits.IS_UPPER_BLOCK);
    }

    private static boolean canReceiveWither(Entity entity) {
        if (((CloudEntity) entity).isInvulnerable()
                || entity.getType() == EntityTypes.WITHER
                || entity.getType() == EntityTypes.WITHER_SKELETON) {
            return false;
        }

        return !(entity instanceof Player player) || (!player.isCreative() && !player.isSpectator());
    }

    private static BlockState proposedState(CloudLevel level, Map<Vector3i, BlockChange> changes, Vector3i position) {
        BlockChange change = changes.get(position);
        return change == null ? level.getBlockState(position) : change.newState();
    }

    private static void breakNaturally(Block block) {
        block.getLevel().breakBlock(block.getPosition(), ItemStack.EMPTY, null, true);
    }
}
