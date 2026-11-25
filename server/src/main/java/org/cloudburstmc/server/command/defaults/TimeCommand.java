package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArgumentType;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.event.level.TimeSkipReason;
import org.cloudburstmc.api.level.DayCycle;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParam;
import org.cloudburstmc.protocol.bedrock.data.command.CommandPermission;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.network.CommandNetworkData;
import org.cloudburstmc.server.command.network.NetworkArgumentType;
import org.cloudburstmc.server.level.CloudWorldClock;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class TimeCommand extends AdvertisedCommand {

    private static final String OVERWORLD_CLOCK = "minecraft:overworld";
    private static final String CLOCK_NAME = "clockName";
    private static final String MARKER_NAME = "timeMarkerName";
    private static final String MARKER_OCCURRENCE = "timeMarkerOccurrence";

    private static final Map<String, Integer> TIME_MARKERS = Map.of(
            "day", DayCycle.DAY,
            "noon", DayCycle.NOON,
            "night", DayCycle.NIGHT,
            "midnight", DayCycle.MIDNIGHT,
            "sunrise", DayCycle.SUNRISE,
            "sunset", DayCycle.SUNSET
    );

    private static final CommandNetworkData NETWORK_DATA = new CommandNetworkData(
            Set.of(), CommandPermission.GAME_DIRECTORS,
            Map.of(
                    "add", new CommandNetworkData.LiteralParameter("mode", "TimeModeAdd"),
                    "set", new CommandNetworkData.LiteralParameter("mode", "TimeModeSet"),
                    "query", new CommandNetworkData.LiteralParameter("mode", "TimeModeQuery"),
                    "pause", new CommandNetworkData.LiteralParameter("mode", "TimeModePause"),
                    "resume", new CommandNetworkData.LiteralParameter("mode", "TimeModeResume"),
                    "of", new CommandNetworkData.LiteralParameter("command", "TimeOfCommand")
            ),
            true
    );

    public TimeCommand() {
        super("time", "commands.time.description", NETWORK_DATA, "cloudburst.command.time.add", "cloudburst.command.time.query", "cloudburst.command.time.set");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        configureModes(builder, false);

        RequiredArgumentBuilder<CommandSourceStack, String> clock = NetworkArgumentType.argument(
                CLOCK_NAME, CommandArgumentTypes.string(),
                new NetworkArgumentType.SoftEnumParameter("WorldClocks", List.of(OVERWORLD_CLOCK))
        );
        configureModes(clock, true);
        builder.then(Commands.literal("of").then(clock));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        return usage();
    }

    private void configureModes(ArgumentBuilder<CommandSourceStack, ?> builder, boolean namedClock) {
        String queryName = namedClock ? "clockQuery" : "time";
        CommandArgumentType<String> queries = namedClock
                ? CommandArgumentTypes.fixedEnum("ClockTimeQuery", "time")
                : CommandArgumentTypes.fixedEnum("TimeQuery2", "time", "daytime", "gametime", "day");

        builder.then(Commands.literal("add")
                .requires(Commands.requiresPermission("cloudburst.command.time.add"))
                .then(Commands.argument("amount", CommandArgumentTypes.integer())
                        .executes(onClock(namedClock, (context, clock) -> changeTime(context, clock, true)))));
        builder.then(Commands.literal("set")
                .requires(Commands.requiresPermission("cloudburst.command.time.set"))
                .then(Commands.argument("amount", CommandArgumentTypes.integer())
                        .executes(onClock(namedClock, (context, clock) -> changeTime(context, clock, false))))
                .then(markerArgument(namedClock)));
        builder.then(Commands.literal("query")
                .requires(Commands.requiresPermission("cloudburst.command.time.query"))
                .then(Commands.argument(queryName, queries)
                        .executes(context -> queryTime(context, namedClock, argumentValue(context, queryName)))));
        builder.then(Commands.literal("pause")
                .requires(Commands.requiresPermission("cloudburst.command.time.set"))
                .executes(onClock(namedClock, (context, clock) -> setPaused(context, clock, true))));
        builder.then(Commands.literal("resume")
                .requires(Commands.requiresPermission("cloudburst.command.time.set"))
                .executes(onClock(namedClock, (context, clock) -> setPaused(context, clock, false))));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> markerArgument(boolean namedClock) {
        return NetworkArgumentType.argument(MARKER_NAME, CommandArgumentTypes.string(), new NetworkArgumentType.NativeParameter(CommandParam.CLOCK_TIME_MARKER_NAME))
                .executes(onClock(namedClock, (context, clock) -> setMarker(context, clock, "next")))
                .then(Commands.argument(MARKER_OCCURRENCE, CommandArgumentTypes.fixedEnum("TimeMarkerOccurrence", "next", "previous", "stay"))
                        .executes(onClock(namedClock, (context, clock) -> setMarker(context, clock, argumentValue(context, MARKER_OCCURRENCE)))));
    }

    private static int changeTime(CommandContext<CommandSourceStack> context, CloudWorldClock clock, boolean add) {
        int amount = argumentValue(context, "amount", Integer.class);
        long target = add ? Math.addExact(clock.getTotalTicks(), amount) : amount;
        if (target < 0) {
            return failure(context, Component.translatable("commands.time.clock.num.tooSmall", Component.text(0)));
        }

        if (!clock.skipTicks(target - clock.getTotalTicks(), TimeSkipReason.COMMAND)) {
            return failure(context, Component.text("The time change was cancelled."));
        }

        Component message = add
                ? Component.translatable("commands.time.clock.added", Component.text(amount), Component.text(OVERWORLD_CLOCK), Component.text(clock.getTotalTicks()))
                : timeSetMessage(clock);
        return broadcastSuccess(context, message);
    }

    private static int setMarker(CommandContext<CommandSourceStack> context, CloudWorldClock clock, String occurrence) {
        String marker = argumentValue(context, MARKER_NAME);
        String path = marker.startsWith("minecraft:") ? marker.substring("minecraft:".length()) : marker;
        Integer markerTick = TIME_MARKERS.get(path);
        if (markerTick == null) {
            return failure(context, Component.translatable("commands.time.clock.timemarker.failed", Component.text(marker), Component.text(OVERWORLD_CLOCK)));
        }

        int offset = markerTick - clock.getTimeOfDay();
        int skip = switch (occurrence) {
            case "next" -> offset > 0 ? offset : offset + DayCycle.TICKS_PER_DAY;
            case "previous" -> offset <= 0 ? offset : offset - DayCycle.TICKS_PER_DAY;
            case "stay" -> offset;
            default -> throw new IllegalStateException("Unexpected time marker occurrence");
        };

        if (Math.addExact(clock.getTotalTicks(), skip) < 0) {
            return failure(context, Component.translatable("commands.time.clock.timemarker.previous.failed", Component.text(marker), Component.text(OVERWORLD_CLOCK)));
        }

        if (!clock.skipTicks(skip, TimeSkipReason.COMMAND)) {
            return failure(context, Component.text("The time change was cancelled."));
        }

        return broadcastSuccess(context, timeSetMessage(clock));
    }

    private static int queryTime(CommandContext<CommandSourceStack> context, boolean namedClock, String query) {
        if (query.equals("gametime")) {
            long ticks = context.getSource().level().getGameTime();
            sender(context).sendMessage(Component.translatable("commands.time.query.gametime", Component.text(ticks)));
            return (int) (ticks % Integer.MAX_VALUE);
        }

        return withClock(context, namedClock, (command, clock) -> queryClock(command, clock, query));
    }

    private static int queryClock(CommandContext<CommandSourceStack> context, CloudWorldClock clock, String query) {
        long value = switch (query) {
            case "daytime" -> clock.getTimeOfDay();
            case "day" -> DayCycle.day(clock.getTotalTicks());
            case "time" -> clock.getTotalTicks();
            default -> throw new IllegalStateException("Unexpected time query");
        };

        Component message = switch (query) {
            case "daytime" -> Component.translatable("commands.time.query.daytime", Component.text(value));
            case "day" -> Component.translatable("commands.time.query.day", Component.text(value));
            default -> Component.translatable("commands.time.clock.query.time", Component.text(OVERWORLD_CLOCK), Component.text(value));
        };

        sender(context).sendMessage(message);
        return (int) (value % Integer.MAX_VALUE);
    }

    private static int setPaused(CommandContext<CommandSourceStack> context, CloudWorldClock clock, boolean paused) {
        clock.setPaused(paused);
        return broadcastSuccess(context, Component.translatable(paused ? "commands.time.clock.paused" : "commands.time.clock.resumed", Component.text(OVERWORLD_CLOCK), Component.text(clock.getTotalTicks())));
    }

    private static Component timeSetMessage(CloudWorldClock clock) {
        return Component.translatable("commands.time.clock.set", Component.text(OVERWORLD_CLOCK), Component.text(clock.getTotalTicks()));
    }

    private static Command<CommandSourceStack> onClock(boolean namedClock, ClockOperation operation) {
        return context -> withClock(context, namedClock, operation);
    }

    private static int withClock(CommandContext<CommandSourceStack> context, boolean namedClock, ClockOperation operation) {
        String name = namedClock ? argumentValue(context, CLOCK_NAME) : OVERWORLD_CLOCK;
        CloudWorldClock clock = clock(context, name);
        if (clock == null) {
            return failure(context, Component.translatable("commands.time.clock.failed", Component.text(name)));
        }
        return operation.run(context, clock);
    }

    private static @Nullable CloudWorldClock clock(CommandContext<CommandSourceStack> context, String name) {
        if (!OVERWORLD_CLOCK.equals(name)) {
            return null;
        }

        if (context.getSource().level().getDefaultClock() instanceof CloudWorldClock clock) {
            return clock;
        }

        return sender(context).getServer().getDefaultLevel().getDefaultClock() instanceof CloudWorldClock clock ? clock : null;
    }

    @FunctionalInterface
    private interface ClockOperation {
        int run(CommandContext<CommandSourceStack> context, CloudWorldClock clock);
    }
}
