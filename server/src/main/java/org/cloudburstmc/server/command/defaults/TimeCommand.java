package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.AdvertisedCommand;
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
                        .executes(this::executeCommand)));
        builder.then(Commands.literal("set")
                .requires(Commands.requiresPermission("cloudburst.command.time.set"))
                .then(Commands.argument("amountSet", CommandArgumentTypes.integer("amount", 0))
                        .executes(this::executeCommand))
                .then(Commands.argument("time", CommandArgumentTypes.fixedEnumNamed("time", "TimeSpec",
                                "day", "sunrise", "noon", "sunset", "night", "midnight"))
                        .executes(this::executeCommand)));
        builder.then(Commands.literal("query")
                .requires(Commands.requiresPermission("cloudburst.command.time.query"))
                .then(Commands.argument("timeQuery", CommandArgumentTypes.fixedEnumNamed("time", "TimeQuery", "daytime", "gametime", "day"))
                        .executes(this::executeCommand)));
        builder.then(Commands.literal("start")
                .requires(Commands.requiresPermission("cloudburst.command.time.start"))
                .executes(this::executeCommand));
        builder.then(Commands.literal("stop")
                .requires(Commands.requiresPermission("cloudburst.command.time.stop"))
                .executes(this::executeCommand));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        CommandSender sender = sender(context);
        if (hasArgument(context, "start")) {
            for (CloudLevel level : ((CloudServer) sender.getServer()).getLevels()) {
                level.checkTime();
                level.startTime();
                level.checkTime();
            }

            CommandUtils.broadcastCommandMessage(sender, Component.text("Restarted the time"));
            return success();
        } else if (hasArgument(context, "stop")) {
            for (CloudLevel level : ((CloudServer) sender.getServer()).getLevels()) {
                level.checkTime();
                level.stopTime();
                level.checkTime();
                CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.time.stop", Component.text(level.getTime())));
            }

            return success();
        } else if (hasArgument(context, "query")) {
            CloudLevel level;
            if (sender instanceof CloudPlayer) {
                level = ((CloudPlayer) sender).getLevel();
            } else {
                level = (CloudLevel) sender.getServer().getDefaultLevel();
            }

            sender.sendMessage(Component.translatable("commands.time.query.gametime", Component.text(level.getTime())));
            return success();
        }

        if (hasArgument(context, "set")) {
            int value;
            if (hasArgument(context, "amountSet")) {
                value = argumentValue(context, "amountSet", Integer.class);
            } else {
                value = switch (argumentValue(context, "time")) {
                    case "day" -> CloudLevel.TIME_DAY;
                    case "night" -> CloudLevel.TIME_NIGHT;
                    case "midnight" -> CloudLevel.TIME_MIDNIGHT;
                    case "noon" -> CloudLevel.TIME_NOON;
                    case "sunrise" -> CloudLevel.TIME_SUNRISE;
                    case "sunset" -> CloudLevel.TIME_SUNSET;
                    default -> 0;
                };
            }

            for (CloudLevel level : ((CloudServer) sender.getServer()).getLevels()) {
                level.checkTime();
                level.setTime(value);
                level.checkTime();
            }
            CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.time.set", Component.text(value)));
        } else if (hasArgument(context, "add")) {
            int value = argumentValue(context, "amount", Integer.class);

            for (CloudLevel level : ((CloudServer) sender.getServer()).getLevels()) {
                level.checkTime();
                level.setTime(level.getTime() + value);
                level.checkTime();
            }
            CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.time.added", Component.text(value)));
        } else {
            return usage();
        }

        return success();
    }
}
