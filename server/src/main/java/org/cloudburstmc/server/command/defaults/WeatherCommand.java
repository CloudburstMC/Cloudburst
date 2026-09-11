package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.network.CommandNetworkData;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.player.CloudPlayer;

public class WeatherCommand extends AdvertisedCommand {

    public WeatherCommand() {
        super("weather", "commands.weather.description", CommandNetworkData.GAME_DIRECTORS,
                "cloudburst.command.weather");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.then(Commands.argument("type", CommandArgumentTypes.fixedEnumNamed("type", "WeatherType", "clear", "rain", "thunder"))
                .executes(this::executeCommand)
                .then(Commands.argument("duration", CommandArgumentTypes.integer(1, 1_000_000))
                        .executes(this::executeCommand)));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        CommandSender sender = sender(context);
        String weather = argumentValue(context, "type");
        CloudLevel level;
        int seconds = hasArgument(context, "duration") ? argumentValue(context, "duration", Integer.class) : 600;

        if (sender instanceof CloudPlayer) {
            level = ((CloudPlayer) sender).getLevel();
        } else {
            level = (CloudLevel) sender.getServer().getDefaultLevel();
        }

        switch (weather) {
            case "clear":
                level.setRaining(false);
                level.setThundering(false);
                level.setRainTime(seconds * 20);
                level.setThunderTime(seconds * 20);
                CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.weather.clear"));
                return success();
            case "rain":
                level.setRaining(true);
                level.setRainTime(seconds * 20);
                CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.weather.rain"));
                return success();
            case "thunder":
                level.setThundering(true);
                level.setRainTime(seconds * 20);
                level.setThunderTime(seconds * 20);
                CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.weather.thunder"));
                return success();
            default:
                return usage();
        }
    }
}
