package org.cloudburstmc.server.block.behavior;

import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.utils.BlockColor;

import java.util.concurrent.ThreadLocalRandom;

public class BlockBehaviorDragonEgg extends BlockBehaviorFallable {

    @Override
    public float getHardness() {
        return 3;
    }

    @Override
    public float getResistance() {
        return 45;
    }

    @Override
    public int getLightLevel() {
        return 1;
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.OBSIDIAN_BLOCK_COLOR;
    }

    @Override
    public boolean isTransparent() {
        return true;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_TOUCH) {
            this.teleport();
        }
        return super.onUpdate(block, type);
    }

    public void teleport() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 1000; ++i) {
            BlockState t = this.getLevel().getBlock(this.getPosition().add(random.nextInt(-16, 16),
                    random.nextInt(-16, 16), random.nextInt(-16, 16)));
            if (t.getId() == BlockTypes.AIR) {
                int diffX = this.getX() - t.getX();
                int diffY = this.getY() - t.getY();
                int diffZ = this.getZ() - t.getZ();
                LevelEventPacket pk = new LevelEventPacket();
                pk.setType(LevelEventType.PARTICLE_DRAGON_EGG);
                pk.setData((((((Math.abs(diffX) << 16) | (Math.abs(diffY) << 8)) | Math.abs(diffZ)) |
                        ((diffX < 0 ? 1 : 0) << 24)) | ((diffY < 0 ? 1 : 0) << 25)) | ((diffZ < 0 ? 1 : 0) << 26));
                pk.setPosition(this.getPosition().toFloat().add(0.5, 0.5, 0.5));
                this.getLevel().addChunkPacket(this.getPosition(), pk);
                this.getLevel().setBlock(this.getPosition(), get(BlockTypes.AIR), true);
                this.getLevel().setBlock(t.getPosition(), this, true);
                return;
            }
        }
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
