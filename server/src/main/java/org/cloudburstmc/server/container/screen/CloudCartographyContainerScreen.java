package org.cloudburstmc.server.container.screen;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.event.inventory.PrepareResultEvent;
import org.cloudburstmc.api.inventory.CartographyScreen;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.CartographyView;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.api.map.MapView;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.server.container.cartography.CartographyResultCalculator;
import org.cloudburstmc.server.container.cartography.CartographyResultCalculator.MapState;
import org.cloudburstmc.server.container.cartography.CartographyResultCalculator.Operation;
import org.cloudburstmc.server.container.cartography.CartographyResultCalculator.Result;
import org.cloudburstmc.server.container.mapping.UIContainerMapping;
import org.cloudburstmc.server.container.view.CloudCartographyView;
import org.cloudburstmc.server.map.CloudMapView;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Computes cartography results without trusting client-created output items.
 */
public class CloudCartographyContainerScreen extends CloudBlockContainerScreen implements CartographyScreen, ResultContainerScreen {

    private final CloudCartographyView cartography;
    private boolean updatingResult;
    private Result prepared = Result.EMPTY;
    private Result preparedCalculation = Result.EMPTY;
    private @Nullable String renameText;
    private long lastSoundTick = -1;

    public CloudCartographyContainerScreen(CloudPlayer player, Block block) {
        super(ScreenTypes.CARTOGRAPHY, player, block);
        this.cartography = new CloudCartographyView(new CloudContainer(3), () -> {
            if (!this.updatingResult) {
                this.recomputeResult();
            }
        });
    }

    @Override
    public CartographyView getCartography() {
        return cartography;
    }

    @Override
    public void open() {
        super.open();
        this.recomputeResult();
    }

    @Override
    public void close() {
        this.updatingResult = true;

        for (ItemStack item : new ItemStack[]{this.cartography.getInput(), this.cartography.getAdditional()}) {
            for (ItemStack remaining : this.player.getContainer().addItem(item)) {
                this.player.dropItem(remaining);
            }
        }

        this.cartography.setInput(ItemStack.EMPTY);
        this.cartography.setAdditional(ItemStack.EMPTY);
        this.cartography.setResult(ItemStack.EMPTY);
        super.close();
    }

    @Override
    public void setSlot(ContainerSlotType type, int slot, ItemStack item) {
        if (this.isResultSlot(type, slot) && !this.updatingResult) {
            throw new IllegalArgumentException("Cannot place into a cartography result slot");
        }

        super.setSlot(type, slot, item);
    }

    @Override
    public void sendSlot(ContainerSlotType type, int slot) {
        if (this.isResultSlot(type, slot) && this.isPredictedResult()) {
            return;
        }

        super.sendSlot(type, slot);
    }

    @Override
    protected Container getStorageContainer() {
        return cartography.getContainer();
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.addMapping(new UIContainerMapping(ContainerSlotType.CARTOGRAPHY_INPUT, cartography, 0));
        this.addMapping(new UIContainerMapping(ContainerSlotType.CARTOGRAPHY_ADDITIONAL, cartography, 1));
        this.addMapping(new UIContainerMapping(ContainerSlotType.CARTOGRAPHY_RESULT, cartography, 2));
        this.addMapping(new UIContainerMapping(ContainerSlotType.CREATED_OUTPUT, cartography, 2));
    }

    @Override
    public boolean isResultSlot(ContainerSlotType type, int slot) {
        return (type == ContainerSlotType.CARTOGRAPHY_RESULT || type == ContainerSlotType.CREATED_OUTPUT) && slot == 50;
    }

    @Override
    public boolean isInputSlot(ContainerSlotType type) {
        return type == ContainerSlotType.CARTOGRAPHY_INPUT || type == ContainerSlotType.CARTOGRAPHY_ADDITIONAL;
    }

    @Override
    public boolean canMergeResult() {
        return this.prepared.canMerge();
    }

    public void updateRenameText(String text) {
        this.renameText = Objects.requireNonNull(text, "text").strip();
        this.recomputeResult();
    }

    public void recomputeResult() {
        if (this.updatingResult) {
            return;
        }

        ItemStack input = this.cartography.getInput();
        ItemStack additional = this.cartography.getAdditional();

        MapView map = this.findMap(input, additional);
        Result calculation = this.calculateResult(input, additional, map);
        this.updatingResult = true;

        try {
            PrepareResultEvent event = new PrepareResultEvent(this, calculation.item());
            this.player.getServer().getEventManager().fire(event);
            if (!input.equals(this.cartography.getInput()) || !additional.equals(this.cartography.getAdditional())) {
                this.prepared = Result.EMPTY;
                this.preparedCalculation = Result.EMPTY;
                this.cartography.setResult(ItemStack.EMPTY);
            } else {
                this.preparedCalculation = calculation;
                this.prepared = calculation.prepare(event.getResult(), input, additional);
                this.cartography.setResult(event.getResult());
            }
        } finally {
            this.updatingResult = false;
        }
    }

