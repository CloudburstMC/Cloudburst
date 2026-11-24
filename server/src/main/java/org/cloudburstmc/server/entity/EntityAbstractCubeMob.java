package org.cloudburstmc.server.entity;

import org.cloudburstmc.api.entity.AbstractCubeMob;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.server.entity.component.MobNameTagInteraction;

import java.util.HashSet;
import java.util.Set;

public abstract class EntityAbstractCubeMob extends CloudMob implements AbstractCubeMob {

    private static final String SIZE_KEY = "Cloudburst.CubeSize";

    private final int defaultSize;
    private int size;

    protected EntityAbstractCubeMob(EntityType<? extends AbstractCubeMob> type, Location location, int defaultSize) {
        super(type, location);
        this.defaultSize = defaultSize;
        this.size = defaultSize;
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.applySize(this.size);
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        this.applySize(getInt(tag, SIZE_KEY, this.defaultSize));
        super.loadCustom(tag);
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        super.saveCustom(tag);
        if (this.size != this.defaultSize) {
            tag.putInt(SIZE_KEY, this.size);
        }
    }

    @Override
    protected Set<String> customKeys() {
        Set<String> keys = new HashSet<>(super.customKeys());
        keys.add(SIZE_KEY);
        return keys;
    }

    @Override
    public final int getSize() {
        return this.size;
    }

    @Override
    public final void setSize(int size) {
        boolean wasAlive = this.isAlive();
        this.applySize(size);
        if (wasAlive && this.isAlive()) {
            this.setHealth(this.getMaxHealth());
        }
        this.markChunkDirty();
    }

    @Override
    public float getWidth() {
        return this.getSize() * this.getUnitDimension();
    }

    @Override
    public float getHeight() {
        return this.getWidth();
    }

    @Override
    public float getLength() {
        return this.getWidth();
    }

    @Override
    protected float getNetworkScale() {
        return super.getNetworkScale() * this.getSize();
    }

    protected int getMaximumSize() {
        return 127;
    }

    protected int getBaseHealthForSize(int size) {
        return size * size;
    }

    protected float getUnitDimension() {
        return 0.51f;
    }

    protected void onSizeChanged() {
    }

    @Override
    protected boolean interactDefault(Player player, ItemStack item, Vector3f clickedPosition) {
        return super.interactDefault(player, item, clickedPosition) || MobNameTagInteraction.tryApply(this, player, item);
    }

    private void applySize(int size) {
        this.size = Math.clamp(size, 1, this.getMaximumSize());
        this.setDefaultMaxHealth(this.getBaseHealthForSize(this.size));
        this.onSizeChanged();
        this.refreshNetworkScale();
        this.recalculateBoundingBox();
    }
}
