package org.cloudburstmc.api.inventory.view;

import lombok.Getter;
import org.cloudburstmc.api.util.Identifier;

import static java.util.Objects.requireNonNull;

/**
 * A type-safe key that identifies a specific kind of {@link SlotGroup}.
 *
 * <p>Each constant in {@link SlotGroupTypes} is an instance of this class, carrying a
 * namespaced {@link Identifier} and the corresponding slot-group interface class. Use these
 * tokens to look up a specific group within an open
 * {@link org.cloudburstmc.api.inventory.InventoryScreen}:</p>
 *
 * <pre>{@code
 * FurnaceView furnace = screen.getSlots(SlotGroupTypes.FURNACE).orElseThrow();
 * furnace.setSmelting(ItemStack.from(ItemTypes.RAW_IRON));
 * }</pre>
 *
 * <p><strong>Equality:</strong> {@code equals()} and {@code hashCode()} are based on the
 * {@link Identifier}, so instances with the same identifier compare equal and are safe to use
 * as {@link java.util.Map} keys or in {@link java.util.Set}s. For hot-path code, prefer
 * identity comparison ({@code ==}) against the well-known constants in {@link SlotGroupTypes},
 * since the constants are singletons and {@code ==} avoids an {@code equals} call entirely.</p>
 *
 * @param <T> the slot-group interface this type represents
 */
@Getter
public final class SlotGroupType<T extends SlotGroup> {

    private final Identifier identifier;
    private final Class<T> slotGroupClass;

    private SlotGroupType(Identifier identifier, Class<T> slotGroupClass) {
        this.identifier = identifier;
        this.slotGroupClass = slotGroupClass;
    }

    /**
     * Creates a new {@code SlotGroupType} token.
     *
     * <p>Every call to this method with the same {@code identifier} produces tokens that are
     * {@link #equals equal} to each other (since equality is based on the identifier). However,
     * for the built-in types prefer the pre-built constants in {@link SlotGroupTypes} and compare
     * with {@code ==} in hot paths — that avoids an {@code equals} call entirely.</p>
     *
     * @param identifier     the namespaced identifier for this type
     * @param slotGroupClass the slot-group interface class
     * @param <T>            the slot-group type
     * @return a new type token
     */
    public static <T extends SlotGroup> SlotGroupType<T> of(Identifier identifier, Class<T> slotGroupClass) {
        requireNonNull(identifier, "identifier");
        requireNonNull(slotGroupClass, "slotGroupClass");
        return new SlotGroupType<>(identifier, slotGroupClass);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SlotGroupType<?> other)) return false;
        return identifier.equals(other.identifier);
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }

    @Override
    public String toString() {
        return "SlotGroupType[" + identifier + "]";
    }
}
