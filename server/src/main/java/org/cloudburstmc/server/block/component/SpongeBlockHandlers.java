package org.cloudburstmc.server.block.component;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.event.block.SpongeAbsorbEvent;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.level.particle.DestroyBlockNoSoundParticle;
import org.cloudburstmc.server.registry.CloudBlockRegistry;

import java.util.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SpongeBlockHandlers {

    private static final int MAX_DEPTH = 6;
    private static final int MAX_REMOVED = 64;

    public static DefaultBlockPlaceHandler place(CloudBlockRegistry registry) {
        return new SpongePlaceHandler(registry);
    }

    public static void absorb(Block sponge) {
        CloudLevel level = (CloudLevel) sponge.getLevel();
        Queue<Node> pending = new ArrayDeque<>();
        Set<Vector3i> visited = new HashSet<>();
        pending.add(new Node(sponge.getPosition(), 0));
        visited.add(sponge.getPosition());
        List<Block> affected = new ArrayList<>(MAX_REMOVED);

        while (!pending.isEmpty() && affected.size() < MAX_REMOVED) {
            Node node = pending.remove();
            for (Direction direction : Direction.values()) {
                Vector3i position = direction.relative(node.position());
                if (!visited.add(position)) {
                    continue;
                }

                Block block = level.getLoadedBlock(position.getX(), position.getY(), position.getZ());
                if (block == null || block.getLiquid().isEmpty()) {
                    continue;
                }

                affected.add(block);
                if (node.depth() < MAX_DEPTH - 1 && affected.size() < MAX_REMOVED) {
                    pending.add(new Node(position, node.depth() + 1));
                }
            }
        }

        if (!affected.isEmpty()) {
            SpongeAbsorbEvent event = new SpongeAbsorbEvent(sponge, affected);
            level.getServer().getEventManager().fire(event);
            if (event.isCancelled()) {
                return;
            }

            int removed = apply(level, event.getAffectedBlocks());
            if (removed == 0) {
                return;
            }

            Vector3i position = sponge.getPosition();
            level.setBlockState(position, BlockTypes.WET_SPONGE.getDefaultState());
            level.addLevelSoundEvent(position, SoundEvent.SPONGE_ABSORB);
            level.addParticle(new DestroyBlockNoSoundParticle(position.toFloat(), BlockTypes.WATER.getDefaultState()));
        }
    }

    private static int apply(CloudLevel level, List<Block> affected) {
        int removed = 0;
        Set<Vector3i> applied = new HashSet<>();
        for (Block planned : List.copyOf(affected)) {
            Vector3i position = planned.getPosition();
            if (planned.getLevel() != level || !applied.add(position)) {
                continue;
            }

            Block block = level.getLoadedBlock(position.getX(), position.getY(), position.getZ());
            if (block == null || block.getLiquid().isEmpty()) {
                continue;
            }

            boolean changed;
            BlockState primary = block.getState();
            if (block.getLiquidLayer() == 1 && isAquaticPlant(primary)) {
                level.breakBlock(position);
                changed = level.getBlock(position).getLiquid().isEmpty();
            } else {
                changed = level.removeLiquid(position);
            }

            if (changed) {
                removed++;
            }
        }

        return removed;
    }

    private static boolean isAquaticPlant(BlockState state) {
        return state.getType() == BlockTypes.KELP
                || state.getType() == BlockTypes.SEAGRASS;
    }

    private record Node(Vector3i position, int depth) {
    }

    private static final class SpongePlaceHandler extends DefaultBlockPlaceHandler {

        private SpongePlaceHandler(CloudBlockRegistry registry) {
            super(registry);
        }

        @Override
        public boolean execute(BlockState state, Player player, Vector3i position, Direction face, Vector3f clickPosition) {
            if (!super.execute(state, player, position, face, clickPosition)) {
                return false;
            }

            absorb(player.getLevel().getBlock(position));
            return true;
        }
    }
}
