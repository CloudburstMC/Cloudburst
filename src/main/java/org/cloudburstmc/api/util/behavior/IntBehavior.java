package org.cloudburstmc.api.util.behavior;

public interface IntBehavior {

    int get(Behavior<Executor> behavior);

    @FunctionalInterface
    interface Executor {
        int execute();
    }
}
