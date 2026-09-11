package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.network.CommandNetworkData;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.player.CloudPlayer;

public class SaveCommand extends AdvertisedCommand {

    public SaveCommand() {
        super("save-all", "commands.save.description", CommandNetworkData.OWNER, "cloudburst.command.save.perform");
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        CommandSender sender = sender(context);
        CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.save.start"));

        for (CloudPlayer player : ((CloudServer) sender.getServer()).getOnlinePlayers().values()) {
            player.save();
        }

        for (CloudLevel level : ((CloudServer) sender.getServer()).getLevels()) {
            level.save(true);
        }

        CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.save.success"));
        return success();
    }
}
