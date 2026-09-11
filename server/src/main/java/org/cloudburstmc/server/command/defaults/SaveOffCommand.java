package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.network.CommandNetworkData;

public class SaveOffCommand extends AdvertisedCommand {

    public SaveOffCommand() {
        super("save-off", "commands.save.description", CommandNetworkData.OWNER, "cloudburst.command.save.disable");
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        CommandSender sender = sender(context);
        sender.getServer().setAutoSave(false);
        CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.save.disabled"));

        return success();
    }
}
