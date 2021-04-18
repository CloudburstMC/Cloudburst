package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.BlockColor;

import static org.cloudburstmc.api.block.BlockTypes.FLOWING_WATER;
import static org.cloudburstmc.api.block.BlockTypes.WATER;

public class BlockBehaviorWater extends BlockBehaviorLiquid {

    public BlockBehaviorWater() {
        super(FLOWING_WATER, WATER);
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        boolean success = target.getLevel().setBlockState(block.getPosition(), item.getBehavior().getBlock(item), true, false);
        if (success) block.getLevel().scheduleUpdate(block.getPosition(), this.tickRate());

        return success;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.WATER_BLOCK_COLOR;
    }

    @Override
    public void onEntityCollide(Block block, Entity entity) {
        super.onEntityCollide(block, entity);

        if (entity.isOnFire()) {
            entity.extinguish();
        }
    }

    @Override
    public int tickRate() {
        return 5;
    }

    @Override
    public boolean usesWaterLogging() {
        return true;
    }
}
