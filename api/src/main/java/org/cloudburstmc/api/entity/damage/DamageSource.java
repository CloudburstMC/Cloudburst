package org.cloudburstmc.api.entity.damage;

import lombok.EqualsAndHashCode;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.entity.Living;
import org.cloudburstmc.api.level.Location;
import org.cloudburstmc.api.player.Player;

import static java.util.Objects.requireNonNull;

/**
 * Identifies the type and origin of damage.
 */
@EqualsAndHashCode
public final class DamageSource {

    private final DamageType damageType;
    private final @Nullable Entity directEntity;
    private final @Nullable Entity causingEntity;
    private final @Nullable Block block;
    private final @Nullable Location damageLocation;

    private DamageSource(DamageType damageType, @Nullable Entity directEntity, @Nullable Entity causingEntity, @Nullable Block block, @Nullable Location damageLocation) {
        this.damageType = requireNonNull(damageType, "damageType");
        this.directEntity = directEntity;
        this.causingEntity = causingEntity;
        this.block = block;
        this.damageLocation = damageLocation;
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
     * Creates a source delivered by and attributed to the same entity.
     *
     * @param damageType the damage type
     * @param entity     the entity responsible for the damage
     * @return the damage source
     */
    public static DamageSource of(DamageType damageType, Entity entity) {
        Entity source = requireNonNull(entity, "entity");
        return builder(damageType)
                .directEntity(source)
                .causingEntity(source)
                .build();
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
     * Returns the explicitly supplied location from which the damage originated.
     *
     * @return the damage location, or {@code null} when none was supplied
     */
    public @Nullable Location getDamageLocation() {
        return this.damageLocation;
    }

    /**
     * Returns the explicit damage location, falling back to the direct entity or block.
     *
     * @return the source location, or {@code null} when it is unknown
     */
    public @Nullable Location getSourceLocation() {
        if (this.damageLocation != null) {
            return this.damageLocation;
        }

        if (this.directEntity != null) {
            return this.directEntity.getLocation();
        }

        if (this.block != null) {
            return Location.from(this.block.getPosition(), this.block.getLevel());
        }

        return null;
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
     * Returns the hunger exhaustion caused by this source.
     *
     * @return the exhaustion amount
     */
    public float getFoodExhaustion() {
        return this.damageType.getExhaustion();
    }

    /**
     * Returns whether this source scales with level difficulty.
     *
     * @return whether difficulty scaling applies
     */
    public boolean scalesWithDifficulty() {
        return switch (this.damageType.getDamageScaling()) {
            case NEVER -> false;
            case ALWAYS -> true;
            case WHEN_CAUSED_BY_LIVING_NON_PLAYER -> this.causingEntity instanceof Living && !(this.causingEntity instanceof Player);
        };
    }

    /**
     * Builds an immutable damage source.
     */
    public static final class Builder {

        private final DamageType damageType;
        private @Nullable Entity directEntity;
        private @Nullable Entity causingEntity;
        private @Nullable Block block;
        private @Nullable Location damageLocation;

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
         * @param damageLocation the damage location
         * @return this builder
         */
        public Builder damageLocation(Location damageLocation) {
            this.damageLocation = requireNonNull(damageLocation, "damageLocation");
            return this;
        }

        /**
         * Creates the damage source.
         *
         * @return the damage source
         */
        public DamageSource build() {
            return new DamageSource(this.damageType, this.directEntity, this.causingEntity, this.block, this.damageLocation);
        }
    }
}
