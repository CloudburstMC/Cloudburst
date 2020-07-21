package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

public class BlockBehaviorFarmland extends BlockBehaviorTransparent {

    @Override
    public float getResistance() {
        return 3;
    }

    @Override
    public float getHardness() {
        return 0.6f;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_SHOVEL;
    }

    @Override
    public float getMaxY() {
        return this.getY() + 0.9375f;
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_RANDOM) {
            Vector3f v = Vector3f.ZERO;

            Vector3i upPos = this.getPosition().up();
            if (this.level.getBlock(upPos) instanceof BlockBehaviorCrops) {
                return 0;
            }

            if (this.level.getBlock(upPos).isSolid()) {
                this.level.setBlock(this.getPosition(), BlockState.get(BlockTypes.DIRT), false, true);

                return Level.BLOCK_UPDATE_RANDOM;
            }

            boolean found = false;

            if (this.level.isRaining()) {
                found = true;
            } else {
                for (int x = this.getX() - 4; x <= this.getX() + 4; x++) {
                    for (int z = this.getZ() - 4; z <= this.getZ() + 4; z++) {
                        for (int y = this.getY(); y <= this.getY() + 1; y++) {
                            if (z == this.getZ() && x == this.getX() && y == this.getY()) {
                                continue;
                            }

                            Identifier block = this.level.getBlockId(x, y, z);

                            if (block == BlockTypes.FLOWING_WATER || block == BlockTypes.WATER) {
                                found = true;
                                break;
                            }
                        }
                    }
                }
            }

            BlockState blockState = this.level.getBlock(this.getPosition().down());
            if (found || blockState instanceof BlockBehaviorWater) {
                if (this.getMeta() < 7) {
                    this.setMeta(7);
                    this.level.setBlock(this.getPosition(), this, false, false);
                }
                return Level.BLOCK_UPDATE_RANDOM;
            }

            if (this.getMeta() > 0) {
                this.setMeta(this.getMeta() - 1);
                this.level.setBlock(this.getPosition(), this, false, false);
            } else {
                this.level.setBlock(this.getPosition(), BlockState.get(BlockTypes.DIRT), false, true);
            }

            return Level.BLOCK_UPDATE_RANDOM;
        }

        return 0;
    }

    @Override
    public Item toItem(BlockState state) {
        return Item.get(BlockTypes.DIRT);
    }

    @Override
    public BlockColor getColor() {
        return BlockColor.DIRT_BLOCK_COLOR;
    }
}
