package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.BlockFactory;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

import java.util.Arrays;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class BlockBehaviorSlab extends BlockBehaviorTransparent {

    public static final BlockColor[] COLORS_1 = new BlockColor[]{
            BlockColor.STONE_BLOCK_COLOR,
            BlockColor.SAND_BLOCK_COLOR,
            BlockColor.WOOD_BLOCK_COLOR,
            BlockColor.STONE_BLOCK_COLOR,
            BlockColor.STONE_BLOCK_COLOR,
            BlockColor.QUARTZ_BLOCK_COLOR,
            BlockColor.NETHERRACK_BLOCK_COLOR
    };
    public static final BlockColor[] COLORS_2 = new BlockColor[]{
            BlockColor.ORANGE_BLOCK_COLOR,
            BlockColor.PURPLE_BLOCK_COLOR,
            BlockColor.CYAN_BLOCK_COLOR,
            BlockColor.CYAN_BLOCK_COLOR,
            BlockColor.CYAN_BLOCK_COLOR,
            BlockColor.STONE_BLOCK_COLOR,
            BlockColor.SAND_BLOCK_COLOR,
            BlockColor.NETHERRACK_BLOCK_COLOR
    };
    public static final BlockColor[] COLORS_3 = new BlockColor[]{
            BlockColor.WHITE_BLOCK_COLOR,
            BlockColor.ORANGE_BLOCK_COLOR,
            BlockColor.STONE_BLOCK_COLOR,
            BlockColor.STONE_BLOCK_COLOR,
            BlockColor.WHITE_BLOCK_COLOR,
            BlockColor.WHITE_BLOCK_COLOR,
            BlockColor.PINK_BLOCK_COLOR,
            BlockColor.PINK_BLOCK_COLOR
    };
    public static final BlockColor[] COLORS_4 = new BlockColor[]{
            BlockColor.STONE_BLOCK_COLOR,
            BlockColor.QUARTZ_BLOCK_COLOR,
            BlockColor.STONE_BLOCK_COLOR,
            BlockColor.SAND_BLOCK_COLOR,
            BlockColor.ORANGE_BLOCK_COLOR
    };

    private final Identifier doubleSlabId;
    private final BlockColor[] colors;

    public BlockBehaviorSlab(Identifier id, Identifier doubleSlabId, BlockColor[] colors) {
        super(id);
        this.doubleSlabId = doubleSlabId;
        this.colors = colors;
    }

    public static BlockFactory factory(Identifier doubleSlabId, BlockColor... colors) {
        return id -> new BlockBehaviorSlab(id, doubleSlabId, Arrays.copyOf(colors, 8));
    }

    @Override
    public BlockColor getColor() {
        return colors[this.getMeta() & 0x07];
    }

    @Override
    public float getMinY() {
        return this.isTopSlab() ? (this.getY() + 0.5f) : this.getY();
    }

    @Override
    public float getMaxY() {
        return this.isTopSlab() ? (this.getY() + 1f) : (this.getY() + 0.5f);
    }

    @Override
    public float getResistance() {
        return 30;
    }

    @Override
    public float getHardness() {
        return 2;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public Item toItem(BlockState state) {
        return Item.get(this.id, this.getMeta() & 0x07);
    }

    @Override
    public boolean place(Item item, Block block, Block target, BlockFace face, Vector3f clickPos, Player player) {
        int meta = this.getMeta() & 0x07;
        boolean isTop;
        BlockBehaviorDoubleSlab dSlab = (BlockBehaviorDoubleSlab) BlockRegistry.get().getBlock(this.doubleSlabId, meta);

        if (face == BlockFace.DOWN) {
            if (checkSlab(target) && ((BlockBehaviorSlab) target).isTopSlab()) {
                if (this.getLevel().setBlock(target.getPosition(), dSlab, true, false)) {
                    dSlab.playPlaceSound();
                    return true;
                }
                return false;
            } else if (checkSlab(blockState) && !((BlockBehaviorSlab) blockState).isTopSlab()) {
                if (this.getLevel().setBlock(blockState.getPosition(), dSlab, true, false)) {
                    dSlab.playPlaceSound();
                    return true;
                }
                return false;
            } else {
                isTop = true;
            }
        } else if (face == BlockFace.UP) {
            if (checkSlab(target) && !((BlockBehaviorSlab) target).isTopSlab()) {
                if (this.getLevel().setBlock(target.getPosition(), dSlab, true, false)) {
                    dSlab.playPlaceSound();
                    return true;
                }
                return false;
            } else if (checkSlab(blockState) && ((BlockBehaviorSlab) blockState).isTopSlab()) {
                if (this.getLevel().setBlock(blockState.getPosition(), dSlab, true, false)) {
                    dSlab.playPlaceSound();
                    return true;
                }
                return false;
            } else {
                isTop = false;
            }
        } else { // Horizontal face
            isTop = clickPos.getY() >= 0.5f;
            if (checkSlab(blockState)
                    && ((isTop && !((BlockBehaviorSlab) blockState).isTopSlab())
                    || (!isTop && ((BlockBehaviorSlab) blockState).isTopSlab()))) {
                if (this.getLevel().setBlock(blockState.getPosition(), dSlab, true, false)) {
                    dSlab.playPlaceSound();
                    return true;
                }
                return false;
            }
        }

        if (blockState instanceof BlockBehaviorSlab && (target.getMeta() & 0x07) != (this.getMeta() & 0x07)) {
            return false;
        }
        this.setMeta(meta + (isTop ? 0x08 : 0));
        return this.getLevel().setBlock(blockState.getPosition(), this, true, true);
    }

    private boolean isTopSlab() {
        return (this.getMeta() & 0x08) == 0x08;
    }

    private boolean checkSlab(BlockState other) {
        return other instanceof BlockBehaviorSlab && ((other.getMeta() & 0x07) == (this.getMeta() & 0x07));
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
