package org.cloudburstmc.server.registry;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;

import java.util.*;

@Log4j2
@UtilityClass
public class VanillaRegistryDiagnostics {
    private final List<String> WARNINGS = new ArrayList<>();
    private final Map<String, MissingBlockStates> MISSING_BLOCK_STATES = new LinkedHashMap<>();

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
        MISSING_BLOCK_STATES.computeIfAbsent(id, ignored -> new MissingBlockStates()).add(states);
    }

    public synchronized void missingVanillaItemDefinition(Identifier id) {
        WARNINGS.add("Unimplemented vanilla item definition: " + id);
    }

    public synchronized void flush() {
        for (String warning : WARNINGS) {
            log.warn(warning);
        }

        MISSING_BLOCK_STATES.forEach((id, states) -> log.warn(
                "Unimplemented vanilla block states for {}: {} state(s), traits={}",
                id,
                states.count,
                states.traitValues
        ));

        WARNINGS.clear();
        MISSING_BLOCK_STATES.clear();
    }

    private static class MissingBlockStates {
        private final Map<String, Set<Object>> traitValues = new LinkedHashMap<>();
        private int count;

        private void add(NbtMap states) {
            this.count++;
            states.forEach((trait, value) -> this.traitValues
                    .computeIfAbsent(trait, ignored -> new LinkedHashSet<>())
                    .add(value));
        }
    }
}
