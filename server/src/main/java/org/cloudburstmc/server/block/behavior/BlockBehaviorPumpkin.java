package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.BlockType;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.item.ToolTypes;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorPumpkin extends BlockBehaviorSolid {

    protected final BlockType type;

    public BlockBehaviorPumpkin(BlockType type) {
        this.type = type;
    }

    @Override
    public float getHardness() {
        return 1;
    }

    @Override
    public float getResistance() {
        return 5;
    }

    @Override
    public ToolType getToolType() {
        return ToolTypes.AXE;
    }

    @Override
    public ItemStack toItem(Block block) {
        return ItemStack.get(block.getState().defaultState());
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        return placeBlock(block, BlockState.get(type).withTrait(
                BlockTraits.DIRECTION,
                (player != null ? player.getHorizontalDirection() : Direction.NORTH).getOpposite()
        ));
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.ORANGE_BLOCK_COLOR;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }
}
