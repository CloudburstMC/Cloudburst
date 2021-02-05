package org.cloudburstmc.server.command.defaults;

import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.locale.TranslationContainer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class PardonIpCommand extends Command {

    public PardonIpCommand() {
        super("pardon-ip", CommandData.builder("pardon-ip")
                .setPermissions("cloudburst.command.unban.ip")
                .setAliases("unbanip", "unban-ip", "pardonip")
                .setUsageMessage("/unbanip <ip>")
                .setParameters(new CommandParameter[]{
                        new CommandParameter("ip")
                })
                .build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }

        if (args.length != 1) {
            return false;
        }

        String value = args[0];

        if (Pattern.matches("^(25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9])\\.(25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]{1}[0-9]{2}|[1-9]{1}[0-9]{1}|[0-9])$", value)) {
            sender.getServer().getIPBans().remove(value);

            try {
                sender.getServer().getNetwork().unblockAddress(InetAddress.getByName(value));
            } catch (UnknownHostException e) {
                sender.sendMessage(new TranslationContainer("commands.unbanip.invalid"));
                return true;
            }

            CommandUtils.broadcastCommandMessage(sender, new TranslationContainer("%commands.unbanip.success", value));
        } else {

            sender.sendMessage(new TranslationContainer("commands.unbanip.invalid"));
        }

        return true;
    }
}
