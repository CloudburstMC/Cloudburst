package org.cloudburstmc.api.potion;

import org.cloudburstmc.api.entity.Entity;

public abstract class Potion {

    private final PotionType type;

    public Potion(PotionType type) {
        this(type, false);
    }

    public Potion(PotionType type, boolean splash) {
        type.setSplash(splash);
        this.type = type;
    }

    public PotionType getType() {
        return this.type;
    }

    public abstract Effect getEffect();

    public abstract void applyPotion(Entity entity);

    public int getDuration() {
        if (this.isSplash()) {
            return (int) (type.getDuration() * 0.75f);
        }
        return type.getDuration();
    }

    public int getLevel() {
        return type.getLevel();
    }

    public boolean isSplash() {
        return type.isSplash();
    }

    public Potion setSplash(boolean splash) {
        this.type.setSplash(splash);
        return this;
    }

    public boolean isInstant() {
        return this.type.isInstant();
    }

}
