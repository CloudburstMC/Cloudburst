package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Furnace;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.blockentity.data.FurnaceData;
import org.cloudburstmc.api.event.inventory.FurnaceBurnEvent;
import org.cloudburstmc.api.event.inventory.FurnaceSmeltEvent;
import org.cloudburstmc.api.event.inventory.FurnaceStartSmeltEvent;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.data.Bucket;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.packet.ContainerSetDataPacket;
import org.cloudburstmc.server.container.ContainerListener;
import org.cloudburstmc.server.crafting.CloudFurnaceRecipe;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.registry.CloudRecipeRegistry;

import java.util.HashSet;

import static org.cloudburstmc.api.block.BlockTypes.FURNACE;
import static org.cloudburstmc.api.block.BlockTypes.LIT_FURNACE;

/**
 * Base block entity implementation for smelting containers (furnace, blast furnace, smoker).
 * Manages a 3-slot container (ingredient, fuel, result) with tick-based smelting logic,
 * and fires {@link org.cloudburstmc.api.event.inventory.FurnaceBurnEvent}
 * and {@link org.cloudburstmc.api.event.inventory.FurnaceSmeltEvent} during processing.
 */
public class FurnaceBlockEntity extends ContainerBlockEntity implements Furnace {

    private static final int SLOT_SMELTING = 0;
    private static final int SLOT_FUEL = 1;
    private static final int SLOT_RESULT = 2;

    protected int currentCookDuration;

    public FurnaceBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public SlotGroupType<? extends SlotGroup> getSlotGroupType() {
        return SlotGroupTypes.FURNACE;
    }

    @Override
    public Furnace getBlockEntity() {
        return this;
    }

    @Override
    protected Class<? extends BlockEntityData> minimumDataClass() {
        return FurnaceData.class;
    }

    @Override
    protected FurnaceData data() {
        return (FurnaceData) super.data();
    }

    @Override
    protected ItemContainer items() {
        return data().getItems();
    }

    @Override
    public void close() {
        if (!closed) {
            for (ContainerListener listener : new HashSet<>(this.getContainer().getListeners())) {
                if (listener instanceof CloudPlayer) {
                    ((CloudPlayer) listener).closeInventory();
                }
            }
            super.close();
        }
    }

    @Override
    public void onBreak() {
        for (ItemStack content : getContainer().getContents()) {
            this.dropItemOnBreak(content);
        }
    }

    protected float getBurnRate() {
        return 1.0f;
    }

    protected void checkFuel(ItemStack fuel) {
        FurnaceBurnEvent ev = new FurnaceBurnEvent(this, fuel,
                (short) CloudItemRegistry.get().requireComponent(fuel.getType(), ItemBehaviors.FUEL_DURATION).get());
        this.server.getEventManager().fire(ev);
        if (ev.isCancelled()) {
            return;
        }

        data().setBurnDuration((short) (ev.getBurnTime() / getBurnRate()));
        data().setBurnTime((short) (ev.getBurnTime() / getBurnRate()));

        if (getBlockState().getType() != getLitType()) {
            lightFurnace();
        }

        if (data().getBurnTime() > 0 && ev.isBurning()) {
            for (ContainerListener listener : this.getContainer().getListeners()) {
                listener.onInventoryDataChange(this.getContainer(), ContainerSetDataPacket.FURNACE_LIT_DURATION, data().getBurnDuration());
            }

            if (fuel.getCount() <= 1) {
                if (fuel.getType() == ItemTypes.BUCKET && fuel.get(ItemDataComponents.BUCKET_DATA) == Bucket.LAVA) {
                    fuel = fuel.toBuilder().amount(1).setData(ItemDataComponents.BUCKET_DATA, Bucket.EMPTY).build();
                } else {
                    fuel = ItemStack.EMPTY;
                }
            } else {
                fuel = fuel.decreaseCount();
            }
            this.setFuel(fuel);
        }
    }

