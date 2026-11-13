package org.cloudburstmc.server.container;

import lombok.Getter;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.registry.ItemRegistry;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.server.item.ItemStackNbtCodec;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.*;
import java.util.function.ObjIntConsumer;

public class CloudContainer implements Container {

    private static final ScopedValue<SlotWriteBatch> SLOT_WRITES = ScopedValue.newInstance();

    protected final ContainerStorage storage;
    @Getter
    protected final Set<ContainerListener> listeners = new HashSet<>();
    protected final int maxStackSize;
    protected final ItemRegistry itemRegistry;
    protected String name;
    protected String title;

    public CloudContainer(int size) {
        this(size, MAX_STACK);
    }

    public CloudContainer(int size, int maxStackSize) {
        this("", "", size, maxStackSize);
    }

    public CloudContainer(String name, String title, int size) {
        this(name, title, size, MAX_STACK);
    }

    public CloudContainer(String name, String title, int size, int maxStackSize) {
        this.name = name;
        this.title = title;
        this.storage = new ArrayContainerStorage(size);
        this.maxStackSize = maxStackSize;
        this.itemRegistry = CloudItemRegistry.get();
    }

    public CloudContainer(CloudContainer... children) {
        this(MAX_STACK, children);
    }

    public CloudContainer(int maxStackSize, CloudContainer... children) {
        this.name = "";
        this.title = "";
        ContainerStorage[] childStorage = new ContainerStorage[children.length];
        for (int i = 0; i < children.length; i++) {
            childStorage[i] = children[i].storage;
        }
        this.storage = new MultiContainerStorage(childStorage);
        this.maxStackSize = maxStackSize;
        this.itemRegistry = CloudItemRegistry.get();
    }

    public static void applySlotWrites(Runnable writer) {
        if (SLOT_WRITES.isBound()) {
            throw new IllegalStateException("Nested slot write batch");
        }

        SlotWriteBatch batch = new SlotWriteBatch();
        try {
            ScopedValue.where(SLOT_WRITES, batch).run(writer);
        } catch (RuntimeException | Error failure) {
            batch.restore();
            throw failure;
        }

        batch.publish();
    }

