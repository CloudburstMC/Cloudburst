package org.cloudburstmc.server.inventory;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.event.entity.EntityInventoryChangeEvent;
import org.cloudburstmc.api.event.inventory.InventoryOpenEvent;
import org.cloudburstmc.api.inventory.Inventory;
import org.cloudburstmc.api.inventory.InventoryHolder;
import org.cloudburstmc.api.inventory.InventoryType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.registry.ItemRegistry;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.cloudburstmc.protocol.bedrock.packet.InventoryContentPacket;
import org.cloudburstmc.protocol.bedrock.packet.InventorySlotPacket;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.entity.BaseEntity;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.player.CloudPlayer;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.*;

import static org.cloudburstmc.api.block.BlockTypes.AIR;
import static org.cloudburstmc.api.item.ItemBehaviors.GET_MAX_STACK_SIZE;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class CloudInventory implements Inventory {

    protected final InventoryType type;

    protected int maxStackSize = MAX_STACK;

    protected int size;

    protected final String name;

    protected final String title;

    public final Int2ObjectMap<ItemStack> slots = new Int2ObjectOpenHashMap<>();

    protected final Set<CloudPlayer> viewers = new HashSet<>();

    protected InventoryHolder holder;

    @Inject
    ItemRegistry itemRegistry;

    public CloudInventory(InventoryHolder holder, InventoryType type) {
        this(holder, type, new HashMap<>());
    }

    public CloudInventory(InventoryHolder holder, InventoryType type, Map<Integer, ItemStack> contents) {
        this(holder, type, contents, null);
    }

    public CloudInventory(InventoryHolder holder, InventoryType type, Map<Integer, ItemStack> contents, Integer overrideSize) {
        this(holder, type, contents, overrideSize, null);
    }

    public CloudInventory(InventoryHolder holder, InventoryType type, Map<Integer, ItemStack> contents, Integer overrideSize, String overrideTitle) {
        this.holder = holder;

        this.type = type;

        if (overrideSize != null) {
            this.size = overrideSize;
        } else {
            this.size = this.type.getDefaultSize();
        }

        if (overrideTitle != null) {
            this.title = overrideTitle;
        } else {
            this.title = this.type.getDefaultTitle();
        }

        this.name = this.type.getDefaultTitle();

        if (!(this instanceof CloudDoubleChestInventory)) {
            this.setContents(contents);
        }
    }

    @Override
    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public int getMaxStackSize() {
        return maxStackSize;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    @NonNull
    public ItemStack getItem(int index) {
        return this.slots.getOrDefault(index, ItemStack.EMPTY);
    }

    @Override
    public Map<Integer, ItemStack> getContents() {
        return new HashMap<>(this.slots);
    }

    @Override
    public void setContents(Map<Integer, ItemStack> items) {
        if (items.size() > this.size) {
            TreeMap<Integer, ItemStack> newItems = new TreeMap<>();
            for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
                newItems.put(entry.getKey(), entry.getValue());
            }
            items = newItems;
            newItems = new TreeMap<>();
            int i = 0;
            for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
                newItems.put(entry.getKey(), entry.getValue());
                i++;
                if (i >= this.size) {
                    break;
                }
            }
            items = newItems;
        }

        for (int i = 0; i < this.size; ++i) {
            if (!items.containsKey(i)) {
                if (this.slots.containsKey(i)) {
                    this.clear(i);
                }
            } else {
                if (!this.setItem(i, items.get(i))) {
                    this.clear(i);
                }
            }
        }
    }

    @Override
    public boolean setItem(int index, ItemStack item, boolean send) {
        if (index < 0 || index >= this.size) {
            return false;
        } else if (item.getType() == AIR || item.getCount() <= 0) {
            return this.clear(index, send);
        }

        InventoryHolder holder = this.getHolder();
        if (holder instanceof BaseEntity) {
            EntityInventoryChangeEvent ev = new EntityInventoryChangeEvent((BaseEntity) holder, this.getItem(index), item, index);
            CloudServer.getInstance().getEventManager().fire(ev);
            if (ev.isCancelled()) {
                this.sendSlot(index, this.getViewers());
                return false;
            }

            item = ev.getNewItem();
        }

        if (holder instanceof BlockEntity) {
            ((BlockEntity) holder).setDirty();
        }

        ItemStack old = this.getItem(index);
        this.slots.put(index, item);
        this.onSlotChange(index, old, send);

        return true;
    }

    @Override
    public boolean contains(ItemStack item) {
        int count = Math.max(1, item.getCount());
        for (ItemStack i : this.getContents().values()) {
            if (item.equals(i)) {
                count -= i.getCount();
                if (count <= 0) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public Map<Integer, ItemStack> all(ItemStack item) {
        Map<Integer, ItemStack> slots = new HashMap<>();
        for (Map.Entry<Integer, ItemStack> entry : this.getContents().entrySet()) {
            if (item.equals(entry.getValue())) {
                slots.put(entry.getKey(), entry.getValue());
            }
        }

        return slots;
    }

    @Override
    public void remove(ItemStack item) {
        for (Map.Entry<Integer, ItemStack> entry : this.getContents().entrySet()) {
            if (item.equals(entry.getValue())) {
                this.clear(entry.getKey());
            }
        }
    }

    @Override
    public int first(ItemStack item, boolean exact) {
        int count = Math.max(1, item.getCount());
        for (Map.Entry<Integer, ItemStack> entry : this.getContents().entrySet()) {
            if (item.equals(entry.getValue()) && (entry.getValue().getCount() == count || (!exact && entry.getValue().getCount() > count))) {
                return entry.getKey();
            }
        }

        return -1;
    }

    @Override
    public int firstEmpty() {
        for (int i = 0; i < this.size; ++i) {
            if (this.getItem(i) == ItemStack.EMPTY) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public int firstNonEmpty() {
        for (int i = 0; i < this.size; ++i) {
            if (this.getItem(i) != ItemStack.EMPTY) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public int firstFit(ItemStack item, boolean single) {
        int count = single ? 1 : item.getCount();
        int maxStackSize = this.itemRegistry.getBehavior(item.getType(), GET_MAX_STACK_SIZE).execute();

        for (int i = 0; i < this.size; ++i) {
            ItemStack slot = this.getItem(i);
            if (slot.getCount() + count < maxStackSize && slot.equals(item)) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public void decrementCount(int slot) {
        ItemStack item = this.getItem(slot);

        if (item.getCount() > 0) {
            this.setItem(slot, item.decreaseCount());
        }
    }

    @Override
    public void incrementCount(int slot) {
        ItemStack item = this.getItem(slot);

        if (item.getType() != AIR) {
            this.setItem(slot, item.decreaseCount());
        }
    }

    @Override
    public boolean canAddItem(ItemStack item) {
        int count = item.getCount();

        if (count <= 0) {
            return true;
        }

        for (int i = 0; i < this.getSize(); ++i) {
            ItemStack slot = this.getItem(i);
            if (item.equals(slot)) {
                int diff;
                if ((diff = this.itemRegistry.getBehavior(slot.getType(), GET_MAX_STACK_SIZE).execute() - slot.getCount()) > 0) {
                    count -= diff;
                }
            } else if (slot.getType() == AIR) {
                count -= this.getMaxStackSize();
            }

            if (count <= 0) {
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings("SuspiciousListRemoveInLoop")
    @Override
    public ItemStack[] addItem(ItemStack... slots) {
        Preconditions.checkNotNull(slots, "slots");

        if (slots.length == 0) {
            return slots;
        }

        List<ItemStack> itemSlots = new ArrayList<>(slots.length);
        for (ItemStack slot : slots) {
            if (slot != ItemStack.EMPTY) {
                itemSlots.add(slot);
            }
        }

        List<Integer> emptySlots = new ArrayList<>();

        for (int i = 0; i < this.getSize(); ++i) {
            ItemStack item = this.getItem(i);
            if (item == ItemStack.EMPTY) {
                emptySlots.add(i);
            }

            int maxStack = this.itemRegistry.getBehavior(item.getType(), GET_MAX_STACK_SIZE).execute();

            var copy = new ArrayList<>(itemSlots);
            for (int j = 0; j < copy.size(); j++) {
                ItemStack slot = copy.get(j);

                if (slot.isSimilarMetadata(item) && item.getCount() < maxStack) {
                    int amount = Math.min(maxStack - item.getCount(), slot.getCount());
                    amount = Math.min(amount, this.getMaxStackSize());
                    if (amount > 0) {
                        slot = slot.decreaseCount(amount);
                        this.setItem(i, item.increaseCount(amount));

                        if (slot.getCount() <= 0) {
                            itemSlots.remove(j);
                        } else {
                            itemSlots.set(j, slot);
                        }
                    }
                }
            }

            if (itemSlots.isEmpty()) {
                break;
            }
        }

        if (!itemSlots.isEmpty() && !emptySlots.isEmpty()) {
            for (int slotIndex : emptySlots) {
                if (!itemSlots.isEmpty()) {
                    ItemStack slot = itemSlots.get(0);
                    int maxStackSize = this.itemRegistry.getBehavior(slot.getType(), GET_MAX_STACK_SIZE).execute();
                    int amount = Math.min(maxStackSize, slot.getCount());
                    amount = Math.min(amount, this.getMaxStackSize());

                    ItemStack item = slot.withCount(amount);
                    this.setItem(slotIndex, item);

                    slot = slot.decreaseCount(amount);
                    if (slot.getCount() <= 0) {
                        itemSlots.remove(0);
                    } else {
                        itemSlots.set(0, slot);
                    }
                }
            }
        }

        return itemSlots.toArray(new ItemStack[0]);
    }

    protected int getEmptySlotsCount() {
        int count = 0;

        for (ItemStack item : this.slots.values()) {
            if (ItemUtils.isNull(item)) {
                count++;
            }
        }

        return count;
    }

    private synchronized Int2ObjectMap<ItemStack> findMergable(@Nonnull ItemStack item) {
        Int2ObjectMap<ItemStack> mergable = new Int2ObjectOpenHashMap<>();

        for (var entry : this.slots.int2ObjectEntrySet()) {
            ItemStack content = entry.getValue();

            if (content != null && content.isMergeable(item)) {
                mergable.put(entry.getIntKey(), content);
            }
        }

        return mergable;
    }

    public boolean addItemToFirstEmptySlot(ItemStack item) {
        int slot = firstEmpty();
        if (slot < 0) {
            return false;
        }

        setItem(slot, item);
        return true;
    }

    @Override
    public ItemStack[] removeItem(ItemStack... slots) {
        List<ItemStack> itemSlots = new ArrayList<>();
        for (ItemStack slot : slots) {
            if (slot != ItemStack.EMPTY) {
                itemSlots.add(slot);
            }
        }

        for (int i = 0; i < this.size; ++i) {
            ItemStack item = this.getItem(i);
            if (item == ItemStack.EMPTY) {
                continue;
            }

            for (ItemStack slot : new ArrayList<>(itemSlots)) {
                if (slot.equals(item)) {
                    int count = Math.min(item.getCount(), slot.getCount());
                    slot = slot.decreaseCount(count);
                    item = item.decreaseCount(count);
                    this.setItem(i, item);
                    if (slot.getCount() <= 0) {
                        itemSlots.remove(slot);
                    }

                }
            }

            if (itemSlots.size() == 0) {
                break;
            }
        }

        return itemSlots.toArray(new ItemStack[0]);
    }

    @Override
    public boolean clear(int index, boolean send) {
        if (this.slots.containsKey(index)) {
            ItemStack item = ItemStack.EMPTY;
            ItemStack old = this.slots.get(index);
            InventoryHolder holder = this.getHolder();
            if (holder instanceof BaseEntity) {
                EntityInventoryChangeEvent ev = new EntityInventoryChangeEvent((BaseEntity) holder, old, item, index);
                CloudServer.getInstance().getEventManager().fire(ev);
                if (ev.isCancelled()) {
                    this.sendSlot(index, this.getViewers());
                    return false;
                }
                item = ev.getNewItem();
            }
            if (holder instanceof BlockEntity) {
                ((BlockEntity) holder).setDirty();
            }

            if (item != ItemStack.EMPTY) {
                this.slots.put(index, item);
            } else {
                this.slots.remove(index);
            }

            this.onSlotChange(index, old, send);
        }

        return true;
    }

    @Override
    public void clearAll() {
        for (Integer index : this.getContents().keySet()) {
            this.clear(index);
        }
    }

    @Override
    public Set<CloudPlayer> getViewers() {
        return viewers;
    }

    @Override
    public InventoryHolder getHolder() {
        return holder;
    }

    @Override
    public void setMaxStackSize(int maxStackSize) {
        this.maxStackSize = maxStackSize;
    }

    public boolean open(Player who) {
        InventoryOpenEvent ev = new InventoryOpenEvent(this, who);
        who.getServer().getEventManager().fire(ev);
        if (ev.isCancelled()) {
            return false;
        }
        this.onOpen(who);

        return true;
    }

    public void close(Player who) {
        this.onClose(who);
    }

    public void onOpen(Player who) {
        this.viewers.add((CloudPlayer) who);
    }

    public void onClose(Player who) {
        this.viewers.remove(who);
    }

    @Override
    public void onSlotChange(int index, ItemStack before, boolean send) {
        if (send) {
            this.sendSlot(index, this.getViewers());
        }
    }

    @Override
    public void sendContents(Player... players) {
        InventoryContentPacket packet = new InventoryContentPacket();
        List<ItemData> contents = new ArrayList<>();
        for (int i = 0; i < this.getSize(); ++i) {
            contents.add(i, ItemUtils.toNetwork(this.getItem(i)));
        }
        packet.setContents(contents);

        for (Player player : players) {
            int id = player.getWindowId(this);
            if (id == -1 || !player.isSpawned()) {
                this.close(player);
                continue;
            }
            packet.setContainerId(id);
            ((CloudPlayer) player).sendPacket(packet);
        }
    }

    @Override
    public boolean isFull() {
        if (this.slots.size() < this.getSize()) {
            return false;
        }

        for (ItemStack item : this.slots.values()) {
            if (ItemUtils.isNull(item) || item.getCount() < this.getMaxStackSize() ||
                    item.getCount() < this.itemRegistry.getBehavior(item.getType(), GET_MAX_STACK_SIZE).execute()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean isEmpty() {
        if (this.getMaxStackSize() <= 0) {
            return false;
        }

        for (ItemStack item : this.slots.values()) {
            if (!ItemUtils.isNull(item)) {
                return false;
            }
        }

        return true;
    }

    public int getFreeSpace(ItemStack item) {
        int itemMaxStackSize = this.itemRegistry.getBehavior(item.getType(), GET_MAX_STACK_SIZE).execute();
        int maxStackSize = Math.min(itemMaxStackSize, this.getMaxStackSize());
        int space = (this.getSize() - this.slots.size()) * maxStackSize;

        for (ItemStack slot : this.getContents().values()) {
            if (slot == null || slot == ItemStack.EMPTY) {
                space += maxStackSize;
                continue;
            }

            if (slot.isMergeable(item)) {
                space += maxStackSize - slot.getCount();
            }
        }

        return space;
    }

    @Override
    public void sendSlot(int index, Player... players) {
        InventorySlotPacket packet = new InventorySlotPacket();
        packet.setSlot(index);
        packet.setItem(ItemUtils.toNetwork(this.getItem(index)));

        for (Player player : players) {
            int id = player.getWindowId(this);
            if (id == -1) {
                this.close(player);
                continue;
            }
            packet.setContainerId(id);
            ((CloudPlayer) player).sendPacket(packet);
        }
    }

    public void saveInventory(NbtMapBuilder tag) {
        List<NbtMap> inventoryItems = new ArrayList<>();

        for (Int2ObjectMap.Entry<ItemStack> slot : this.slots.int2ObjectEntrySet()) {
            ItemStack item = slot.getValue();
            if (item != ItemStack.EMPTY) {
                inventoryItems.add(ItemUtils.serializeItem(item, slot.getIntKey()));
            }
        }

        tag.putList("Inventory", NbtType.COMPOUND, inventoryItems);
    }

    @NonNull
    @Override
    public InventoryType getType() {
        return type;
    }
}
