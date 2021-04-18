package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.Beacon;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.data.BlockColor;
import org.cloudburstmc.server.inventory.CloudBeaconInventory;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.network.protocol.types.ContainerIds;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.BlockEntityRegistry;

public class BlockBehaviorBeacon extends BlockBehaviorTransparent {


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
                BlockEntityRegistry.get().newEntity(BlockEntityTypes.BEACON, (CloudChunk) block.getChunk(), block.getPosition());
            }

            player.addWindow(new CloudBeaconInventory((CloudPlayer) player), ContainerIds.BEACON);
        }
        return true;
    }

    @Override
    public boolean place(ItemStack item, Block block, Block target, Direction face, Vector3f clickPos, Player player) {
        boolean blockSuccess = super.place(item, block, target, face, clickPos, player);

        if (blockSuccess) {
            BlockEntityRegistry.get().newEntity(BlockEntityTypes.BEACON, (CloudChunk) block.getChunk(), block.getPosition());
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


}
