package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockStates;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.level.particle.SmokeParticle;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.utils.BlockColor;

import java.util.ArrayDeque;
import java.util.Queue;

import static org.cloudburstmc.server.block.BlockTypes.*;
import static org.cloudburstmc.server.utils.data.SpongeType.DRY;
import static org.cloudburstmc.server.utils.data.SpongeType.WET;

public class BlockBehaviorSponge extends BlockBehaviorSolid {


    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.YELLOW_BLOCK_COLOR;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        Level level = block.getLevel();

        val state = item.getBehavior().getBlock(item);
        boolean blockSet = placeBlock(block, state);

        if (blockSet) {
            val type = state.ensureTrait(BlockTraits.SPONGE_TYPE);
            if (type == WET && level.getDimension() == Level.DIMENSION_NETHER) {
                block.set(state.withTrait(BlockTraits.SPONGE_TYPE, DRY));

                block.getLevel().addSound(block.getPosition(), Sound.RANDOM_FIZZ);

                for (int i = 0; i < 8; ++i) {
                    //TODO: Use correct smoke particle
                    block.getLevel().addParticle(new SmokeParticle(block.getPosition().add(Math.random(), 1, Math.random())));
                }
            } else if (type == DRY && performWaterAbsorb(block.refresh())) {
                block.set(state.withTrait(BlockTraits.SPONGE_TYPE, WET));

                for (int i = 0; i < 4; i++) {
                    LevelEventPacket packet = new LevelEventPacket();
                    packet.setType(LevelEventType.PARTICLE_DESTROY_BLOCK);
                    packet.setPosition(block.getPosition().toFloat().add(0.5, 0.5, 0.5));
                    packet.setData(BlockRegistry.get().getRuntimeId(BlockStates.FLOWING_WATER));
                    level.addChunkPacket(block.getPosition(), packet);
                }
            }
        }
        return blockSet;
    }

    private boolean performWaterAbsorb(Block block) {
        Queue<Entry> entries = new ArrayDeque<>();

        entries.add(new Entry(block, 0));

        Entry entry;
        int waterRemoved = 0;
        while (waterRemoved < 64 && (entry = entries.poll()) != null) {
            for (Direction face : Direction.values()) {

                val faceBlock = entry.block.getSide(face);
                val faceState = faceBlock.getState();
                if (faceState.getType() == FLOWING_WATER || faceState.getType() == WATER) {
                    faceBlock.set(BlockStates.AIR);

                    ++waterRemoved;
                    if (entry.distance < 6) {
                        entries.add(new Entry(faceBlock, entry.distance + 1));
                    }
                } else if (faceState.getType() == AIR) {
                    if (entry.distance < 6) {
                        entries.add(new Entry(faceBlock, entry.distance + 1));
                    }
                }
            }
        }
        return waterRemoved > 0;
    }

    private static class Entry {
        private final Block block;
        private final int distance;

        public Entry(Block block, int distance) {
            this.block = block;
            this.distance = distance;
        }
    }
}
