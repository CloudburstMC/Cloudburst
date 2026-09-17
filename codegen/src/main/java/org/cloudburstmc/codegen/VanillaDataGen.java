package org.cloudburstmc.codegen;

import lombok.experimental.UtilityClass;

import java.io.IOException;

@UtilityClass
public class VanillaDataGen {
    static void main() throws IOException {
        BiomeDataGen.generate();
        ItemDataGen.generate();
    }
}
