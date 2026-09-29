package org.cloudburstmc.codegen.sound;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.codegen.support.DataGenSupport;
import org.cloudburstmc.codegen.support.IdentifierCatalogGenerator;

import java.io.IOException;

@UtilityClass
public class SoundDataGen {
    public static void generate() throws IOException {
        IdentifierCatalogGenerator.generate("org.cloudburstmc.api.level.sound", "SoundType", "SoundTypes",
                "Built-in named sounds. Custom sounds can be referenced with {@link SoundType#of(Identifier)}.",
                DataGenSupport.loadIdentifiers("sound/sound_types.json", "sounds"));
    }
}
