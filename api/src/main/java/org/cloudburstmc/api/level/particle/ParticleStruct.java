package org.cloudburstmc.api.level.particle;

import java.util.Map;

/**
 * A structured emitter variable, such as a color with r, g, b, and a members.
 *
 * @param members the named values, copied on construction
 */
public record ParticleStruct(Map<String, ParticleVariable> members) implements ParticleVariable {

    public ParticleStruct {
        members = Map.copyOf(members);
        for (String name : members.keySet()) {
            if (!name.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                throw new IllegalArgumentException("Invalid particle variable member: " + name);
            }
        }
    }
}
