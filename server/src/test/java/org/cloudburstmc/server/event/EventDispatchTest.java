package org.cloudburstmc.server.event;

import org.cloudburstmc.api.event.Cancellable;
import org.cloudburstmc.api.event.EventPriority;
import org.cloudburstmc.api.event.Listener;
import org.cloudburstmc.api.event.server.ServerEvent;
import org.cloudburstmc.api.plugin.PluginContainer;
import org.cloudburstmc.api.plugin.PluginManager;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventDispatchTest {

    @Test
    void respectsPriorityAndCancellationForNonPublicListeners() {
        PluginContainer plugin = InterfaceProxy.create(PluginContainer.class);
        PluginManager plugins = InterfaceProxy.create(PluginManager.class,
                Map.of("fromInstance", Optional.of(plugin)));
        CloudEventManager manager = new CloudEventManager(plugins);
        CancellationListeners listener = new CancellationListeners();
        manager.registerListeners(new Object(), listener);

        manager.fire(new Notification());

        assertEquals(List.of("cancel", "observe"), listener.calls);
    }

    private static class Notification extends ServerEvent implements Cancellable {
    }

    private static class CancellationListeners {
        private final List<String> calls = new ArrayList<>();

        @Listener(priority = EventPriority.LOWEST)
        private void cancel(Notification event) {
            this.calls.add("cancel");
            event.setCancelled(true);
        }

        @Listener(ignoreCancelled = true)
        private void skipCancelled(Notification event) {
            this.calls.add("unexpected");
        }

        @Listener(priority = EventPriority.MONITOR)
        private void observe(Notification event) {
            this.calls.add("observe");
        }
    }
}
