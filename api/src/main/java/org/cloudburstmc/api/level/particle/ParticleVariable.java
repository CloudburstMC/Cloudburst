package org.cloudburstmc.api.level.particle;

/**
 * A numeric value or a structured group of values supplied to an emitter's Molang variables.
 */
public sealed interface ParticleVariable permits ParticleNumber, ParticleStruct {
}
