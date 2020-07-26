package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
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

public class BlockBehaviorSponge extends BlockBehaviorSolid {

    @Override
    public float getHardness() {
        return 0.6f;
    }

    @Override
    public float getResistance() {
        return 3;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.YELLOW_BLOCK_COLOR;
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        Level level = block.getLevel();
        boolean blockSet = level.setBlock(block.getPosition(), this);

        if (blockSet) {
            if (this.getMeta() == WET && level.getDimension() == Level.DIMENSION_NETHER) {
                level.setBlock(block.getPosition(), BlockState.get(SPONGE, DRY));
                this.getLevel().addSound(block.getPosition(), Sound.RANDOM_FIZZ);

                for (int i = 0; i < 8; ++i) {
                    //TODO: Use correct smoke particle
                    this.getLevel().addParticle(new SmokeParticle(block.getPosition().add(Math.random(), 1, Math.random())));
                }
            } else if (this.getMeta() == DRY && performWaterAbsorb(block)) {
                level.setBlock(block.getPosition(), BlockState.get(SPONGE, WET));

                for (int i = 0; i < 4; i++) {
                    LevelEventPacket packet = new LevelEventPacket();
                    packet.setType(LevelEventType.PARTICLE_DESTROY_BLOCK);
                    packet.setPosition(block.getPosition().toFloat().add(0.5, 0.5, 0.5));
                    packet.setData(BlockRegistry.get().getRuntimeId(FLOWING_WATER, 0));
                    level.addChunkPacket(this.getPosition(), packet);
                }
            }
        }
        return blockSet;
    }

    private boolean performWaterAbsorb(BlockState blockState) {
        Queue<Entry> entries = new ArrayDeque<>();

        entries.add(new Entry(blockState, 0));

        Entry entry;
        int waterRemoved = 0;
        while (waterRemoved < 64 && (entry = entries.poll()) != null) {
            for (Direction face : Direction.values()) {

                BlockState faceBlockState = entry.blockState.getSide(face);
                if (faceBlockState.getId() == FLOWING_WATER || faceBlockState.getId() == WATER) {
                    this.level.setBlock(faceBlockState.getPosition(), BlockState.get(AIR, 0));
                    ++waterRemoved;
                    if (entry.distance < 6) {
                        entries.add(new Entry(faceBlockState, entry.distance + 1));
                    }
                } else if (faceBlockState.getId() == AIR) {
                    if (entry.distance < 6) {
                        entries.add(new Entry(faceBlockState, entry.distance + 1));
                    }
                }
            }
        }
        return waterRemoved > 0;
    }

    private static class Entry {
        private final BlockState blockState;
        private final int distance;

        public Entry(BlockState blockState, int distance) {
            this.blockState = blockState;
            this.distance = distance;
        }
    }
}
