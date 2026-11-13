package org.cloudburstmc.server.container.view;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.Beacon;
import org.cloudburstmc.api.inventory.view.BlockBeaconView;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTags;
import org.cloudburstmc.server.blockentity.BeaconBlockEntity;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

public class CloudBeaconView extends CloudSlotGroupBase implements BlockBeaconView {

    private final BeaconBlockEntity beacon;

    public CloudBeaconView(BeaconBlockEntity beacon) {
        super(SlotGroupTypes.BEACON, new CloudContainer(1, 1));
        this.beacon = beacon;
    }

    @Override
    public Block getBlock() {
        return this.beacon.getBlock();
    }

    @Override
    public Beacon getBlockEntity() {
        return this.beacon;
    }

    @Override
    public ItemStack getPayment() {
        return this.getItem(0);
    }

    @Override
    public void setPayment(ItemStack item) {
        if (!item.isEmpty() && isInvalidPayment(item)) {
            throw new IllegalArgumentException("Invalid beacon payment");
        }

        this.setItem(0, item);
    }

    public static boolean isInvalidPayment(ItemStack item) {
        return item.getCount() != 1
                || !CloudItemRegistry.get().getTag(ItemTags.BEACON_PAYMENT_ITEMS).isTagged(item.getType());
    }
}
