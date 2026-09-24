package org.cloudburstmc.api.level.particle;

import org.cloudburstmc.api.util.Identifier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Identifies a particle that can be spawned in a level.
 */
public class ParticleType {
    private final Identifier id;

    private ParticleType(Identifier id) {
        this.id = checkNotNull(id, "id");
    }

    /**
     * Creates a particle type for an identifier. This may be used for particles
     * supplied by a resource pack. Built-in types are available from
     * {@link ParticleTypes}.
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
    public String toString() {
        return "ParticleType{id=" + this.id + '}';
    }
}
