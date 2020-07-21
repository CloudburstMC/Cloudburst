package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

public class BlockBehaviorLog extends BlockBehaviorSolid {

    public static final int UP_DOWN = 0 << 2;
    public static final int EAST_WEST = 1 << 2;
    public static final int NORTH_SOUTH = 2 << 2;
    public static final int ALL = 3 << 2;

    protected static final Identifier[] STRIPPED_IDS = new Identifier[]{
            BlockTypes.STRIPPED_OAK_LOG,
            BlockTypes.STRIPPED_SPRUCE_LOG,
            BlockTypes.STRIPPED_BIRCH_LOG,
            BlockTypes.STRIPPED_JUNGLE_LOG,
            BlockTypes.STRIPPED_ACACIA_LOG,
            BlockTypes.STRIPPED_DARK_OAK_LOG
    };

    protected static final short[] FACES = new short[]{
            0,
            0,
            0b1000,
            0b1000,
            0b0100,
            0b0100
    };

    @Override
    public float getHardness() {
        return 2;
    }

    @Override
    public float getResistance() {
        return 10;
    }

    @Override
    public int getBurnChance() {
        return 5;
    }

    @Override
    public int getBurnAbility() {
        return 10;
    }

    public static void upgradeLegacyBlock(int[] blockState) {
        if ((blockState[1] & 0b1100) == 0b1100) { // old full bark texture
            blockState[0] = BlockRegistry.get().getLegacyId(BlockTypes.WOOD);
            blockState[1] = blockState[1] & 0x03; // gets only the log type and set pillar to y
        }
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean onActivate(Block block, Item item, Player player) {
        if (!item.isAxe() || !item.useOn(this)) {
            return false;
        }

        int log2Damage = this instanceof BlockBehaviorLog2 ? 4 : 0;
        int damage = (this.getMeta() & -0b100) ^ this.getMeta();
        BlockState strippedBlockState = BlockState.get(STRIPPED_IDS[damage + log2Damage], (this.getMeta() >> 2));
        this.getLevel().setBlock(this.getPosition(), strippedBlockState, true, true);
        return true;
    }

    @Override
    public boolean place(Item item, Block block, Block target, BlockFace face, Vector3f clickPos, Player player) {
        // Convert the old log bark to the new wood block
        if ((this.getMeta() & 0b1100) == 0b1100) {
            BlockState woodBlockState = BlockState.get(BlockTypes.WOOD, this.getMeta() & 0x03, this.getPosition(), this.getLevel());
            return woodBlockState.place(item, blockState, target, face, clickPos, player);
        }

        this.setMeta(((this.getMeta() & 0x03) | FACES[face.getIndex()]));
        this.getLevel().setBlock(blockState.getPosition(), this, true, true);

        return true;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_AXE;
    }

    @Override
    public BlockColor getColor() {
        switch (getMeta() & 0x03) {
            default:
            case OAK:
                return BlockColor.WOOD_BLOCK_COLOR;
            case SPRUCE:
                return BlockColor.SPRUCE_BLOCK_COLOR;
            case BIRCH:
                return BlockColor.SAND_BLOCK_COLOR;
            case JUNGLE:
                return BlockColor.DIRT_BLOCK_COLOR;
        }
    }

    @Override
    public Item toItem(BlockState state) {
        if ((getMeta() & 0b1100) == 0b1100) {
            return Item.get(BlockTypes.WOOD, this.getMeta() & 0x3);
        } else {
            return Item.get(id, this.getMeta() & 0x03);
        }
    }
}
