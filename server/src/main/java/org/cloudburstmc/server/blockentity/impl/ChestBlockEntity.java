package org.cloudburstmc.server.blockentity.impl;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.blockentity.BlockEntity;
import org.cloudburstmc.server.blockentity.BlockEntityType;
import org.cloudburstmc.server.blockentity.Chest;
import org.cloudburstmc.server.inventory.ChestInventory;
import org.cloudburstmc.server.inventory.ContainerInventory;
import org.cloudburstmc.server.inventory.DoubleChestInventory;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.level.chunk.Chunk;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.utils.Identifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class ChestBlockEntity extends BaseBlockEntity implements Chest {

    private final ChestInventory inventory = new ChestInventory(this);

    private DoubleChestInventory doubleInventory = null;
    private Vector3i pairPosition;
    private boolean pairlead;
    private boolean findable;

    public ChestBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForList("Items", NbtType.COMPOUND, tags -> {
            for (NbtMap itemTag : tags) {
                ItemStack item = ItemUtils.deserializeItem(itemTag);
                this.inventory.setItem(itemTag.getByte("Slot"), item);
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
        for (Map.Entry<Integer, ItemStack> entry : this.inventory.getContents().entrySet()) {
            items.add(ItemUtils.serializeItem(entry.getValue(), entry.getKey()));
        }
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
            unpair();

            for (Player player : new HashSet<>(this.getInventory().getViewers())) {
                player.removeWindow(this.getInventory());
            }

            for (Player player : new HashSet<>(this.getInventory().getViewers())) {
                player.removeWindow(this.getRealInventory());
            }
            super.close();
        }
    }

    @Override
    public void onBreak() {
        for (ItemStack content : inventory.getContents().values()) {
            this.getLevel().dropItem(this.getPosition(), content);
        }
        inventory.clearAll(); // Stop items from being moved around by another player in the inventory
    }

    @Override
    public boolean isValid() {
        Identifier blockId = this.getBlockState().getType();
        return blockId == BlockIds.CHEST || blockId == BlockIds.TRAPPED_CHEST;
    }


    @Override
    public ContainerInventory getInventory() {
        if (this.doubleInventory == null && this.isPaired()) {
            this.checkPairing();
        }

        return this.doubleInventory != null ? this.doubleInventory : this.inventory;
    }

    public ChestInventory getRealInventory() {
        return inventory;
    }

    protected void checkPairing() {
        ChestBlockEntity pair = this.getPair();

        if (pair != null) {
            if (!pair.isPaired()) {
                pair.createPair(this);
                pair.checkPairing();
            }

            if (pair.doubleInventory != null) {
                this.doubleInventory = pair.doubleInventory;
            } else if (this.doubleInventory == null) {
                if ((pair.pairPosition.getX() + pair.pairPosition.getZ() << 15) >
                        (this.pairPosition.getX() + this.pairPosition.getZ() << 15)) { //Order them correctly
                    this.doubleInventory = new DoubleChestInventory(pair, this);
                } else {
                    this.doubleInventory = new DoubleChestInventory(this, pair);
                }
            }
        } else {
            if (this.pairPosition != null && this.getLevel().isChunkLoaded(this.pairPosition)) {
                this.doubleInventory = null;
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

        this.doubleInventory = null;
        this.pairPosition = null;

        this.spawnToAll();

        if (chest != null) {
            chest.pairPosition = null;
            chest.doubleInventory = null;
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
