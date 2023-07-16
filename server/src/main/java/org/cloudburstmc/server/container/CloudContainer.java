package org.cloudburstmc.server.container;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.container.Container;
import org.cloudburstmc.api.container.ContainerListener;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.registry.ItemRegistry;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.item.ItemUtils;

import java.util.*;
import java.util.function.ObjIntConsumer;

import static org.cloudburstmc.api.block.BlockTypes.AIR;
import static org.cloudburstmc.api.item.ItemBehaviors.GET_MAX_STACK_SIZE;

public class CloudContainer implements Container {

    //    protected final String name;
//    protected final String title;
    protected final ContainerStorage storage;
    protected final Set<ContainerListener> listeners = new HashSet<>();
    protected final int maxStackSize;
    @Inject
    ItemRegistry itemRegistry;

    public CloudContainer(int size) {
        this(size, MAX_STACK);
    }

    public CloudContainer(int size, int maxStackSize) {
        this.storage = new ArrayContainerStorage(size);
        this.maxStackSize = maxStackSize;
    }

    public CloudContainer(CloudContainer... children) {
        this(MAX_STACK, children);
    }

    public CloudContainer(int maxStackSize, CloudContainer... children) {
        ContainerStorage[] childStorage = new ContainerStorage[children.length];
        for (int i = 0; i < children.length; i++) {
            childStorage[i] = children[i].storage;
        }
        this.storage = new MultiContainerStorage(childStorage);
        this.maxStackSize = maxStackSize;
    }

    @Override
    public int size() {
        return storage.size();
    }

    public void setSize(int size) {
        this.storage.resize(size);
    }

    @Override
    public int getMaxStackSize() {
        return maxStackSize;
    }

    @Override
    public String getName() {
        return null; // TODO
    }

    @Override
    public String getTitle() {
        return null; // TODO
    }

    @Override
    @NonNull
    public ItemStack getItem(int index) {
        return this.storage.get(index);
    }

    @Override
    public ItemStack[] getContents() {
        return this.storage.getContents();
    }

    @Override
    public void setContents(ItemStack[] items) {
        if (items.length > this.size()) {
            throw new IllegalArgumentException("Invalid inventory size; expected " + this.size() + " or less");
        } else if (items.length < this.size()) {
            items = Arrays.copyOf(items, this.size());
        }

        for (int i = 0; i < this.size(); ++i) {
            if (items[i] == null) {
                items[i] = ItemStack.EMPTY;
            }
        }

        this.storage.setContents(items);

        for (ContainerListener listener : this.listeners) {
            listener.onInventoryContentsChange(this);
        }
    }

    @Override
    public void setItem(int index, @NonNull ItemStack item) {
        Objects.requireNonNull(item, "item");
        checkSlotIndex(index);

        // TODO: check if the item is valid with the registry

//        InventoryHolder holder = this.getHolder();
//        if (holder instanceof BaseEntity) {
//            EntityInventoryChangeEvent ev = new EntityInventoryChangeEvent((BaseEntity) holder, this.getItem(index), item, index);
//            CloudServer.getInstance().getEventManager().fire(ev);
//            if (ev.isCancelled()) {
//                this.sendSlot(index, this.getListeners());
//                return false;
//            }
//
//            item = ev.getNewItem();
//        }
//
//        if (holder instanceof BlockEntity) {
//            ((BlockEntity) holder).setDirty();
//        }

        ItemStack old = this.storage.get(index);
        if (old.equals(item)) {
            return;
        }
        this.storage.set(index, item);
        this.onSlotChange(index, old);
    }

