package org.cloudburstmc.server.testutil;

import com.google.common.base.Defaults;
import com.google.common.primitives.Primitives;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import static java.util.Objects.requireNonNull;

@UtilityClass
public class InterfaceProxy {

    public static <T> T create(Class<T> type) {
        return create(type, Map.of());
    }

    public static <T> T create(Class<T> type, Map<String, ?> returnValues) {
        requireNonNull(type, "type");
        requireNonNull(returnValues, "returnValues");
        if (!type.isInterface()) {
            throw new IllegalArgumentException(type.getName() + " is not an interface");
        }

        Object proxy = Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (instance, method, arguments) -> invoke(instance, method, arguments, type, returnValues));
        return type.cast(proxy);
    }

    private static Object invoke(Object proxy, Method method, Object[] arguments, Class<?> type, Map<String, ?> returnValues) {
        if (method.getDeclaringClass() == Object.class) {
            return switch (method.getName()) {
                case "equals" -> arguments != null && arguments.length == 1 && proxy == arguments[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "InterfaceProxy[" + type.getName() + "]";
                default -> throw new IllegalStateException("Unsupported Object method: " + method);
            };
        }

        if (!returnValues.containsKey(method.getName())) {
            return Defaults.defaultValue(method.getReturnType());
        }

        Object returnValue = returnValues.get(method.getName());
        if (returnValue != null && !Primitives.wrap(method.getReturnType()).isInstance(returnValue)) {
            throw new IllegalStateException("Configured return value for " + method + " has type " + returnValue.getClass().getName());
        }

        return returnValue;
    }
}
