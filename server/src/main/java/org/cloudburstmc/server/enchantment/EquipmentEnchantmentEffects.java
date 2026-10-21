package org.cloudburstmc.server.enchantment;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.inventory.view.ArmorView;
import org.cloudburstmc.api.item.EquipmentSlot;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.server.registry.CloudEnchantmentRegistry;

public class EquipmentEnchantmentEffects {

    private final ItemStack[] armor = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};

    private @Nullable Entity vehicle;
    private @Nullable Level level;
    private @Nullable Vector3i position;
    private boolean onGround;

    public void update(Entity wearer, ArmorView equipment) {
        Vector3i position = wearer.getPosition().toInt();
        Entity vehicle = wearer.getVehicle();
        boolean changed = wearer.getLevel() != this.level || !position.equals(this.position)
                || this.onGround != wearer.isOnGround() || this.vehicle != vehicle;
        this.position = position;
        this.level = wearer.getLevel();
        this.onGround = wearer.isOnGround();
        this.vehicle = vehicle;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!slot.isArmor()) {
                continue;
            }

            int index = slot.getArmorSlot();
            ItemStack item = equipment.getItem(index);
            ItemStack previous = this.armor[index];
            this.armor[index] = item;
            if (changed || !item.equals(previous)) {
                CloudEnchantmentRegistry.get().applyLocationEffects(item, wearer, slot);
            }
        }
    }
}
