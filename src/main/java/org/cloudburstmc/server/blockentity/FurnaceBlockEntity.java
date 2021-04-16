package org.cloudburstmc.server.blockentity;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import com.nukkitx.protocol.bedrock.packet.ContainerSetDataPacket;
import lombok.val;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockType;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Furnace;
import org.cloudburstmc.api.event.inventory.FurnaceBurnEvent;
import org.cloudburstmc.api.event.inventory.FurnaceSmeltEvent;
import org.cloudburstmc.api.inventory.FurnaceInventory;
import org.cloudburstmc.api.inventory.InventoryType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.data.Bucket;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.server.crafting.FurnaceRecipe;
import org.cloudburstmc.server.inventory.CloudFurnaceInventory;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.registry.CloudRecipeRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.cloudburstmc.api.block.BlockTypes.FURNACE;

/**
 * @author MagicDroidX
 */
public class FurnaceBlockEntity extends BaseBlockEntity implements Furnace {

    protected final FurnaceInventory inventory;
    protected short burnTime = 0;
    protected short cookTime = 0;
    protected short burnDuration = 0;

    protected FurnaceBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position, InventoryType inventoryType) {
        super(type, chunk, position);
        this.inventory = new CloudFurnaceInventory(this, inventoryType);
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
        tag.listenForShort("BurnDuration", this::setBurnDuration);
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        tag.putShort("CookTime", cookTime);
        tag.putShort("BurnTime", burnTime);
        tag.putShort("BurnDuration", burnDuration);
        List<NbtMap> items = new ArrayList<>();
        for (Map.Entry<Integer, ItemStack> entry : this.inventory.getContents().entrySet()) {
            items.add(ItemUtils.serializeItem(entry.getValue(), entry.getKey()));
        }
        tag.putList("Items", NbtType.COMPOUND, items);
    }

    @Override
    public void close() {
        if (!closed) {
            for (CloudPlayer player : new HashSet<>(this.getInventory().getViewers())) {
                player.removeWindow(this.getInventory());
            }
            super.close();
        }
    }

    @Override
    public void onBreak() {
        for (ItemStack content : inventory.getContents().values()) {
            this.getLevel().dropItem(this.getPosition(), content);
        }
    }

    @Override
    public boolean isValid() {
        return getBlockState().getType() == FURNACE;
    }

    @Override
    public CloudFurnaceInventory getInventory() {
        return (CloudFurnaceInventory) inventory;
    }

    protected float getBurnRate() {
        return 1.0f;
    }

    protected void checkFuel(ItemStack fuel) {
        val behavior = fuel.getBehavior();
        FurnaceBurnEvent ev = new FurnaceBurnEvent(this, fuel, behavior.getFuelTime(fuel));
        this.server.getEventManager().fire(ev);
        if (ev.isCancelled()) {
            return;
        }

        burnDuration = (short) (ev.getBurnTime() / getBurnRate());
        burnTime = (short) (ev.getBurnTime() / getBurnRate());

        if (getBlockState().ensureTrait(BlockTraits.IS_EXTINGUISHED)) {
            lightFurnace();
        }

        if (burnTime > 0 && ev.isBurning()) {
            for (CloudPlayer p : this.getInventory().getViewers()) {
                ContainerSetDataPacket packet = new ContainerSetDataPacket();
                packet.setWindowId(p.getWindowId(this.getInventory()));
                packet.setProperty(ContainerSetDataPacket.FURNACE_LIT_DURATION);
                packet.setValue(burnDuration);

                p.sendPacket(packet);
            }

            if (fuel.getAmount() <= 1) {
                if (fuel.getType() == ItemTypes.BUCKET && fuel.getMetadata(Bucket.class) == Bucket.LAVA) {
                    fuel = fuel.toBuilder().amount(1).itemData(Bucket.EMPTY).build();
                } else {
                    fuel = CloudItemRegistry.AIR;
                }
            } else {
                fuel = fuel.decrementAmount();
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
        ItemStack fuel = this.inventory.getFuel();
        ItemStack raw = this.inventory.getSmelting();
        ItemStack product = this.inventory.getResult();
        BlockState state = getBlockState();
        BlockType blockType = state.getType();
        FurnaceRecipe smelt = CloudRecipeRegistry.get().matchFurnaceRecipe(raw, product, this.getBlockState().getType().getId());
        boolean canSmelt = (smelt != null && raw.getAmount() > 0 && ((smelt.getResult().equals(product)
                && product.getAmount() < product.getBehavior().getMaxStackSize(product)) || product.isNull()));

        if (burnTime <= 0 && canSmelt && fuel.getBehavior().getFuelTime(fuel) != 0 && fuel.getAmount() > 0) {
            this.checkFuel(fuel);
        }

        if (burnTime > 0) {
            burnTime--;

            if (smelt != null && canSmelt) {
                cookTime++;
                if (cookTime >= (200 / getBurnRate())) {
                    product = smelt.getResult().incrementAmount();

                    FurnaceSmeltEvent ev = new FurnaceSmeltEvent(this, raw, product);
                    this.server.getEventManager().fire(ev);
                    if (!ev.isCancelled()) {
                        this.inventory.setResult(ev.getResult());
                        if (raw.getAmount() <= 1) {
                            raw = CloudItemRegistry.AIR;
                        } else {
                            raw = raw.decrementAmount();
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
            if (!state.ensureTrait(BlockTraits.IS_EXTINGUISHED)) {
                extinguishFurnace();
            }
            burnTime = 0;
            cookTime = 0;
        }

        for (CloudPlayer player : this.getInventory().getViewers()) {
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
        this.getLevel().setBlockState(this.getPosition(), getBlockState().withTrait(BlockTraits.IS_EXTINGUISHED, true), true);
    }

    protected void lightFurnace() {
        this.getLevel().setBlockState(this.getPosition(), getBlockState().withTrait(BlockTraits.IS_EXTINGUISHED, false), true);
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

    public int getBurnDuration() {
        return burnDuration;
    }

    public void setBurnDuration(int burnDuration) {
        this.burnDuration = (short) burnDuration;
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }

    @Override
    public int[] getHopperPullSlots() {
        return new int[]{CloudFurnaceInventory.SLOT_RESULT};
    }

    @Override
    public int[] getHopperPushSlots(Direction direction, ItemStack item) {
        return new int[]{direction == Direction.DOWN ? CloudFurnaceInventory.SLOT_SMELTING : CloudFurnaceInventory.SLOT_FUEL};
    }
}
