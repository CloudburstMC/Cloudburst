package org.cloudburstmc.server.registry;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;

import java.util.ArrayList;
import java.util.List;

@Log4j2
@UtilityClass
public class VanillaRegistryDiagnostics {
    private final List<String> WARNINGS = new ArrayList<>();

    public synchronized void duplicateBlock(Identifier id) {
        WARNINGS.add("Duplicate block registration: " + id);
    }

    public synchronized void duplicateVanillaBlock(Identifier id) {
        WARNINGS.add("Duplicate vanilla block registration: " + id);
    }

    public synchronized void duplicateVanillaItem(Identifier id) {
        WARNINGS.add("Duplicate vanilla item registration: " + id);
    }

    public synchronized void duplicateBlockItem(Identifier id) {
        WARNINGS.add("Duplicate block-backed item registration: " + id);
    }

    public synchronized void missingVanillaBlockState(String id, NbtMap states) {
        WARNINGS.add("Unimplemented vanilla block state: " + id + " " + states);
    }

    public synchronized void missingVanillaItemDefinition(Identifier id) {
        WARNINGS.add("Unimplemented vanilla item definition: " + id);
    }

    public synchronized void flush() {
        for (String warning : WARNINGS) {
            log.warn(warning);
        }
        WARNINGS.clear();
    }
}
