package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.server.command.CommandTreeValidator;
import org.cloudburstmc.server.command.ServerCommand;
import org.cloudburstmc.server.diagnostics.ServerTickStatistics;
import org.cloudburstmc.server.testutil.InterfaceProxy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class PerformanceCommandsTest {

    @Test
    void reportsTpsWithoutNeedingSparkOrMemoryPermission() throws CommandSyntaxException {
        CommandDispatcher<CommandSourceStack> dispatcher = dispatcher(new TpsCommand(statistics()));
        List<String> messages = new ArrayList<>();
        CommandSourceStack source = source(Set.of("cloudburst.command.tps"), messages);

        assertEquals(1, dispatcher.execute("tps", source));
        assertEquals(List.of("TPS from last 1m, 5m, 15m: 20.0, 20.0, 20.0"), messages);
        assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("tps mem", source));
    }

    @Test
    void showsMemoryOnlyWhenAllowedAndWarnsOnce() throws CommandSyntaxException {
        CommandDispatcher<CommandSourceStack> dispatcher = dispatcher(new TpsCommand(statistics()));
        List<String> messages = new ArrayList<>();
        CommandSourceStack source = source(Set.of("cloudburst.command.tps", "cloudburst.command.tpsmemory"), messages);

        assertEquals(1, dispatcher.execute("tps mem", source));
        assertEquals(3, messages.size());
        assertTrue(messages.get(1).startsWith("Current memory usage: "));
        assertTrue(messages.get(2).startsWith("Memory usage fluctuates"));

        messages.clear();
        assertEquals(1, dispatcher.execute("tps mem", source));
        assertEquals(2, messages.size());
    }

    @Test
    void reportsAverageMinimumAndMaximumTickTimes() throws CommandSyntaxException {
        CommandDispatcher<CommandSourceStack> dispatcher = dispatcher(new MsptCommand(statistics()));
        List<String> messages = new ArrayList<>();

        assertEquals(1, dispatcher.execute("mspt", source(Set.of("cloudburst.command.mspt"), messages)));
        assertEquals(List.of("Server tick times (avg/min/max) from last 5s, 10s, 1m:",
                "42.0/42.0/42.0, 42.0/42.0/42.0, 42.0/42.0/42.0"), messages);
    }

    @Test
    void requiresEachCommandsPermission() {
        List<String> messages = new ArrayList<>();
        CommandSourceStack source = source(Set.of(), messages);

        assertThrows(CommandSyntaxException.class,
                () -> dispatcher(new TpsCommand(statistics())).execute("tps", source));
        assertThrows(CommandSyntaxException.class,
                () -> dispatcher(new MsptCommand(statistics())).execute("mspt", source));
        assertTrue(messages.isEmpty());
    }

    private static ServerTickStatistics statistics() {
        AtomicLong clock = new AtomicLong();
        ServerTickStatistics statistics = new ServerTickStatistics(clock::get);
        statistics.start();
        clock.set(TimeUnit.MILLISECONDS.toNanos(50));
        statistics.recordTick(TimeUnit.MILLISECONDS.toNanos(42));
        return statistics;
    }

    private static CommandDispatcher<CommandSourceStack> dispatcher(ServerCommand command) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(command.getName())
                .requires(Commands.requiresPermission(command.getAccessPermissions().iterator().next()));
        command.configure(root, command.getName(), InterfaceProxy.create(CommandArguments.class));
        CommandTreeValidator.validate(root.build());

        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.register(root);
        return dispatcher;
    }

    private static CommandSourceStack source(Set<String> permissions, List<String> messages) {
        CommandSender sender = InterfaceProxy.create(CommandSender.class, (proxy, method, arguments) -> {
            if (method.getName().equals("hasPermission") && arguments != null && arguments[0] instanceof String permission) {
                return permissions.contains(permission);
            }

            if (method.getName().equals("sendMessage") && arguments != null && arguments[0] instanceof Component message) {
                messages.add(PlainTextComponentSerializer.plainText().serialize(message));
                return null;
            }

            throw new AssertionError("Unexpected sender access: " + method);
        });

        return InterfaceProxy.create(CommandSourceStack.class, Map.of("sender", sender));
    }
}
