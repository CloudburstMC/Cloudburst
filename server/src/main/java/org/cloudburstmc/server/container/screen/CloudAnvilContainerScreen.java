package org.cloudburstmc.server.container.screen;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.inventory.AnvilScreen;
import org.cloudburstmc.api.inventory.ScreenTypes;
import org.cloudburstmc.api.inventory.view.AnvilView;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerSlotType;
import org.cloudburstmc.server.container.Container;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.anvil.AnvilResultCalculator;
import org.cloudburstmc.server.container.mapping.UIContainerMapping;
import org.cloudburstmc.server.container.view.CloudAnvilView;
import org.cloudburstmc.server.player.CloudPlayer;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

public class CloudAnvilContainerScreen extends CloudBlockContainerScreen implements AnvilScreen, ResultContainerScreen {

    private static final int ANVIL_INPUT_PROTOCOL_SLOT = 1;
    private static final int ANVIL_MATERIAL_PROTOCOL_SLOT = 2;
    private static final int ANVIL_RESULT_PROTOCOL_SLOT = 50;
    private static final int REPAIR_COST_PROPERTY = 0;
    private static final float ANVIL_DAMAGE_CHANCE = 0.12F;

    private final CloudAnvilView anvilSection;
    private final Runnable refreshResult = this::recomputeResult;
    private boolean updatingResult;
    private boolean onlyRenaming;

    public CloudAnvilContainerScreen(CloudPlayer player, Block block) {
        super(ScreenTypes.ANVIL, player, block);
        this.anvilSection = new CloudAnvilView();
    }

    @Override
    public AnvilView getAnvil() {
        return anvilSection;
    }

    @Override
    protected Container getStorageContainer() {
        return anvilSection.getContainer();
    }

    @Override
    public void open() {
        super.open();
        recomputeResult();
    }

    @Override
    public void close() {
        returnTemporaryItem(anvilSection.getInput());
        returnTemporaryItem(anvilSection.getMaterial());
        anvilSection.setInput(ItemStack.EMPTY);
        anvilSection.setMaterial(ItemStack.EMPTY);
        anvilSection.setResult(ItemStack.EMPTY);
        anvilSection.setRepairCost(0);
        anvilSection.setRepairItemCountCost(0);
        onlyRenaming = false;
        super.close();
    }

    @Override
    public void setSlot(ContainerSlotType type, int slot, ItemStack item) {
        super.setSlot(type, slot, item);
        if (!updatingResult && (type == ContainerSlotType.ANVIL_INPUT || type == ContainerSlotType.ANVIL_MATERIAL)) {
            CloudContainer.afterSlotWrites(this.refreshResult);
        }
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.addMapping(new UIContainerMapping(ContainerSlotType.ANVIL_INPUT, anvilSection, 0));
        this.addMapping(new UIContainerMapping(ContainerSlotType.ANVIL_MATERIAL, anvilSection, 1));
        this.addMapping(new UIContainerMapping(ContainerSlotType.ANVIL_RESULT, anvilSection, 2));
        this.addMapping(new UIContainerMapping(ContainerSlotType.CREATED_OUTPUT, anvilSection, 2));
    }

    public void updateRenameText(String text) {
        anvilSection.setRenameText(text);
        recomputeResult();
    }

    public boolean isResultSlot(ContainerSlotType type, int slot) {
        return (type == ContainerSlotType.ANVIL_RESULT || type == ContainerSlotType.CREATED_OUTPUT)
                && slot == ANVIL_RESULT_PROTOCOL_SLOT;
    }

    public @Nullable ResultTake prepareTake(ItemStack expected, int count) {
        ItemStack result = this.anvilSection.getResult();
        if (!result.equals(expected)) {
            return null;
        }

        return this.prepareTake(result, this.anvilSection.getRepairCost(), this.anvilSection.getRepairItemCountCost(), this.onlyRenaming, count);
    }

    public ResultQuote quoteRename(String name) {
        AnvilResultCalculator.AnvilResult result = this.calculateResult(name);
        return new ResultQuote(result.result(), count -> this.isInvalidResultCount(result.result(), count),
                count -> this.prepareTake(result.result(), result.repairCost(), result.repairItemCountCost(), result.onlyRenaming(), count));
    }

