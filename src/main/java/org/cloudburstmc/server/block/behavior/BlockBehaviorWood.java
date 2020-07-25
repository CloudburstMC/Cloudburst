package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

//Block state information: https://hastebin.com/emuvawasoj.js
public class BlockBehaviorWood extends BlockBehaviorSolid {
    public static final int OAK = 0;
    public static final int SPRUCE = 1;
    public static final int BIRCH = 2;
    public static final int JUNGLE = 3;
    public static final int ACACIA = 4;
    public static final int DARK_OAK = 5;
    private static final int STRIPPED_BIT = 0b1000;
    private static final int AXIS_Y = 0;
    private static final int AXIS_X = 1 << 4;
    private static final int AXIS_Z = 2 << 4;

    @Override
    public float getHardness() {
        return 2;
    }

    @Override
    public float getResistance() {
        return 15;
    }

    @Override
    public int getBurnChance() {
        return 5;
    }

    @Override
    public int getBurnAbility() {
        return 20;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_AXE;
    }

    @Override
    public Item toItem(BlockState state) {
        return Item.get(id, getMeta() & 0xF);
    }


    @Override
    public boolean canBeActivated() {
        return !isStripped();
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        if (!item.isAxe() || !player.isCreative() && !item.useOn(this)) {
            return false;
        }

        setMeta(getMeta() | STRIPPED_BIT);  // adds the offset for stripped woods
        getLevel().setBlock(this.getPosition(), this, true);
        return true;
    }

    public boolean isStripped() {
        return (getMeta() & STRIPPED_BIT) != 0;
    }

    public void setStripped(boolean stripped) {
        setMeta((getMeta() & ~STRIPPED_BIT) | (stripped ? STRIPPED_BIT : 0));
    }

    public Direction.Axis getAxis() {
        switch (getMeta() & 0x30) {
            default:
            case AXIS_Y:
                return Direction.Axis.Y;
            case AXIS_X:
                return Direction.Axis.X;
            case AXIS_Z:
                return Direction.Axis.Z;
        }
    }

    public void setAxis(Direction.Axis axis) {
        int axisProp;
        switch (axis) {
            default:
            case Y:
                axisProp = AXIS_Y;
                break;
            case X:
                axisProp = AXIS_X;
                break;
            case Z:
                axisProp = AXIS_Z;
                break;
        }
        setMeta((getMeta() & ~0x30) | axisProp);
    }

    @Override
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        setAxis(face.getAxis());
        return super.place(item, blockState, target, face, clickPos, player);
    }

    public int getWoodType() {
        return getMeta() & 0b111;
    }

    public void setWoodType(int woodType) {
        if (woodType < OAK || woodType > DARK_OAK) {
            woodType = OAK;
        }
        setMeta((getMeta() & -0b1000) | woodType);
    }

    @Override
    public BlockColor getColor(BlockState state) {
        switch (getWoodType()) {
            default:
            case OAK:
                return BlockColor.WOOD_BLOCK_COLOR;
            case SPRUCE:
                return BlockColor.SPRUCE_BLOCK_COLOR;
            case BIRCH:
                return BlockColor.SAND_BLOCK_COLOR;
            case JUNGLE:
                return BlockColor.DIRT_BLOCK_COLOR;
            case ACACIA:
                return BlockColor.ORANGE_BLOCK_COLOR;
            case DARK_OAK: //DARK OAK
                return BlockColor.BROWN_BLOCK_COLOR;
        }
    }
}
