package org.cloudburstmc.server.container.screen;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.event.inventory.PrepareResultEvent;
import org.cloudburstmc.api.event.inventory.PrepareSmithingEvent;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.SmithingScreen;
import org.cloudburstmc.api.inventory.view.SmithingView;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.sound.SoundTypes;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.server.container.mapping.UIContainerMapping;
import org.cloudburstmc.server.container.view.CloudSmithingView;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudRecipeRegistry;
import org.cloudburstmc.server.registry.CloudRecipeRegistry.SmithingResult;

import java.util.List;
import java.util.function.BiConsumer;

import static org.cloudburstmc.server.container.view.CloudSmithingView.*;

public class CloudSmithingContainerScreen extends CloudBlockContainerScreen implements SmithingScreen, ResultContainerScreen {

    private static final int RESULT_UI_SLOT = 50;
    private static final int BASE_UI_SLOT = 51;
    private static final int ADDITION_UI_SLOT = 52;
    private static final int TEMPLATE_UI_SLOT = 53;

    private final CloudSmithingView smithingView;
    private @Nullable PreparedResult prepared;
    private boolean updating;
    private boolean closed;

    public CloudSmithingContainerScreen(CloudPlayer player, Block block) {
        super(ScreenTypes.SMITHING, player, block);
        this.smithingView = new CloudSmithingView(this::prepareResult);
    }

    @Override
    public SmithingView getSmithing() {
        return smithingView;
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }

        this.closed = true;
        Inputs inputs = this.inputs();
        this.prepared = null;
        this.smithingView.setTemplate(ItemStack.EMPTY);
        this.smithingView.setBase(ItemStack.EMPTY);
        this.smithingView.setAddition(ItemStack.EMPTY);
        this.smithingView.updateResult(ItemStack.EMPTY);
        super.close();

        for (ItemStack input : List.of(inputs.template(), inputs.base(), inputs.addition())) {
            if (!input.isEmpty()) {
                for (ItemStack remaining : this.player.getContainer().addItem(input)) {
                    this.player.dropItem(remaining);
                }
            }
        }
    }

    @Override
    protected Container getStorageContainer() {
        return smithingView.getContainer();
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.addMapping(new UIContainerMapping(ContainerSlotType.SMITHING_TABLE_TEMPLATE, smithingView, TEMPLATE_SLOT));
        this.addMapping(new UIContainerMapping(ContainerSlotType.SMITHING_TABLE_INPUT, smithingView, BASE_SLOT));
        this.addMapping(new UIContainerMapping(ContainerSlotType.SMITHING_TABLE_MATERIAL, smithingView, ADDITION_SLOT));
        this.addMapping(new UIContainerMapping(ContainerSlotType.SMITHING_TABLE_RESULT, smithingView, RESULT_SLOT));
        this.addMapping(new UIContainerMapping(ContainerSlotType.CREATED_OUTPUT, smithingView, RESULT_SLOT));
    }

    @Override
    public boolean isResultSlot(ContainerSlotType type, int slot) {
        return slot == RESULT_UI_SLOT && (type == ContainerSlotType.SMITHING_TABLE_RESULT || type == ContainerSlotType.CREATED_OUTPUT);
    }

    @Override
    public boolean isInputSlot(ContainerSlotType type) {
        return type == ContainerSlotType.SMITHING_TABLE_TEMPLATE
                || type == ContainerSlotType.SMITHING_TABLE_INPUT
                || type == ContainerSlotType.SMITHING_TABLE_MATERIAL;
    }

    @Override
    public ItemStack takeResult(ItemStack expected, int count) {
        PreparedResult result = this.prepared;
        Block block = this.getBlock();
        if (this.closed || this.updating || result == null || this.isInvalidResultCount(expected, count)
                || !expected.equals(result.output()) || !expected.equals(this.smithingView.getResult())
                || !result.inputs().equals(this.inputs())
                || block.getLevel() != this.player.getLevel()
                || block.getLevel().getBlock(block.getPosition()).getState().getType() != BlockTypes.SMITHING_TABLE) {
            return ItemStack.EMPTY;
        }

        this.updating = true;
        this.prepared = null;
        try {
            Inputs inputs = result.inputs();
            SmithingResult recipe = result.recipe();
            this.smithingView.updateResult(ItemStack.EMPTY);
            this.smithingView.setTemplate(inputs.template().decreaseCount(recipe.templateCost()));
            this.smithingView.setBase(inputs.base().decreaseCount(recipe.baseCost()));
            this.smithingView.setAddition(inputs.addition().decreaseCount(recipe.additionCost()));
        } finally {
            this.updating = false;
        }

        block.getLevel().playSound(block.getPosition(), SoundTypes.SMITHING_TABLE_USE);
        return expected;
    }

    @Override
    public void trackResultSlots(BiConsumer<ContainerSlotType, Integer> tracker) {
        tracker.accept(ContainerSlotType.SMITHING_TABLE_TEMPLATE, TEMPLATE_UI_SLOT);
        tracker.accept(ContainerSlotType.SMITHING_TABLE_INPUT, BASE_UI_SLOT);
        tracker.accept(ContainerSlotType.SMITHING_TABLE_MATERIAL, ADDITION_UI_SLOT);
        tracker.accept(ContainerSlotType.SMITHING_TABLE_RESULT, RESULT_UI_SLOT);
    }

    public void prepareResult() {
        if (this.closed || this.updating) {
            return;
        }

        this.updating = true;
        this.prepared = null;
        try {
            Inputs inputs = this.inputs();
            SmithingResult result = CloudRecipeRegistry.get().findSmithingResult(inputs.template(), inputs.base(), inputs.addition());
            if (result == null) {
                this.smithingView.updateResult(ItemStack.EMPTY);
                return;
            }

            PrepareResultEvent event = new PrepareSmithingEvent(this, result.item(), result.recipe());
            this.player.getServer().getEventManager().fire(event);
            ItemStack output = this.closed || !inputs.equals(this.inputs()) ? ItemStack.EMPTY : event.getResult();
            if (!output.isEmpty()) {
                this.prepared = new PreparedResult(inputs, result, output);
            }
            this.smithingView.updateResult(output);
        } finally {
            this.updating = false;
        }
    }

    private Inputs inputs() {
        return new Inputs(this.smithingView.getTemplate(), this.smithingView.getBase(), this.smithingView.getAddition());
    }

    private record Inputs(ItemStack template, ItemStack base, ItemStack addition) {
    }

    private record PreparedResult(Inputs inputs, SmithingResult recipe, ItemStack output) {
    }
}
