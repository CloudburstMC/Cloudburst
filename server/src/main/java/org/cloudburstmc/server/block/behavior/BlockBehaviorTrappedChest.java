package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.GenericMath;
import com.nukkitx.math.vector.Vector3f;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.Chest;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.math.BlockFace.Plane;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockEntityRegistry;

import static org.cloudburstmc.server.blockentity.BlockEntityTypes.CHEST;

public class BlockBehaviorTrappedChest extends BlockBehaviorChest {

    @Override
    public boolean place(Item item, Block block, Block target, BlockFace face, Vector3f clickPos, Player player) {
        int[] faces = {2, 5, 3, 4};

        Chest chest = null;
        this.setMeta(faces[player != null ? player.getDirection().getHorizontalIndex() : 0]);

        for (BlockFace side : Plane.HORIZONTAL) {
            if ((this.getMeta() == 4 || this.getMeta() == 5) && (side == BlockFace.WEST || side == BlockFace.EAST)) {
                continue;
            } else if ((this.getMeta() == 3 || this.getMeta() == 2) && (side == BlockFace.NORTH || side == BlockFace.SOUTH)) {
                continue;
            }
            BlockState c = this.getSide(side);
            if (c instanceof BlockBehaviorTrappedChest && c.getMeta() == this.getMeta()) {
                BlockEntity blockEntity = this.getLevel().getBlockEntity(c.getPosition());
                if (blockEntity instanceof Chest && !((Chest) blockEntity).isPaired()) {
                    chest = (Chest) blockEntity;
                    break;
                }
            }
        }

        this.getLevel().setBlock(block.getPosition(), this, true, true);

        Chest chest1 = BlockEntityRegistry.get().newEntity(CHEST, this.getChunk(), this.getPosition());
        chest1.loadAdditionalData(item.getTag());
        if (item.hasCustomName()) {
            chest1.setCustomName(item.getCustomName());
        }

        if (chest != null) {
            chest1.pairWith(chest);
            chest.pairWith(chest1);
        }

        return true;
    }

    @Override
    public int getWeakPower(BlockFace face) {
        int playerCount = 0;

        BlockEntity blockEntity = this.level.getBlockEntity(this.getPosition());

        if (blockEntity instanceof Chest) {
            playerCount = ((Chest) blockEntity).getInventory().getViewers().size();
        }

        return GenericMath.clamp(playerCount, 0, 15);
    }

    @Override
    public int getStrongPower(BlockFace side) {
        return side == BlockFace.UP ? this.getWeakPower(side) : 0;
    }

    @Override
    public boolean isPowerSource() {
        return true;
    }
}
