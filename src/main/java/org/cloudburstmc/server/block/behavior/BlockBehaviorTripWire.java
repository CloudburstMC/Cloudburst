package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.registry.CloudBlockRegistry;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import static org.cloudburstmc.api.block.BlockTypes.TRIPWIRE;

public class BlockBehaviorTripWire extends FloodableBlockBehavior {

    @Override
    public ItemStack toItem(Block block) {
        return CloudItemRegistry.get().getItem(ItemTypes.STRING);
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

        var state = block.getState();
        boolean powered = this.isPowered(state);

        if (!powered) {
            var bs = state.withTrait(BlockTraits.IS_POWERED, true);
            block.set(bs, true, false);
            this.updateHook(block, bs, false);

            block.getLevel().scheduleUpdate(block.refresh().getPosition(), 10);
        }
    }

    public void updateHook(Block block, BlockState blockState, boolean scheduleUpdate) {
        for (Direction side : new Direction[]{Direction.SOUTH, Direction.WEST}) {
            for (int i = 1; i < 42; ++i) {
                var b = block.getSide(side, i);
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
        if (type == CloudLevel.BLOCK_UPDATE_SCHEDULED) {
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
                block.getLevel().scheduleUpdate(block.getPosition(), 10);
            } else {
                var state = block.getState().withTrait(BlockTraits.IS_POWERED, false);
                block.set(state, true, false);
                this.updateHook(block, state, false);
            }
            return type;
        }

        return 0;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        var state = CloudBlockRegistry.get().getBlock(TRIPWIRE);
        placeBlock(block, state);
        this.updateHook(block, state, false);

        return true;
    }

    @Override
    public boolean onBreak(Block block, ItemStack item) {
        if (item.getType() == ItemTypes.SHEARS) {
            var state = block.getState().withTrait(BlockTraits.IS_DISARMED, true);
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


}
