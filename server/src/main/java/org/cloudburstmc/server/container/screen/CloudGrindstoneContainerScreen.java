package org.cloudburstmc.server.container.screen;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.event.block.BlockExpEvent;
import org.cloudburstmc.api.event.inventory.PrepareGrindstoneEvent;
import org.cloudburstmc.api.inventory.GrindstoneScreen;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.GrindstoneView;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.FullContainerName;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.server.container.grindstone.GrindstoneResultCalculator;
import org.cloudburstmc.server.container.mapping.UIContainerMapping;
import org.cloudburstmc.server.container.view.CloudGrindstoneView;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

import static org.cloudburstmc.server.container.view.CloudGrindstoneView.*;

public class CloudGrindstoneContainerScreen extends CloudBlockContainerScreen implements GrindstoneScreen, ResultContainerScreen {

    private static final int INPUT_UI_SLOT = 16;
    private static final int ADDITIONAL_UI_SLOT = 17;
    private static final int RESULT_UI_SLOT = 50;

    private final CloudGrindstoneView grindstoneView;
    private @Nullable Preview preview;
    private boolean updating;
    private boolean closed;

    public CloudGrindstoneContainerScreen(CloudPlayer player, Block block) {
        super(ScreenTypes.GRINDSTONE, player, block);
        this.grindstoneView = new CloudGrindstoneView(this::prepareResult);
    }

    @Override
    public GrindstoneView getGrindstone() {
        return this.grindstoneView;
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }

        this.closed = true;
        Inputs inputs = this.inputs();
        this.clearSlots();
        super.close();

        for (ItemStack input : List.of(inputs.first(), inputs.second())) {
            if (!input.isEmpty()) {
                for (ItemStack remaining : this.player.getContainer().addItem(input)) {
                    this.player.dropItem(remaining);
                }
            }
        }
    }

    @Override
    protected Container getStorageContainer() {
        return this.grindstoneView.getContainer();
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.addMapping(new UIContainerMapping(ContainerSlotType.GRINDSTONE_INPUT, this.grindstoneView, INPUT_SLOT));
        this.addMapping(new UIContainerMapping(ContainerSlotType.GRINDSTONE_ADDITIONAL, this.grindstoneView, ADDITIONAL_SLOT));
        this.addMapping(new UIContainerMapping(ContainerSlotType.GRINDSTONE_RESULT, this.grindstoneView, RESULT_SLOT));
        this.addMapping(new UIContainerMapping(ContainerSlotType.CREATED_OUTPUT, this.grindstoneView, RESULT_SLOT));
    }

    @Override
    public boolean isResultSlot(ContainerSlotType type, int slot) {
        return slot == RESULT_UI_SLOT && (type == ContainerSlotType.GRINDSTONE_RESULT || type == ContainerSlotType.CREATED_OUTPUT);
    }

    @Override
    public boolean isInputSlot(ContainerSlotType type) {
        return type == ContainerSlotType.GRINDSTONE_INPUT || type == ContainerSlotType.GRINDSTONE_ADDITIONAL;
    }

    @Override
    protected @Nullable FullContainerName uiSlotContainerName(ContainerSlotType type) {
        return switch (type) {
            case GRINDSTONE_INPUT, GRINDSTONE_ADDITIONAL, GRINDSTONE_RESULT, CREATED_OUTPUT -> null;
            default -> super.uiSlotContainerName(type);
        };
    }

    @Override
    public void trackResultSlots(BiConsumer<ContainerSlotType, Integer> tracker) {
        tracker.accept(ContainerSlotType.GRINDSTONE_INPUT, INPUT_UI_SLOT);
        tracker.accept(ContainerSlotType.GRINDSTONE_ADDITIONAL, ADDITIONAL_UI_SLOT);
        tracker.accept(ContainerSlotType.GRINDSTONE_RESULT, RESULT_UI_SLOT);
    }

    @Override
    public @Nullable ResultTake prepareTake(ItemStack expected, int count) {
        Preview prepared = this.preview;
        if (prepared == null || expected.isEmpty() || this.isInvalidResultCount(expected, count) || this.isStale(prepared, expected)) {
            return null;
        }

        int cost = GrindstoneResultCalculator.experienceCost(prepared.inputs().first(), prepared.inputs().second());
        int minimum = Math.ceilDiv(cost, 2);
        int experience = minimum == 0 ? 0 : minimum + ThreadLocalRandom.current().nextInt(minimum);
        BlockExpEvent event = new BlockExpEvent(this.getBlock(), experience);
        this.updating = true;
        try {
            this.player.getServer().getEventManager().fire(event);
        } finally {
            this.updating = false;
        }

        if (this.isStale(prepared, expected)) {
            return null;
        }

        return new ResultTake(expected, this::clearSlots, () -> {
            this.player.getLevel().dropExpOrb(this.getBlock().getPosition(), event.getExpToDrop());
            this.player.getLevel().addLevelSoundEvent(this.getBlock().getPosition(), SoundEvent.GRINDSTONE_USE);
        });
    }

    private boolean isStale(Preview prepared, ItemStack expected) {
        Block block = this.getBlock();
        return this.closed || this.updating || this.preview != prepared || !expected.equals(prepared.output())
                || !expected.equals(this.grindstoneView.getResult()) || !prepared.inputs().equals(this.inputs())
                || block.getLevel() != this.player.getLevel() || !this.player.canInteractWithBlock(block.getPosition())
                || block.getLevel().getBlock(block.getPosition()).getState().getType() != BlockTypes.GRINDSTONE;
    }

    private void prepareResult() {
        if (this.closed || this.updating) {
            return;
        }

        this.updating = true;
        this.preview = null;
        try {
            Inputs inputs = this.inputs();
            ItemStack result = GrindstoneResultCalculator.calculate(inputs.first(), inputs.second());
            PrepareGrindstoneEvent event = new PrepareGrindstoneEvent(this, result);
            this.player.getServer().getEventManager().fire(event);

            ItemStack output = this.closed || !inputs.equals(this.inputs()) || !inputs.canConsume() ? ItemStack.EMPTY : event.getResult().normalizeBlockState();
            this.grindstoneView.updateResult(output);
            if (!output.isEmpty()) {
                this.preview = new Preview(inputs, output);
            }
        } finally {
            this.updating = false;
        }
    }

    private void clearSlots() {
        this.preview = null;
        this.updating = true;
        try {
            this.grindstoneView.updateResult(ItemStack.EMPTY);
            this.grindstoneView.getContainer().setItem(CloudGrindstoneView.INPUT_SLOT, ItemStack.EMPTY);
            this.grindstoneView.getContainer().setItem(CloudGrindstoneView.ADDITIONAL_SLOT, ItemStack.EMPTY);
        } finally {
            this.updating = false;
        }
    }

    private Inputs inputs() {
        return new Inputs(this.grindstoneView.getInput(), this.grindstoneView.getAdditional());
    }

    private record Inputs(ItemStack first, ItemStack second) {
        private boolean canConsume() {
            return (!this.first.isEmpty() || !this.second.isEmpty()) && this.first.getCount() <= 1 && this.second.getCount() <= 1;
        }
    }

    private record Preview(Inputs inputs, ItemStack output) {
    }
}
