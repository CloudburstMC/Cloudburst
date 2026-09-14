package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.network.CommandNetworkData;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.player.CloudPlayer;

public class TimeCommand extends AdvertisedCommand {

    public TimeCommand() {
        super("time", "commands.time.description", CommandNetworkData.GAME_DIRECTORS,
                "cloudburst.command.time.add", "cloudburst.command.time.query",
                "cloudburst.command.time.set", "cloudburst.command.time.start", "cloudburst.command.time.stop");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.then(Commands.literal("add")
                .requires(Commands.requiresPermission("cloudburst.command.time.add"))
                .then(Commands.argument("amount", CommandArgumentTypes.integer(0))
                        .executes(context -> this.executeCommand(context, this::addTime))));
        builder.then(Commands.literal("set")
                .requires(Commands.requiresPermission("cloudburst.command.time.set"))
                .then(Commands.argument("amountSet", CommandArgumentTypes.integer("amount", 0))
                        .executes(context -> this.executeCommand(context, this::setNumericTime)))
                .then(Commands.argument("time", CommandArgumentTypes.fixedEnumNamed("time", "TimeSpec",
                                "day", "sunrise", "noon", "sunset", "night", "midnight"))
                        .executes(context -> this.executeCommand(context, this::setNamedTime))));
        builder.then(Commands.literal("query")
                .requires(Commands.requiresPermission("cloudburst.command.time.query"))
                .then(Commands.argument("timeQuery", CommandArgumentTypes.fixedEnumNamed("time", "TimeQuery", "daytime", "gametime", "day"))
                        .executes(context -> this.executeCommand(context, this::queryTime))));
        builder.then(Commands.literal("start")
                .requires(Commands.requiresPermission("cloudburst.command.time.start"))
                .executes(context -> this.executeCommand(context, this::startTime)));
        builder.then(Commands.literal("stop")
                .requires(Commands.requiresPermission("cloudburst.command.time.stop"))
                .executes(context -> this.executeCommand(context, this::stopTime)));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        return usage();
    }

    private int addTime(CommandContext<CommandSourceStack> context) {
        int value = argumentValue(context, "amount", Integer.class);
        for (CloudLevel level : server(context).getLevels()) {
            level.checkTime();
            level.setTime(level.getTime() + value);
            level.checkTime();
        }

        CommandUtils.broadcastCommandMessage(sender(context), Component.translatable("commands.time.added", Component.text(value)));
        return success();
    }

    private int setNumericTime(CommandContext<CommandSourceStack> context) {
        return setTime(context, argumentValue(context, "amountSet", Integer.class));
    }

    private int setNamedTime(CommandContext<CommandSourceStack> context) {
        int value = switch (argumentValue(context, "time")) {
            case "day" -> CloudLevel.TIME_DAY;
            case "sunrise" -> CloudLevel.TIME_SUNRISE;
            case "noon" -> CloudLevel.TIME_NOON;
            case "sunset" -> CloudLevel.TIME_SUNSET;
            case "night" -> CloudLevel.TIME_NIGHT;
            case "midnight" -> CloudLevel.TIME_MIDNIGHT;
            default -> throw new IllegalStateException("Unexpected time preset");
        };

        return setTime(context, value);
    }

    private int setTime(CommandContext<CommandSourceStack> context, int value) {
        for (CloudLevel level : server(context).getLevels()) {
            level.checkTime();
            level.setTime(value);
            level.checkTime();
        }

        CommandUtils.broadcastCommandMessage(sender(context), Component.translatable("commands.time.set", Component.text(value)));
        return success();
    }

    private int queryTime(CommandContext<CommandSourceStack> context) {
        CloudLevel level = sender(context) instanceof CloudPlayer player ? player.getLevel()
                : (CloudLevel) sender(context).getServer().getDefaultLevel();

        long value = switch (argumentValue(context, "timeQuery")) {
            case "daytime" -> Math.floorMod(level.getTime(), CloudLevel.TIME_FULL);
            case "gametime" -> level.getCurrentTick();
            case "day" -> Math.floorDiv(level.getTime(), CloudLevel.TIME_FULL);
            default -> throw new IllegalStateException("Unexpected time query");
        };

        sender(context).sendMessage(Component.translatable("commands.time.query.gametime", Component.text(value)));
        return success();
    }

    private int startTime(CommandContext<CommandSourceStack> context) {
        for (CloudLevel level : server(context).getLevels()) {
            level.checkTime();
            level.startTime();
            level.checkTime();
        }

        CommandUtils.broadcastCommandMessage(sender(context), Component.text("Restarted the time"));
        return success();
    }

    private int stopTime(CommandContext<CommandSourceStack> context) {
        for (CloudLevel level : server(context).getLevels()) {
            level.checkTime();
            level.stopTime();
            level.checkTime();
            CommandUtils.broadcastCommandMessage(sender(context), Component.translatable("commands.time.stop", Component.text(level.getTime())));
        }

        return success();
    }

    private CloudServer server(CommandContext<CommandSourceStack> context) {
        return (CloudServer) sender(context).getServer();
    }
}
