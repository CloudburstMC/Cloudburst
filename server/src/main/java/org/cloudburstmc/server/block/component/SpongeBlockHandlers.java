package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.*;
import org.cloudburstmc.api.event.block.SpongeAbsorbEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.api.level.gamerule.GameRules;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.LevelEvent;
import org.cloudburstmc.server.block.BlockLayerRules;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.network.LevelEffectPacketFactory;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public class SpongeBlockHandlers {

    private static final int MAX_DEPTH = 6;
    private static final int MAX_REMOVED = 64;

    public static void absorb(Block sponge) {
        Level level = sponge.getLevel();
        if (level.getBlockState(sponge.getPosition()).getType() != BlockTypes.SPONGE) {
            return;
        }

        List<BlockReplacement> changes = collect(sponge);
        if (changes.isEmpty()) {
            return;
        }

        SpongeAbsorbEvent event = new SpongeAbsorbEvent(sponge, changes);
        level.getServer().getEventManager().fire(event);
        if (event.isCancelled() || level.getBlockState(sponge.getPosition()) != sponge.getState()) {
            return;
        }

        List<AbsorbedBlock> changed = new ArrayList<>();
        level.batchBlockUpdates(() -> {
            changed.addAll(apply(level, event.getBlocks(), sponge.getPosition()));
            if (!changed.isEmpty()) {
                level.setBlockState(sponge.getPosition(), BlockTypes.WET_SPONGE.getDefaultState());
            }
        });

        if (changed.isEmpty()) {
            return;
        }

        level.playSound(sponge.getPosition(), SoundTypes.SPONGE_ABSORB);
        for (AbsorbedBlock block : changed) {
            for (ItemStack drop : block.drops()) {
                level.dropItem(block.position(), drop);
            }
        }
    }

    public static void dry(Block sponge, boolean waterEvaporates) {
        if (!waterEvaporates) {
            return;
        }

        Level level = sponge.getLevel();
        if (level.setBlockState(sponge.getPosition(), BlockTypes.SPONGE.getDefaultState())) {
            level.playSound(sponge.getPosition(), SoundTypes.RANDOM_FIZZ, 1, 0.7f + ThreadLocalRandom.current().nextFloat() * 0.14f);
            if (level instanceof CloudLevel cloudLevel) {
                cloudLevel.addChunkPacket(sponge.getPosition(), LevelEffectPacketFactory.event(sponge.getPosition().toFloat().add(0.5f, 0.5f, 0.5f), LevelEvent.PARTICLE_EVAPORATE_WATER, 0));
            }
        }
    }

    private static List<BlockReplacement> collect(Block sponge) {
        Level level = sponge.getLevel();
        ArrayDeque<Node> pending = new ArrayDeque<>();
        Set<Vector3i> visited = new HashSet<>();
        List<BlockReplacement> changes = new ArrayList<>(MAX_REMOVED);
        pending.add(new Node(sponge.getPosition(), 0));
        visited.add(sponge.getPosition());

        while (!pending.isEmpty() && changes.size() < MAX_REMOVED) {
            Node node = pending.remove();
            for (Direction direction : Direction.values()) {
                Vector3i position = direction.relative(node.position());
                if (!visited.add(position)) {
                    continue;
                }

                Block block = level.getLoadedBlock(position);
                BlockReplacement change = block == null ? null : replacement(block);
                if (change == null) {
                    continue;
                }

                changes.add(change);
                if (changes.size() == MAX_REMOVED) {
                    break;
                }

                if (node.depth() + 1 < MAX_DEPTH) {
                    pending.add(new Node(position, node.depth() + 1));
                }
            }
        }

        return changes;
    }

    private static @Nullable BlockReplacement replacement(Block block) {
        if (!block.getLiquid().getType().isSameFamily(LiquidTypes.WATER)) {
            return null;
        }

        BlockState state = block.getState();
        if (state.getType().isLiquid() || state.getType() == BlockTypes.BUBBLE_COLUMN || state.is(BlockTags.WATER_SOURCE_PLANTS)) {
            return new BlockReplacement(block, BlockStates.AIR, BlockStates.AIR);
        }

        return BlockLayerRules.canCoexist(state, BlockStates.AIR) ? new BlockReplacement(block, state, BlockStates.AIR) : null;
    }

    private static List<AbsorbedBlock> apply(Level level, List<BlockReplacement> changes, Vector3i spongePosition) {
        Set<Vector3i> applied = new HashSet<>();
        List<BlockReplacement> valid = new ArrayList<>();
        for (BlockReplacement change : List.copyOf(changes)) {
            Block before = change.block();
            Vector3i position = before.getPosition();
            if (before.getLevel() != level || position.equals(spongePosition) || !applied.add(position)) {
                continue;
            }

            Block current = level.getLoadedBlock(position);
            if (current == null || current.getState() != before.getState() || current.getSecondaryState() != before.getSecondaryState()
                    || replacement(current) == null || !BlockLayerRules.canCoexist(change.primaryState(), change.secondaryState())) {
                continue;
            }

            valid.add(change);
        }

        List<AbsorbedBlock> changed = new ArrayList<>();
        for (BlockReplacement change : valid) {
            Block before = change.block();
            Vector3i position = before.getPosition();

            List<ItemStack> drops = before.getState().is(BlockTags.WATER_SOURCE_PLANTS) && change.primaryState() == BlockStates.AIR
                    && level.getGameRules().get(GameRules.DO_TILE_DROPS)
                    ? before.requireComponent(BlockComponents.GET_LOOT).execute(before, new BlockLootContext(ItemStack.EMPTY, null, ThreadLocalRandom.current()))
                    : List.of();

            level.setBlockState(position, change.primaryState());
            level.setBlockState(position, BlockLayer.SECONDARY, change.secondaryState());
            level.setBlockState(position, change.primaryState());

            Block after = level.getLoadedBlock(position);
            if (after != null && (after.getState() != before.getState() || after.getSecondaryState() != before.getSecondaryState())) {
                changed.add(new AbsorbedBlock(position, drops));
            }
        }

        return changed;
    }

    private record Node(Vector3i position, int depth) {
    }

    private record AbsorbedBlock(Vector3i position, List<ItemStack> drops) {
    }
}
