package org.cloudburstmc.server.blockentity;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.BlockLayer;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Chest;
import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.component.ChestBlockHandlers;
import org.cloudburstmc.server.blockentity.data.CloudChestData;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.level.chunk.CloudChunk;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.Set;

public class ChestBlockEntity extends ContainerBlockEntity implements Chest {

    private static final String PAIR_X = "pairx";
    private static final String PAIR_Z = "pairz";
    private static final String PAIR_LEAD = "pairlead";
    private static final Set<String> PAIR_KEYS = Set.of(PAIR_X, PAIR_Z, PAIR_LEAD);

    private final CloudChestData persistenceState;
    private final ChestLid lid = new ChestLid(this);
    private @Nullable PairedChestStorage pairedInventory;

    public ChestBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        CloudChestData state = new CloudChestData();
        super(type, chunk, position, state);
        this.persistenceState = state;
        state.setItems(new ChestStorage(this));
    }

    @Override
    public BlockState getBlockState() {
        Vector3i pos = this.getPosition();
        return this.getChunk().getBlockState(pos.getX() & 15, pos.getY(), pos.getZ() & 15, BlockLayer.PRIMARY);
    }

    @Override
    public CloudContainer getBlockInventory() {
        return this.storage();
    }

    @Override
    public ItemContainer getInventory() {
        return this.getContainer();
    }

    @Override
    public CloudContainer getContainer() {
        ChestBlockEntity pair = this.getPair();
        if (pair == null) {
            this.discardInvalidPair();
            return this.getBlockInventory();
        }

        if (this.pairedInventory == null) {
            this.joinInventories(pair);
        }

        return this.pairedInventory;
    }

    @Override
    protected ItemContainer items() {
        return this.data().getItems();
    }

    private ChestStorage storage() {
        return (ChestStorage) this.items();
    }

    private void onInventoryChanged() {
        this.setDirty();
        this.updateComparator();
        ChestBlockEntity pair = this.getPair();
        if (pair != null) {
            pair.updateComparator();
        }
    }

    private void updateComparator() {
        if (this.getLevel() != null && !this.isClosed()) {
            this.getLevel().updateComparatorOutputLevel(this.getPosition());
        }
    }

    public boolean isPaired() {
        return this.data().getPairPosition() != null;
    }

    public boolean isPairLead() {
        return this.data().isPairLead();
    }

    @Override
    public @Nullable ChestBlockEntity getPair() {
        if (this.isClosed()) {
            return null;
        }

        ChestBlockEntity pair = this.findPairedEntity();
        if (pair == null || pair.isClosed() || this.isIncompatiblePartner(pair)) {
            return null;
        }

        return this.getPosition().equals(pair.data().getPairPosition()) ? pair : null;
    }

    public boolean pairWith(ChestBlockEntity other) {
        if (this.cannotFormPair(other)) {
            return false;
        }

        this.lid.closeViewers();
        other.lid.closeViewers();

        // Closing an inventory can fire events that change either chest.
        if (this.cannotFormPair(other)) {
            return false;
        }

        Direction facing = this.getBlockState().ensureTrait(BlockTraits.CARDINAL_DIRECTION).toDirection();
        boolean lead = facing.rotateCounterClockwise().relative(this.getPosition()).equals(other.getPosition());
        this.data().setPairPosition(other.getPosition());
        this.data().setPairLead(lead);
        other.data().setPairPosition(this.getPosition());
        other.data().setPairLead(!lead);
        this.joinInventories(other);
        this.pairChanged();
        other.pairChanged();
        return true;
    }

    public void unpair() {
        if (!this.isPaired()) {
            return;
        }

        ChestBlockEntity other = this.findPairedEntity();
        this.lid.closeViewers();
        this.clearPair();
        this.pairChanged();

        if (other != null && this.getPosition().equals(other.data().getPairPosition())) {
            other.lid.closeViewers();
            other.clearPair();
            other.pairChanged();
        }
    }

    private boolean cannotFormPair(ChestBlockEntity other) {
        return this.isClosed() || other.isClosed() || this.isPaired() || other.isPaired() || this.isIncompatiblePartner(other);
    }

    private boolean isIncompatiblePartner(ChestBlockEntity other) {
        if (this == other || this.getChunk().getLevel() != other.getChunk().getLevel()) {
            return true;
        }

        BlockState state = this.getBlockState();
        if (!ChestBlockHandlers.canJoin(state, other.getBlockState())) {
            return true;
        }

        Direction facing = state.ensureTrait(BlockTraits.CARDINAL_DIRECTION).toDirection();
        Vector3i pos = this.getPosition();
        return !facing.rotateClockwise().relative(pos).equals(other.getPosition()) && !facing.rotateCounterClockwise().relative(pos).equals(other.getPosition());
    }

    private @Nullable ChestBlockEntity findPairedEntity() {
        Vector3i position = this.data().getPairPosition();
        CloudChunk chunk = position == null ? null : this.getPairChunk(position);
        if (chunk != null && chunk.getBlockEntity(position.getX() & 15, position.getY(), position.getZ() & 15) instanceof ChestBlockEntity pair) {
            return pair;
        }
        return null;
    }

    private @Nullable CloudChunk getPairChunk(Vector3i position) {
        CloudChunk ownChunk = this.getChunk();
        if (ownChunk.getX() == (position.getX() >> 4) && ownChunk.getZ() == (position.getZ() >> 4)) {
            return ownChunk;
        }
        return this.getLevel() == null ? null : this.getLevel().getLoadedChunk(position);
    }

    private void discardInvalidPair() {
        Vector3i position = this.data().getPairPosition();
        if (position != null && this.getPairChunk(position) != null) {
            this.clearPair();
            this.pairChanged();
        }
    }

    private void joinInventories(ChestBlockEntity other) {
        ChestBlockEntity first = this.isPairLead() ? this : other;
        ChestBlockEntity second = first == this ? other : this;
        PairedChestStorage inventory = new PairedChestStorage(first.storage(), second.storage());
        this.pairedInventory = inventory;
        other.pairedInventory = inventory;
    }

    private void clearPair() {
        this.data().setPairPosition(null);
        this.data().setPairLead(false);
        this.pairedInventory = null;
    }

    private void pairChanged() {
        this.setDirty();
        this.queueClientUpdate();
        this.updateComparator();
    }

    @Override
    public boolean isBlocked() {
        ChestBlockEntity pair = this.getPair();
        return ChestBlockHandlers.isBlocked(this.getBlock()) || pair != null && ChestBlockHandlers.isBlocked(pair.getBlock());
    }

    @Override
    public boolean isLidOpen() {
        return this.lid.isOpen();
    }

    @Override
    public void setLidOpen(boolean open) {
        this.lid.setOpen(open);
    }

    public int getRedstoneSignal() {
        return this.lid.signal();
    }

    public boolean hasViewers() {
        return this.lid.hasViewers();
    }

    public void startOpen(CloudPlayer player) {
        this.lid.startOpen(player);
    }

    public void stopOpen(CloudPlayer player) {
        this.lid.stopOpen(player);
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }

    @Override
    public void applyPlacementItem(ItemStack item) {
        this.setCustomName(item.get(ItemDataComponents.CUSTOM_NAME));
    }

    @Override
    public void onBreak() {
        this.unpair();
        this.lid.closeViewers();
        CloudContainer inventory = this.getBlockInventory();
        for (ItemStack item : inventory.getContents()) {
            this.dropItemOnBreak(item);
        }
        inventory.clear();
    }

    @Override
    public void close() {
        if (!this.isClosed()) {
            ChestBlockEntity pair = this.getPair();
            this.lid.closeViewers();
            this.pairedInventory = null;
            if (pair != null) {
                pair.pairedInventory = null;
            }
            super.close();
        }
    }

    @Override
    protected CloudChestData data() {
        return this.persistenceState;
    }

    public boolean isFindable() {
        return this.data().isFindable();
    }

    public @Nullable String getLootTable() {
        return this.data().getLootTable();
    }

    public @Nullable Integer getLootTableSeed() {
        return this.data().getLootTableSeed();
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        if (tag.containsKey(PAIR_X) && tag.containsKey(PAIR_Z)) {
            Vector3i position = Vector3i.from(tag.getInt(PAIR_X), this.getPosition().getY(), tag.getInt(PAIR_Z));
            this.data().setPairPosition(position);
            this.data().setPairLead(tag.getBoolean(PAIR_LEAD));
        } else {
            this.clearPair();
        }
        this.pairedInventory = null;
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        this.writePair(tag, true);
    }

    @Override
    protected void saveCustomClient(NbtMapBuilder tag) {
        this.writePair(tag, false);
        String name = this.getCustomName();
        if (name != null) {
            tag.putString("CustomName", name);
        }
    }

    @Override
    protected Set<String> customKeys() {
        return PAIR_KEYS;
    }

    private void writePair(NbtMapBuilder tag, boolean includeNonLeadFlag) {
        Vector3i position = this.data().getPairPosition();
        if (position == null) {
            return;
        }

        tag.putInt(PAIR_X, position.getX());
        tag.putInt(PAIR_Z, position.getZ());
        if (includeNonLeadFlag || this.isPairLead()) {
            tag.putBoolean(PAIR_LEAD, this.isPairLead());
        }
    }

    private static class ChestStorage extends CloudContainer {

        private final ChestBlockEntity owner;

        private ChestStorage(ChestBlockEntity owner) {
            super(CloudChestData.SIZE);
            this.owner = owner;
        }

        @Override
        protected void onSlotChange(int slot, ItemStack previous) {
            this.owner.onInventoryChanged();
            super.onSlotChange(slot, previous);
            PairedChestStorage pair = this.owner.pairedInventory;
            if (pair != null) {
                pair.childSlotChanged(this, slot, previous);
            }
        }

        private void pairedSlotChanged(int slot, ItemStack previous) {
            this.owner.onInventoryChanged();
            super.onSlotChange(slot, previous);
        }
    }

    private static class PairedChestStorage extends CloudContainer {

        private final ChestStorage first;
        private final ChestStorage second;

        private PairedChestStorage(ChestStorage first, ChestStorage second) {
            super(first, second);
            this.first = first;
            this.second = second;
        }

        @Override
        public void setItem(int slot, @NonNull ItemStack item) {
            if (this.first.owner.pairedInventory != this || this.second.owner.pairedInventory != this) {
                throw new IllegalStateException("Double chest is no longer paired");
            }
            super.setItem(slot, item);
        }

        @Override
        protected void onSlotChange(int slot, ItemStack previous) {
            int split = this.first.size();
            if (slot < split) {
                this.first.pairedSlotChanged(slot, previous);
            } else {
                this.second.pairedSlotChanged(slot - split, previous);
            }
            super.onSlotChange(slot, previous);
        }

        private void childSlotChanged(ChestStorage child, int slot, ItemStack previous) {
            super.onSlotChange(child == this.first ? slot : this.first.size() + slot, previous);
        }
    }
}
