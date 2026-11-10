package org.cloudburstmc.server.block.component;

import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.block.component.*;
import org.cloudburstmc.api.block.trait.IntegerBlockTrait;
import org.cloudburstmc.api.event.entity.EntityChangeBlockEvent;
import org.cloudburstmc.api.event.player.PlayerHarvestBlockEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.util.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

public enum GrowingVines {
    CAVE(Direction.DOWN, BlockTypes.CAVE_VINES, BlockTraits.CAVE_VINE_AGE),
    TWISTING(Direction.UP, BlockTypes.TWISTING_VINES, BlockTraits.TWISTING_VINES_AGE),
    WEEPING(Direction.DOWN, BlockTypes.WEEPING_VINES, BlockTraits.WEEPING_VINES_AGE);

    private static final int MAX_AGE = 25;

    public static final UseBlockHandler HARVEST_BERRIES = (block, player, face, held) -> {
        BlockState original = block.getState();
        if (player == null || held.getType() == ItemTypes.BONE_MEAL || !hasBerries(original)) {
            return false;
        }

        if (held.getType() == ItemTypes.SHEARS && CAVE.age(original) < MAX_AGE && CAVE.isTip(block)) {
            return false;
        }

        Level level = block.getLevel();
        BlockState picked = CAVE.state(CAVE.age(original), false, false);
        EntityChangeBlockEvent change = new EntityChangeBlockEvent(player, block, picked);
        level.getServer().getEventManager().fire(change);
        if (change.isCancelled() || level.getBlockState(block.getPosition()) != original) {
            return true;
        }

        PlayerHarvestBlockEvent harvest = new PlayerHarvestBlockEvent(player, block, List.of(ItemStack.from(ItemTypes.GLOW_BERRIES)));
        level.getServer().getEventManager().fire(harvest);
        if (harvest.isCancelled() || level.getBlockState(block.getPosition()) != original) {
            return true;
        }

        block.set(picked);

        for (ItemStack drop : harvest.getItemsHarvested()) {
            if (drop != null && !drop.isEmpty()) {
                level.dropItem(block.getPosition().toFloat().add(0.5f, 0.5f, 0.5f), drop);
            }
        }

        level.playSound(block.getPosition(), SoundTypes.PICK_BERRIES_CAVE_VINES);
        return true;
    };

    public static final BlockLootHandler CAVE_LOOT = (block, context) -> hasBerries(block.getState()) ? List.of(ItemStack.from(ItemTypes.GLOW_BERRIES)) : List.of();

    public static final PickBlockHandler PICK_CAVE_VINE = block -> ItemStack.from(ItemTypes.GLOW_BERRIES);

    private final Direction growthDirection;
    private final BlockType plain;
    private final IntegerBlockTrait ageTrait;

    GrowingVines(Direction growthDirection, BlockType plain, IntegerBlockTrait ageTrait) {
        this.growthDirection = growthDirection;
        this.plain = plain;
        this.ageTrait = ageTrait;
    }

    public SurviveBlockHandler survival() {
        return this::canSurvive;
    }

    public PlacementStateHandler placement() {
        return (state, block, player, face, click) -> {
            if (this.contains(block.getState().getType()) || !this.canSurvive(block)) {
                return null;
            }

            int age = player == null ? this.age(state) : ThreadLocalRandom.current().nextInt(MAX_AGE);
            return this.state(age, hasBerries(state), this.isTip(block));
        };
    }

    public NeighborBlockHandler neighborChanged() {
        return (block, neighbor) -> {
            if (!this.canSurvive(block)) {
                block.getLevel().scheduleUpdate(block.getPosition(), 1);
            } else {
                this.updateBerryForm(block);
            }
        };
    }

    public TickBlockHandler supportTick() {
        return (block, random) -> {
            if (!this.canSurvive(block)) {
                block.getLevel().breakBlock(block.getPosition(), ItemStack.EMPTY, null, true);
            }
        };
    }

    public TickBlockHandler growthTick() {
        return (block, random) -> {
            BlockState current = block.getState();
            int age = this.age(current);
            if (age >= MAX_AGE || random.nextFloat() >= 0.1f || !this.canSurvive(block)) {
                return;
            }

            Block target = block.getSide(this.growthDirection);
            if (VineBlockHandlers.isEmpty(target)) {
                BlockState growth = this.state(age + 1, this == CAVE && random.nextFloat() < 0.11f, true);
                VineBlockHandlers.spread(block, target, growth);
            }
        };
    }

    public FertilizeBlockHandler fertilizer() {
        return (block, player, random) -> this == CAVE ? fertilizeCaveVine(block) : this.growWithBonemeal(block, random);
    }

    public static boolean canPrune(Block block) {
        BlockType type = block.getState().getType();
        if (CAVE.contains(type)) {
            return CAVE.isTip(block);
        }

        if (TWISTING.contains(type)) {
            return TWISTING.isTip(block);
        }

        return !WEEPING.contains(type) || WEEPING.isTip(block);
    }

    private boolean contains(BlockType type) {
        return type == this.plain || (this == CAVE && (type == BlockTypes.CAVE_VINES_BODY_WITH_BERRIES || type == BlockTypes.CAVE_VINES_HEAD_WITH_BERRIES));
    }

    private boolean canSurvive(Block block) {
        Block support = block.getSide(this.growthDirection.getOpposite());
        return this.contains(support.getState().getType()) || support.isFaceSturdy(this.growthDirection, SupportType.FULL);
    }

    private boolean isTip(Block block) {
        return !this.contains(block.getSide(this.growthDirection).getState().getType());
    }

    private void updateBerryForm(Block block) {
        BlockState current = block.getState();
        if (this == CAVE && hasBerries(current)) {
            BlockState next = this.state(this.age(current), true, this.isTip(block));
            if (next != current) {
                block.set(next);
            }
        }
    }

    private FertilizationResult growWithBonemeal(Block block, RandomGenerator random) {
        Block tip = block;
        while (!this.isTip(tip)) {
            tip = tip.getSide(this.growthDirection);
        }

        Block next = tip.getSide(this.growthDirection);
        int age = this.age(tip.getState());
        List<BlockChange> changes = new ArrayList<>();

        for (double chance = 1; VineBlockHandlers.isEmpty(next) && random.nextDouble() < chance; chance *= 0.826) {
            age = Math.min(MAX_AGE, age + 1);
            changes.add(new BlockChange(next, this.state(age, false, true)));
            next = next.getSide(this.growthDirection);
        }

        return new FertilizationResult(changes, List.of());
    }

    private static FertilizationResult fertilizeCaveVine(Block block) {
        BlockState state = block.getState();
        if (hasBerries(state)) {
            return FertilizationResult.none();
        }

        BlockState berries = CAVE.state(CAVE.age(state), true, CAVE.isTip(block));
        return new FertilizationResult(List.of(new BlockChange(block, berries)), List.of());
    }

    private BlockState state(int age, boolean berries, boolean tip) {
        BlockType type = this.plain;
        if (this == CAVE && berries) {
            type = tip ? BlockTypes.CAVE_VINES_HEAD_WITH_BERRIES : BlockTypes.CAVE_VINES_BODY_WITH_BERRIES;
        }

        return type.getDefaultState().withTrait(this.ageTrait, age);
    }

    private int age(BlockState state) {
        return state.ensureTrait(this.ageTrait);
    }

    private static boolean hasBerries(BlockState state) {
        BlockType type = state.getType();
        return type == BlockTypes.CAVE_VINES_HEAD_WITH_BERRIES || type == BlockTypes.CAVE_VINES_BODY_WITH_BERRIES;
    }
}
