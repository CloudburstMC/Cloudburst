package org.cloudburstmc.server.blockentity;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cloudburstmc.api.inventory.ItemContainer;
import org.cloudburstmc.api.item.ItemStack;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Shared check for the DTO-as-runtime-state migration: a migrated runtime class must not
 * keep a second copy of its DTO's state. A field duplicates DTO state either by declaring
 * an instance field whose name matches a field of its DTO hierarchy, or by declaring an
 * instance field of a state-carrying type ({@link ItemContainer} or any subtype,
 * {@link ItemStack}, {@link ItemStack} arrays, {@code int[]}/{@code short[]} arrays)
 * anywhere in the runtime class or its server superclasses up to {@link BaseBlockEntity}.
 * Anything the runtime still declares outside the allowed exceptions is transient or
 * hook-owned state, never a second copy of DTO state.
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
        assertKeepsNoDuplicateState(runtimeClass, dataClass, Map.of());
    }

    /**
     * Asserts the runtime class keeps no duplicate of its DTO's state, except for the
     * given fields.
     *
     * @param runtimeClass      the migrated block-entity class
     * @param dataClass         its DTO class
     * @param allowedExceptions fields the runtime may keep anyway, each mapped to the
     *                          reason it holds no DTO state
     */
    static void assertKeepsNoDuplicateState(Class<?> runtimeClass, Class<?> dataClass,
            Map<String, String> allowedExceptions) {
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

        List<Field> runtimeFields = new ArrayList<>();
        for (Class<?> current = runtimeClass;
             current != null && current != Object.class && current != BaseBlockEntity.class;
             current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                runtimeFields.add(field);
            }
        }

        Set<String> allowed = allowedExceptions.keySet();
        List<String> duplicates = new ArrayList<>();
        for (Field field : runtimeFields) {
            if (allowed.contains(field.getName())) {
                continue;
            }
            if (dtoFields.contains(field.getName()) || isDuplicateStateType(field.getType())) {
                duplicates.add(field.getDeclaringClass().getSimpleName() + "#" + field.getName()
                        + ":" + field.getType().getSimpleName());
            }
        }

        for (String allowedName : allowed) {
            boolean known = runtimeFields.stream().anyMatch(field -> field.getName().equals(allowedName));
            assertTrue(known, () -> runtimeClass.getSimpleName() + " allows unknown field "
                    + allowedName + "; the exception is vacuous");
        }

        assertTrue(duplicates.isEmpty(),
                () -> runtimeClass.getSimpleName() + " keeps duplicate state for " + duplicates
                        + "; state must live in " + dataClass.getSimpleName() + " only");
    }

    /**
     * Returns whether the type itself carries persisted item state, whatever its name:
     * an {@link ItemContainer} (or any subtype), an {@link ItemStack}, or an array
     * shaped like the pre-migration duplicates ({@code ItemStack[]}, {@code int[]},
     * {@code short[]}).
     *
     * @param type the field type to test
     * @return true when a field of this type duplicates DTO state
     */
    private static boolean isDuplicateStateType(Class<?> type) {
        if (type.isArray()) {
            Class<?> component = type.getComponentType();
            return component == ItemStack.class || component == int.class || component == short.class;
        }
        return ItemContainer.class.isAssignableFrom(type) || ItemStack.class.isAssignableFrom(type);
    }
}
