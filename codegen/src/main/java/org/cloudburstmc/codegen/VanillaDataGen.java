package org.cloudburstmc.codegen;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.codegen.biome.BiomeDataGen;
import org.cloudburstmc.codegen.item.ItemDataGen;
import org.cloudburstmc.codegen.particle.ParticleDataGen;
import org.cloudburstmc.codegen.sound.SoundDataGen;

import java.io.IOException;

@UtilityClass
public class VanillaDataGen {
    static void main() throws IOException {
        BiomeDataGen.generate();
        ItemDataGen.generate();
        SoundDataGen.generate();
        ParticleDataGen.generate();
    }
}
