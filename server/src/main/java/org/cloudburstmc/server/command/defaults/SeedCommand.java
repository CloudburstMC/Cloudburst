package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.player.CloudPlayer;

public class SeedCommand extends AdvertisedCommand {

    public SeedCommand() {
        super("seed", "", "cloudburst.command.seed");
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        CommandSender sender = sender(context);
        long seed;
        if (sender instanceof CloudPlayer) {
            seed = ((CloudPlayer) sender).getLevel().getSeed();
        } else {
            seed = sender.getServer().getDefaultLevel().getSeed();
        }

        sender.sendMessage(Component.translatable("commands.seed.success", Component.text(seed)));

        return success();
    }
}
