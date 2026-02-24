package org.cloudburstmc.api.inventory;

import lombok.Getter;
import org.cloudburstmc.api.util.Identifier;

import java.util.Objects;

/**
 * A type-safe key that identifies a specific kind of {@link InventoryScreen}.
 *
 * <p>Constants are defined in {@link ScreenTypes}. Use these to check what type of
 * screen a player has open, or to create typed references:</p>
 *
 * <pre>{@code
 * if (screen.getType() == ScreenTypes.FURNACE) {
 *     FurnaceScreen furnace = (FurnaceScreen) screen;
 *     furnace.getFurnace().setSmelting(stack);
 * }
 * }</pre>
 *
 * <p><strong>Equality:</strong> {@code equals()} and {@code hashCode()} are based on the
 * {@link Identifier}, so instances with the same identifier compare equal and are safe to use
 * as {@link java.util.Map} keys or in {@link java.util.Set}s. For hot-path code, prefer
 * identity comparison ({@code ==}) against the well-known constants in {@link ScreenTypes},
 * since the constants are singletons and {@code ==} avoids an {@code equals} call entirely.</p>
 *
 * @param <T> the screen interface this type represents
 */
@Getter
public final class ScreenType<T extends InventoryScreen> {

    private final Identifier identifier;
    private final Class<T> screenClass;

    private ScreenType(Identifier identifier, Class<T> screenClass) {
        this.identifier = identifier;
        this.screenClass = screenClass;
    }

    /**
     * Creates a new {@code ScreenType} token.
     *
     * @param identifier  the namespaced identifier for this type
     * @param screenClass the screen interface class
     * @param <T>         the screen type
     * @return a new type token
     */
    public static <T extends InventoryScreen> ScreenType<T> of(Identifier identifier, Class<T> screenClass) {
        Objects.requireNonNull(identifier, "identifier");
        Objects.requireNonNull(screenClass, "screenClass");
        return new ScreenType<>(identifier, screenClass);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScreenType<?> other)) return false;
        return identifier.equals(other.identifier);
    }

    @Override
    public int hashCode() {
        return identifier.hashCode();
    }

    @Override
    public String toString() {
        return "ScreenType[" + identifier + "]";
    }
}
