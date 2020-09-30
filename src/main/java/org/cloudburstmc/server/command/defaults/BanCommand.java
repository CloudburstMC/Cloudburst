package org.cloudburstmc.server.command.defaults;

import com.nukkitx.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandSender;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.event.player.PlayerKickEvent;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.player.Player;

import java.util.Arrays;
import java.util.StringJoiner;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class BanCommand extends Command {

    public BanCommand() {
        super("ban", CommandData.builder("ban")
                .setDescription("commands.ban.description")
                .setUsageMessage("/ban <player> [reason]")
                .setPermissions("cloudburst.command.ban.player")
                .setParameters(
                        new CommandParameter[]{
                                new CommandParameter("player", CommandParamType.TARGET, false),
                                new CommandParameter("reason", CommandParamType.STRING, true)
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

        String name = args[0];
        StringJoiner reason = new StringJoiner(" ");
        for (int i = 1; i < args.length; i++) {
            reason.add(args[i]);
        }

        sender.getServer().getNameBans().addBan(name, reason.toString(), null, sender.getName());

        Player player = sender.getServer().getPlayerExact(name);
        if (player != null) {
            player.kick(PlayerKickEvent.Reason.NAME_BANNED, !reason.toString().isEmpty() ? "Banned by admin. Reason: " + reason : "Banned by admin");
        }

        CommandUtils.broadcastCommandMessage(sender, new TranslationContainer("%commands.ban.success", player != null ? player.getName() : name));

        return true;
    }
}
