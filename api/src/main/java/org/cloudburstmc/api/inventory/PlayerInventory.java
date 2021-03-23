package org.cloudburstmc.api.inventory;

import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;

public interface PlayerInventory extends Inventory {

    @Override
    Player getHolder();

    int getHeldItemIndex();

    default void setHeldItemIndex(int index) {
        setHeldItemIndex(index, true);
    }

    void setHeldItemIndex(int index, boolean send);

    void decrementHandCount();

    void incrementHandCount();

    ItemStack getItemInHand();

    boolean setItemInHand(ItemStack item);

    ItemStack getArmorItem(int index);

    default boolean setArmorItem(int index, ItemStack item) {
        return setArmorItem(index, item, false);
    }

    boolean setArmorItem(int index, ItemStack item, boolean ignoreArmorEvents);

    default ItemStack getHelmet() {
        return getArmorItem(0);
    }

    default ItemStack getChestplate() {
        return getArmorItem(1);
    }

    default ItemStack getLeggings() {
        return getArmorItem(2);
    }

    default ItemStack getBoots() {
        return getArmorItem(3);
    }

    default boolean setHelmet(ItemStack helm) {
        return setArmorItem(0, helm);
    }

    default boolean setChestplate(ItemStack chestplate) {
        return setArmorItem(1, chestplate);
    }

    default boolean setLeggings(ItemStack leggings) {
        return setArmorItem(2, leggings);
    }

    default boolean setBoots(ItemStack boots) {
        return setArmorItem(3, boots);
    }

    ItemStack[] getArmorContents();

    ItemStack getOffHand();

    void setOffHandContents(ItemStack offhand);

    void sendCreativeContents();
}
