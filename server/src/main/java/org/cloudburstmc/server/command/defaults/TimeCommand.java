package org.cloudburstmc.server.command.defaults;

import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.utils.TextFormat;

/**
 * Created on 2015/11/11 by xtypr.
 * Package cn.nukkit.command.defaults in project Nukkit .
 */
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
                sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.permission"));

                return true;
            }
            for (CloudLevel level : ((CloudServer) sender.getServer()).getLevels()) {
                level.checkTime();
                level.startTime();
                level.checkTime();
            }
            CommandUtils.broadcastCommandMessage(sender, "Restarted the time");
            return true;
        } else if ("stop".equals(args[0])) {
            if (!sender.hasPermission("cloudburst.command.time.stop")) {
                sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.permission"));

                return true;
            }
            for (CloudLevel level : ((CloudServer) sender.getServer()).getLevels()) {
                level.checkTime();
                level.stopTime();
                level.checkTime();
                CommandUtils.broadcastCommandMessage(sender, new TranslationContainer("%commands.time.stop", level.getTime()));
            }
            return true;
        } else if ("query".equals(args[0])) {
            if (!sender.hasPermission("cloudburst.command.time.query")) {
                sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.permission"));

                return true;
            }
            CloudLevel level;
            if (sender instanceof CloudPlayer) {
                level = ((CloudPlayer) sender).getLevel();
            } else {
                level = (CloudLevel) sender.getServer().getDefaultLevel();
            }
            sender.sendMessage(new TranslationContainer("commands.time.query.gametime", level.getTime()));
            return true;
        }


        if (args.length < 2) {
            return false;
        }

        if ("set".equals(args[0])) {
            if (!sender.hasPermission("cloudburst.command.time.set")) {
                sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.permission"));

                return true;
            }

            int value;
            if ("day".equals(args[1])) {
                value = CloudLevel.TIME_DAY;
            } else if ("night".equals(args[1])) {
                value = CloudLevel.TIME_NIGHT;
            } else if ("midnight".equals(args[1])) {
                value = CloudLevel.TIME_MIDNIGHT;
            } else if ("noon".equals(args[1])) {
                value = CloudLevel.TIME_NOON;
            } else if ("sunrise".equals(args[1])) {
                value = CloudLevel.TIME_SUNRISE;
            } else if ("sunset".equals(args[1])) {
                value = CloudLevel.TIME_SUNSET;
            } else {
                try {
                    value = Math.max(0, Integer.parseInt(args[1]));
                } catch (Exception e) {
                    return false;
                }
            }

            for (CloudLevel level : ((CloudServer) sender.getServer()).getLevels()) {
                level.checkTime();
                level.setTime(value);
                level.checkTime();
            }
            CommandUtils.broadcastCommandMessage(sender, new TranslationContainer("%commands.time.set", value));
        } else if ("add".equals(args[0])) {
            if (!sender.hasPermission("cloudburst.command.time.add")) {
                sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.permission"));

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
            CommandUtils.broadcastCommandMessage(sender, new TranslationContainer("%commands.time.added", value));
        } else {
            return false;
        }

        return true;
    }
}
