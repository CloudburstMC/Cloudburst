package org.cloudburstmc.codegen.particle;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.codegen.support.DataGenSupport;
import org.cloudburstmc.codegen.support.IdentifierCatalogGenerator;
import org.cloudburstmc.protocol.bedrock.data.ParticleType;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

@UtilityClass
public class ParticleDataGen {
    public static void generate() throws IOException {
        IdentifierCatalogGenerator.generate("org.cloudburstmc.api.level.particle", "ParticleType", "ParticleTypes",
                "Built-in particle effects. Use {@code Level.spawnParticleEffect} for named resource-pack emitters.",
                Arrays.stream(ParticleType.values()).filter(type -> type != ParticleType.UNDEFINED)
                        .map(type -> type.name().toLowerCase(Locale.ROOT)).toList());
        IdentifierCatalogGenerator.generate("org.cloudburstmc.api.level.particle", "ParticleEmitterType", "ParticleEmitterTypes",
                "Built-in named emitters. Custom emitters can be referenced with {@link ParticleEmitterType#of(Identifier)}.",
                DataGenSupport.loadIdentifiers("particle/particle_emitters.json", "emitters"));
    }
}
