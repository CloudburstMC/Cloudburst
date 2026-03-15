package org.cloudburstmc.server.command.defaults;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.Objects;
import java.util.StringJoiner;

public class TellCommand extends Command {

    public TellCommand() {
        super("tell", CommandData.builder("tell")
                .setDescription("commands.tell.description")
                .setUsageMessage("/tell <player> <message>")
                .setAliases("w", "msg")
                .setPermissions("cloudburst.command.tell")
                .setParameters(new CommandParameter[]{
                        new CommandParameter("player", CommandParamType.TARGET, false),
                        new CommandParameter("message")
                })
                .build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }

        if (args.length < 2) {
            return false;
        }

        String name = args[0].toLowerCase();

        CloudPlayer player = (CloudPlayer) sender.getServer().getPlayer(name);
        if (player == null) {
            sender.sendMessage(Component.translatable("commands.generic.player.notFound"));
            return true;
        }

        if (Objects.equals(player, sender)) {
            sender.sendMessage(Component.translatable("commands.message.sameTarget").color(NamedTextColor.RED));
            return true;
        }

        StringJoiner msg = new StringJoiner(" ");
        for (int i = 1; i < args.length; i++) {
            msg.add(args[i]);
        }

        Component senderDisplayName = (sender instanceof CloudPlayer) ? ((CloudPlayer) sender).displayName() : sender.name();

        sender.sendMessage(Component.translatable("commands.message.display.outgoing", player.displayName(), Component.text(msg.toString())));
        player.sendMessage(Component.translatable("commands.message.display.incoming", senderDisplayName, Component.text(msg.toString())));

        return true;
    }
}
