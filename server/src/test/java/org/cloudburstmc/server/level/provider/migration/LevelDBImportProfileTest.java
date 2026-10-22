package org.cloudburstmc.server.level.provider.migration;

import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LevelDBImportProfileTest {

    @Test
    void generatorNamesDoNotSelectNumericMappings() {
        for (String generator : List.of("normal", "default", "cloudburst:standard")) {
            NbtMap metadata = NbtMap.builder().putString("generatorName", generator)
                    .putString("generatorOptions", "").build();

            assertThrows(IOException.class, () -> LevelDBImportProfile.detect(metadata, null));
        }
    }

    @Test
    void detectsBedrockIndependentlyOfLegacyGeneratorFields() throws IOException {
        NbtMap metadata = NbtMap.builder().putString("generatorName", "default")
                .putString("generatorOptions", "")
                .putList("lastOpenedWithVersion", NbtType.INT, List.of(1, 21, 100, 23, 0)).build();

        assertSame(LevelDBImportProfile.BEDROCK, LevelDBImportProfile.detect(metadata, null));
    }

    @Test
    void explicitNukkitMarkerSelectsItsOwnFinalizationRules() throws IOException {
        NbtMap metadata = NbtMap.builder().putInt("PM1EGen", 2)
                .putList("lastOpenedWithVersion", NbtType.INT, List.of(1, 21, 100, 23, 0)).build();

        assertSame(LevelDBImportProfile.NUKKIT, LevelDBImportProfile.detect(metadata, null));
        assertSame(LevelDBImportProfile.BEDROCK, LevelDBImportProfile.detect(metadata, LevelDBImportProfile.BEDROCK));
    }

    @Test
    void permitsAnExplicitProfileForAmbiguousSources() throws IOException {
        assertSame(LevelDBImportProfile.NUKKIT, LevelDBImportProfile.detect(NbtMap.EMPTY, LevelDBImportProfile.NUKKIT));
        assertThrows(IOException.class, () -> LevelDBImportProfile.detect(NbtMap.EMPTY, LevelDBImportProfile.CLOUDBURST));
    }

    @Test
    void requiresTheCurrentNativeStorageMarker() throws IOException {
        NbtMap metadata = NbtMap.builder().putInt("CloudburstStorageVersion", 1).putInt("PM1EGen", 2).build();

        assertSame(LevelDBImportProfile.CLOUDBURST, LevelDBImportProfile.detect(metadata, LevelDBImportProfile.NUKKIT));
        assertThrows(IOException.class, () -> LevelDBImportProfile.detect(
                metadata.toBuilder().putInt("CloudburstStorageVersion", 2).build(), null));
        assertThrows(IOException.class, () -> LevelDBImportProfile.detect(
                NbtMap.builder().putCompound("CloudburstClock", NbtMap.EMPTY).build(), null));
    }
}
