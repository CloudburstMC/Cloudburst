package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.event.level.TimeSkipReason;
import org.cloudburstmc.api.level.DayCycle;
import org.cloudburstmc.protocol.bedrock.data.command.CommandPermission;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.network.CommandNetworkData;
import org.cloudburstmc.server.level.CloudLevel;

import java.util.Map;
import java.util.Set;

public class TimeCommand extends AdvertisedCommand {

    private static final CommandNetworkData NETWORK_DATA = new CommandNetworkData(
            Set.of(), CommandPermission.GAME_DIRECTORS, Map.of(
            "add", new CommandNetworkData.LiteralParameter("mode", "TimeModeAdd"),
            "set", new CommandNetworkData.LiteralParameter("mode", "TimeModeSet"),
            "query", new CommandNetworkData.LiteralParameter("mode", "TimeModeQuery"))
    );

    public TimeCommand() {
        super("time", "commands.time.description", NETWORK_DATA,
                "cloudburst.command.time.add", "cloudburst.command.time.query",
                "cloudburst.command.time.set");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.then(Commands.literal("add")
                .requires(Commands.requiresPermission("cloudburst.command.time.add"))
                .then(Commands.argument("amount", CommandArgumentTypes.integer())
                        .executes(context -> this.executeCommand(context, this::addTime))));
        builder.then(Commands.literal("set")
                .requires(Commands.requiresPermission("cloudburst.command.time.set"))
                .then(Commands.argument("amount", CommandArgumentTypes.integer(0))
                        .executes(context -> this.executeCommand(context, this::setNumericTime)))
                .then(Commands.argument("time", CommandArgumentTypes.fixedEnumNamed("time", "TimeSpec",
                                "day", "sunrise", "noon", "sunset", "night", "midnight"))
                        .executes(context -> this.executeCommand(context, this::setNamedTime))));
        builder.then(Commands.literal("query")
                .requires(Commands.requiresPermission("cloudburst.command.time.query"))
                .then(Commands.argument("time", CommandArgumentTypes.fixedEnumNamed("time", "TimeQuery", "daytime", "gametime", "day"))
                        .executes(context -> this.executeCommand(context, this::queryTime))));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        return usage();
    }

    private int addTime(CommandContext<CommandSourceStack> context) {
        int value = argumentValue(context, "amount", Integer.class);
        CloudLevel level = level(context);
        if (!level.hasDaylightCycle()) {
            return noClock(context);
        }

        long target = Math.max(0, Math.addExact(level.getFullTime(), value));
        if (!level.skipTime(Math.subtractExact(target, level.getFullTime()), TimeSkipReason.COMMAND)) {
            return failure(context, Component.text("The time change was cancelled."));
        }

        CommandUtils.broadcastCommandMessage(sender(context), Component.translatable("commands.time.added", Component.text(value)));
        return success();
    }

    private int setNumericTime(CommandContext<CommandSourceStack> context) {
        return setTime(context, argumentValue(context, "amount", Integer.class));
    }

    private int setNamedTime(CommandContext<CommandSourceStack> context) {
        long value = switch (argumentValue(context, "time")) {
            case "day" -> DayCycle.DAY;
            case "sunrise" -> DayCycle.SUNRISE;
            case "noon" -> DayCycle.NOON;
            case "sunset" -> DayCycle.SUNSET;
            case "night" -> DayCycle.NIGHT;
            case "midnight" -> DayCycle.MIDNIGHT;
            default -> throw new IllegalStateException("Unexpected time preset");
        };

        return setTime(context, DayCycle.atOrAfter(level(context).getFullTime(), value));
    }

    private int setTime(CommandContext<CommandSourceStack> context, long value) {
        CloudLevel level = level(context);
        if (!level.hasDaylightCycle()) {
            return noClock(context);
        }

        if (value == level.getFullTime()) {
            return failure(context, Component.text("The daylight clock is already at that time."));
        }

        if (!level.skipTime(Math.subtractExact(value, level.getFullTime()), TimeSkipReason.COMMAND)) {
            return failure(context, Component.text("The time change was cancelled."));
        }

        CommandUtils.broadcastCommandMessage(sender(context), Component.translatable("commands.time.set", Component.text(value)));
        return success();
    }

    private int queryTime(CommandContext<CommandSourceStack> context) {
        CloudLevel level = level(context);
        String query = argumentValue(context, "time");
        if (!query.equals("gametime") && !level.hasDaylightCycle()) {
            return noClock(context);
        }

        long value = switch (query) {
            case "daytime" -> level.getTime();
            case "gametime" -> level.getGameTime();
            case "day" -> DayCycle.day(level.getFullTime());
            default -> throw new IllegalStateException("Unexpected time query");
        };

        sender(context).sendMessage(Component.translatable("commands.time.query.gametime", Component.text(value)));
        return (int) (value % Integer.MAX_VALUE);
    }

    private static int noClock(CommandContext<CommandSourceStack> context) {
        return failure(context, Component.text("This level has no daylight cycle."));
    }

    private static CloudLevel level(CommandContext<CommandSourceStack> context) {
        return (CloudLevel) context.getSource().level();
    }
}
