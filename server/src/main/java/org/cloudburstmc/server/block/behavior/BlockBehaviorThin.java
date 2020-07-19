package org.cloudburstmc.server.block.behavior;

import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.math.SimpleAxisAlignedBB;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.LevelException;

import static org.cloudburstmc.server.block.BlockTypes.GLASS;
import static org.cloudburstmc.server.block.BlockTypes.GLASS_PANE;

/**
 * Created on 2015/12/6 by xtypr.
 * Package cn.nukkit.block in project Nukkit .
 */
public abstract class BlockBehaviorThin extends BlockBehaviorTransparent {

    public BlockBehaviorThin(Identifier id) {
        super(id);
    }

    @Override
    public boolean isSolid() {
        return false;
    }

    protected AxisAlignedBB recalculateBoundingBox() {
        float f = 0.4375f;
        float f1 = 0.5625f;
        float f2 = 0.4375f;
        float f3 = 0.5625f;
        try {
            boolean flag = this.canConnect(this.north());
            boolean flag1 = this.canConnect(this.south());
            boolean flag2 = this.canConnect(this.west());
            boolean flag3 = this.canConnect(this.east());
            if ((!flag2 || !flag3) && (flag2 || flag3 || flag || flag1)) {
                if (flag2) {
                    f = 0;
                } else if (flag3) {
                    f1 = 1;
                }
            } else {
                f = 0;
                f1 = 1;
            }
            if ((!flag || !flag1) && (flag2 || flag3 || flag || flag1)) {
                if (flag) {
                    f2 = 0;
                } else if (flag1) {
                    f3 = 1;
                }
            } else {
                f2 = 0;
                f3 = 1;
            }
        } catch (LevelException ignore) {
            //null sucks
        }
        return new SimpleAxisAlignedBB(
                this.getX() + f,
                this.getY(),
                this.getZ() + f2,
                this.getX() + f1,
                this.getY() + 1,
                this.getZ() + f3
        );
    }

    public boolean canConnect(BlockState blockState) {
        return blockState.isSolid() || blockState.getId() == this.getId() || blockState.getId() == GLASS_PANE || blockState.getId() == GLASS;
    }

}
