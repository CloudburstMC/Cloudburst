package org.cloudburstmc.api.level.particle;

import org.cloudburstmc.api.util.Identifier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Identifies a built-in particle effect. Named resource-pack emitters are
 * spawned separately with {@link org.cloudburstmc.api.level.Level#spawnParticleEffect(Identifier, org.cloudburstmc.math.vector.Vector3f)}.
 */
public class ParticleType implements ParticleOptions {

    private final Identifier id;

    private ParticleType(Identifier id) {
        this.id = checkNotNull(id, "id");
    }

    /**
     * Creates a reference to a built-in particle identifier. The identifier
     * must be supported when the particle is spawned. Known types are available
     * from {@link ParticleTypes}.
     *
     * @param id particle identifier
     * @return particle type
     */
    public static ParticleType of(Identifier id) {
        return new ParticleType(id);
    }

    /**
     * Returns the particle identifier.
     *
     * @return particle identifier
     */
    public Identifier getId() {
        return this.id;
    }

    @Override
    public ParticleType getType() {
        return this;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof ParticleType particle && this.id.equals(particle.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return "ParticleType{id=" + this.id + '}';
    }
}
