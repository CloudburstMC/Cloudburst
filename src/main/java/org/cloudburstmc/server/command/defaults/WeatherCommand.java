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

/**
 * author: Angelic47
 * Nukkit Project
 */
public class WeatherCommand extends Command {

    public WeatherCommand() {
        super("weather", CommandData.builder("weather")
                .setDescription("commands.weather.description")
                .setUsageMessage("/weather <clear|rain|thunder> [time]")
                .setPermissions("cloudburst.command.weather")
                .setParameters(new CommandParameter[]{
                        new CommandParameter("clear|rain|thunder", CommandParamType.STRING, false),
                        new CommandParameter("duration in seconds", CommandParamType.INT, true)
                }).build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }
        if (args.length == 0 || args.length > 2) {
            return false;
        }

        String weather = args[0];
        World world;
        int seconds;
        if (args.length > 1) {
            try {
                seconds = Integer.parseInt(args[1]);
            } catch (Exception e) {
                return false;
            }
        } else {
            seconds = 600 * 20;
        }

        if (sender instanceof Player) {
            world = ((Player) sender).getWorld();
        } else {
            world = sender.getServer().getDefaultWorld();
        }

        switch (weather) {
            case "clear":
                world.setRaining(false);
                world.setThundering(false);
                world.setRainTime(seconds * 20);
                world.setThunderTime(seconds * 20);
                CommandUtils.broadcastCommandMessage(sender,
                        new TranslationContainer("%commands.weather.clear"));
                return true;
            case "rain":
                world.setRaining(true);
                world.setRainTime(seconds * 20);
                CommandUtils.broadcastCommandMessage(sender,
                        new TranslationContainer("%commands.weather.rain"));
                return true;
            case "thunder":
                world.setThundering(true);
                world.setRainTime(seconds * 20);
                world.setThunderTime(seconds * 20);
                CommandUtils.broadcastCommandMessage(sender,
                        new TranslationContainer("%commands.weather.thunder"));
                return true;
            default:
                return false;
        }

    }
}
