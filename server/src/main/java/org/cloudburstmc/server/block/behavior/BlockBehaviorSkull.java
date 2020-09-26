package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import lombok.val;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTraits;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.blockentity.Skull;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.item.ToolType;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorSkull extends BlockBehaviorTransparent {

    @Override
    public float getHardness() {
        return 1;
    }

    @Override
    public float getResistance() {
        return 5;
    }

    @Override
    public boolean isSolid() {
        return false;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        if (face == Direction.DOWN) {
            return false;
        }

        placeBlock(block, BlockState.get(BlockTypes.SKULL).withTrait(BlockTraits.FACING_DIRECTION, face));

        Skull skull = BlockEntityRegistry.get().newEntity(BlockEntityTypes.SKULL, block);
        skull.loadAdditionalData(item.getTag());
        skull.setSkullType(item.getMeta());
        skull.setRotation((player.getYaw() * 16 / 360) + 0.5f);

        // TODO: 2016/2/3 SPAWN WITHER

        return true;
    }

    @Override
    public ItemStack toItem(Block block) {
        val be = block.getLevel().getBlockEntity(block.getPosition());

        int meta = 0;
        if (be instanceof Skull) {
            meta = ((Skull) be).getSkullType();
        }

        return ItemStack.get(ItemTypes.SKULL, meta);
    }

    @Override
    public ToolType getToolType() {
        return ItemToolBehavior.TYPE_PICKAXE;
    }

    @Override
    public BlockColor getColor(Block block) {
        return BlockColor.AIR_BLOCK_COLOR;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
