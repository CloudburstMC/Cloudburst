package org.cloudburstmc.server.container.screen;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.event.player.PlayerChangeBeaconEffectEvent;
import org.cloudburstmc.api.inventory.BeaconScreen;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.BlockBeaconView;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.potion.EffectType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.blockentity.BeaconBlockEntity;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.server.container.mapping.ContainerMapping;
import org.cloudburstmc.server.container.view.CloudBeaconView;
import org.cloudburstmc.server.network.NetworkUtils;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;

/**
 * Holds a player's payment until it is confirmed or returned.
 */
public class CloudBeaconContainerScreen extends CloudBlockContainerScreen implements BeaconScreen {

    public static final int PAYMENT_SLOT = 27;

    private final BeaconBlockEntity beacon;
    private final CloudBeaconView beaconSection;
    private boolean closed;

    public CloudBeaconContainerScreen(CloudPlayer player, Block block) {
        super(ScreenTypes.BEACON, player, block);

        BlockEntity entity = block.getLevel().getBlockEntity(block.getPosition());
        if (entity == null) {
            entity = CloudBlockEntityRegistry.get().newEntity(BlockEntityTypes.BEACON, block);
        }

        if (!(entity instanceof BeaconBlockEntity beaconEntity)) {
            throw new IllegalStateException("Expected a beacon at " + block.getPosition());
        }

        this.beacon = beaconEntity;
        this.beaconSection = new CloudBeaconView(this.beacon);
    }

    @Override
    public void open() {
        this.beacon.spawnTo(this.player);
        super.open();
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }

        this.closed = true;
        ItemStack payment = this.beaconSection.getPayment();
        this.beaconSection.setPayment(ItemStack.EMPTY);
        super.close();

        if (!payment.isEmpty()) {
            for (ItemStack remainder : this.player.getContainer().addItem(payment)) {
                this.player.dropItem(remainder);
            }
        }
    }

    @Override
    public BlockBeaconView getBeacon() {
        return this.beaconSection;
    }

    @Override
    protected Container getStorageContainer() {
        return this.beaconSection.getContainer();
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.addMapping(new ContainerMapping(ContainerSlotType.BEACON_PAYMENT, this.beaconSection, 1, -PAYMENT_SLOT));
    }

    public @Nullable PaymentConfirmation preparePayment(int primaryId, int secondaryId, ItemStack payment) {
        if (this.cannotConfirmPayment(payment) || primaryId <= 0 || primaryId > 255 || secondaryId < 0 || secondaryId > 255) {
            return null;
        }

        EffectType primary;
        EffectType secondary;
        try {
            primary = NetworkUtils.effectFromNetwork((byte) primaryId);
            secondary = secondaryId == 0 ? null : NetworkUtils.effectFromNetwork((byte) secondaryId);
        } catch (IllegalArgumentException ignored) {
            return null;
        }

        this.beacon.refreshStructure();
        if (this.cannotConfirmPayment(payment) || !this.beacon.isActive() || !BeaconBlockEntity.supportsSelection(this.beacon.getTier(), primary, secondary)) {
            return null;
        }

        PlayerChangeBeaconEffectEvent event = new PlayerChangeBeaconEffectEvent(this.player, primary, secondary, this.beacon.getBlock());
        this.player.getServer().getEventManager().fire(event);
        if (event.isCancelled() || this.cannotConfirmPayment(payment)) {
            return null;
        }

        EffectType selectedPrimary = event.getPrimary();
        EffectType selectedSecondary = event.getSecondary();
        try {
            if (selectedPrimary != null) {
                NetworkUtils.effectToNetwork(selectedPrimary);
            }

            if (selectedSecondary != null) {
                NetworkUtils.effectToNetwork(selectedSecondary);
            }
        } catch (IllegalArgumentException ignored) {
            return null;
        }

        return new PaymentConfirmation(event.willConsumeItem(), () -> this.beacon.confirmEffects(selectedPrimary, selectedSecondary));
    }

    private boolean cannotConfirmPayment(ItemStack payment) {
        return this.closed
                || this.player.getInventoryManager().getScreen() != this
                || this.player.getLevel() != this.beacon.getLevel()
                || !this.player.isAlive()
                || this.player.isSpectator()
                || this.beacon.isClosed()
                || !this.beacon.isValid()
                || !this.player.canInteractWithBlock(this.beacon.getPosition())
                || CloudBeaconView.isInvalidPayment(payment)
                || !payment.equals(this.beaconSection.getPayment());
    }

    public record PaymentConfirmation(boolean consumeItem, Runnable commit) {
    }
}
