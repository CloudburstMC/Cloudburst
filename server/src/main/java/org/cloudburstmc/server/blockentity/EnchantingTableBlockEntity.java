package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.EnchantingTable;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;

public class EnchantingTableBlockEntity extends BaseBlockEntity implements EnchantingTable {

    public EnchantingTableBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }

    @Override
    public void applyPlacementItem(ItemStack item) {
        String name = item.get(ItemDataComponents.CUSTOM_NAME);
        if (name != null && !name.isEmpty()) {
            this.setCustomName(name);
            this.setDirty();
        }
    }
}
