package org.cloudburstmc.server.entity;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import org.cloudburstmc.api.entity.Creature;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.inventory.InventoryHolder;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.inventory.CloudCreatureInventory;
import org.cloudburstmc.server.item.ItemUtils;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class EntityCreature extends EntityLiving implements Creature, InventoryHolder {

    private CloudCreatureInventory inventory;

    public EntityCreature(EntityType<?> type, Location location) {
        super(type, location);
        if (!(this instanceof Player)) { // Player Inventory Manager will handle main inventory creation
            this.inventory = new CloudCreatureInventory(this);
        }
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
                getInventory().setItem(itemTag.getByte("Slot"), ItemUtils.deserializeItem(itemTag));
            }
        });

        tag.listenForList("Offhand", NbtType.COMPOUND, items -> getInventory().setOffHandItem(ItemUtils.deserializeItem(items.get(0))));

        tag.listenForList("Armor", NbtType.COMPOUND, items -> {
            for (int i = 0; i < 4; ++i) {
                getInventory().setArmorItem(i, ItemUtils.deserializeItem(items.get(i)));
            }
        });
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);
        getInventory().saveInventory(tag);
    }
}
