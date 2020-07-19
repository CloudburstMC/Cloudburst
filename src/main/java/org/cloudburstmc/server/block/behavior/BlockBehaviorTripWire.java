package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.Identifier;

import static org.cloudburstmc.server.block.BlockTypes.TRIPWIRE;

/**
 * @author CreeperFace
 */
public class BlockBehaviorTripWire extends FloodableBlockBehavior {

    public BlockBehaviorTripWire(Identifier id) {
        super(id);
    }

    @Override
    public boolean canPassThrough() {
        return true;
    }

    @Override
    public float getResistance() {
        return 0;
    }

    @Override
    public float getHardness() {
        return 0;
    }

    @Override
    public AxisAlignedBB getBoundingBox() {
        return null;
    }

    @Override
    public Item toItem() {
        return Item.get(ItemIds.STRING);
    }

    public boolean isPowered() {
        return (this.getMeta() & 1) > 0;
    }

    public void setPowered(boolean value) {
        if (value ^ this.isPowered()) {
            this.setMeta(this.getMeta() ^ 0x01);
        }
    }

    public boolean isAttached() {
        return (this.getMeta() & 4) > 0;
    }

    public void setAttached(boolean value) {
        if (value ^ this.isAttached()) {
            this.setMeta(this.getMeta() ^ 0x04);
        }
    }

    public boolean isDisarmed() {
        return (this.getMeta() & 8) > 0;
    }

    public void setDisarmed(boolean value) {
        if (value ^ this.isDisarmed()) {
            this.setMeta(this.getMeta() ^ 0x08);
        }
    }

    @Override
    public void onEntityCollide(Entity entity) {
        if (!entity.canTriggerPressurePlate()) {
            return;
        }

        boolean powered = this.isPowered();

        if (!powered) {
            this.setPowered(true);
            this.level.setBlock(this.getPosition(), this, true, false);
            this.updateHook(false);

            this.level.scheduleUpdate(this, 10);
        }
    }

    public void updateHook(boolean scheduleUpdate) {
        for (BlockFace side : new BlockFace[]{BlockFace.SOUTH, BlockFace.WEST}) {
            for (int i = 1; i < 42; ++i) {
                BlockState blockState = this.getSide(side, i);

                if (blockState instanceof BlockBehaviorTripWireHook) {
                    BlockBehaviorTripWireHook hook = (BlockBehaviorTripWireHook) blockState;

                    if (hook.getFacing() == side.getOpposite()) {
                        hook.calculateState(false, true, i, this);
                    }

                    /*if(scheduleUpdate) {
                        this.level.scheduleUpdate(hook, 10);
                    }*/
                    break;
                }

                if (blockState.getId() != TRIPWIRE) {
                    break;
                }
            }
        }
    }

    @Override
    public int onUpdate(int type) {
        if (type == Level.BLOCK_UPDATE_SCHEDULED) {
            if (!isPowered()) {
                return type;
            }

            boolean found = false;
            for (Entity entity : this.level.getCollidingEntities(this.getCollisionBoxes())) {
                if (!entity.canTriggerPressurePlate()) {
                    continue;
                }

                found = true;
            }

            if (found) {
                this.level.scheduleUpdate(this, 10);
            } else {
                this.setPowered(false);
                this.level.setBlock(this.getPosition(), this, true, false);
                this.updateHook(false);
            }
            return type;
        }

        return 0;
    }

    @Override
    public boolean place(Item item, BlockState blockState, BlockState target, BlockFace face, Vector3f clickPos, Player player) {
        this.getLevel().setBlock(this.getPosition(), this, true, true);
        this.updateHook(false);

        return true;
    }

    @Override
    public boolean onBreak(Item item) {
        if (item.getId() == ItemIds.SHEARS) {
            this.setDisarmed(true);
            this.level.setBlock(this.getPosition(), this, true, false);
            this.updateHook(false);
            super.onBreak(item);
        } else {
            this.setPowered(true);
            super.onBreak(item);
            this.updateHook(true);
        }

        return true;
    }

    @Override
    public float getMaxY() {
        return this.getY() + 0.5f;
    }

    @Override
    protected AxisAlignedBB recalculateCollisionBoundingBox() {
        return this;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }

    @Override
    public boolean canWaterlogFlowing() {
        return true;
    }
}
