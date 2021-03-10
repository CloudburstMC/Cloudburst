package org.cloudburstmc.api.potion;

import com.nukkitx.math.vector.Vector3i;
import lombok.Getter;
import lombok.NonNull;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.util.Identifier;

@Getter
public abstract class Effect {
    private EffectType type;
    private int duration;
    private int amplifier;
    private boolean visible = true;
    private boolean ambient;

    public Effect(@NonNull EffectType type) {
        this.type = type;
    }

    public Effect setDuration(int ticks) {
        this.duration = ticks;
        return this;
    }

    public Effect setAmplifier(int amplifier) {
        this.amplifier = amplifier;
        return this;
    }

    public Effect setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public Effect setAmbient(boolean ambient) {
        this.ambient = ambient;
        return this;
    }

    public boolean isBad() {
        return type.isBad();
    }

    public int[] getColor() {
        Vector3i color = type.getColor();
        return new int[]{color.getX(), color.getY(), color.getZ()};
    }

    public Identifier getId() {
        return type.getId();
    }

    public abstract boolean canTick();

    public abstract void applyEffect(Entity entity);

    public abstract void add(Entity entity);

    public abstract void remove(Entity entity);
}