    public static void afterSlotWrites(Runnable callback) {
        if (SLOT_WRITES.isBound()) {
            SLOT_WRITES.get().callbacks.add(callback);
        } else {
            callback.run();
        }
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
    public int getMaxStackSize(int slot) {
        return getMaxStackSize();
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
    public void forEachSlot(ObjIntConsumer<ItemStack> consumer) {
        for (int i = 0; i < this.size(); i++) {
            ItemStack item = this.getItem(i);
            consumer.accept(item, i);
        }
    }

    @Override
    public void setItem(int index, @NonNull ItemStack item) {
        Objects.requireNonNull(item, "item");
        checkSlotIndex(index);

        if (!item.isEmpty()) {
            this.itemRegistry.getId(item.getType());
            item = item.normalizeBlockState();
        }

        ItemStack old = this.storage.get(index);
        if (old.equals(item)) {
            return;
        }

        this.storage.set(index, item);

        if (SLOT_WRITES.isBound()) {
            SLOT_WRITES.get().record(this, index, old);
        } else {
            this.onSlotChange(index, old);
        }
    }

    @Override
    public void setContents(ItemStack[] items) {
        if (items.length > this.size()) {
            throw new IllegalArgumentException("Invalid inventory size. Expected " + this.size() + " or less");
        } else if (items.length < this.size()) {
            items = Arrays.copyOf(items, this.size());
        }

        for (int i = 0; i < this.size(); ++i) {
            this.setItem(i, items[i] != null ? items[i] : ItemStack.EMPTY);
        }

        for (ContainerListener listener : this.listeners) {
            listener.onInventoryContentsChange(this);
        }
    }

    protected void checkSlotIndex(int index) {
        if (index < 0 || index >= this.size()) {
            throw new IndexOutOfBoundsException("Slot index out of range: " + index);
        }
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < this.size(); i++) {
            ItemStack item = this.getItem(i);
            if (!item.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean isFull() {
        for (int i = 0; i < this.size(); i++) {
            ItemStack item = this.getItem(i);
            if (item.isEmpty() || item.getCount() < this.stackLimit(item)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean contains(ItemStack item) {
        int count = Math.max(1, item.getCount());
        for (int i = 0; i < this.size(); i++) {
            ItemStack slot = this.getItem(i);
            if (slot.isStackableWith(item)) {
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
            if (slot.isStackableWith(item)) {
                slots.put(i, slot);
            }
        }

        return slots;
    }

    @Override
    public int first(ItemStack item, boolean exact) {
        int count = Math.max(1, item.getCount());
        for (int i = 0; i < this.size(); ++i) {
            ItemStack slot = this.getItem(i);
            if (slot.isStackableWith(item) && (slot.getCount() == count || (!exact && slot.getCount() >= count))) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public int firstEmpty() {
        for (int i = 0; i < this.size(); ++i) {
            if (this.getItem(i).isEmpty()) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public int firstNonEmpty() {
        for (int i = 0; i < this.size(); ++i) {
            if (!this.getItem(i).isEmpty()) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public int firstFit(ItemStack item, boolean single) {
        if (item.isEmpty()) {
            return firstEmpty();
        }

        ItemStack normalized = item.normalizeBlockState();
        int count = single ? 1 : normalized.getCount();
        int maxStackSize = this.stackLimit(normalized);

        for (int i = 0; i < this.size(); ++i) {
            ItemStack slot = this.getItem(i);
            if (slot.isEmpty()) {
                if (count <= maxStackSize) {
                    return i;
                }
                continue;
            }

            if (count <= maxStackSize - slot.getCount() && slot.isStackableWith(normalized)) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public boolean canAddItem(ItemStack item) {
        return item.isEmpty() || this.getFreeSpace(item) >= item.getCount();
    }

    public int getFreeSpace(ItemStack item) {
        if (item.isEmpty()) {
            return 0;
        }

        ItemStack normalized = item.normalizeBlockState();
        int limit = this.stackLimit(normalized);
        int space = 0;
        for (int slot = 0; slot < this.size(); slot++) {
            ItemStack current = this.getItem(slot);
            if (current.isEmpty()) {
                space += limit;
            } else if (current.isStackableWith(normalized)) {
                space += Math.max(0, limit - current.getCount());
            }
        }

        return space;
    }

    private int stackLimit(ItemStack item) {
        int itemLimit = this.itemRegistry.requireComponent(item.getType(), ItemBehaviors.GET_MAX_STACK_SIZE).execute(item);
        return Math.clamp(itemLimit, 0, Math.max(0, this.getMaxStackSize()));
    }

    @Override
    public ItemStack[] addItem(ItemStack... items) {
        List<ItemStack> leftovers = new ArrayList<>();
        for (ItemStack item : items) {
            if (item.isEmpty()) {
                continue;
            }

            this.itemRegistry.getId(item.getType());
            ItemStack normalized = item.normalizeBlockState();
            int remaining = normalized.getCount();
            int limit = this.stackLimit(normalized);
            for (int slot = 0; slot < this.size() && remaining > 0; slot++) {
                ItemStack current = this.getItem(slot);
                if (!current.isEmpty() && current.isStackableWith(normalized)) {
                    int added = Math.clamp(limit - current.getCount(), 0, remaining);
                    if (added > 0) {
                        this.setItem(slot, current.increaseCount(added));
                        remaining -= added;
                    }
                }
            }

            for (int slot = 0; slot < this.size() && remaining > 0 && limit > 0; slot++) {
                if (this.getItem(slot).isEmpty()) {
                    int added = Math.min(remaining, limit);
                    this.setItem(slot, normalized.withCount(added));
                    remaining -= added;
                }
            }

            if (remaining > 0) {
                leftovers.add(normalized.withCount(remaining));
            }
        }

        return leftovers.toArray(ItemStack[]::new);
    }

    @Override
    public ItemStack[] removeItem(ItemStack... items) {
        List<ItemStack> leftovers = new ArrayList<>();
        for (ItemStack item : items) {
            if (item.isEmpty()) {
                continue;
            }

            ItemStack normalized = item.normalizeBlockState();
            int remaining = normalized.getCount();
            for (int slot = 0; slot < this.size() && remaining > 0; slot++) {
                ItemStack current = this.getItem(slot);
                if (!current.isEmpty() && current.isStackableWith(normalized)) {
                    int removed = Math.min(remaining, current.getCount());
                    this.setItem(slot, current.decreaseCount(removed));
                    remaining -= removed;
                }
            }

            if (remaining > 0) {
                leftovers.add(normalized.withCount(remaining));
            }
        }

        return leftovers.toArray(ItemStack[]::new);
    }

    @Override
    public void remove(ItemStack item) {
        for (int i = 0; i < this.size(); ++i) {
            if (this.getItem(i).isStackableWith(item)) {
                this.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public void incrementCount(int slot) {
        ItemStack item = this.getItem(slot);
        if (!item.isEmpty() && item.getCount() > 0) {
            this.setItem(slot, item.increaseCount());
        }
    }

    @Override
    public void decrementCount(int slot) {
        ItemStack item = this.getItem(slot);
        if (item.getCount() > 0) {
            this.setItem(slot, item.decreaseCount());
        }
    }

    @Override
    public void clear() {
        boolean changed = false;
        for (int index = 0; index < this.size(); index++) {
            if (!this.getItem(index).isEmpty()) {
                this.setItem(index, ItemStack.EMPTY);
                changed = true;
            }
        }

        if (changed) {
            this.onContentsChange();
        }
    }

    public List<NbtMap> toNbt() {
        List<NbtMap> items = new ArrayList<>();

        for (int i = 0; i < this.size(); i++) {
            ItemStack item = this.getItem(i);
            if (!item.isEmpty()) {
                items.add(ItemStackNbtCodec.serializeItem(item, i));
            }
        }

        return items;
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

    @Override
    public void refresh() {
        for (ContainerListener listener : this.listeners) {
            listener.onInventoryContentsChange(this);
        }
    }

    public void close() {
        for (ContainerListener listener : this.listeners) {
            listener.onInventoryRemoved(this);
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

    private static class SlotWriteBatch {
        private final SequencedMap<WrittenSlot, ItemStack> originals = new LinkedHashMap<>();
        private final Set<Runnable> callbacks = new LinkedHashSet<>();

        private void record(CloudContainer container, int index, ItemStack old) {
            this.originals.putIfAbsent(new WrittenSlot(container, index), old);
        }

        private void restore() {
            for (Map.Entry<WrittenSlot, ItemStack> entry : this.originals.sequencedEntrySet().reversed()) {
                WrittenSlot slot = entry.getKey();
                slot.container().storage.set(slot.index(), entry.getValue());
            }
        }

        private void publish() {
            this.originals.forEach((slot, old) -> {
                if (!slot.container().getItem(slot.index()).equals(old)) {
                    slot.container().onSlotChange(slot.index(), old);
                }
            });
            this.callbacks.forEach(Runnable::run);
        }
    }

    private record WrittenSlot(CloudContainer container, int index) {
    }
}
