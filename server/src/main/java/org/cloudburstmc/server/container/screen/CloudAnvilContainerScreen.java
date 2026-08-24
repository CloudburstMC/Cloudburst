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
import org.cloudburstmc.server.container.anvil.AnvilResultCalculator;
import org.cloudburstmc.server.container.mapping.ContainerMapping;
import org.cloudburstmc.server.container.view.CloudAnvilView;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.concurrent.ThreadLocalRandom;

public class CloudAnvilContainerScreen extends CloudBlockContainerScreen implements AnvilScreen {

    private static final int ANVIL_INPUT_PROTOCOL_SLOT = 1;
    private static final int ANVIL_MATERIAL_PROTOCOL_SLOT = 2;
    private static final int ANVIL_RESULT_PROTOCOL_SLOT = 50;
    private static final int REPAIR_COST_PROPERTY = 0;
    private static final float ANVIL_DAMAGE_CHANCE = 0.12F;

    private final CloudAnvilView anvilSection;
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
            recomputeResult();
        }
    }

    @Override
    protected void setupMappings() {
        super.setupMappings();
        this.addMapping(new ContainerMapping(ContainerSlotType.ANVIL_INPUT, anvilSection, 1, -1));
        this.addMapping(new ContainerMapping(ContainerSlotType.ANVIL_MATERIAL, anvilSection, 1, -1));
        this.addMapping(new ContainerMapping(ContainerSlotType.ANVIL_RESULT, anvilSection, 1, -48));
        this.addMapping(new ContainerMapping(ContainerSlotType.CREATED_OUTPUT, anvilSection, 1, -48));
    }

    public void updateRenameText(String text) {
        anvilSection.setRenameText(text);
        recomputeResult();
    }

    public boolean isResultSlot(ContainerSlotType type, int slot) {
        return (type == ContainerSlotType.ANVIL_RESULT || type == ContainerSlotType.CREATED_OUTPUT)
                && slot == ANVIL_RESULT_PROTOCOL_SLOT;
    }

    public ItemStack takeResult() {
        recomputeResult();
        ItemStack result = anvilSection.getResult();
        int repairCost = anvilSection.getRepairCost();
        if (result.isEmpty() || repairCost <= 0 || !canTakeResult()) {
            return ItemStack.EMPTY;
        }

        payRepairCost(repairCost);
        consumeInputs();
        anvilSection.setResult(ItemStack.EMPTY);
        anvilSection.setRepairCost(0);
        anvilSection.setRepairItemCountCost(0);
        anvilSection.setRenameText(null);
        onlyRenaming = false;
        damageAnvil();
        player.getLevel().addLevelSoundEvent(block.getPosition(), SoundEvent.RANDOM_ANVIL_USE);
        return result;
    }

    public void trackAnvilSlots(ItemStackRequestSlotTracker tracker) {
        tracker.track(ContainerSlotType.ANVIL_INPUT, ANVIL_INPUT_PROTOCOL_SLOT);
        tracker.track(ContainerSlotType.ANVIL_MATERIAL, ANVIL_MATERIAL_PROTOCOL_SLOT);
        tracker.track(ContainerSlotType.ANVIL_RESULT, ANVIL_RESULT_PROTOCOL_SLOT);
        tracker.track(ContainerSlotType.CREATED_OUTPUT, ANVIL_RESULT_PROTOCOL_SLOT);
    }

    private void recomputeResult() {
        AnvilResultCalculator.AnvilResult result = AnvilResultCalculator.calculate(new AnvilResultCalculator.AnvilContext(
                anvilSection.getInput(),
                anvilSection.getMaterial(),
                anvilSection.getRenameText(),
                anvilSection.getMaximumRepairCost(),
                player.isCreative(),
                anvilSection.bypassesEnchantmentLevelRestriction()
        ));

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

    private boolean canTakeResult() {
        return player.isCreative() || player.getExperienceLevel() >= anvilSection.getRepairCost();
    }

    private void payRepairCost(int repairCost) {
        if (player.isCreative()) {
            return;
        }

        player.setExperience(player.getExperience(), Math.max(0, player.getExperienceLevel() - repairCost));
    }

    private void consumeInputs() {
        updatingResult = true;
        try {
            anvilSection.setInput(ItemStack.EMPTY);

            if (anvilSection.getRepairItemCountCost() > 0) {
                ItemStack material = anvilSection.getMaterial();
                anvilSection.setMaterial(material.decreaseCount(anvilSection.getRepairItemCountCost()));
            } else if (!onlyRenaming) {
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

    @FunctionalInterface
    public interface ItemStackRequestSlotTracker {
        void track(ContainerSlotType type, int slot);
    }
}
