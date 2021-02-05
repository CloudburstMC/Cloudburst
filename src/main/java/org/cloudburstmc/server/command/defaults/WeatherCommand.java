package org.cloudburstmc.server.command.defaults;

import com.nukkitx.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandSender;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.player.CloudPlayer;

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
        CloudLevel level;
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

        if (sender instanceof CloudPlayer) {
            level = ((CloudPlayer) sender).getLevel();
        } else {
            level = sender.getServer().getDefaultLevel();
        }

        switch (weather) {
            case "clear":
                level.setRaining(false);
                level.setThundering(false);
                level.setRainTime(seconds * 20);
                level.setThunderTime(seconds * 20);
                CommandUtils.broadcastCommandMessage(sender,
                        new TranslationContainer("%commands.weather.clear"));
                return true;
            case "rain":
                level.setRaining(true);
                level.setRainTime(seconds * 20);
                CommandUtils.broadcastCommandMessage(sender,
                        new TranslationContainer("%commands.weather.rain"));
                return true;
            case "thunder":
                level.setThundering(true);
                level.setRainTime(seconds * 20);
                level.setThunderTime(seconds * 20);
                CommandUtils.broadcastCommandMessage(sender,
                        new TranslationContainer("%commands.weather.thunder"));
                return true;
            default:
                return false;
        }

    }
}
