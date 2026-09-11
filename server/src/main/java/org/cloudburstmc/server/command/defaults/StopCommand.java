package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.network.CommandNetworkData;

public class StopCommand extends AdvertisedCommand {

    public StopCommand() {
        super("stop", "commands.stop.description", CommandNetworkData.OWNER, "cloudburst.command.stop");
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        CommandSender sender = sender(context);
        CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.stop.start"));
        sender.getServer().shutdown();

        return success();
    }
}
