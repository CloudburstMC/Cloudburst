package org.cloudburstmc.server.blockentity.impl;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import com.nukkitx.protocol.bedrock.packet.ContainerSetDataPacket;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.blockentity.BlockEntityType;
import org.cloudburstmc.server.blockentity.Furnace;
import org.cloudburstmc.server.event.inventory.FurnaceBurnEvent;
import org.cloudburstmc.server.event.inventory.FurnaceSmeltEvent;
import org.cloudburstmc.server.inventory.FurnaceInventory;
import org.cloudburstmc.server.inventory.FurnaceRecipe;
import org.cloudburstmc.server.inventory.InventoryType;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.level.chunk.Chunk;
import org.cloudburstmc.server.math.BlockFace;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.Identifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.cloudburstmc.server.block.BlockTypes.*;

/**
 * @author MagicDroidX
 */
public class FurnaceBlockEntity extends BaseBlockEntity implements Furnace {

    protected final FurnaceInventory inventory;
    protected short burnTime = 0;
    protected short cookTime = 0;
    protected short maxTime = 0;

    protected FurnaceBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position, InventoryType inventoryType) {
        super(type, chunk, position);
        this.inventory = new FurnaceInventory(this, inventoryType);
    }

    public FurnaceBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        this(type, chunk, position, InventoryType.FURNACE);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForList("Items", NbtType.COMPOUND, tags -> {
            for (NbtMap itemTag : tags) {
                this.inventory.setItem(itemTag.getByte("Slot"), ItemUtils.deserializeItem(itemTag));
            }
        });
        tag.listenForShort("CookTime", this::setCookTime);
        tag.listenForShort("BurnTime", this::setBurnTime);
        tag.listenForShort("MaxTime", this::setMaxTime);
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        tag.putShort("CookTime", cookTime);
        tag.putShort("BurnTime", burnTime);
        tag.putShort("MaxTime", maxTime);
        List<NbtMap> items = new ArrayList<>();
        for (Map.Entry<Integer, Item> entry : this.inventory.getContents().entrySet()) {
            items.add(ItemUtils.serializeItem(entry.getValue(), entry.getKey()));
        }
        tag.putList("Items", NbtType.COMPOUND, items);
    }

    @Override
    public void close() {
        if (!closed) {
            for (Player player : new HashSet<>(this.getInventory().getViewers())) {
                player.removeWindow(this.getInventory());
            }
            super.close();
        }
    }

    @Override
    public void onBreak() {
        for (Item content : inventory.getContents().values()) {
            this.getLevel().dropItem(this.getPosition(), content);
        }
    }

    @Override
    public boolean isValid() {
        Identifier blockId = getBlock().getId();
        return blockId == FURNACE || blockId == LIT_FURNACE;
    }

    @Override
    public FurnaceInventory getInventory() {
        return inventory;
    }

    protected float getBurnRate() {
        return 1.0f;
    }

    protected void checkFuel(Item fuel) {
        FurnaceBurnEvent ev = new FurnaceBurnEvent(this, fuel, fuel.getFuelTime() == null ? 0 : fuel.getFuelTime());
        this.server.getPluginManager().callEvent(ev);
        if (ev.isCancelled()) {
            return;
        }

        maxTime = (short) (ev.getBurnTime() / getBurnRate());
        burnTime = (short) (ev.getBurnTime() / getBurnRate());

        if (this.getBlock().getId() == BlockTypes.FURNACE
                || this.getBlock().getId() == BlockTypes.SMOKER
                || this.getBlock().getId() == BlockTypes.BLAST_FURNACE) {
            lightFurnace();
        }

        if (burnTime > 0 && ev.isBurning()) {
            for (Player p : this.getInventory().getViewers()) {
                ContainerSetDataPacket packet = new ContainerSetDataPacket();
                packet.setWindowId(p.getWindowId(this.getInventory()));
                packet.setProperty(ContainerSetDataPacket.FURNACE_LIT_DURATION);
                packet.setValue(maxTime);

                p.sendPacket(packet);
            }
            fuel.setCount(fuel.getCount() - 1);
            if (fuel.getCount() == 0) {
                if (fuel.getId() == ItemIds.BUCKET && fuel.getMeta() == 10) {
                    fuel.setMeta(0);
                    fuel.setCount(1);
                } else {
                    fuel = Item.get(AIR, 0, 0);
                }
            }
            this.inventory.setFuel(fuel);
        }
    }

    @Override
    public boolean onUpdate() {
        if (this.closed) {
            return false;
        }

        this.timing.startTiming();

        boolean ret = false;
        Item fuel = this.inventory.getFuel();
        Item raw = this.inventory.getSmelting();
        Item product = this.inventory.getResult();
        Identifier blockId = getBlock().getId();
        FurnaceRecipe smelt = this.server.getCraftingManager().matchFurnaceRecipe(raw, blockId);
        boolean canSmelt = (smelt != null && raw.getCount() > 0 && ((smelt.getResult().equals(product, true)
                && product.getCount() < product.getMaxStackSize()) || product.getId() == AIR));

        if (burnTime <= 0 && canSmelt && fuel.getFuelTime() != null && fuel.getCount() > 0) {
            this.checkFuel(fuel);
        }

        if (burnTime > 0) {
            burnTime--;

            if (smelt != null && canSmelt) {
                cookTime++;
                if (cookTime >= (200 / getBurnRate())) {
                    product = Item.get(smelt.getResult().getId(), smelt.getResult().getMeta(), product.getCount() + 1);

                    FurnaceSmeltEvent ev = new FurnaceSmeltEvent(this, raw, product);
                    this.server.getPluginManager().callEvent(ev);
                    if (!ev.isCancelled()) {
                        this.inventory.setResult(ev.getResult());
                        raw.setCount(raw.getCount() - 1);
                        if (raw.getCount() == 0) {
                            raw = Item.get(AIR, 0, 0);
                        }
                        this.inventory.setSmelting(raw);
                    }

                    cookTime -= (200 / getBurnRate());
                }
            } else if (burnTime <= 0) {
                burnTime = 0;
                cookTime = 0;
            } else {
                cookTime = 0;
            }
            ret = true;
        } else {
            if (blockId == BlockTypes.LIT_FURNACE || blockId == LIT_BLAST_FURNACE || blockId == LIT_SMOKER) {
                extinguishFurnace();
            }
            burnTime = 0;
            cookTime = 0;
        }

        for (Player player : this.getInventory().getViewers()) {
            byte windowId = player.getWindowId(this.getInventory());
            if (windowId > 0) {
                ContainerSetDataPacket packet = new ContainerSetDataPacket();
                packet.setWindowId(windowId);
                packet.setProperty(ContainerSetDataPacket.FURNACE_TICK_COUNT);
                packet.setValue(cookTime);
                player.sendPacket(packet);

                packet = new ContainerSetDataPacket();
                packet.setWindowId(windowId);
                packet.setProperty(ContainerSetDataPacket.FURNACE_LIT_TIME);
                packet.setValue(burnTime);
                player.sendPacket(packet);
            }
        }

        this.lastUpdate = System.currentTimeMillis();

        this.timing.stopTiming();

        return ret;
    }

    protected void extinguishFurnace() {
        this.getLevel().setBlock(this.getPosition(), BlockState.get(FURNACE, this.getBlock().getMeta()), true);
    }

    protected void lightFurnace() {
        this.getLevel().setBlock(this.getPosition(), BlockState.get(LIT_FURNACE, this.getBlock().getMeta()), true);
    }

    public int getBurnTime() {
        return burnTime;
    }

    public void setBurnTime(int burnTime) {
        this.burnTime = (short) burnTime;
        if (burnTime > 0) {
            this.scheduleUpdate();
        }
    }

    public int getCookTime() {
        return cookTime;
    }

    public void setCookTime(int cookTime) {
        this.cookTime = (short) cookTime;
    }

    public int getMaxTime() {
        return maxTime;
    }

    public void setMaxTime(int maxTime) {
        this.maxTime = (short) maxTime;
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }

    @Override
    public int[] getHopperPullSlots() {
        return new int[]{FurnaceInventory.SLOT_RESULT};
    }

    @Override
    public int[] getHopperPushSlots(BlockFace direction, Item item) {
        return new int[]{direction == BlockFace.DOWN ? FurnaceInventory.SLOT_SMELTING : FurnaceInventory.SLOT_FUEL};
    }
}
