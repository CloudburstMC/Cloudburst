package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;

import static org.cloudburstmc.server.block.BlockTypes.TRIPWIRE;

public class BlockBehaviorTripWire extends FloodableBlockBehavior {

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
    public ItemStack toItem(Block block) {
        return ItemStack.get(ItemTypes.STRING);
    }

    public boolean isPowered(BlockState state) {
        return state.ensureTrait(BlockTraits.IS_POWERED);
    }

    public boolean isAttached(BlockState state) {
        return state.ensureTrait(BlockTraits.IS_ATTACHED);
    }

    public boolean isDisarmed(BlockState state) {
        return state.ensureTrait(BlockTraits.IS_DISARMED);
    }

    @Override
    public void onEntityCollide(Block block, Entity entity) {
        if (!entity.canTriggerPressurePlate()) {
            return;
        }

        val state = block.getState();
        boolean powered = this.isPowered(state);

        if (!powered) {
            val bs = state.withTrait(BlockTraits.IS_POWERED, true);
            block.set(bs, true, false);
            this.updateHook(block, bs, false);

            block.getLevel().scheduleUpdate(block.refresh(), 10);
        }
    }

    public void updateHook(Block block, BlockState blockState, boolean scheduleUpdate) {
        for (Direction side : new Direction[]{Direction.SOUTH, Direction.WEST}) {
            for (int i = 1; i < 42; ++i) {
                val b = block.getSide(side, i);
                BlockState state = b.getState();

                if (state.getType() == BlockTypes.TRIPWIRE_HOOK) {
                    if (state.ensureTrait(BlockTraits.DIRECTION) == side.getOpposite()) {
                        ((BlockBehaviorTripWireHook) state.getBehavior()).calculateState(b, false, true, i, blockState);
                    }

                    /*if(scheduleUpdate) {
                        this.level.scheduleUpdate(hook, 10);
                    }*/
                    break;
                }

                if (state.getType() != TRIPWIRE) {
                    break;
                }
            }
        }
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_SCHEDULED) {
            if (!isPowered(block.getState())) {
                return type;
            }

            boolean found = false;
            for (Entity entity : block.getLevel().getCollidingEntities(this.getCollisionBoxes(block))) {
                if (!entity.canTriggerPressurePlate()) {
                    continue;
                }

                found = true;
            }

            if (found) {
                block.getLevel().scheduleUpdate(block, 10);
            } else {
                val state = block.getState().withTrait(BlockTraits.IS_POWERED, false);
                block.set(state, true, false);
                this.updateHook(block, state, false);
            }
            return type;
        }

        return 0;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        val state = BlockState.get(TRIPWIRE);
        placeBlock(block, state);
        this.updateHook(block, state, false);

        return true;
    }

    @Override
    public boolean onBreak(Block block, ItemStack item) {
        if (item.getType() == ItemTypes.SHEARS) {
            val state = block.getState().withTrait(BlockTraits.IS_DISARMED, true);
            block.set(state, true, false);

            this.updateHook(block, state, false);
            super.onBreak(block, item);
        } else {
            super.onBreak(block, item);
            this.updateHook(block, block.getState().withTrait(BlockTraits.IS_POWERED, true), true);
        }

        return true;
    }

//    @Override //TODO: bounding box
//    public float getMaxY() {
//        return this.getY() + 0.5f;
//    }
//
//    @Override
//    protected AxisAlignedBB recalculateCollisionBoundingBox() {
//        return this;
//    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }

    @Override
    public boolean canWaterlogFlowing() {
        return true;
    }
}
