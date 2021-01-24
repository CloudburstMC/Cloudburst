package org.cloudburstmc.server.inventory;

import com.nukkitx.math.vector.Vector3i;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.world.World;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class FakeBlockMenu implements InventoryHolder {

    private final Inventory inventory;
    private final Block blockState;

    public FakeBlockMenu(Inventory inventory, Block blockState) {
        this.inventory = inventory;
        this.blockState = blockState;
    }

    public Vector3i getPosition() {
        return blockState.getPosition();
    }

    public World getWorld() {
        return blockState.getWorld();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
