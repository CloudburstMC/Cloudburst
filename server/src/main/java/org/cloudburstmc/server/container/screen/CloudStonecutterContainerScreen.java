package org.cloudburstmc.server.container.screen;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.crafting.Recipe;
import org.cloudburstmc.api.event.inventory.PrepareResultEvent;
import org.cloudburstmc.api.event.player.PlayerStonecutterRecipeSelectEvent;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.StonecutterScreen;
import org.cloudburstmc.api.inventory.view.BlockStonecutterView;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.protocol.bedrock.data.inventory.FullContainerName;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.server.container.mapping.UIContainerMapping;
import org.cloudburstmc.server.container.view.CloudStonecutterView;
import org.cloudburstmc.server.crafting.CloudStonecuttingRecipe;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudRecipeRegistry;

import java.util.function.BiConsumer;

public class CloudStonecutterContainerScreen extends CloudBlockContainerScreen implements StonecutterScreen, ResultContainerScreen {

    public static final int INPUT_UI_SLOT = 3;
    private static final int RESULT_UI_SLOT = 50;

    private final CloudStonecutterView stonecutter;
    private boolean closed;
    private int lastSoundTick = -1;

    public CloudStonecutterContainerScreen(CloudPlayer player, Block block) {
        super(ScreenTypes.STONECUTTER, player, block);
        this.stonecutter = new CloudStonecutterView(block);
    }

    @Override
    public BlockStonecutterView getStonecutter() {
        return this.stonecutter;
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }

        this.closed = true;
        ItemStack input = this.stonecutter.getInput();
        this.stonecutter.setInput(ItemStack.EMPTY);
        super.close();

        if (!input.isEmpty()) {
            for (ItemStack remainder : this.player.getContainer().addItem(input)) {
                this.player.dropItem(remainder);
            }
        }
    }

    @Override
    protected Container getStorageContainer() {
        return this.stonecutter.getContainer();
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.addMapping(new UIContainerMapping(ContainerSlotType.STONECUTTER_INPUT, this.stonecutter, CloudStonecutterView.INPUT_SLOT));
        this.addMapping(new UIContainerMapping(ContainerSlotType.STONECUTTER_RESULT, this.stonecutter, CloudStonecutterView.RESULT_SLOT));
        this.addMapping(new UIContainerMapping(ContainerSlotType.CREATED_OUTPUT, this.stonecutter, CloudStonecutterView.RESULT_SLOT));
    }

    @Override
    protected @Nullable FullContainerName uiSlotContainerName(ContainerSlotType type) {
        return switch (type) {
            case STONECUTTER_INPUT, STONECUTTER_RESULT, CREATED_OUTPUT -> null;
            default -> super.uiSlotContainerName(type);
        };
    }

    @Override
    public boolean isResultSlot(ContainerSlotType type, int slot) {
        return slot == RESULT_UI_SLOT && (type == ContainerSlotType.STONECUTTER_RESULT || type == ContainerSlotType.CREATED_OUTPUT);
    }

    @Override
    public boolean isInputSlot(ContainerSlotType type) {
        return type == ContainerSlotType.STONECUTTER_INPUT;
    }

    @Override
    public void trackResultSlots(BiConsumer<ContainerSlotType, Integer> tracker) {
        tracker.accept(ContainerSlotType.STONECUTTER_INPUT, INPUT_UI_SLOT);
        tracker.accept(ContainerSlotType.STONECUTTER_RESULT, RESULT_UI_SLOT);
    }

    @Override
    public @Nullable ResultTake prepareTake(ItemStack expected, int count) {
        return null;
    }

    public @Nullable ResultQuote quoteRecipe(int recipeNetworkId, int requestedCrafts) {
        if (requestedCrafts <= 0) {
            return null;
        }

        Selection selection = this.prepareSelection(recipeNetworkId);
        if (selection == null || selection.output().isEmpty()) {
            return null;
        }

        ItemStack output = selection.output();
        int maxCrafts = Math.min(requestedCrafts, selection.input().getCount());
        return new ResultQuote(output, count -> isInvalidCutCount(output.getCount(), count, maxCrafts), count -> this.prepareCut(selection, count, maxCrafts));
    }

    public @Nullable Runnable prepareRecipeSelection(int recipeNetworkId) {
        Selection selection = this.prepareSelection(recipeNetworkId);
        return selection == null ? null : () -> this.stonecutter.updateSelection(selection.recipe(), selection.output());
    }

    private @Nullable Selection prepareSelection(int recipeNetworkId) {
        ItemStack input = this.stonecutter.getInput();
        if (this.isUnavailable(input)) {
            return null;
        }

        CloudRecipeRegistry registry = CloudRecipeRegistry.get();
        if (!(registry.getRecipeFromNetId(recipeNetworkId) instanceof CloudStonecuttingRecipe recipe) || !registry.matchesStonecuttingInput(recipe, input)) {
            return null;
        }

        if (this.stonecutter.getSelectedRecipe() != recipe) {
            PlayerStonecutterRecipeSelectEvent selection = new PlayerStonecutterRecipeSelectEvent(this.player, this, recipe);
            this.player.getServer().getEventManager().fire(selection);

            Recipe replacement = registry.getRecipe(selection.getRecipe().getId());
            if (selection.isCancelled() || this.isUnavailable(input)
                    || !(replacement instanceof CloudStonecuttingRecipe selected)
                    || !registry.matchesStonecuttingInput(selected, input)) {
                return null;
            }
            recipe = selected;
        }

        PrepareResultEvent prepare = new PrepareResultEvent(this, recipe.getResult());
        this.player.getServer().getEventManager().fire(prepare);

        ItemStack output = prepare.getResult().normalizeBlockState();
        return this.isUnavailable(input) ? null : new Selection(input, recipe, output);
    }

    private static boolean isInvalidCutCount(int outputPerCut, int count, int maxCrafts) {
        return outputPerCut <= 0 || count <= 0 || count % outputPerCut != 0 || count / outputPerCut > maxCrafts;
    }

    private @Nullable ResultTake prepareCut(Selection selection, int count, int maxCrafts) {
        ItemStack input = selection.input();
        ItemStack output = selection.output();
        if (this.isUnavailable(input) || isInvalidCutCount(output.getCount(), count, maxCrafts)) {
            return null;
        }

        int remainingCount = input.getCount() - count / output.getCount();
        ItemStack remaining = remainingCount == 0 ? ItemStack.EMPTY : input.withCount(remainingCount);
        return new ResultTake(
                output.withCount(count),
                () -> this.stonecutter.finishCut(remaining, selection.recipe(), output),
                this::playCutSound
        );
    }

    private boolean isUnavailable(ItemStack input) {
        return this.closed
                || this.player.getInventoryManager().getScreen() != this
                || this.player.getLevel() != this.block.getLevel()
                || !this.player.isAlive()
                || this.player.isSpectator()
                || !this.player.canInteractWithBlock(this.block.getPosition())
                || this.player.getLevel().getBlockState(this.block.getPosition()).getType() != BlockTypes.STONECUTTER_BLOCK
                || input.isEmpty()
                || input.get(ItemDataComponents.ITEM_LOCK) != null
                || this.stonecutter.getInput() != input;
    }

    private void playCutSound() {
        int tick = this.player.getServer().getTick();
        if (this.lastSoundTick != tick) {
            this.lastSoundTick = tick;
            this.player.getLevel().addLevelSoundEvent(this.block.getPosition(), SoundEvent.UI_STONECUTTER_USE);
        }
    }

    private record Selection(ItemStack input, CloudStonecuttingRecipe recipe, ItemStack output) {
    }
}
