package org.cloudburstmc.server.container.view;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.Beacon;
import org.cloudburstmc.api.inventory.view.BlockBeaconView;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.potion.EffectType;
import org.cloudburstmc.server.blockentity.BeaconBlockEntity;
import org.cloudburstmc.server.container.CloudContainer;

/**
 * View-layer slot group for the beacon. Holds one slot: the payment item.
 * This is an ephemeral section; the beacon block entity has no item storage.
 */
public class CloudBeaconView extends CloudSlotGroupBase implements BlockBeaconView {

    private final Block block;

    public CloudBeaconView(Block block) {
        super(SlotGroupTypes.BEACON, new CloudContainer(1));
        this.block = block;
    }

    @Override
    public Block getBlock() {
        return block;
    }

    @Override
    public Beacon getBlockEntity() {
        return (Beacon) block.getLevel().getBlockEntity(block.getPosition());
    }

    @Override
    public ItemStack getPayment() {
        return getItem(0);
    }

    @Override
    public void setPayment(ItemStack item) {
        setItem(0, item);
    }

    private BeaconBlockEntity beaconEntity() {
        Beacon be = getBlockEntity();
        return be instanceof BeaconBlockEntity entity ? entity : null;
    }

    @Override
    public int getTier() {
        BeaconBlockEntity entity = beaconEntity();
        return entity != null ? entity.getPowerLevel() : 0;
    }

    @Override
    public @Nullable EffectType getPrimaryEffect() {
        BeaconBlockEntity entity = beaconEntity();
        return entity != null ? entity.getPrimaryEffect() : null;
    }

    @Override
    public void setPrimaryEffect(@Nullable EffectType effect) {
        BeaconBlockEntity entity = beaconEntity();
        if (entity != null) {
            entity.setPrimaryEffect(effect);
        }
    }

    @Override
    public @Nullable EffectType getSecondaryEffect() {
        BeaconBlockEntity entity = beaconEntity();
        return entity != null ? entity.getSecondaryEffect() : null;
    }

    @Override
    public void setSecondaryEffect(@Nullable EffectType effect) {
        BeaconBlockEntity entity = beaconEntity();
        if (entity != null) {
            entity.setSecondaryEffect(effect);
        }
    }
}
