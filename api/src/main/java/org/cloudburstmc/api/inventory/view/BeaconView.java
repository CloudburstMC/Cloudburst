package org.cloudburstmc.api.inventory.view;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.potion.EffectType;

/**
 * Represents the beacon payment slot and its active effects.
 *
 * <p>The tier reflects the height of the pyramid below the beacon block (0 = no pyramid, 4 = full pyramid).
 * The primary and secondary effects are the potion effects currently configured by the player.</p>
 */
public interface BeaconView extends SlotGroup {

    /**
     * Returns the item currently in the beacon's payment slot.
     *
     * @return the payment item (never {@code null}; compare with {@link ItemStack#EMPTY} to test for empty)
     */
    ItemStack getPayment();

    /**
     * Sets the item in the beacon's payment slot.
     *
     * @param item the item to place in the payment slot
     */
    void setPayment(ItemStack item);

    /**
     * Returns the current power tier of this beacon (0–4), derived from the pyramid height.
     *
     * @return the beacon tier
     */
    int getTier();

    /**
     * Returns the primary effect currently set on this beacon, or {@code null} if none is set.
     *
     * @return the primary effect, or {@code null}
     */
    @Nullable
    EffectType getPrimaryEffect();

    /**
     * Sets the primary effect on this beacon.
     *
     * @param effect the effect to set, or {@code null} to clear
     */
    void setPrimaryEffect(@Nullable EffectType effect);

    /**
     * Returns the secondary effect currently set on this beacon, or {@code null} if none is set.
     *
     * @return the secondary effect, or {@code null}
     */
    @Nullable
    EffectType getSecondaryEffect();

    /**
     * Sets the secondary effect on this beacon.
     *
     * @param effect the effect to set, or {@code null} to clear
     */
    void setSecondaryEffect(@Nullable EffectType effect);
}
