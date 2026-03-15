package org.cloudburstmc.server.command.defaults;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.player.CloudPlayer;

public class TimeCommand extends Command {

    public TimeCommand() {
        super("time", CommandData.builder("time")
                .setDescription("commands.time.description")
                .setUsageMessage("/time <add|set|start|stop> [value]")
                .setPermissions("cloudburst.command.time.add",
                        "cloudburst.command.time.set",
                        "cloudburst.command.time.start",
                        "cloudburst.command.time.stop")
                .setParameters(new CommandParameter[]{
                        new CommandParameter("start|stop", CommandParamType.STRING, false)
                }, new CommandParameter[]{
                        new CommandParameter("add|set", CommandParamType.STRING, false),
                        new CommandParameter("value", CommandParamType.INT, false)
                }, new CommandParameter[]{
                        new CommandParameter("add|set", CommandParamType.STRING, false),
                        new CommandParameter("value", CommandParamType.STRING, false)
                })
                .build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }

        if (args.length < 1) {
            return false;
        }

        if ("start".equals(args[0])) {
            if (!sender.hasPermission("cloudburst.command.time.start")) {
                sender.sendMessage(Component.translatable("commands.generic.permission").color(NamedTextColor.RED));
                return true;
            }
            for (CloudLevel level : ((CloudServer) sender.getServer()).getLevels()) {
                level.checkTime();
                level.startTime();
                level.checkTime();
            }
            CommandUtils.broadcastCommandMessage(sender, Component.text("Restarted the time"));
            return true;
        } else if ("stop".equals(args[0])) {
            if (!sender.hasPermission("cloudburst.command.time.stop")) {
                sender.sendMessage(Component.translatable("commands.generic.permission").color(NamedTextColor.RED));
                return true;
            }
            for (CloudLevel level : ((CloudServer) sender.getServer()).getLevels()) {
                level.checkTime();
                level.stopTime();
                level.checkTime();
                CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.time.stop", Component.text(level.getTime())));
            }
            return true;
        } else if ("query".equals(args[0])) {
            if (!sender.hasPermission("cloudburst.command.time.query")) {
                sender.sendMessage(Component.translatable("commands.generic.permission").color(NamedTextColor.RED));
                return true;
            }
            CloudLevel level;
            if (sender instanceof CloudPlayer) {
                level = ((CloudPlayer) sender).getLevel();
            } else {
                level = (CloudLevel) sender.getServer().getDefaultLevel();
            }
            sender.sendMessage(Component.translatable("commands.time.query.gametime", Component.text(level.getTime())));
            return true;
        }

        if (args.length < 2) {
            return false;
        }

        if ("set".equals(args[0])) {
            if (!sender.hasPermission("cloudburst.command.time.set")) {
                sender.sendMessage(Component.translatable("commands.generic.permission").color(NamedTextColor.RED));
                return true;
            }

            int value;
            switch (args[1]) {
                case "day" -> value = CloudLevel.TIME_DAY;
                case "night" -> value = CloudLevel.TIME_NIGHT;
                case "midnight" -> value = CloudLevel.TIME_MIDNIGHT;
                case "noon" -> value = CloudLevel.TIME_NOON;
                case "sunrise" -> value = CloudLevel.TIME_SUNRISE;
                case "sunset" -> value = CloudLevel.TIME_SUNSET;
                case null, default -> {
                    try {
                        value = Math.max(0, Integer.parseInt(args[1]));
                    } catch (Exception e) {
                        return false;
                    }
                }
            }

            for (CloudLevel level : ((CloudServer) sender.getServer()).getLevels()) {
                level.checkTime();
                level.setTime(value);
                level.checkTime();
            }
            CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.time.set", Component.text(value)));
        } else if ("add".equals(args[0])) {
            if (!sender.hasPermission("cloudburst.command.time.add")) {
                sender.sendMessage(Component.translatable("commands.generic.permission").color(NamedTextColor.RED));
                return true;
            }

            int value;
            try {
                value = Math.max(0, Integer.parseInt(args[1]));
            } catch (Exception e) {
                return false;
            }

            for (CloudLevel level : ((CloudServer) sender.getServer()).getLevels()) {
                level.checkTime();
                level.setTime(level.getTime() + value);
                level.checkTime();
            }
            CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.time.added", Component.text(value)));
        } else {
            return false;
        }

        return true;
    }
}
