package org.cloudburstmc.api.level.particle;

import org.cloudburstmc.api.util.Identifier;

import java.util.Objects;

/**
 * Identifies a resource-pack particle emitter. Custom identifiers need not be
 * registered with the server, but must exist in the recipients' resource packs.
 */
public class ParticleEmitterType {

    private final Identifier id;

    private ParticleEmitterType(Identifier id) {
        this.id = Objects.requireNonNull(id, "id");
        if (id.getNamespace().isEmpty() || id.getName().isEmpty()) {
            throw new IllegalArgumentException("Particle emitter identifier must not be empty");
        }
    }

    /**
     * Creates a reference to a named emitter.
     *
     * @param id the resource-pack emitter identifier
     * @return the emitter type
     */
    public static ParticleEmitterType of(Identifier id) {
        return new ParticleEmitterType(id);
    }

    /**
     * Returns the emitter identifier.
     *
     * @return the emitter identifier
     */
    public Identifier getId() {
        return this.id;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof ParticleEmitterType emitter && this.id.equals(emitter.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return this.id.toString();
    }
}