    @Override
    public boolean isInvalidResultCount(ItemStack result, int count) {
        if (count <= 0 || result.isEmpty() || this.prepared.operation() == Operation.NONE) {
            return true;
        }

        if (this.prepared.operation() == Operation.RENAME) {
            return count > result.getCount()
                    || (long) count * this.prepared.inputCount() > this.cartography.getInput().getCount()
                    || (long) count * this.prepared.additionalCount() > this.cartography.getAdditional().getCount();
        }

        if (count % result.getCount() != 0 || (this.prepared.createsMapCopy() && count != result.getCount())) {
            return true;
        }

        long crafts = count / result.getCount();
        return crafts * this.prepared.inputCount() > this.cartography.getInput().getCount()
                || crafts * this.prepared.additionalCount() > this.cartography.getAdditional().getCount();
    }

    @Override
    public ItemStack takeResult(ItemStack expected, int count) {
        ItemStack result = this.cartography.getResult();
        ItemStack input = this.cartography.getInput();

        MapView original = this.findMap(input, this.cartography.getAdditional());
        Result calculation = this.calculateResult(input, this.cartography.getAdditional(), original);

        if (result.isEmpty() || !result.equals(expected) || this.isInvalidResultCount(result, count) || this.prepared.operation() == Operation.NONE || !this.preparedCalculation.equals(calculation)) {
            return ItemStack.EMPTY;
        }

        Operation operation = this.prepared.operation();
        int inputCount = this.prepared.consumedInput(count, result.getCount());
        int additionalCount = this.prepared.consumedAdditional(count, result.getCount());
        result = result.withCount(count);

        if (this.prepared.createsMapCopy() && result.getType() == ItemTypes.FILLED_MAP && Objects.equals(result.get(ItemDataComponents.MAP_ID), calculation.item().get(ItemDataComponents.MAP_ID))) {
            Objects.requireNonNull(original, "original map");
            MapState outputMap = Objects.requireNonNull(this.prepared.outputMap(), "output map");
            CloudMapView copy = this.player.getServer().maps().copy(original,
                    outputMap.scale(),
                    outputMap.locked(),
                    outputMap.tracking());

            if (operation == Operation.SCALE) {
                copy.initializeTerrain(this.player);
            }

            result = result.toBuilder()
                    .setData(ItemDataComponents.MAP_ID, copy.getId())
                    .setData(ItemDataComponents.MAP_TRACKING_POSITION, copy.isTrackingPosition())
                    .build();
        }

        this.updatingResult = true;
        try {
            this.cartography.setInput(input.decreaseCount(inputCount));
            this.cartography.setAdditional(this.cartography.getAdditional().decreaseCount(additionalCount));
            this.cartography.setResult(ItemStack.EMPTY);
            if (operation != Operation.RENAME || (this.cartography.getInput().isEmpty() && this.cartography.getAdditional().isEmpty())) {
                this.renameText = null;
            }
        } finally {
            this.updatingResult = false;
        }

        long tick = this.player.getServer().getTick();
        if (this.lastSoundTick != tick) {
            this.player.playSound(SoundTypes.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1, 1);
            this.lastSoundTick = tick;
        }

        this.recomputeResult();
        return result;
    }

    @Override
    public void trackResultSlots(BiConsumer<ContainerSlotType, Integer> tracker) {
        tracker.accept(ContainerSlotType.CARTOGRAPHY_INPUT, 12);
        tracker.accept(ContainerSlotType.CARTOGRAPHY_ADDITIONAL, 13);

        if (!this.isPredictedResult()) {
            tracker.accept(ContainerSlotType.CARTOGRAPHY_RESULT, 50);
        }
    }

    private boolean isPredictedResult() {
        ItemStack result = this.cartography.getResult();
        return !result.isEmpty() && result.equals(this.preparedCalculation.item());
    }

    private @Nullable MapView findMap(ItemStack input, ItemStack additional) {
        ItemStack item = input.getType() == ItemTypes.FILLED_MAP ? input : additional;
        if (item.getType() != ItemTypes.FILLED_MAP) {
            return null;
        }

        Long id = item.get(ItemDataComponents.MAP_ID);
        return id == null ? null : this.player.getServer().maps().get(id).orElse(null);
    }

    private Result calculateResult(ItemStack input, ItemStack additional, @Nullable MapView map) {
        MapState state = map == null ? null : new MapState(map.getScale(), map.isLocked(), map.isTrackingPosition());
        return CartographyResultCalculator.calculate(input, additional, state, this.renameText);
    }
}
