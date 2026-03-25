package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Furnace;
import org.cloudburstmc.api.event.inventory.FurnaceBurnEvent;
import org.cloudburstmc.api.event.inventory.FurnaceSmeltEvent;
import org.cloudburstmc.api.event.inventory.FurnaceStartSmeltEvent;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemComponents;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.data.Bucket;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.packet.ContainerSetDataPacket;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.container.ContainerListener;
import org.cloudburstmc.server.crafting.CloudFurnaceRecipe;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.registry.CloudRecipeRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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

    protected short burnTime = 0;
    protected short cookTime = 0;
    protected short burnDuration = 0;
    protected int currentCookDuration;

    public FurnaceBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position, new CloudContainer(3));
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
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForList("Items", NbtType.COMPOUND, tags -> {
            for (NbtMap itemTag : tags) {
                this.container.setItem(itemTag.getByte("Slot"), ItemUtils.deserializeItem(itemTag));
            }
        });
        tag.listenForShort("CookTime", this::setCookTime);
        tag.listenForShort("BurnTime", this::setBurnTime);
        tag.listenForShort("BurnDuration", this::setBurnDuration);
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        tag.putShort("CookTime", cookTime);
        tag.putShort("BurnTime", burnTime);
        tag.putShort("BurnDuration", burnDuration);
        List<NbtMap> items = new ArrayList<>();
        container.forEachSlot((itemStack, slot) -> {
            items.add(ItemUtils.serializeItem(itemStack, slot));
        });
        tag.putList("Items", NbtType.COMPOUND, items);
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
        for (ItemStack content : container.getContents()) {
            this.getLevel().dropItem(this.getPosition(), content);
        }
    }

    @Override
    public boolean isValid() {
        BlockType type = getBlockState().getType();
        return type == FURNACE || type == LIT_FURNACE;
    }

    protected float getBurnRate() {
        return 1.0f;
    }

    protected void checkFuel(ItemStack fuel) {
        FurnaceBurnEvent ev = new FurnaceBurnEvent(this, fuel,
                (short) CloudItemRegistry.get().getComponent(fuel.getType(), ItemComponents.FUEL_DURATION).get());
        this.server.getEventManager().fire(ev);
        if (ev.isCancelled()) {
            return;
        }

        burnDuration = (short) (ev.getBurnTime() / getBurnRate());
        burnTime = (short) (ev.getBurnTime() / getBurnRate());

        if (getBlockState().getType() != getLitType()) {
            lightFurnace();
        }

        if (burnTime > 0 && ev.isBurning()) {
            for (ContainerListener listener : this.container.getListeners()) {
                listener.onInventoryDataChange(this.container, ContainerSetDataPacket.FURNACE_LIT_DURATION, burnDuration);
            }

            if (fuel.getCount() <= 1) {
                if (fuel.getType() == ItemTypes.BUCKET && fuel.get(ItemKeys.BUCKET_DATA) == Bucket.LAVA) {
                    fuel = fuel.toBuilder().amount(1).data(ItemKeys.BUCKET_DATA, Bucket.EMPTY).build();
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
                (product.isEmpty() || (smelt.getResult().equals(product) && product.getCount() < CloudItemRegistry.get().getComponent(product.getType(), ItemComponents.GET_MAX_STACK_SIZE).execute(product)));

        if (
                burnTime <= 0 && canSmelt
                        && CloudItemRegistry.get().getComponent(fuel.getType(), ItemComponents.FUEL_DURATION).get() > 0
                        && fuel.getCount() > 0) {
            this.checkFuel(fuel);
        }

        if (burnTime > 0) {
            burnTime--;

            if (smelt != null && canSmelt) {
                if (cookTime == 0) {
                    int defaultDuration = (int) (200 / getBurnRate());
                    FurnaceStartSmeltEvent startEv = new FurnaceStartSmeltEvent(this, smelt, defaultDuration);
                    this.server.getEventManager().fire(startEv);
                    if (!startEv.isCancelled()) {
                        currentCookDuration = startEv.getCookTime();
                        cookTime++;
                    }
                } else {
                    cookTime++;
                }

                if (cookTime > 0 && cookTime >= currentCookDuration) {
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

                    cookTime -= currentCookDuration;
                }
            } else if (burnTime <= 0) {
                burnTime = 0;
                cookTime = 0;
            } else {
                cookTime = 0;
            }
            ret = true;
        } else {
            if (state.getType() == getLitType()) {
                extinguishFurnace();
            }
            burnTime = 0;
            cookTime = 0;
        }

        for (ContainerListener listener : this.container.getListeners()) {
            listener.onInventoryDataChange(this.container, ContainerSetDataPacket.FURNACE_TICK_COUNT, cookTime);
            listener.onInventoryDataChange(this.container, ContainerSetDataPacket.FURNACE_LIT_TIME, burnTime);
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

    @Override
    public int getCookProgress() {
        return cookTime;
    }

    @Override
    public void setCookProgress(int ticks) {
        this.cookTime = (short) Math.max(0, Math.min(ticks, getCookDuration()));
        for (ContainerListener listener : this.container.getListeners()) {
            listener.onInventoryDataChange(this.container, ContainerSetDataPacket.FURNACE_TICK_COUNT, this.cookTime);
        }
    }

    @Override
    public int getCookDuration() {
        return (int) (200 / getBurnRate());
    }

    @Override
    public int getBurnProgress() {
        return burnTime;
    }

    @Override
    public void setBurnProgress(int ticks) {
        setBurnTime(Math.max(0, ticks));
        for (ContainerListener listener : this.container.getListeners()) {
            listener.onInventoryDataChange(this.container, ContainerSetDataPacket.FURNACE_LIT_TIME, this.burnTime);
        }
    }

    @Override
    public int getBurnDuration() {
        return burnDuration;
    }

    @Override
    public void setBurnDuration(int ticks) {
        this.burnDuration = (short) Math.max(0, ticks);
        for (ContainerListener listener : this.container.getListeners()) {
            listener.onInventoryDataChange(this.container, ContainerSetDataPacket.FURNACE_LIT_DURATION, this.burnDuration);
        }
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }

    @Override
    public ItemStack getResult() {
        return this.container.getItem(SLOT_RESULT);
    }

    @Override
    public void setResult(ItemStack item) {
        this.container.setItem(SLOT_RESULT, item);
    }

    @Override
    public ItemStack getFuel() {
        return this.container.getItem(SLOT_FUEL);
    }

    @Override
    public void setFuel(ItemStack item) {
        this.container.setItem(SLOT_FUEL, item);
    }

    @Override
    public ItemStack getSmelting() {
        return this.container.getItem(SLOT_SMELTING);
    }

    @Override
    public void setSmelting(ItemStack item) {
        this.container.setItem(SLOT_SMELTING, item);
    }
}
