package org.cloudburstmc.server.command.defaults;

import com.nukkitx.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.player.GameMode;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.utils.TextFormat;

/**
 * Created on 2015/11/13 by xtypr.
 * Package cn.nukkit.command.defaults in project Nukkit .
 */
public class GamemodeCommand extends Command {

    public GamemodeCommand() {
        super("gamemode", CommandData.builder("gamemode")
                .setDescription("commands.gamemode.description")
                .setUsageMessage("/gamemode <mode> [player]")
                .setAliases("gm")
                .addPermission("cloudburst.command.gamemode.survival")
                .addPermission("cloudburst.command.gamemode.creative")
                .addPermission("cloudburst.command.gamemode.adventure")
                .addPermission("cloudburst.command.gamemode.spectator")
                .addPermission("cloudburst.command.gamemode.other")
                .setParameters(
                        new CommandParameter[]{
                                new CommandParameter("mode", CommandParamType.INT, false),
                                new CommandParameter("player", CommandParamType.TARGET, true)
                        }, new CommandParameter[]{
                                new CommandParameter("mode", new String[]{"survival", "s", "creative", "c",
                                        "adventure", "a", "spectator", "spc", "view", "v"}),
                                new CommandParameter("player", CommandParamType.TARGET, true)
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

        GameMode gameMode = GameMode.from(args[0].toLowerCase());
        if (gameMode == null) {
            sender.sendMessage("Unknown game mode"); //TODO: translate?
            return true;
        }

        CommandSender target = sender;
        if (args.length > 1) {
            if (sender.hasPermission("cloudburst.command.gamemode.other")) {
                target = (CommandSender) sender.getServer().getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.player.notFound"));
                    return true;
                }
            } else {
                sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.permission"));
                return true;
            }
        } else if (!(sender instanceof CloudPlayer)) {
            return false;
        }

        if (!sender.hasPermission("cloudburst.command.gamemode." + gameMode.getName())) {
            sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.permission"));
            return true;
        }

        if (!((CloudPlayer) target).setGamemode(gameMode)) {
            sender.sendMessage("Game mode update for " + target.getName() + " failed");
        } else {
            if (target.equals(sender)) {
                CommandUtils.broadcastCommandMessage(sender, new TranslationContainer("%commands.gamemode.success.self", gameMode.getTranslation()));
            } else {
                target.sendMessage(new TranslationContainer("gameMode.changed"));
                CommandUtils.broadcastCommandMessage(sender, new TranslationContainer("%commands.gamemode.success.other", target.getName(), gameMode.getTranslation()));
            }
        }

        return true;
    }
}
