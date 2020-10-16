package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.Server;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.entity.Entity;
import org.cloudburstmc.server.event.block.BlockGrowEvent;
import org.cloudburstmc.server.event.entity.EntityDamageByBlockEvent;
import org.cloudburstmc.server.event.entity.EntityDamageEvent;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.math.Direction.Plane;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

import static org.cloudburstmc.server.block.BlockTypes.*;

public class BlockBehaviorCactus extends BlockBehaviorTransparent {


    @Override
    public boolean hasEntityCollision() {
        return true;
    }

//    @Override //TODO: bounding box
//    public float getMinX() {
//        return this.getX() + 0.0625f;
//    }
//
//    @Override
//    public float getMinY() {
//        return this.getY();
//    }
//
//    @Override
//    public float getMinZ() {
//        return this.getZ() + 0.0625f;
//    }
//
//    @Override
//    public float getMaxX() {
//        return this.getX() + 0.9375f;
//    }
//
//    @Override
//    public float getMaxY() {
//        return this.getY() + 0.9375f;
//    }
//
//    @Override
//    public float getMaxZ() {
//        return this.getZ() + 0.9375f;
//    }


//    @Override
//    public AxisAlignedBB getBoundingBox(Block block) {
//        return new SimpleAxisAlignedBB(block.getX(), block.getY(), block.getZ(), block.getX() + 1, block.getY() + 1, block.getZ() + 1);
//    }

    @Override
    public void onEntityCollide(Block block, Entity entity) {
        entity.attack(new EntityDamageByBlockEvent(block, entity, EntityDamageEvent.DamageCause.CONTACT, 1));
    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            Block down = block.down();
            BlockState downState = down.getState();
            if (downState.getType() != SAND && downState.getType() != CACTUS) {
                block.getLevel().useBreakOn(block.getPosition());
            } else {
                for (Direction direction : Plane.HORIZONTAL) {
                    Block side = block.getSide(direction);
                    if (!side.getState().getBehavior().canBeFlooded(side.getState())) {
                        block.getLevel().useBreakOn(block.getPosition());
                    }
                }
            }
        } else if (type == Level.BLOCK_UPDATE_RANDOM) {
            if (block.down().getState().getType() != CACTUS) {
                int age = block.getState().ensureTrait(BlockTraits.AGE);

                if (age == 0x0f) {
                    for (int y = 1; y < 3; ++y) {
                        Block b = block.getLevel().getBlock(block.getX(), block.getY() + y, block.getZ());
                        if (b.getState().getType() == AIR) {
                            BlockGrowEvent event = new BlockGrowEvent(b, BlockState.get(CACTUS));
                            Server.getInstance().getEventManager().fire(event);
                            if (!event.isCancelled()) {
                                block.set(event.getNewState(), true);
                            }
                        }
                    }

                    block.set(block.getState().withTrait(BlockTraits.AGE, 0), false, false);
                } else {
                    block.set(block.getState().withTrait(BlockTraits.AGE, age + 1), false, false);
                }
            }
        }

        return 0;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        Block down = block.down();
        if (!block.isWaterlogged() && (down.getState().getType() == SAND || down.getState().getType() == CACTUS)) {
            for (Direction direction : Plane.HORIZONTAL) {
                BlockState state = block.getSide(direction).getState();

                if (!state.getBehavior().canBeFlooded(state)) {
                    return false;
                }
            }

            placeBlock(block, item);
            return true;
        }
        return false;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.FOLIAGE_BLOCK_COLOR;
    }

    @Override
    public ItemStack[] getDrops(Block block, ItemStack hand) {
        return new ItemStack[]{
                ItemStack.get(CACTUS, 1)
        };
    }


}
