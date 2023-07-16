package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Chest;
import org.cloudburstmc.api.container.ContainerListener;
import org.cloudburstmc.api.container.ContainerViewTypes;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.container.CloudContainer;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ChestBlockEntity extends ContainerBlockEntity implements Chest {

    private CloudContainer combinedContainer = null;
    private Vector3i pairPosition;
    private boolean pairlead;
    private boolean findable;

    public ChestBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position, new CloudContainer(27), ContainerViewTypes.CHEST);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForList("Items", NbtType.COMPOUND, tags -> {
            for (NbtMap itemTag : tags) {
                ItemStack item = ItemUtils.deserializeItem(itemTag);
                this.setItem(itemTag.getByte("Slot"), item);
            }
        });
        if (tag.containsKey("pairx") && tag.containsKey("pairz")) {
            this.pairPosition = Vector3i.from(tag.getInt("pairx"), this.getPosition().getY(), tag.getInt("pairz"));
        }
        tag.listenForBoolean("pairlead", this::setPairlead);
        tag.listenForBoolean("Findable", this::setFindable);
    }

    @Override
    protected void saveClientData(NbtMapBuilder tag) {
        super.saveClientData(tag);
        if (this.pairPosition != null && this.pairlead) {
            tag.putInt("pairx", this.pairPosition.getX());
            tag.putInt("pairz", this.pairPosition.getZ());
            tag.putBoolean("pairlead", this.pairlead);
        }
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        List<NbtMap> items = new ArrayList<>();
        this.container.forEachSlot((itemStack, slot) -> items.add(ItemUtils.serializeItem(itemStack, slot)));
        tag.putList("Items", NbtType.COMPOUND, items);
        tag.putBoolean("Findable", this.findable);
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
                    ((CloudPlayer) listener).getInventoryManager().closeScreen();
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
            this.getLevel().dropItem(this.getPosition(), content);
        }
        container.clear(); // Stop items from being moved around by another player in the inventory
    }

    @Override
    public boolean isValid() {
        var blockId = this.getBlockState().getType();
        return blockId == BlockTypes.CHEST || blockId == BlockTypes.TRAPPED_CHEST;
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
                if ((pair.pairPosition.getX() + pair.pairPosition.getZ() << 15) >
                        (this.pairPosition.getX() + this.pairPosition.getZ() << 15)) { // Order them correctly
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
        if (this.isPaired()) {
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
