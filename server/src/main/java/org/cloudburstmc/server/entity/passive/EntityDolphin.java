package org.cloudburstmc.server.entity.passive;

import lombok.Getter;
import org.cloudburstmc.api.entity.EntityType;
import org.cloudburstmc.api.entity.damage.DamageSource;
import org.cloudburstmc.api.entity.damage.DamageTypes;
import org.cloudburstmc.api.entity.passive.Dolphin;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

import java.util.HashSet;
import java.util.Set;

public class EntityDolphin extends Animal implements Dolphin {

    private static final int MAX_NATURAL_MOISTNESS = 2400;
    private static final String MOISTNESS_KEY = "Moistness";

    @Getter(onMethod_ = @Override)
    private int moistness = MAX_NATURAL_MOISTNESS;

    public EntityDolphin(EntityType<Dolphin> type, Location location) {
        super(type, location);
    }

    @Override
    protected String getDefaultName() {
        return "Dolphin";
    }

    @Override
    public float getWidth() {
        return 0.9f;
    }

    @Override
    public float getHeight() {
        return 0.6f;
    }

    @Override
    protected void initEntity() {
        super.initEntity();
        this.setDefaultMaxHealth(10);
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        super.loadCustom(tag);
        this.moistness = Math.max(0, getInt(tag, MOISTNESS_KEY, MAX_NATURAL_MOISTNESS));
    }

    @Override
    protected void saveCustom(NbtMapBuilder tag) {
        super.saveCustom(tag);
        if (this.moistness != MAX_NATURAL_MOISTNESS) {
            tag.putInt(MOISTNESS_KEY, this.moistness);
        }
    }

    @Override
    protected Set<String> customKeys() {
        Set<String> keys = new HashSet<>(super.customKeys());
        keys.add(MOISTNESS_KEY);
        return Set.copyOf(keys);
    }

    @Override
    public void setMoistness(int moistness) {
        if (moistness < 0) {
            throw new IllegalArgumentException("Moistness must be non-negative");
        }

        if (this.moistness != moistness) {
            this.moistness = moistness;
            this.markChunkDirty();
        }
    }

    @Override
    public boolean entityBaseTick(int elapsedTicks) {
        boolean needsNextTick = super.entityBaseTick(elapsedTicks);
        if (!this.isAlive() || elapsedTicks <= 0) {
            return needsNextTick;
        }

        if (this.isInWaterOrRain()) {
            this.setMoistness(MAX_NATURAL_MOISTNESS);
        } else {
            this.setMoistness(Math.max(0, this.moistness - elapsedTicks));
            if (this.moistness == 0) {
                this.damage(1, DamageSource.of(DamageTypes.DRY_OUT));
            }
        }

        return needsNextTick;
    }

    @Override
    public ItemStack[] getDrops() {
        return new ItemStack[]{ItemStack.builder().itemType(ItemTypes.COD).build()};
    }
}
