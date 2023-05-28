package org.cloudburstmc.api.util.behavior;

import org.cloudburstmc.api.data.BehaviorKey;

import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public final class BehaviorBuilder {

    private final Map<BehaviorKey<?, ?>, List<Object>> behaviors = new IdentityHashMap<>();

    private BehaviorBuilder() {
    }

    public static BehaviorBuilder create() {
        return new BehaviorBuilder();
    }

    public static BehaviorBuilder extend(BehaviorBuilder builder) {
        BehaviorBuilder extendedBuilder = new BehaviorBuilder();
        extendedBuilder.behaviors.putAll(builder.behaviors);
        return extendedBuilder;
    }

    public <T, R> BehaviorBuilder extend(BehaviorKey<T, R> key, T function) {
        checkNotNull(key, "key");
        checkNotNull(function, "function");
        checkArgument(key.getType() == function.getClass(), "%s does not match expected type of %s for '%s'",
                function.getClass(), key.getType(), key.getId());

        List<Object> functions = this.behaviors.computeIfAbsent(key, behaviorKey -> {
            var list = new ArrayList<>();
            list.add(null);
            return list;
        });

        functions.add(function);
        return this;
    }

    public <T, R> BehaviorBuilder overwrite(BehaviorKey<T, R> key, T function) {
        checkNotNull(key, "key");
        checkNotNull(function, "function");
        checkArgument(key.getType().isInstance(function), "%s does not match expected type of %s for '%s'",
                function.getClass(), key.getType(), key.getId());

        List<Object> functions = this.behaviors.computeIfAbsent(key, behaviorKey -> new ArrayList<>());

        functions.clear();
        functions.add(function);
        return this;
    }

    @SuppressWarnings("unchecked")
    public void applyTo(BehaviorCollection collection) {
        for (Map.Entry<BehaviorKey<?, ?>, List<Object>> entry : this.behaviors.entrySet()) {
            ListIterator<Object> iterator = entry.getValue().listIterator();

            Object function = iterator.next();
            if (function == null) {
                collection.extend((BehaviorKey<Object, ?>) entry.getKey(), iterator.next());
            } else {
                collection.overwrite((BehaviorKey<Object, ?>) entry.getKey(), function);
            }
            while (iterator.hasNext()) {
                collection.extend((BehaviorKey<Object, ?>) entry.getKey(), iterator.next());
            }
        }
    }
}
