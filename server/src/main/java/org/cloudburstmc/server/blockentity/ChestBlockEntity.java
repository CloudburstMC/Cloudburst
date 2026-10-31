package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Chest;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.blockentity.data.ChestData;
import org.cloudburstmc.api.inventory.ItemContainer;
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
    private boolean pairlead;

    public ChestBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public SlotGroupType<? extends SlotGroup> getSlotGroupType() {
        return SlotGroupTypes.CHEST;
    }

    @Override
    protected Class<? extends BlockEntityData> minimumDataClass() {
        return ChestData.class;
    }

    @Override
    protected ChestData data() {
        return (ChestData) super.data();
    }

    @Override
    protected ItemContainer items() {
        return data().getItems();
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        if (tag.containsKey("pairx") && tag.containsKey("pairz")) {
            data().setPairPosition(Vector3i.from(tag.getInt("pairx"), this.getPosition().getY(), tag.getInt("pairz")));
        }
        tag.listenForBoolean("pairlead", this::setPairlead);
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        Vector3i pairPosition = data().getPairPosition();
        if (pairPosition != null) {
            tag.putInt("pairx", pairPosition.getX());
            tag.putInt("pairz", pairPosition.getZ());
            tag.putBoolean("pairlead", this.pairlead);
        }
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
        Vector3i pairPosition = data().getPairPosition();
        if (pairPosition != null && this.pairlead) {
            tag.putInt("pairx", pairPosition.getX());
            tag.putInt("pairz", pairPosition.getZ());
            tag.putBoolean("pairlead", this.pairlead);
        }
    }

    public String getLootTable() {
        return data().getLootTable();
    }

    public void setLootTable(String lootTable) {
        data().setLootTable(lootTable);
    }

    public Integer getLootTableSeed() {
        return data().getLootTableSeed();
    }

    public void setLootTableSeed(Integer lootTableSeed) {
        data().setLootTableSeed(lootTableSeed);
    }

    public void setPairlead(boolean pairlead) {
        this.pairlead = pairlead;
    }

    public boolean isFindable() {
        return data().isFindable();
    }

    public void setFindable(boolean findable) {
        data().setFindable(findable);
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
        CloudContainer single = (CloudContainer) items();
        for (ItemStack content : single.getContents()) {
            this.dropItemOnBreak(content);
        }
        single.clear(); // Stop items from being moved around by another player in the inventory
    }

    @Override
    public CloudContainer getContainer() {
        if (this.combinedContainer == null && this.isPaired()) {
            this.checkPairing();
        }

        return this.combinedContainer != null ? this.combinedContainer : (CloudContainer) items();
    }

    protected void checkPairing() {
        ChestBlockEntity pair = this.getPair();

        if (pair != null) {
            if (!pair.isPaired()) {
                pair.createPair(this);
                pair.checkPairing();
            }

            createCombinedContainer(pair);
        } else {
            Vector3i pairPosition = data().getPairPosition();
            if (pairPosition != null && this.getLevel().isChunkLoaded(pairPosition)) {
                this.combinedContainer = null;
                data().setPairPosition(null);
                this.pairlead = false;
            }
        }
    }

    /**
     * Builds this chest's side of a pairing with the pair chest: adopts the pair's combined
     * container when it has one, otherwise creates the shared container over the two chests'
     * DTO containers with today's slot ordering.
     *
     * @param pair the paired chest
     */
    void createCombinedContainer(ChestBlockEntity pair) {
        if (pair.combinedContainer != null) {
            this.combinedContainer = pair.combinedContainer;
        } else if (this.combinedContainer == null) {
            Vector3i pairPosition = pair.data().getPairPosition();
            Vector3i thisPosition = this.data().getPairPosition();
            if (((pairPosition.getX() + pairPosition.getZ()) << 15) >
                    ((thisPosition.getX() + thisPosition.getZ()) << 15)) {
                this.combinedContainer = new CloudContainer((CloudContainer) pair.items(), (CloudContainer) items());
            } else {
                this.combinedContainer = new CloudContainer((CloudContainer) items(), (CloudContainer) pair.items());
            }
        }
    }

    public boolean isPaired() {
        return data().getPairPosition() != null;
    }

    public ChestBlockEntity getPair() {
        Vector3i pairPosition = data().getPairPosition();
        if (this.isPaired() && this.getLevel() != null) {
            BlockEntity blockEntity = this.getLevel().getLoadedBlockEntity(pairPosition);
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
        data().setPairPosition(chest.getPosition());
        chest.data().setPairPosition(this.getPosition());
    }

    public boolean unpair() {
        if (!this.isPaired()) {
            return false;
        }

        ChestBlockEntity chest = this.getPair();

        this.combinedContainer = null;
        data().setPairPosition(null);

        this.spawnToAll();

        if (chest != null) {
            chest.data().setPairPosition(null);
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
            Vector3i pairPosition = data().getPairPosition();
            if (pairPosition != null) {
                ChestBlockEntity other = getPair();
                if (other == null) {
                    return true;
                }
                if (!other.isPaired()) {
                    other.data().setPairPosition(this.getPosition());
                    checkPairing();
                    other.checkPairing();
                }
            }
        }
        return false;
    }
}
