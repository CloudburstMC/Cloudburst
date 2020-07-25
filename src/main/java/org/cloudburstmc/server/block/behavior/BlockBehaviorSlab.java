package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.data.StoneSlabType;

import java.util.EnumMap;

public class BlockBehaviorSlab extends BlockBehaviorTransparent {

    static final EnumMap<StoneSlabType, BlockColor> COLORS = new EnumMap<>(StoneSlabType.class);

    static {
        COLORS.put(StoneSlabType.SMOOTH_STONE, BlockColor.STONE_BLOCK_COLOR);
        COLORS.put(StoneSlabType.SANDSTONE, BlockColor.SAND_BLOCK_COLOR);
        COLORS.put(StoneSlabType.WOOD, BlockColor.WOOD_BLOCK_COLOR);
        COLORS.put(StoneSlabType.COBBLESTONE, BlockColor.STONE_BLOCK_COLOR);
        COLORS.put(StoneSlabType.BRICK, BlockColor.STONE_BLOCK_COLOR);
        COLORS.put(StoneSlabType.STONE_BRICK, BlockColor.STONE_BLOCK_COLOR);
        COLORS.put(StoneSlabType.QUARTZ, BlockColor.QUARTZ_BLOCK_COLOR);
        COLORS.put(StoneSlabType.NETHER_BRICK, BlockColor.NETHERRACK_BLOCK_COLOR);
        COLORS.put(StoneSlabType.RED_SANDSTONE, BlockColor.ORANGE_BLOCK_COLOR);
        COLORS.put(StoneSlabType.PURPUR, BlockColor.PURPLE_BLOCK_COLOR);
        COLORS.put(StoneSlabType.PRISMARINE_ROUGH, BlockColor.CYAN_BLOCK_COLOR);
        COLORS.put(StoneSlabType.PRISMARINE_DARK, BlockColor.CYAN_BLOCK_COLOR);
        COLORS.put(StoneSlabType.PRISMARINE_BRICK, BlockColor.CYAN_BLOCK_COLOR);
        COLORS.put(StoneSlabType.MOSSY_COBBLESTONE, BlockColor.STONE_BLOCK_COLOR);
        COLORS.put(StoneSlabType.SMOOTH_SANDSTONE, BlockColor.SAND_BLOCK_COLOR);
        COLORS.put(StoneSlabType.RED_NETHER_BRICK, BlockColor.NETHERRACK_BLOCK_COLOR);
        COLORS.put(StoneSlabType.END_STONE_BRICK, BlockColor.WHITE_BLOCK_COLOR);
        COLORS.put(StoneSlabType.SMOOTH_RED_SANDSTONE, BlockColor.ORANGE_BLOCK_COLOR);
        COLORS.put(StoneSlabType.POLISHED_ANDESITE, BlockColor.STONE_BLOCK_COLOR);
        COLORS.put(StoneSlabType.ANDESITE, BlockColor.STONE_BLOCK_COLOR);
        COLORS.put(StoneSlabType.DIORITE, BlockColor.WHITE_BLOCK_COLOR);
        COLORS.put(StoneSlabType.POLISHED_DIORITE, BlockColor.WHITE_BLOCK_COLOR);
        COLORS.put(StoneSlabType.GRANITE, BlockColor.PINK_BLOCK_COLOR);
        COLORS.put(StoneSlabType.POLISHED_GRANITE, BlockColor.PINK_BLOCK_COLOR);
        COLORS.put(StoneSlabType.MOSSY_STONE_BRICK, BlockColor.STONE_BLOCK_COLOR);
        COLORS.put(StoneSlabType.SMOOTH_QUARTZ, BlockColor.QUARTZ_BLOCK_COLOR);
        COLORS.put(StoneSlabType.STONE, BlockColor.STONE_BLOCK_COLOR);
        COLORS.put(StoneSlabType.CUT_SANDSTONE, BlockColor.SAND_BLOCK_COLOR);
        COLORS.put(StoneSlabType.CUT_RED_SANDSTONE, BlockColor.ORANGE_BLOCK_COLOR);
    }

    @Override
    public BlockColor getColor(BlockState state) {
        StoneSlabType type = state.ensureTrait(BlockTraits.STONE_SLAB_TYPE);
        return COLORS.get(type);
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
    public boolean place(Item item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        int meta = this.getMeta() & 0x07;
        boolean isTop;
        BlockBehaviorDoubleSlab dSlab = (BlockBehaviorDoubleSlab) BlockRegistry.get().getBlock(this.doubleSlabId, meta);

        if (face == Direction.DOWN) {
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
        } else if (face == Direction.UP) {
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
