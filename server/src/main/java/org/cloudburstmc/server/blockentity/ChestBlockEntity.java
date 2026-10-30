package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Chest;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.blockentity.data.ChestData;
import org.cloudburstmc.api.blockentity.data.ContainerData;
import org.cloudburstmc.api.blockentity.data.SlottedItem;
import org.cloudburstmc.server.container.ContainerListener;
import org.cloudburstmc.api.inventory.view.BlockStorageView;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.inventory.view.SlotGroupTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.HashSet;
import java.util.Set;

/**
 * Block entity implementation for a chest: a 27-slot storage container. Two adjacent chests can be
 * paired to form a double chest backed by a shared combined container.
 */
public class ChestBlockEntity extends ContainerBlockEntity implements Chest, BlockStorageView {

    private CloudContainer combinedContainer = null;
    private Vector3i pairPosition;
    private boolean pairlead;
    private boolean findable;
    private String lootTable;
    private Integer lootTableSeed;

    public ChestBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position, new CloudContainer(27));
    }

    @Override
    public SlotGroupType<? extends SlotGroup> getSlotGroupType() {
        return SlotGroupTypes.CHEST;
    }

    @Override
    protected void applyData(BlockEntityData data) {
        super.applyData(data);
        ChestData chest = (ChestData) data;
        for (SlottedItem entry : chest.getItems()) {
            this.setItem(entry.slot(), entry.item());
        }
        this.findable = chest.isFindable();
        this.lootTable = chest.getLootTable();
        this.lootTableSeed = chest.getLootTableSeed();
    }

    @Override
    protected void fillData(BlockEntityData data) {
        super.fillData(data);
        ChestData chest = (ChestData) data;
        chest.setItems(ContainerData.slotted(this.container.getContents()));
        chest.setFindable(this.findable);
        chest.setLootTable(this.lootTable);
        chest.setLootTableSeed(this.lootTableSeed);
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        if (tag.containsKey("pairx") && tag.containsKey("pairz")) {
            this.pairPosition = Vector3i.from(tag.getInt("pairx"), this.getPosition().getY(), tag.getInt("pairz"));
        }
        tag.listenForBoolean("pairlead", this::setPairlead);
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        writePair(tag);
    }

    @Override
    protected void saveCustomClient(NbtMapBuilder tag) {
        writePair(tag);
    }

    @Override
    protected Set<String> customKeys() {
        return Set.of("pairx", "pairz", "pairlead");
    }

    private void writePair(NbtMapBuilder tag) {
        if (this.pairPosition != null && this.pairlead) {
            tag.putInt("pairx", this.pairPosition.getX());
            tag.putInt("pairz", this.pairPosition.getZ());
            tag.putBoolean("pairlead", this.pairlead);
        }
    }

    public String getLootTable() {
        return lootTable;
    }

    public void setLootTable(String lootTable) {
        this.lootTable = lootTable;
    }

    public Integer getLootTableSeed() {
        return lootTableSeed;
    }

    public void setLootTableSeed(Integer lootTableSeed) {
        this.lootTableSeed = lootTableSeed;
    }

    public void setPairlead(boolean pairlead) {
        this.pairlead = pairlead;
    }

    public boolean isFindable() {
        return findable;
    }

    public void setFindable(boolean findable) {
        this.findable = findable;
    }

    @Override
    public void close() {
        if (!closed) {
            for (ContainerListener listener : new HashSet<>(this.getContainer().getListeners())) {
                if (listener instanceof CloudPlayer) {
                    ((CloudPlayer) listener).closeInventory();
                }
            }

//            for (InventoryListener listener : new HashSet<>(this.getInventory().getListeners())) {
//                listener.removeWindow(this.getRealInventory());
//            }
            super.close();
        }
    }

    @Override
    public void onBreak() {
        if (this.isPaired()) {
            unpair();
        }
        for (ItemStack content : container.getContents()) {
            this.dropItemOnBreak(content);
        }
        container.clear(); // Stop items from being moved around by another player in the inventory
    }

    @Override
    public CloudContainer getContainer() {
        if (this.combinedContainer == null && this.isPaired()) {
            this.checkPairing();
        }

        return this.combinedContainer != null ? this.combinedContainer : this.container;
    }

    protected void checkPairing() {
        ChestBlockEntity pair = this.getPair();

        if (pair != null) {
            if (!pair.isPaired()) {
                pair.createPair(this);
                pair.checkPairing();
            }

            if (pair.combinedContainer != null) {
                this.combinedContainer = pair.combinedContainer;
            } else if (this.combinedContainer == null) {
                if (((pair.pairPosition.getX() + pair.pairPosition.getZ()) << 15) >
                        ((this.pairPosition.getX() + this.pairPosition.getZ()) << 15)) {
                    this.combinedContainer = new CloudContainer(pair.container, this.container);
                } else {
                    this.combinedContainer = new CloudContainer(this.container, pair.container);
                }
            }
        } else {
            if (this.pairPosition != null && this.getLevel().isChunkLoaded(this.pairPosition)) {
                this.combinedContainer = null;
                this.pairPosition = null;
                this.pairlead = false;
            }
        }
    }

    public boolean isPaired() {
        return this.pairPosition != null;
    }

    public ChestBlockEntity getPair() {
        if (this.isPaired() && this.getLevel() != null) {
            BlockEntity blockEntity = this.getLevel().getLoadedBlockEntity(this.pairPosition);
            if (blockEntity instanceof ChestBlockEntity) {
                return (ChestBlockEntity) blockEntity;
            }
        }

        return null;
    }

    public boolean pairWith(Chest chest) {
        if (this.isPaired() || chest.isPaired() || this.getBlockState().getType() != chest.getBlockState().getType()) {
            return false;
        }

        this.createPair((ChestBlockEntity) chest);
        this.pairlead = true;

        chest.spawnToAll();
        this.spawnToAll();
        this.checkPairing();

        return true;
    }

    private void createPair(ChestBlockEntity chest) {
        this.pairPosition = chest.getPosition();
        chest.pairPosition = this.getPosition();
    }

    public boolean unpair() {
        if (!this.isPaired()) {
            return false;
        }

        ChestBlockEntity chest = this.getPair();

        this.combinedContainer = null;
        this.pairPosition = null;

        this.spawnToAll();

        if (chest != null) {
            chest.pairPosition = null;
            chest.combinedContainer = null;
            chest.pairlead = false;
            chest.checkPairing();
            chest.spawnToAll();
        }
        this.checkPairing();

        return true;
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }

    @Override
    public boolean onUpdate() {
        super.onUpdate();
        if (this.pairlead) {
            if (this.pairPosition != null) {
                ChestBlockEntity other = getPair();
                if (other == null) {
                    return true;
                }
                if (!other.isPaired()) {
                    other.pairPosition = this.getPosition();
                    checkPairing();
                    other.checkPairing();
                }
            }
        }
        return false;
    }
}
