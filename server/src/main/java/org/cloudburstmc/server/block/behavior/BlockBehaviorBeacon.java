package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.blockentity.Beacon;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.BlockEntityTypes;
import org.cloudburstmc.server.inventory.BeaconInventory;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.behavior.ItemToolBehavior;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.network.protocol.types.ContainerIds;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;
import org.cloudburstmc.server.utils.BlockColor;

public class BlockBehaviorBeacon extends BlockBehaviorTransparent {

    @Override
    public float getHardness() {
        return 3;
    }

    @Override
    public float getResistance() {
        return 15;
    }

    @Override
    public int getLightLevel(Block block) {
        return 15;
    }

    @Override
    public int getToolType() {
        return ItemToolBehavior.TYPE_PICKAXE;
    }

    @Override
    public boolean canBeActivated(Block block) {
        return true;
    }

    @Override
    public boolean onActivate(Block block, ItemStack item, Player player) {
        if (player != null) {
            BlockEntity t = block.getLevel().getBlockEntity(block.getPosition());
            if (!(t instanceof Beacon)) {
                t.close();
                BlockEntityRegistry.get().newEntity(BlockEntityTypes.BEACON, block.getChunk(), block.getPosition());
            }

            player.addWindow(new BeaconInventory(player.getUIInventory(), block), ContainerIds.BEACON);
        }
        return true;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        boolean blockSuccess = super.place(item, block, target, face, clickPos, player);

        if (blockSuccess) {
            BlockEntityRegistry.get().newEntity(BlockEntityTypes.BEACON, block.getChunk(), block.getPosition());
        }

        return blockSuccess;
    }

    @Override
    public boolean canBePushed() {
        return false;
    }

    @Override
    public BlockColor getColor(Block state) {
        return BlockColor.DIAMOND_BLOCK_COLOR;
    }

    @Override
    public boolean canWaterlogSource() {
        return true;
    }
}
