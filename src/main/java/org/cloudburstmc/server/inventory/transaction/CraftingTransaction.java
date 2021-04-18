package org.cloudburstmc.server.inventory.transaction;

import com.nukkitx.protocol.bedrock.data.inventory.ContainerId;
import com.nukkitx.protocol.bedrock.packet.ContainerClosePacket;
import org.cloudburstmc.api.event.inventory.CraftItemEvent;
import org.cloudburstmc.api.inventory.CraftingGrid;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.server.crafting.CraftingRecipe;
import org.cloudburstmc.server.inventory.transaction.action.InventoryAction;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.registry.CloudRecipeRegistry;
import org.cloudburstmc.server.scheduler.Task;

import java.util.Arrays;
import java.util.List;

import static org.cloudburstmc.api.block.BlockTypes.CRAFTING_TABLE;
import static org.cloudburstmc.api.block.BlockTypes.FURNACE;

/**
 * @author CreeperFace
 */
public class CraftingTransaction extends InventoryTransaction {

    protected int gridSize;

    protected ItemStack[][] inputs;

    protected ItemStack[][] secondaryOutputs;

    protected ItemStack primaryOutput;

    protected CraftingRecipe recipe;

    public CraftingTransaction(CloudPlayer source, List<InventoryAction> actions) {
        super(source, actions, false);

        this.gridSize = (source.getInventory().getCraftingGrid().getCraftingGridType() == CraftingGrid.Type.CRAFTING_GRID_BIG) ? 3 : 2;
        ItemStack air = CloudItemRegistry.AIR;
        this.inputs = new ItemStack[gridSize][gridSize];
        for (ItemStack[] a : this.inputs) {
            Arrays.fill(a, air);
        }

        this.secondaryOutputs = new ItemStack[gridSize][gridSize];
        for (ItemStack[] a : this.secondaryOutputs) {
            Arrays.fill(a, air);
        }

        init(source, actions);
    }

    public void setInput(int index, ItemStack item) {
        int y = index / this.gridSize;
        int x = index % this.gridSize;

        if (this.inputs[y][x].isNull()) {
            inputs[y][x] = item;
        } else if (!inputs[y][x].equals(item)) {
            throw new RuntimeException("Input " + index + " has already been set and does not match the current item (expected " + inputs[y][x] + ", got " + item + ")");
        }
    }

    public ItemStack[][] getInputMap() {
        return inputs;
    }

    public void setExtraOutput(int index, ItemStack item) {
        int y = (index / this.gridSize);
        int x = index % gridSize;

        if (secondaryOutputs[y][x].isNull()) {
            secondaryOutputs[y][x] = item;
        } else if (!secondaryOutputs[y][x].equals(item)) {
            throw new RuntimeException("Output " + index + " has already been set and does not match the current item (expected " + secondaryOutputs[y][x] + ", got " + item + ")");
        }
    }

    public ItemStack getPrimaryOutput() {
        return primaryOutput;
    }

    public void setPrimaryOutput(ItemStack item) {
        if (primaryOutput == null) {
            primaryOutput = item;
        } else if (!primaryOutput.equals(item)) {
            throw new RuntimeException("Primary result item has already been set and does not match the current item (expected " + primaryOutput + ", got " + item + ")");
        }
    }

    public CraftingRecipe getRecipe() {
        return recipe;
    }

    private ItemStack[][] reindexInputs() {
        int xMin = gridSize - 1;
        int yMin = gridSize - 1;

        int xMax = 0;
        int yMax = 0;

        for (int y = 0; y < this.inputs.length; y++) {
            ItemStack[] row = this.inputs[y];

            for (int x = 0; x < row.length; x++) {
                ItemStack item = row[x];

                if (!item.isNull()) {
                    xMin = Math.min(x, xMin);
                    yMin = Math.min(y, yMin);

                    xMax = Math.max(x, xMax);
                    yMax = Math.max(y, yMax);
                }
            }
        }

        final int height = yMax - yMin + 1;
        final int width = xMax - xMin + 1;

        if (height < 1 || width < 1) {
            return new ItemStack[0][];
        }

        ItemStack[][] reindexed = new ItemStack[height][width];

        for (int y = yMin, i = 0; y <= yMax; y++, i++) {
            System.arraycopy(inputs[y], xMin, reindexed[i], 0, width);
        }

        return reindexed;
    }

    public boolean canExecute() {
        ItemStack[][] inputs = reindexInputs();

        recipe = (CraftingRecipe) CloudRecipeRegistry.get().matchRecipe(inputs, this.primaryOutput, this.secondaryOutputs, null);

        return this.recipe != null && super.canExecute();
    }

    protected boolean callExecuteEvent() {
        CraftItemEvent ev;

        this.source.getServer().getEventManager().fire(ev = new CraftItemEvent(this.source, (ItemStack[]) Arrays.stream(this.inputs).toArray(), this.recipe ));
        return !ev.isCancelled();
    }

    protected void sendInventories() {
        super.sendInventories();

        /*
         * TODO: HACK!
         * we can't resend the contents of the crafting window, so we force the client to close it instead.
         * So people don't whine about messy desync issues when someone cancels CraftItemEvent, or when a crafting
         * transaction goes wrong.
         */
        ContainerClosePacket packet = new ContainerClosePacket();
        packet.setId((byte) ContainerId.NONE);
        source.getServer().getScheduler().scheduleDelayedTask(new Task() {
            @Override
            public void onRun(int currentTick) {
                source.sendPacket(packet);
            }
        }, 20);

        this.source.getCraftingInventory().resetCraftingGrid();
    }

    public boolean execute() {
        if (super.execute()) {
            ItemType id = this.primaryOutput.getType();
            if (id == CRAFTING_TABLE) {
                source.awardAchievement("buildWorkBench");
            } else if (id == ItemTypes.WOODEN_PICKAXE) {
                source.awardAchievement("buildPickaxe");
            } else if (id == FURNACE) {
                source.awardAchievement("buildFurnace");
            } else if (id == ItemTypes.WOODEN_HOE) {
                source.awardAchievement("buildHoe");
            } else if (id == ItemTypes.BREAD) {
                source.awardAchievement("makeBread");
            } else if (id == ItemTypes.CAKE) {
                source.awardAchievement("bakeCake");
            } else if (id == ItemTypes.STONE_PICKAXE || id == ItemTypes.GOLDEN_PICKAXE || id == ItemTypes.IRON_PICKAXE || id == ItemTypes.DIAMOND_PICKAXE) {
                source.awardAchievement("buildBetterPickaxe");
            } else if (id == ItemTypes.WOODEN_SWORD) {
                source.awardAchievement("buildSword");
            } else if (id == ItemTypes.DIAMOND) {
                source.awardAchievement("diamond");
            }

            return true;
        }

        return false;
    }
}
