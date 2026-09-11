package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.network.CommandNetworkData;

public class SaveOnCommand extends AdvertisedCommand {

    public SaveOnCommand() {
        super("save-on", "commands.save-on.description", CommandNetworkData.OWNER, "cloudburst.command.save.enable");
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        CommandSender sender = sender(context);
        sender.getServer().setAutoSave(true);
        CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.save.enabled"));

        return success();
    }
}
