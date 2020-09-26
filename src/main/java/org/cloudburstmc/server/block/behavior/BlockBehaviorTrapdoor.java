package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.event.block.BlockRedstoneEvent;
import org.cloudburstmc.server.event.block.DoorToggleEvent;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.level.Sound;
import org.cloudburstmc.server.math.AxisAlignedBB;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.math.SimpleAxisAlignedBB;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorTrapdoor extends BlockBehaviorTransparent {

    public static final int TRAPDOOR_OPEN_BIT = 0x08;
    public static final int TRAPDOOR_TOP_BIT = 0x04;

    private static final AxisAlignedBB[] boundingBoxDamage = new AxisAlignedBB[16];

    protected BlockColor blockColor;

    public BlockBehaviorTrapdoor() {
        this(BlockColor.WOOD_BLOCK_COLOR);
    }

    public BlockBehaviorTrapdoor(BlockColor blockColor) {
        this.blockColor = blockColor;
    }

    static {
        for (int damage = 0; damage < 16; damage++) {
            AxisAlignedBB bb;
            float f = 0.1875f;
            if ((damage & TRAPDOOR_TOP_BIT) > 0) {
                bb = new SimpleAxisAlignedBB(
                        0,
                        1 - f,
                        0,
                        1,
                        1,
                        1
                );
            } else {
                bb = new SimpleAxisAlignedBB(
                        0,
                        0,
                        0,
                        1,
                        0 + f,
                        1
                );
            }
            if ((damage & TRAPDOOR_OPEN_BIT) > 0) {
                if ((damage & 0x03) == 0) {
                    bb.setBounds(
                            0,
                            0,
                            1 - f,
                            1,
                            1,
                            1
                    );
                } else if ((damage & 0x03) == 1) {
                    bb.setBounds(
                            0,
                            0,
                            0,
                            1,
                            1,
                            0 + f
                    );
                }
                if ((damage & 0x03) == 2) {
                    bb.setBounds(
                            1 - f,
                            0,
                            0,
                            1,
                            1,
                            1
                    );
                }
                if ((damage & 0x03) == 3) {
                    bb.setBounds(
                            0,
                            0,
                            0,
                            0 + f,
                            1,
                            1
                    );
                }
            }
            boundingBoxDamage[damage] = bb;
        }
    }

    @Override
    public float getHardness() {
        return 3;
    }

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public ToolType getToolType() {
        return ItemToolBehavior.TYPE_AXE;
    }

    @Override
    public float getResistance() {
        return 15;
    }

//    private AxisAlignedBB getRelativeBoundingBox() { //TODO: bounding box
//        return boundingBoxDamage[this.getMeta()];
//    }
//
//    @Override
//    public float getMinX() {
//        return this.getX() + getRelativeBoundingBox().getMinX();
//    }
//
//    @Override
//    public float getMaxX() {
//        return this.getX() + getRelativeBoundingBox().getMaxX();
//    }
//
//    @Override
//    public float getMinY() {
//        return this.getY() + getRelativeBoundingBox().getMinY();
//    }
//
//    @Override
//    public float getMaxY() {
//        return this.getY() + getRelativeBoundingBox().getMaxY();
//    }
//
//    @Override
//    public float getMinZ() {
//        return this.getZ() + getRelativeBoundingBox().getMinZ();
//    }
//
//    @Override
//    public float getMaxZ() {
//        return this.getZ() + getRelativeBoundingBox().getMaxZ();
//    }

//    public static BlockFactory factory(BlockColor blockColor) {
//        return identifier -> new BlockBehaviorTrapdoor(identifier, blockColor);
//    }

    @Override
    public int onUpdate(Block block, int type) {
        if (type == Level.BLOCK_UPDATE_REDSTONE) {
            val level = block.getLevel();
            val open = isOpen(block.getState());
            if ((!open && level.isBlockPowered(block.getPosition())) || (open && !level.isBlockPowered(block.getPosition()))) {
                level.getServer().getEventManager().fire(new BlockRedstoneEvent(block, open ? 15 : 0, open ? 0 : 15));

                block.set(block.getState().toggleTrait(BlockTraits.IS_OPEN));
                level.addSound(block.getPosition(), open ? Sound.RANDOM_DOOR_CLOSE : Sound.RANDOM_DOOR_OPEN);
                return type;
            }
        }

        return 0;
    }

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(block.getState().defaultState());
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        if (toggle(block, player)) {
            block.getLevel().addSound(block.getPosition(), isOpen(block.getState()) ? Sound.RANDOM_DOOR_CLOSE : Sound.RANDOM_DOOR_OPEN);
            return true;
        }
        return false;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        Direction facing;
        boolean top;

        if (face.getAxis().isHorizontal() || player == null) {
            facing = face;
            top = clickPos.getY() > 0.5f;
        } else {
            facing = player.getDirection().getOpposite();
            top = face != Direction.UP;
        }

        return placeBlock(block, item.getBlock()
                .withTrait(BlockTraits.DIRECTION, facing)
                .withTrait(BlockTraits.IS_UPSIDE_DOWN, top)
        );
    }

    public boolean toggle(Block block, Player player) {
        DoorToggleEvent ev = new DoorToggleEvent(block, player);
        block.getLevel().getServer().getEventManager().fire(ev);
        if (ev.isCancelled()) {
            return false;
        }

        block.set(block.getState().toggleTrait(BlockTraits.IS_OPEN), true);
        return true;
    }

    public boolean isOpen(BlockState state) {
        return state.ensureTrait(BlockTraits.IS_OPEN);
    }

    public boolean isTop(BlockState state) {
        return state.ensureTrait(BlockTraits.IS_UPSIDE_DOWN);
    }

    @Override
    public BlockColor getColor(Block block) {
        return this.blockColor;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
