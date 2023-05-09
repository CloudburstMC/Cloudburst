package org.cloudburstmc.server.command.defaults;

import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.locale.TranslationContainer;

/**
 * Created on 2015/11/12 by xtypr.
 * Package cn.nukkit.command.defaults in project Nukkit .
 */
public class DefaultGamemodeCommand extends Command {

    public DefaultGamemodeCommand() {
        super("defaultgamemode", CommandData.builder("defaultgamemode")
                .setDescription("commands.defaultgamemode.description")
                .setUsageMessage("/defaultgamemode <mode>")
                .setPermissions("cloudburst.command.defaultgamemode")
                .setParameters(new CommandParameter[]{
                        new CommandParameter("mode", CommandParamType.INT, false)
                }, new CommandParameter[]{
                        new CommandParameter("mode", new String[]{"survival", "creative", "s", "c",
                                "adventure", "a", "spectator", "view", "v"})
                })
                .build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }
        if (args.length == 0) {
            return false;
        }
        try {
            GameMode gameMode = GameMode.from(args[0].toLowerCase());

            ((CloudServer)sender.getServer()).getConfig().setGamemode(gameMode);
            sender.sendMessage(new TranslationContainer("commands.defaultgamemode.success", gameMode.getTranslation()));
        } catch (IllegalArgumentException e) {
            sender.sendMessage("Unknown game mode"); //TODO: translate?
        }
        return true;
    }
}
