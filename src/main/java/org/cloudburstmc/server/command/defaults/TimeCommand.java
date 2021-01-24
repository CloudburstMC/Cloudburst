package org.cloudburstmc.server.command.defaults;

import com.nukkitx.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandSender;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.player.Player;
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
            for (World world : sender.getServer().getWorlds()) {
                world.checkTime();
                world.startTime();
                world.checkTime();
            }
            CommandUtils.broadcastCommandMessage(sender, "Restarted the time");
            return true;
        } else if ("stop".equals(args[0])) {
            if (!sender.hasPermission("cloudburst.command.time.stop")) {
                sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.permission"));

                return true;
            }
            for (World world : sender.getServer().getWorlds()) {
                world.checkTime();
                world.stopTime();
                world.checkTime();
                CommandUtils.broadcastCommandMessage(sender, new TranslationContainer("%commands.time.stop", world.getTime()));
            }
            return true;
        } else if ("query".equals(args[0])) {
            if (!sender.hasPermission("cloudburst.command.time.query")) {
                sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.permission"));

                return true;
            }
            World world;
            if (sender instanceof Player) {
                world = ((Player) sender).getWorld();
            } else {
                world = sender.getServer().getDefaultWorld();
            }
            sender.sendMessage(new TranslationContainer("commands.time.query.gametime", world.getTime()));
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
                value = World.TIME_DAY;
            } else if ("night".equals(args[1])) {
                value = World.TIME_NIGHT;
            } else if ("midnight".equals(args[1])) {
                value = World.TIME_MIDNIGHT;
            } else if ("noon".equals(args[1])) {
                value = World.TIME_NOON;
            } else if ("sunrise".equals(args[1])) {
                value = World.TIME_SUNRISE;
            } else if ("sunset".equals(args[1])) {
                value = World.TIME_SUNSET;
            } else {
                try {
                    value = Math.max(0, Integer.parseInt(args[1]));
                } catch (Exception e) {
                    return false;
                }
            }

            for (World world : sender.getServer().getWorlds()) {
                world.checkTime();
                world.setTime(value);
                world.checkTime();
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

            for (World world : sender.getServer().getWorlds()) {
                world.checkTime();
                world.setTime(world.getTime() + value);
                world.checkTime();
            }
            CommandUtils.broadcastCommandMessage(sender, new TranslationContainer("%commands.time.added", value));
        } else {
            return false;
        }

        return true;
    }
}
