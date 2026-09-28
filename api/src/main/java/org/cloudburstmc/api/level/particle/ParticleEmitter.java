package org.cloudburstmc.api.level.particle;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.util.Identifier;

import java.util.Map;
import java.util.Objects;

/**
 * Describes a named resource-pack emitter and its initial Molang variables.
 * When attached to an entity, the spawn position is an offset from that entity.
 *
 * @param type       the emitter present in the recipients' resource packs
 * @param attachment the entity to follow, or null for a fixed position
 * @param variables  the initial variables keyed by {@code variable.<name>}, copied on construction
 */
public record ParticleEmitter(ParticleEmitterType type, @Nullable Entity attachment, Map<String, ParticleVariable> variables) {

    public ParticleEmitter {
        Objects.requireNonNull(type, "type");
        variables = Map.copyOf(variables);
        for (String name : variables.keySet()) {
            if (!name.matches("variable\\.[A-Za-z_][A-Za-z0-9_]*")) {
                throw new IllegalArgumentException("Invalid particle variable name: " + name);
            }
        }
    }

    /**
     * Creates an unattached emitter without variable overrides.
     *
     * @param id the emitter identifier
     */
    public ParticleEmitter(Identifier id) {
        this(ParticleEmitterType.of(id));
    }

    /**
     * Creates an unattached emitter without variable overrides.
     *
     * @param type the emitter type
     */
    public ParticleEmitter(ParticleEmitterType type) {
        this(type, null, Map.of());
    }
}
