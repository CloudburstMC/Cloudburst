package org.cloudburstmc.server.block.behavior;

import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.packet.LevelEventPacket;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.player.Player;
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
    public int getLightLevel(Block block) {
        return 1;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.OBSIDIAN_BLOCK_COLOR;
    }

    @Override
    public boolean isTransparent() {
        return true;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == World.BLOCK_UPDATE_TOUCH) {
            this.teleport(block);
        }
        return super.onUpdate(block, type);
    }

    public void teleport(Block block) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 1000; ++i) {
            Block t = block.getWorld().getBlock(block.getPosition().add(random.nextInt(-16, 16),
                    random.nextInt(-16, 16), random.nextInt(-16, 16)));
            BlockBehavior behavior = t.getState().getBehavior();
            if (t.getState().getType() == BlockIds.AIR || (behavior instanceof BlockBehaviorLiquid && ((BlockBehaviorLiquid) behavior).usesWaterLogging())) {
                int diffX = block.getX() - t.getX();
                int diffY = block.getY() - t.getY();
                int diffZ = block.getZ() - t.getZ();
                LevelEventPacket pk = new LevelEventPacket();
                pk.setType(LevelEventType.PARTICLE_DRAGON_EGG);
                pk.setData((((((Math.abs(diffX) << 16) | (Math.abs(diffY) << 8)) | Math.abs(diffZ)) |
                        ((diffX < 0 ? 1 : 0) << 24)) | ((diffY < 0 ? 1 : 0) << 25)) | ((diffZ < 0 ? 1 : 0) << 26));
                pk.setPosition(block.getPosition().toFloat().add(0.5f, 0.5f, 0.5f));
                block.getWorld().addChunkPacket(block.getPosition(), pk);
                removeBlock(block, true);

                placeBlock(t, block.getState());
                return;
            }
        }
    }

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        teleport(block);
        return true;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
