package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.network.CommandNetworkData;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.StringJoiner;

public class ListCommand extends AdvertisedCommand {

    public ListCommand() {
        super("list", "commands.list.description", CommandNetworkData.ANY_NOT_CHEAT,
                "cloudburst.command.list");
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        CommandSender sender = sender(context);
        StringJoiner online = new StringJoiner(", ");
        int onlineCount = 0;
        for (CloudPlayer player : ((CloudServer) sender.getServer()).getOnlinePlayers().values()) {
            if (player.isOnline() && (!(sender instanceof CloudPlayer) || ((CloudPlayer) sender).canSee(player))) {
                online.add(player.getName());
                ++onlineCount;
            }
        }

        sender.sendMessage(Component.translatable("commands.players.list",
                Component.text(onlineCount),
                Component.text(sender.getServer().getMaxPlayers())));
        sender.sendMessage(Component.text(online.toString()));

        return success();
    }
}
