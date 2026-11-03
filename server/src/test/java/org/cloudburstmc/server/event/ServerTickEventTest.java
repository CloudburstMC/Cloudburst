package org.cloudburstmc.server.event;

import org.cloudburstmc.api.event.EventException;
import org.cloudburstmc.api.event.Listener;
import org.cloudburstmc.api.event.server.*;
import org.cloudburstmc.api.plugin.PluginContainer;
import org.cloudburstmc.api.plugin.PluginManager;
import org.cloudburstmc.api.registry.ResourcePackRegistry;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ServerTickEventTest {

    @Test
    void exposesSynchronousNonCancellableTickNotifications() {
        ServerTickStartEvent start = new ServerTickStartEvent(1);
        ServerTickEndEvent end = new ServerTickEndEvent(1, 12.5, 0);

        assertEquals(1, start.getTickNumber());
        assertEquals(start.getTickNumber(), end.getTickNumber());
        assertEquals(12.5, end.getTickDuration());
        assertFalse(start.isAsynchronous());
        assertFalse(end.isAsynchronous());
        assertThrows(EventException.class, () -> start.setCancelled(true));
        assertThrows(EventException.class, () -> end.setCancelled(true));
    }

    @Test
    void updatesRemainingBudgetWithoutChangingRecordedDuration() throws InterruptedException {
        long budget = TimeUnit.SECONDS.toNanos(1);
        ServerTickEndEvent end = new ServerTickEndEvent(42, 5, budget);
        long before = end.getTimeRemaining();

        Thread.sleep(5);

        assertTrue(end.getTimeRemaining() < before);
        assertTrue(before <= budget);
        assertEquals(5, end.getTickDuration());
    }

    @Test
    void preservesOverdueBudgetsAndWrappedTickNumbers() {
        long overdue = -TimeUnit.MILLISECONDS.toNanos(100);
        ServerTickEndEvent end = new ServerTickEndEvent(Integer.MIN_VALUE, 150, overdue);

        assertEquals(Integer.MIN_VALUE, end.getTickNumber());
        assertTrue(end.getTimeRemaining() <= overdue);
        assertEquals(150, end.getTickDuration());
    }

    @Test
    void rejectsInvalidTickDurations() {
        for (double invalid : new double[]{-1, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new ServerTickEndEvent(1, invalid, 0));
        }
    }

    @Test
    void deliversTickAndLifecycleEventsToServerEventListeners() {
        PluginContainer plugin = InterfaceProxy.create(PluginContainer.class);
        PluginManager plugins = InterfaceProxy.create(PluginManager.class,
                Map.of("fromInstance", Optional.of(plugin)));
        CloudEventManager manager = new CloudEventManager(plugins);
        ServerEvents listener = new ServerEvents();
        manager.registerListeners(new Object(), listener);

        manager.fire(new ServerInitializationEvent());
        manager.fire(new RegistriesClosedEvent(InterfaceProxy.create(ResourcePackRegistry.class)));
        manager.fire(new ServerLoadEvent(ServerLoadType.STARTUP));
        manager.fire(new ServerTickStartEvent(1));
        manager.fire(new ServerTickEndEvent(1, 1, 0));
        manager.fire(new ServerShutdownEvent());

        assertEquals(List.of(ServerInitializationEvent.class, RegistriesClosedEvent.class, ServerLoadEvent.class,
                ServerTickStartEvent.class, ServerTickEndEvent.class, ServerShutdownEvent.class), listener.received);
    }

    private static class ServerEvents {
        private final List<Class<?>> received = new ArrayList<>();

        @Listener(ignoreCancelled = true)
        public void onServerEvent(ServerEvent event) {
            assertFalse(event.isAsynchronous());
            this.received.add(event.getClass());
        }
    }
}
