package org.cloudburstmc.server.blockentity;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Shared check for the DTO-as-runtime-state migration: a migrated runtime class must not
 * declare an instance field whose name matches a field of its DTO hierarchy. Anything the
 * runtime still declares is transient or hook-owned state, never a second copy of DTO state.
 */
final class DtoStateChecks {

    private DtoStateChecks() {
    }

    /**
     * Asserts the runtime class keeps no duplicate of its DTO's state.
     *
     * @param runtimeClass the migrated block-entity class
     * @param dataClass    its DTO class
     */
    static void assertKeepsNoDuplicateState(Class<?> runtimeClass, Class<?> dataClass) {
        Set<String> dtoFields = new HashSet<>();
        for (Class<?> current = dataClass; current != null && current != Object.class;
             current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                dtoFields.add(field.getName());
            }
        }

        List<String> duplicates = new ArrayList<>();
        for (Field field : runtimeClass.getDeclaredFields()) {
            if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (dtoFields.contains(field.getName())) {
                duplicates.add(field.getName());
            }
        }

        assertTrue(duplicates.isEmpty(),
                () -> runtimeClass.getSimpleName() + " keeps duplicate state for " + duplicates
                        + "; state must live in " + dataClass.getSimpleName() + " only");
    }
}
