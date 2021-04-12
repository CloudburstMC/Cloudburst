package org.cloudburstmc.server.entity;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtType;
import org.cloudburstmc.api.entity.Creature;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.inventory.InventoryHolder;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.server.inventory.CloudCreatureInventory;
import org.cloudburstmc.server.item.ItemUtils;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class EntityCreature extends EntityLiving implements Creature, InventoryHolder {

    private CloudCreatureInventory inventory = new CloudCreatureInventory(this);

    public EntityCreature(EntityType<?> type, Location location) {
        super(type, location);
    }

    @Override
    public CloudCreatureInventory getInventory() {
        return this.inventory;
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForList("Inventory", NbtType.COMPOUND, items -> {
            for (NbtMap itemTag : items) {
                int slot = itemTag.getByte("Slot");
                if (slot >= 0 && slot < 9) { //hotbar
                    //Old hotbar saving stuff, useless now
                } else if (slot >= 100 && slot < 105) {
                    this.inventory.setItem(this.inventory.getSize() + slot - 100, ItemUtils.deserializeItem(itemTag));
                } else {
                    this.inventory.setItem(slot - 9, ItemUtils.deserializeItem(itemTag));
                }
            }
        });
    }
}
