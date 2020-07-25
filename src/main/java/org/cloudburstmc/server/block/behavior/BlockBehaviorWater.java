package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockFactory;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

import static org.cloudburstmc.server.block.BlockTypes.FLOWING_WATER;

public class BlockBehaviorWater extends BlockBehaviorLiquid {

    protected BlockBehaviorWater(Identifier flowingId, Identifier stationaryId) {
        super(flowingId, stationaryId);
    }

    @Override
    public boolean place(Item item, Block block, Block target, BlockFace face, Vector3f clickPos, Player player) {
        boolean success = target.getLevel().setBlock(blockState.getPosition(), this, true, false);
        if (success) this.getLevel().scheduleUpdate(this, this.tickRate());

        return success;
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.WATER_BLOCK_COLOR;
    }

    @Override
    public BlockState getBlock(int meta) {
        return BlockState.get(FLOWING_WATER, meta);
    }

    @Override
    public void onEntityCollide(Entity entity) {
        super.onEntityCollide(entity);

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

    public static BlockFactory factory(Identifier stationaryId) {
        return id -> new BlockBehaviorWater(id, stationaryId);
    }
}
