package org.cloudburstmc.api.util.behavior;

public interface FloatBehavior {

    float getProperty(Behavior<FloatBehavior> behavior);

    @FunctionalInterface
    interface Executor {
        float execute();
    }
}