    @Override
    public boolean contains(ItemStack item) {
        int count = Math.max(1, item.getCount());
        for (int i = 0; i < this.size(); i++) {
            ItemStack slot = this.getItem(i);
            if (item.equals(slot)) {
                count -= slot.getCount();
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
        for (int i = 0; i < this.size(); ++i) {
            ItemStack slot = this.getItem(i);
            if (item.equals(slot)) {
                slots.put(i, slot);
            }
        }

        return slots;
    }

    @Override
    public void remove(ItemStack item) {
        for (int i = 0; i < this.size(); ++i) {
            if (item.equals(this.getItem(i))) {
                this.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public int first(ItemStack item, boolean exact) {
        int count = Math.max(1, item.getCount());
        for (int i = 0; i < this.size(); ++i) {
            ItemStack slot = this.getItem(i);
            if (item.equals(slot) && (slot.getCount() == count || (!exact && slot.getCount() > count))) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public int firstEmpty() {
        for (int i = 0; i < this.size(); ++i) {
            if (this.getItem(i) == ItemStack.EMPTY) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public int firstNonEmpty() {
        for (int i = 0; i < this.size(); ++i) {
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

        for (int i = 0; i < this.size(); ++i) {
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

        for (int i = 0; i < this.size(); ++i) {
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

        for (int i = 0; i < this.size(); ++i) {
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

        for (int i = 0; i < this.size(); i++) {
            if (this.getItem(i) == ItemStack.EMPTY) {
                count++;
            }
        }

        return count;
    }

    private synchronized Int2ObjectMap<ItemStack> findCombinable(@NonNull ItemStack item) {
        Int2ObjectMap<ItemStack> combinable = new Int2ObjectOpenHashMap<>();

        for (int i = 0; i < this.size(); i++) {
            ItemStack content = this.getItem(i);

            if (content.isCombinable(item)) {
                combinable.put(i, content);
            }
        }

        return combinable;
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

        for (int i = 0; i < this.size(); ++i) {
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

    protected void checkSlotIndex(int index) {
        if (index < 0 || index >= this.size()) {
            throw new IndexOutOfBoundsException("Slot index out of range: " + index);
        }
    }

    @Override
    public void clear() {
        boolean changed = false;
        for (int index = 0; index < this.size(); index++) {
            if (this.getItem(index) != ItemStack.EMPTY) {
                this.setItem(index, ItemStack.EMPTY);
                changed = true;
            }
        }
        if (changed) {
            this.onContentsChange();
        }
    }

    public void addContainerListener(ContainerListener listener) {
        Objects.requireNonNull(listener, "listener");
        this.listeners.add(listener);

        listener.onInventoryContentsChange(this);
    }

    public void removeContainerListener(ContainerListener listener) {
        Objects.requireNonNull(listener, "listener");
        this.listeners.remove(listener);
    }

    public Set<ContainerListener> getListeners() {
        return listeners;
    }

    @Override
    public void forEachSlot(ObjIntConsumer<ItemStack> consumer) {
        for (int i = 0; i < this.size(); i++) {
            ItemStack item = this.getItem(i);
            consumer.accept(item, i);
        }
    }

    protected void onSlotChange(int index, ItemStack old) {
        for (ContainerListener listener : this.listeners) {
            listener.onInventorySlotChange(this, index);
        }
    }

    protected void onContentsChange() {
        for (ContainerListener listener : this.listeners) {
            listener.onInventoryContentsChange(this);
        }
    }

    @Override
    public void refresh() {
        for (ContainerListener listener : this.listeners) {
            listener.onInventoryContentsChange(this);
        }
    }


    @Override
    public boolean isFull() {
        if (this.size() < this.size()) {
            return false;
        }

        for (int i = 0; i < this.size(); i++) {
            ItemStack item = this.getItem(i);
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

        for (int i = 0; i < this.size(); i++) {
            ItemStack item = this.getItem(i);
            if (item != ItemStack.EMPTY) {
                return false;
            }
        }

        return true;
    }

    public int getFreeSpace(ItemStack item) {
        int itemMaxStackSize = this.itemRegistry.getBehavior(item.getType(), GET_MAX_STACK_SIZE).execute();
        int maxStackSize = Math.min(itemMaxStackSize, this.getMaxStackSize());
        int space = (this.size() - this.size()) * maxStackSize;

        for (ItemStack slot : this.getContents()) {
            if (slot == null || slot == ItemStack.EMPTY) {
                space += maxStackSize;
                continue;
            }

            if (slot.isCombinable(item)) {
                space += maxStackSize - slot.getCount();
            }
        }

        return space;
    }

    public void saveInventory(NbtMapBuilder tag) {
        List<NbtMap> inventoryItems = new ArrayList<>();

        for (int i = 0; i < this.size(); i++) {
            ItemStack item = this.getItem(i);
            if (item != ItemStack.EMPTY) {
                inventoryItems.add(ItemUtils.serializeItem(item, i));
            }
        }

        tag.putList("Inventory", NbtType.COMPOUND, inventoryItems);
    }

    public void close() {
        for (ContainerListener listener : this.listeners) {
            listener.onInventoryRemoved(this);
        }
    }
}
