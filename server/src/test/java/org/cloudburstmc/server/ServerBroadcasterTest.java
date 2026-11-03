package org.cloudburstmc.server;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.event.EventManager;
import org.cloudburstmc.api.event.server.BroadcastMessageEvent;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class ServerBroadcasterTest {

    @Test
    void publicApiUsesTheDefaultPermissionAndCountsConsoleAndPlayers() {
        List<Component> consoleMessages = new ArrayList<>();
        List<Component> playerMessages = new ArrayList<>();
        List<Component> excludedMessages = new ArrayList<>();
        Set<String> permission = Set.of(Server.BROADCAST_CHANNEL_USERS);
        CommandSender console = recipient(CommandSender.class, permission, consoleMessages);
        Player included = recipient(Player.class, permission, playerMessages);
        Player excluded = recipient(Player.class, Set.of(), excludedMessages);
        ServerBroadcaster broadcaster = new ServerBroadcaster(Thread.currentThread(), events(event -> {}), console);
        broadcaster.addPlayer(included);
        broadcaster.addPlayer(excluded);
        Server server = server(broadcaster);
        Component message = Component.text("Hello", NamedTextColor.GREEN);

        assertEquals(2, server.broadcast(message));
        assertEquals(List.of(message), consoleMessages);
        assertEquals(List.of(message), playerMessages);
        assertTrue(excludedMessages.isEmpty());
    }

    @Test
    void explicitPermissionSelectsRecipients() {
        List<Component> consoleMessages = new ArrayList<>();
        List<Component> playerMessages = new ArrayList<>();
        CommandSender console = recipient(CommandSender.class, Set.of(), consoleMessages);
        Player player = recipient(Player.class, Set.of("test.broadcast"), playerMessages);
        ServerBroadcaster broadcaster = new ServerBroadcaster(Thread.currentThread(), events(event -> {}), console);
        broadcaster.addPlayer(player);
        Component message = Component.text("Restricted");

        assertEquals(1, server(broadcaster).broadcast(message, "test.broadcast"));
        assertTrue(consoleMessages.isEmpty());
        assertEquals(List.of(message), playerMessages);
    }

    @Test
    void listenersCanReplaceTheMessageAndRecipients() {
        List<Component> consoleMessages = new ArrayList<>();
        List<Component> playerMessages = new ArrayList<>();
        CommandSender console = recipient(CommandSender.class, Set.of(Server.BROADCAST_CHANNEL_USERS), consoleMessages);
        Player added = recipient(Player.class, Set.of(), playerMessages);
        Component replacement = Component.text("Replacement", NamedTextColor.RED);
        EventManager events = events(event -> {
            event.message(replacement);
            event.getRecipients().remove(console);
            event.getRecipients().add(added);
        });
        ServerBroadcaster broadcaster = new ServerBroadcaster(Thread.currentThread(), events, console);

        assertEquals(1, server(broadcaster).broadcast(Component.text("Original")));
        assertTrue(consoleMessages.isEmpty());
        assertEquals(List.of(replacement), playerMessages);
    }

    @Test
    void cancellationPreventsDeliveryAndReturnsZero() {
        List<Component> messages = new ArrayList<>();
        CommandSender console = recipient(CommandSender.class, Set.of(Server.BROADCAST_CHANNEL_USERS), messages);
        ServerBroadcaster broadcaster = new ServerBroadcaster(Thread.currentThread(), events(event -> event.setCancelled(true)), console);

        assertEquals(0, server(broadcaster).broadcast(Component.text("Cancelled")));
        assertTrue(messages.isEmpty());
    }

    @Test
    void listenersCanAddRecipientsEvenWhenNoPermissionMatched() {
        List<Component> messages = new ArrayList<>();
        CommandSender console = recipient(CommandSender.class, Set.of(), new ArrayList<>());
        Player added = recipient(Player.class, Set.of(), messages);
        ServerBroadcaster broadcaster = new ServerBroadcaster(Thread.currentThread(), events(event -> event.getRecipients().add(added)), console);
        Component message = Component.text("Added by listener");

        assertEquals(1, server(broadcaster).broadcast(message));
        assertEquals(List.of(message), messages);
    }

    @Test
    void marksOffThreadBroadcastsAsAsynchronousWithoutMovingTheirExecution() {
        List<Boolean> asynchronous = new ArrayList<>();
        List<Thread> threads = new ArrayList<>();
        CommandSender console = recipient(CommandSender.class, Set.of(), new ArrayList<>());
        EventManager events = events(event -> {
            asynchronous.add(event.isAsynchronous());
            threads.add(Thread.currentThread());
        });
        ServerBroadcaster broadcaster = new ServerBroadcaster(Thread.currentThread(), events, console);

        broadcaster.broadcast(Component.text("Server thread"), Server.BROADCAST_CHANNEL_USERS);
        Thread worker = CompletableFuture.supplyAsync(() -> {
            broadcaster.broadcast(Component.text("Worker thread"), Server.BROADCAST_CHANNEL_USERS);
            return Thread.currentThread();
        }).join();

        assertEquals(List.of(false, true), asynchronous);
        assertEquals(List.of(Thread.currentThread(), worker), threads);
    }

    @Test
    void tracksOnlineMembershipWithoutDuplicateDeliveries() {
        List<Component> messages = new ArrayList<>();
        CommandSender console = recipient(CommandSender.class, Set.of(), new ArrayList<>());
        Player player = recipient(Player.class, Set.of(Server.BROADCAST_CHANNEL_USERS), messages);
        ServerBroadcaster broadcaster = new ServerBroadcaster(Thread.currentThread(), events(event -> {}), console);
        broadcaster.addPlayer(player);
        broadcaster.addPlayer(player);
        Component message = Component.text("Once");

        assertEquals(1, server(broadcaster).broadcast(message));

        broadcaster.removePlayer(player);

        assertEquals(0, server(broadcaster).broadcast(Component.text("Offline")));
        assertEquals(List.of(message), messages);
    }

    @Test
    void validatesArgumentsBeforeFiringTheEvent() {
        AtomicInteger calls = new AtomicInteger();
        CommandSender console = recipient(CommandSender.class, Set.of(), new ArrayList<>());
        ServerBroadcaster broadcaster = new ServerBroadcaster(Thread.currentThread(), events(event -> calls.incrementAndGet()), console);

        assertThrows(NullPointerException.class, () -> broadcaster.broadcast(null, Server.BROADCAST_CHANNEL_USERS));
        assertThrows(NullPointerException.class, () -> broadcaster.broadcast(Component.empty(), null));
        assertThrows(IllegalArgumentException.class, () -> broadcaster.broadcast(Component.empty(), ""));
        assertEquals(0, calls.get());
    }

    private static Server server(ServerBroadcaster broadcaster) {
        return InterfaceProxy.create(Server.class, (proxy, method, arguments) -> {
            if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, arguments);
            }

            if (method.getName().equals("broadcast") && arguments != null && arguments[0] instanceof Component message && arguments[1] instanceof String permission) {
                return broadcaster.broadcast(message, permission);
            }

            throw new AssertionError("Unexpected server access: " + method);
        });
    }

    private static EventManager events(Consumer<BroadcastMessageEvent> listener) {
        return InterfaceProxy.create(EventManager.class, (proxy, method, arguments) -> {
            if (method.getName().equals("fire") && arguments != null && arguments[0] instanceof BroadcastMessageEvent event) {
                listener.accept(event);
                return null;
            }

            throw new AssertionError("Unexpected event manager access: " + method);
        });
    }

    private static <T extends CommandSender> T recipient(Class<T> type, Set<String> permissions, List<Component> messages) {
        return InterfaceProxy.create(type, (proxy, method, arguments) -> {
            if (method.getName().equals("hasPermission") && arguments != null && arguments[0] instanceof String permission) {
                return permissions.contains(permission);
            }

            if (method.getName().equals("sendMessage") && arguments != null && arguments[0] instanceof Component message) {
                messages.add(message);
                return null;
            }

            throw new AssertionError("Unexpected recipient access: " + method);
        });
    }
}
