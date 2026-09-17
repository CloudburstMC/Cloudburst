package org.cloudburstmc.api.item;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;

/**
 * Read-only access to item stack data components.
 */
public interface ItemDataComponentView {

    /**
     * Returns a component value.
     *
     * @param type component type
     * @param <T>  component value type
     * @return component value, or {@code null} when absent
     */
    <T> @Nullable T get(ItemDataComponentType<T> type);

    /**
     * Returns a component value or a caller-supplied fallback.
     *
     * @param type     component type
     * @param fallback value returned when the component is absent
     * @param <T>      component value type
     * @return component value or fallback
     */
    <T> T getOrDefault(ItemDataComponentType<T> type, T fallback);

    /**
     * Returns whether the component is explicitly present.
     *
     * @param type component type
     * @return {@code true} if present
     */
    boolean has(ItemDataComponentType<?> type);

    /**
     * Returns the explicitly stored components.
     *
     * @return immutable component map
     */
    Map<ItemDataComponentType<?>, ?> getDataComponents();
}
