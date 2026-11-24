package org.cloudburstmc.api.entity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.api.inventory.view.ArmorView;
import org.cloudburstmc.api.inventory.view.OffhandView;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.skin.Skin;

import java.util.UUID;

/**
 * A player or standalone human with a skin and main inventory.
 */
public interface HumanEntity extends LivingEntity {

    /**
     * Returns this human's UUID, distinct from its numeric entity ID.
     * For a standalone human, this UUID is stable only for the lifetime of the instance.
     */
    UUID getServerId();

    /**
     * Returns the skin, or null if no appearance has been configured.
     */
    @Nullable
    Skin getSkin();

    /**
     * Replaces the appearance with a valid, non-null skin.
     */
    void setSkin(Skin skin);

    /**
     * Returns the live 36-slot main inventory, excluding armor and offhand.
     */
    ItemContainer getInventory();

    /**
     * Returns the selected slot in the main inventory's hotbar (0 through 8).
     */
    int getSelectedHotbarSlot();

    /**
     * Selects a hotbar slot (0 through 8).
     *
     * @throws IllegalArgumentException if the slot is outside the hotbar
     */
    void setSelectedHotbarSlot(int slot);

    /**
     * Returns the item in the selected hotbar slot.
     */
    default ItemStack getMainHandItem() {
        return this.getInventory().getItem(this.getSelectedHotbarSlot());
    }

    /**
     * Replaces the item in the selected hotbar slot. Use {@link ItemStack#EMPTY} to clear it.
     */
    default void setMainHandItem(ItemStack item) {
        this.getInventory().setItem(this.getSelectedHotbarSlot(), item);
    }

    /**
     * Returns the live view of this human's armor slots.
     */
    ArmorView getArmor();

    /**
     * Returns the live view of this human's offhand slot.
     */
    OffhandView getOffhand();

    /**
     * Returns whether this human is sprinting.
     */
    boolean isSprinting();

    /**
     * Starts or stops sprinting.
     */
    void setSprinting(boolean sprinting);

    /**
     * Returns the requested locomotion state. Use {@link #getPose()} for the
     * effective pose after collision and movement constraints.
     */
    boolean isSneaking();

    /**
     * Requests or clears sneaking. The effective pose may differ.
     */
    void setSneaking(boolean sneaking);

    /**
     * Returns whether crawling is requested.
     */
    boolean isCrawling();

    /**
     * Requests or clears crawling. The effective pose may differ.
     */
    void setCrawling(boolean crawling);

    /**
     * Returns whether swimming is requested, not whether the current pose is swimming.
     */
    boolean isSwimming();

    /**
     * Requests or clears swimming. The effective pose may differ.
     */
    void setSwimming(boolean swimming);

    /**
     * Returns whether this human is actively gliding.
     */
    boolean isGliding();

    /**
     * Requests a change to the gliding state. Other poses may take precedence.
     */
    void setGliding(boolean gliding);
}
