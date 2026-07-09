package org.cloudburstmc.server.command.defaults;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.player.OfflinePlayer;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;

import java.util.Optional;
import java.util.UUID;

public class OpCommand extends Command {

    public OpCommand() {
        super("op", CommandData.builder("op")
                .setDescription("commands.op.description")
                .setUsageMessage("/op <player>")
                .setPermissions("cloudburst.command.op.give")
                .setParameters(new CommandParameter[]{
                        new CommandParameter("player", CommandParamType.TARGET, false)
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
        Optional<UUID> uuid = ((CloudServer) sender.getServer()).lookupName(name);

        CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.op.success",
                Component.text(name)));
        if (uuid.isPresent()) {
            OfflinePlayer player = sender.getServer().getOfflinePlayer(uuid.get());
            player.getPlayer().ifPresent(onlinePlayer -> onlinePlayer.sendMessage(
                    Component.translatable("commands.op.message").color(NamedTextColor.GRAY)));
            player.setOp(true);
        } else {
            sender.getServer().addOp(name);
        }

        return true;
    }
}
