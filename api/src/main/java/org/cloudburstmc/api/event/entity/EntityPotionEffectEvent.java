package org.cloudburstmc.api.event.entity;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.Living;
import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.potion.EffectType;
import org.cloudburstmc.api.potion.PotionEffect;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Called before an entity's active potion effects change.
 */
public class EntityPotionEffectEvent extends EntityEvent implements Cancellable {
    private final @Nullable PotionEffect oldEffect;
    private final @Nullable PotionEffect newEffect;
    private final @Nullable Entity source;
    private final PotionEffectCause cause;
    private final PotionEffectAction action;
    private boolean override;

    /**
     * Creates an entity potion effect event.
     *
     * @param entity    affected entity
     * @param oldEffect active effect being replaced or removed, or {@code null}
     * @param newEffect effect being added or considered as a replacement, or {@code null}
     * @param source    entity responsible for the change, or {@code null}
     * @param cause     reason for the change
     * @param action    change being performed
     * @param override  whether the new effect should replace the active effect
     */
    public EntityPotionEffectEvent(Living entity, @Nullable PotionEffect oldEffect,
                                   @Nullable PotionEffect newEffect, @Nullable Entity source,
                                   PotionEffectCause cause, PotionEffectAction action, boolean override) {
        this.entity = requireNonNull(entity, "entity");
        this.oldEffect = oldEffect;
        this.newEffect = newEffect;
        this.source = source;
        this.cause = requireNonNull(cause, "cause");
        this.action = requireNonNull(action, "action");
        this.override = override;

        checkArgument(switch (action) {
            case ADDED -> oldEffect == null && newEffect != null;
            case CHANGED -> oldEffect != null && newEffect != null;
            case REMOVED -> oldEffect != null && newEffect == null;
        }, "effects do not match action %s", action);

        checkArgument(oldEffect == null || newEffect == null || oldEffect.getType() == newEffect.getType(),
                "oldEffect and newEffect must have the same type");
    }

    @Override
    public Living getEntity() {
        return (Living) this.entity;
    }

    /**
     * Returns the active effect before the change.
     *
     * @return active effect, or {@code null} when adding an effect
     */
    public @Nullable PotionEffect getOldEffect() {
        return this.oldEffect;
    }

    /**
     * Returns the proposed effect after the change.
     *
     * @return proposed effect, or {@code null} when removing an effect
     */
    public @Nullable PotionEffect getNewEffect() {
        return this.newEffect;
    }

    /**
     * Returns the entity responsible for the change.
     *
     * @return responsible entity, or {@code null}
     */
    public @Nullable Entity getSource() {
        return this.source;
    }

    /**
     * Returns why the effect is changing.
     *
     * @return change cause
     */
    public PotionEffectCause getCause() {
        return this.cause;
    }

    /**
     * Returns the change being performed.
     *
     * @return change action
     */
    public PotionEffectAction getAction() {
        return this.action;
    }

    /**
     * Returns the effect type being changed.
     *
     * @return effect type
     */
    public EffectType getEffectType() {
        return this.oldEffect != null ? this.oldEffect.getType() : requireNonNull(this.newEffect).getType();
    }

    /**
     * Returns whether the proposed effect will replace the active effect.
     *
     * @return whether the proposed effect replaces the active effect
     */
    public boolean isOverride() {
        return this.override;
    }

    /**
     * Sets whether the proposed effect should replace the active effect.
     * This value is used only for {@link PotionEffectAction#CHANGED}.
     *
     * @param override whether to replace the active effect
     */
    public void setOverride(boolean override) {
        this.override = override;
    }
}