    @Override
    public boolean onUpdate() {
        if (this.closed) {
            return false;
        }

        this.timing.startTiming();

        boolean ret = false;

        ItemStack fuel = this.getFuel();
        ItemStack raw = this.getSmelting();
        ItemStack product = this.getResult();
        BlockState state = getBlockState();
        BlockType blockType = state.getType();
        CloudFurnaceRecipe smelt = CloudRecipeRegistry.get().matchFurnaceRecipe(raw, product, this.getBlockState().getType().getId());
        boolean canSmelt = smelt != null && raw.getCount() > 0 &&
                (product.isEmpty() || (smelt.getResult().equals(product) && product.getCount() < CloudItemRegistry.get().requireComponent(product.getType(), ItemBehaviors.GET_MAX_STACK_SIZE).execute(product)));

        if (
                data().getBurnTime() <= 0 && canSmelt
                        && CloudItemRegistry.get().requireComponent(fuel.getType(), ItemBehaviors.FUEL_DURATION).get() > 0
                        && fuel.getCount() > 0) {
            this.checkFuel(fuel);
        }

        if (data().getBurnTime() > 0) {
            data().setBurnTime((short) (data().getBurnTime() - 1));

            if (smelt != null && canSmelt) {
                if (data().getCookTime() == 0) {
                    int defaultDuration = (int) (200 / getBurnRate());
                    FurnaceStartSmeltEvent startEv = new FurnaceStartSmeltEvent(this, smelt, defaultDuration);
                    this.server.getEventManager().fire(startEv);
                    if (!startEv.isCancelled()) {
                        currentCookDuration = startEv.getCookTime();
                        data().setCookTime((short) (data().getCookTime() + 1));
                    }
                } else {
                    data().setCookTime((short) (data().getCookTime() + 1));
                }

                if (data().getCookTime() > 0 && data().getCookTime() >= currentCookDuration) {
                    product = smelt.getResult().increaseCount();

                    FurnaceSmeltEvent ev = new FurnaceSmeltEvent(this, raw, product);
                    this.server.getEventManager().fire(ev);
                    if (!ev.isCancelled()) {
                        this.setResult(ev.getResult());
                        if (raw.getCount() <= 1) {
                            raw = ItemStack.EMPTY;
                        } else {
                            raw = raw.decreaseCount();
                        }
                        this.setSmelting(raw);
                    }

                    data().setCookTime((short) (data().getCookTime() - currentCookDuration));
                }
            } else if (data().getBurnTime() <= 0) {
                data().setBurnTime((short) 0);
                data().setCookTime((short) 0);
            } else {
                data().setCookTime((short) 0);
            }
            ret = true;
        } else {
            if (state.getType() == getLitType()) {
                extinguishFurnace();
            }
            data().setBurnTime((short) 0);
            data().setCookTime((short) 0);
        }

        for (ContainerListener listener : this.getContainer().getListeners()) {
            listener.onInventoryDataChange(this.getContainer(), ContainerSetDataPacket.FURNACE_TICK_COUNT, data().getCookTime());
            listener.onInventoryDataChange(this.getContainer(), ContainerSetDataPacket.FURNACE_LIT_TIME, data().getBurnTime());
        }

        this.lastUpdate = System.currentTimeMillis();

        this.timing.stopTiming();

        return ret;
    }

    protected BlockType getLitType() {
        return LIT_FURNACE;
    }

    protected BlockType getUnlitType() {
        return FURNACE;
    }

    protected void extinguishFurnace() {
        BlockState current = getBlockState();
        this.getLevel().setBlockState(this.getPosition(),
                getUnlitType().getDefaultState().copyTraits(current), true);
    }

    protected void lightFurnace() {
        BlockState current = getBlockState();
        this.getLevel().setBlockState(this.getPosition(),
                getLitType().getDefaultState().copyTraits(current), true);
    }

    public int getBurnTime() {
        return data().getBurnTime();
    }

    public void setBurnTime(int burnTime) {
        data().setBurnTime((short) burnTime);
        if (burnTime > 0) {
            this.scheduleUpdate();
        }
    }

    public int getCookTime() {
        return data().getCookTime();
    }

    public void setCookTime(int cookTime) {
        data().setCookTime((short) cookTime);
    }

    @Override
    public int getCookProgress() {
        return data().getCookTime();
    }

    @Override
    public void setCookProgress(int ticks) {
        data().setCookTime((short) Math.max(0, Math.min(ticks, getCookDuration())));
        for (ContainerListener listener : this.getContainer().getListeners()) {
            listener.onInventoryDataChange(this.getContainer(), ContainerSetDataPacket.FURNACE_TICK_COUNT, data().getCookTime());
        }
    }

    @Override
    public int getCookDuration() {
        return (int) (200 / getBurnRate());
    }

    @Override
    public int getBurnProgress() {
        return data().getBurnTime();
    }

    @Override
    public void setBurnProgress(int ticks) {
        setBurnTime(Math.max(0, ticks));
        for (ContainerListener listener : this.getContainer().getListeners()) {
            listener.onInventoryDataChange(this.getContainer(), ContainerSetDataPacket.FURNACE_LIT_TIME, data().getBurnTime());
        }
    }

    @Override
    public int getBurnDuration() {
        return data().getBurnDuration();
    }

    @Override
    public void setBurnDuration(int ticks) {
        data().setBurnDuration((short) Math.max(0, ticks));
        for (ContainerListener listener : this.getContainer().getListeners()) {
            listener.onInventoryDataChange(this.getContainer(), ContainerSetDataPacket.FURNACE_LIT_DURATION, data().getBurnDuration());
        }
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }

    @Override
    public ItemStack getResult() {
        return items().getItem(SLOT_RESULT);
    }

    @Override
    public void setResult(ItemStack item) {
        items().setItem(SLOT_RESULT, item);
    }

    @Override
    public ItemStack getFuel() {
        return items().getItem(SLOT_FUEL);
    }

    @Override
    public void setFuel(ItemStack item) {
        items().setItem(SLOT_FUEL, item);
    }

    @Override
    public ItemStack getSmelting() {
        return items().getItem(SLOT_SMELTING);
    }

    @Override
    public void setSmelting(ItemStack item) {
        items().setItem(SLOT_SMELTING, item);
    }
}
