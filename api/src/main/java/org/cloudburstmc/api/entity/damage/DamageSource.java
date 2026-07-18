package org.cloudburstmc.api.entity.damage;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.level.Location;

import static java.util.Objects.requireNonNull;

/**
 * Identifies the type and origin of damage.
 */
public final class DamageSource {

    private final DamageType damageType;
    private final @Nullable Entity directEntity;
    private final @Nullable Entity causingEntity;
    private final @Nullable Block block;
    private final @Nullable Location location;

    private DamageSource(DamageType damageType, @Nullable Entity directEntity, @Nullable Entity causingEntity,
                         @Nullable Block block, @Nullable Location location) {
        this.damageType = requireNonNull(damageType, "damageType");
        this.directEntity = directEntity;
        this.causingEntity = causingEntity;
        this.block = block;
        this.location = location;
    }

    /**
     * Creates a source with no specific origin.
     *
     * @param damageType the damage type
     * @return the damage source
     */
    public static DamageSource of(DamageType damageType) {
        return builder(damageType).build();
    }

    /**
     * Creates a builder for a damage source of the supplied type.
     *
     * @param damageType the damage type
     * @return a new damage source builder
     */
    public static Builder builder(DamageType damageType) {
        return new Builder(damageType);
    }

    /**
     * Returns the kind of damage represented by this source.
     *
     * @return the damage type
     */
    public DamageType getDamageType() {
        return this.damageType;
    }

    /**
     * Returns the entity that directly delivered the damage.
     *
     * <p>For projectile damage, this is the projectile rather than its owner.</p>
     *
     * @return the direct entity, or {@code null} when no entity delivered the damage
     */
    public @Nullable Entity getDirectEntity() {
        return this.directEntity;
    }

    /**
     * Returns the entity to which the damage is attributed.
     *
     * <p>For projectile damage, this is the projectile owner.</p>
     *
     * @return the causing entity, or {@code null} when the damage has no entity cause
     */
    public @Nullable Entity getCausingEntity() {
        return this.causingEntity;
    }

    /**
     * Returns the block that caused the damage.
     *
     * @return the causing block, or {@code null} when the damage has no block cause
     */
    public @Nullable Block getBlock() {
        return this.block;
    }

    /**
     * Returns the explicit location from which the damage originated.
     *
     * @return the damage location, or {@code null} when none was supplied
     */
    public @Nullable Location getLocation() {
        return this.location;
    }

    /**
     * Returns whether the damage was delivered through another entity.
     *
     * @return whether the direct and causing entities differ
     */
    public boolean isIndirect() {
        return this.directEntity != this.causingEntity;
    }

    /**
     * Returns the explicit damage location, falling back to the causing entity or block.
     *
     * @return the source location, or {@code null} when it is unknown
     */
    public @Nullable Location getSourceLocation() {
        if (this.location != null) {
            return this.location;
        }

        if (this.causingEntity != null) {
            return this.causingEntity.getLocation();
        }

        if (this.block != null) {
            return Location.from(this.block.getPosition(), this.block.getLevel());
        }

        return null;
    }

    /**
     * Builds an immutable damage source.
     */
    public static final class Builder {

        private final DamageType damageType;
        private @Nullable Entity directEntity;
        private @Nullable Entity causingEntity;
        private @Nullable Block block;
        private @Nullable Location location;

        private Builder(DamageType damageType) {
            this.damageType = requireNonNull(damageType, "damageType");
        }

        /**
         * Sets the entity that directly delivers the damage.
         *
         * @param directEntity the direct entity
         * @return this builder
         */
        public Builder directEntity(Entity directEntity) {
            this.directEntity = requireNonNull(directEntity, "directEntity");
            return this;
        }

        /**
         * Sets the entity to which the damage is attributed.
         *
         * @param causingEntity the causing entity
         * @return this builder
         */
        public Builder causingEntity(Entity causingEntity) {
            this.causingEntity = requireNonNull(causingEntity, "causingEntity");
            return this;
        }

        /**
         * Sets the block that caused the damage.
         *
         * @param block the causing block
         * @return this builder
         */
        public Builder block(Block block) {
            this.block = requireNonNull(block, "block");
            return this;
        }

        /**
         * Sets the location from which the damage originated.
         *
         * @param location the damage location
         * @return this builder
         */
        public Builder location(Location location) {
            this.location = requireNonNull(location, "location");
            return this;
        }

        /**
         * Creates the damage source.
         *
         * @return the damage source
         */
        public DamageSource build() {
            return new DamageSource(this.damageType, this.directEntity, this.causingEntity, this.block, this.location);
        }
    }
}
