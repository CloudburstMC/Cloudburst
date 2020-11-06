package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.*;
import org.cloudburstmc.server.event.block.BlockRedstoneEvent;
import org.cloudburstmc.server.event.block.DoorToggleEvent;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;

public abstract class BlockBehaviorDoor extends BlockBehaviorTransparent {

    public static int DOOR_OPEN_BIT = 0x04;
    public static int DOOR_TOP_BIT = 0x08;
    public static int DOOR_HINGE_BIT = 0x01;
    public static int DOOR_POWERED_BIT = 0x02;

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }


//    @Override
//    public AxisAlignedBB getBoundingBox(Block block) {

//        float f = 0.1875f;
//
//        AxisAlignedBB bb = new SimpleAxisAlignedBB(
//                block.getX(),
//                block.getY(),
//                block.getZ(),
//                block.getX() + 1,
//                block.getY() + 2,
//                block.getZ() + 1
//        );
//
//        int j = isTop() ? (block.down().getMeta() & 0x03) : getMeta() & 0x03;
//        boolean isOpen = isOpen();
//        boolean isRight = isRightHinged();
//
//        if (j == 0) {
//            if (isOpen) {
//                if (!isRight) {
//                    bb.setBounds(
//                            this.getX(),
//                            this.getY(),
//                            this.getZ(),
//                            this.getX() + 1,
//                            this.getY() + 1,
//                            this.getZ() + f
//                    );
//                } else {
//                    bb.setBounds(
//                            this.getX(),
//                            this.getY(),
//                            this.getZ() + 1 - f,
//                            this.getX() + 1,
//                            this.getY() + 1,
//                            this.getZ() + 1
//                    );
//                }
//            } else {
//                bb.setBounds(
//                        this.getX(),
//                        this.getY(),
//                        this.getZ(),
//                        this.getX() + f,
//                        this.getY() + 1,
//                        this.getZ() + 1
//                );
//            }
//        } else if (j == 1) {
//            if (isOpen) {
//                if (!isRight) {
//                    bb.setBounds(
//                            this.getX() + 1 - f,
//                            this.getY(),
//                            this.getZ(),
//                            this.getX() + 1,
//                            this.getY() + 1,
//                            this.getZ() + 1
//                    );
//                } else {
//                    bb.setBounds(
//                            this.getX(),
//                            this.getY(),
//                            this.getZ(),
//                            this.getX() + f,
//                            this.getY() + 1,
//                            this.getZ() + 1
//                    );
//                }
//            } else {
//                bb.setBounds(
//                        this.getX(),
//                        this.getY(),
//                        this.getZ(),
//                        this.getX() + 1,
//                        this.getY() + 1,
//                        this.getZ() + f
//                );
//            }
//        } else if (j == 2) {
//            if (isOpen) {
//                if (!isRight) {
//                    bb.setBounds(
//                            this.getX(),
//                            this.getY(),
//                            this.getZ() + 1 - f,
//                            this.getX() + 1,
//                            this.getY() + 1,
//                            this.getZ() + 1
//                    );
//                } else {
//                    bb.setBounds(
//                            this.getX(),
//                            this.getY(),
//                            this.getZ(),
//                            this.getX() + 1,
//                            this.getY() + 1,
//                            this.getZ() + f
//                    );
//                }
//            } else {
//                bb.setBounds(
//                        this.getX() + 1 - f,
//                        this.getY(),
//                        this.getZ(),
//                        this.getX() + 1,
//                        this.getY() + 1,
//                        this.getZ() + 1
//                );
//            }
//        } else if (j == 3) {
//            if (isOpen) {
//                if (!isRight) {
//                    bb.setBounds(
//                            this.getX(),
//                            this.getY(),
//                            this.getZ(),
//                            this.getX() + f,
//                            this.getY() + 1,
//                            this.getZ() + 1
//                    );
//                } else {
//                    bb.setBounds(
//                            this.getX() + 1 - f,
//                            this.getY(),
//                            this.getZ(),
//                            this.getX() + 1,
//                            this.getY() + 1,
//                            this.getZ() + 1
//                    );
//                }
//            } else {
//                bb.setBounds(
//                        this.getX(),
//                        this.getY(),
//                        this.getZ() + 1 - f,
//                        this.getX() + 1,
//                        this.getY() + 1,
//                        this.getZ() + 1
//                );
//            }
//        }
//
//        return bb; //TODO: bounding box
//        return new SimpleAxisAlignedBB(block.getPosition(), block.getPosition().add(1, 1, 1));
//    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_NORMAL) {
            if (block.down().getState().getType() == BlockTypes.AIR) {
                Block up = block.up();

                if (up.getState().inCategory(BlockCategory.DOOR)) {
                    removeBlock(up, true);
                    block.getLevel().useBreakOn(block.getPosition());
                }

                return Level.BLOCK_UPDATE_NORMAL;
            }
        }

        if (type == Level.BLOCK_UPDATE_REDSTONE) {
            boolean open = isOpen(block);
            if ((!open && block.getLevel().isBlockPowered(block.getPosition())) || (open && !block.getLevel().isBlockPowered(block.getPosition()))) {
                block.getLevel().getServer().getEventManager().fire(new BlockRedstoneEvent(block, open ? 15 : 0, open ? 0 : 15));

                this.toggle(block, null);
            }
        }

        return 0;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (block.getY() > 254) return false;
        if (face == Direction.UP) {
            Block blockUp = block.up();
            BlockBehavior upBehavior = blockUp.getState().getBehavior();
            Block blockDown = block.down();
            BlockBehavior downBehavior = blockDown.getState().getBehavior();

            if (!upBehavior.canBeReplaced(blockUp) || downBehavior.isTransparent(blockDown.getState())) {
                return false;
            }

            Direction direction = player != null ? player.getHorizontalDirection() : Direction.NORTH;

            BlockState left = block.getSide(direction.rotateYCCW()).getState();
            BlockState right = block.getSide(direction.rotateY()).getState();

            BlockState door = item.getBehavior().getBlock(item).withTrait(BlockTraits.DIRECTION, direction);
            placeBlock(block, door);

            door = door.withTrait(BlockTraits.IS_UPPER_BLOCK, true);
            if (left.getType() == block.getState().getType() || (!right.getBehavior().isTransparent(right) && left.getBehavior().isTransparent(left))) { //Door hinge
                door = door.withTrait(BlockTraits.IS_DOOR_HINGE, false);
            }

            placeBlock(blockUp, door);

            Block newBlock = block.refresh();
            if (!this.isOpen(newBlock) && block.getLevel().isBlockPowered(block.getPosition())) {
                this.toggle(newBlock, null);
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean onBreak(Block block, ItemStack item) {
        Block otherPart;
        if (isTop(block.getState())) {
            otherPart = block.down();
        } else {
            otherPart = block.up();
        }

        BlockState state = otherPart.getState();
        if (state.getType() == block.getState().getType()) {
            state.getBehavior().removeBlock(otherPart, true);
        }

        super.onBreak(block, item);

        return true;
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        if (!this.toggle(block, player)) {
            return false;
        }

        block.getLevel().addSound(block.getPosition(), isOpen(block) ? Sound.RANDOM_DOOR_OPEN : Sound.RANDOM_DOOR_CLOSE);
        return true;
    }

    public boolean toggle(Block block, Player player) {
        DoorToggleEvent event = new DoorToggleEvent(block, player);
        block.getLevel().getServer().getEventManager().fire(event);

        if (event.isCancelled()) {
            return false;
        }

        Block down;
        if (isTop(block.getState())) {
            down = block.down();
        } else {
            down = block;
        }

        if (down.up().getState().getType() != down.getState().getType()) {
            return false;
        }

        down.set(down.getState().toggleTrait(BlockTraits.IS_OPEN), true);
        return true;
    }

    public boolean isOpen(Block block) {
        if (isTop(block.getState())) {
            return block.down().getState().ensureTrait(BlockTraits.IS_OPEN);
        } else {
            return block.getState().ensureTrait(BlockTraits.IS_OPEN);
        }
    }

    public boolean isTop(BlockState state) {
        return state.ensureTrait(BlockTraits.IS_UPPER_BLOCK);
    }

    public boolean isRightHinged(BlockState state) {
        return state.ensureTrait(BlockTraits.IS_DOOR_HINGE);
    }


}