    private @Nullable ResultTake prepareTake(ItemStack result, int repairCost, int materialCost, boolean renaming, int count) {
        if (result.isEmpty() || this.isInvalidResultCount(result, count) || repairCost <= 0 || (!this.player.isCreative() && this.player.getExperienceLevel() < repairCost)) {
            return null;
        }

        return new ResultTake(result, () -> {
            this.consumeInputs(materialCost, renaming);
            this.anvilSection.setResult(ItemStack.EMPTY);
        }, () -> {
            this.anvilSection.setRepairCost(0);
            this.anvilSection.setRepairItemCountCost(0);
            this.anvilSection.setRenameText(null);
            this.onlyRenaming = false;
            this.payRepairCost(repairCost);
            this.damageAnvil();
            this.player.getLevel().addLevelSoundEvent(this.block.getPosition(), SoundEvent.RANDOM_ANVIL_USE);
        });
    }

    @Override
    public boolean isInputSlot(ContainerSlotType type) {
        return type == ContainerSlotType.ANVIL_INPUT || type == ContainerSlotType.ANVIL_MATERIAL;
    }

    @Override
    public void trackResultSlots(BiConsumer<ContainerSlotType, Integer> tracker) {
        tracker.accept(ContainerSlotType.ANVIL_INPUT, ANVIL_INPUT_PROTOCOL_SLOT);
        tracker.accept(ContainerSlotType.ANVIL_MATERIAL, ANVIL_MATERIAL_PROTOCOL_SLOT);
        tracker.accept(ContainerSlotType.ANVIL_RESULT, ANVIL_RESULT_PROTOCOL_SLOT);
    }

    private void recomputeResult() {
        AnvilResultCalculator.AnvilResult result = this.calculateResult(this.anvilSection.getRenameText());

        updatingResult = true;
        try {
            anvilSection.setResult(result.result());
            anvilSection.setRepairCost(result.repairCost());
            anvilSection.setRepairItemCountCost(result.repairItemCountCost());
            onlyRenaming = result.onlyRenaming();
            player.onInventoryDataChange(anvilSection.getContainer(), REPAIR_COST_PROPERTY, result.repairCost());
        } finally {
            updatingResult = false;
        }
    }

    private AnvilResultCalculator.AnvilResult calculateResult(@Nullable String name) {
        return AnvilResultCalculator.calculate(new AnvilResultCalculator.AnvilContext(
                this.anvilSection.getInput(), this.anvilSection.getMaterial(), name,
                this.anvilSection.getMaximumRepairCost(), this.player.isCreative(),
                this.anvilSection.bypassesEnchantmentLevelRestriction())
        );
    }

    private void payRepairCost(int repairCost) {
        if (player.isCreative()) {
            return;
        }

        player.setExperience(player.getExperience(), Math.max(0, player.getExperienceLevel() - repairCost));
    }

    private void consumeInputs(int materialCost, boolean renaming) {
        updatingResult = true;
        try {
            anvilSection.setInput(ItemStack.EMPTY);

            if (materialCost > 0) {
                ItemStack material = anvilSection.getMaterial();
                anvilSection.setMaterial(material.decreaseCount(materialCost));
            } else if (!renaming) {
                anvilSection.setMaterial(ItemStack.EMPTY);
            }
        } finally {
            updatingResult = false;
        }
    }

    private void damageAnvil() {
        if (player.isCreative() || ThreadLocalRandom.current().nextFloat() >= ANVIL_DAMAGE_CHANCE) {
            return;
        }

        BlockState current = block.refresh().getState();
        BlockState next = switch (current.getType().getId().getName()) {
            case "anvil" -> BlockTypes.CHIPPED_ANVIL.getDefaultState().copyTraits(current);
            case "chipped_anvil" -> BlockTypes.DAMAGED_ANVIL.getDefaultState().copyTraits(current);
            case "damaged_anvil" -> BlockStates.AIR;
            default -> current;
        };

        if (next != current) {
            block.set(next);
        }
    }

    private void returnTemporaryItem(ItemStack item) {
        if (item.isEmpty()) {
            return;
        }

        for (ItemStack remaining : player.getContainer().addItem(item)) {
            player.dropItem(remaining);
        }
    }

}
