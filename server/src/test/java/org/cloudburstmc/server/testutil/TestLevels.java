package org.cloudburstmc.server.testutil;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.level.Level;
import org.cloudburstmc.server.level.DimensionHeight;

import java.lang.reflect.InvocationHandler;
import java.util.HashMap;
import java.util.Map;

/**
 * Level stubs with real vertical bounds.
 */
@UtilityClass
public class TestLevels {

    /**
     * Returns a level spanning {@code [minHeight, maxHeight)}. Default methods such as
     * {@code getSectionIndex} run their real implementation. The given return values answer
     * the other methods, {@code getServer} defaults to a server holding the test block registry,
     * and anything else returns its type's default.
     *
     * @param minHeight    the lowest valid y
     * @param maxHeight    one past the highest valid y
     * @param returnValues return values for further methods, by name
     * @return the level
     */
    public static Level level(int minHeight, int maxHeight, Map<String, ?> returnValues) {
        Map<String, Object> values = new HashMap<>(returnValues);
        values.put("getMinHeight", minHeight);
        values.put("getMaxHeight", maxHeight);
        values.putIfAbsent("getServer", InterfaceProxy.create(Server.class,
                Map.of("getBlockRegistry", BlockTestRegistry.get())));
        Level fallback = InterfaceProxy.create(Level.class, values);

        return InterfaceProxy.create(Level.class, (InvocationHandler) (proxy, method, arguments) -> {
            if (method.isDefault() && !values.containsKey(method.getName())) {
                return InvocationHandler.invokeDefault(proxy, method, arguments);
            }

            return method.invoke(fallback, arguments);
        });
    }

    /**
     * Returns a level spanning {@code [minHeight, maxHeight)}.
     *
     * @param minHeight the lowest valid y
     * @param maxHeight one past the highest valid y
     * @return the level
     */
    public static Level level(int minHeight, int maxHeight) {
        return level(minHeight, maxHeight, Map.of());
    }

    /**
     * @return a level with the overworld's bounds
     */
    public static Level overworld() {
        return level(DimensionHeight.OVERWORLD.getMinHeight(), DimensionHeight.OVERWORLD.getMaxHeight());
    }
}
