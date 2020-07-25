package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.blockentity.Skull;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.math.BlockFace;
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
    public boolean place(Item item, Block block, Block target, BlockFace face, Vector3f clickPos, Player player) {
        switch (face) {
            case NORTH:
            case SOUTH:
            case EAST:
            case WEST:
            case UP:
                this.setMeta(face.getIndex());
                break;
            case DOWN:
            default:
                return false;
        }
        this.getLevel().setBlock(blockState.getPosition(), this, true, true);

        Skull skull = BlockEntityRegistry.get().newEntity(BlockEntityTypes.SKULL, this.getChunk(), this.getPosition());
        skull.loadAdditionalData(item.getTag());
        skull.setSkullType(item.getMeta());
        skull.setRotation((player.getYaw() * 16 / 360) + 0.5f);

        // TODO: 2016/2/3 SPAWN WITHER

        return true;
    }

    @Override
    public Item toItem(BlockState state) {
        Skull blockEntity = (Skull) getLevel().getBlockEntity(this.getPosition());
        int meta = 0;
        if (blockEntity != null) meta = blockEntity.getSkullType();
        return Item.get(ItemIds.SKULL, meta);
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public BlockColor getColor(BlockState state) {
        return BlockColor.AIR_BLOCK_COLOR;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
