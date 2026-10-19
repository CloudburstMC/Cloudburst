package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.ban.BanEntry;
import org.cloudburstmc.api.ban.BanList;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.player.KickReason;
import org.cloudburstmc.api.player.OfflinePlayer;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.network.CommandNetworkData;

import java.util.UUID;

public class BanCommand extends AdvertisedCommand {

    public BanCommand() {
        super("ban", "commands.ban.description", CommandNetworkData.NOT_CHEAT, "cloudburst.command.ban");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.then(Commands.argument("player", CommandArgumentTypes.string())
                .executes(this::executeCommand)
                .then(Commands.argument("reason", CommandArgumentTypes.message())
                        .executes(this::executeCommand)));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        CommandSender sender = sender(context);
        String name = argumentValue(context, "player");
        OfflinePlayer player = sender.getServer().getOfflinePlayer(name);
        UUID uuid = player.getUniqueId();
        if (uuid == null) {
            return failure(context, Component.translatable("commands.generic.player.notFound"));
        }

        BanList<UUID> bans = sender.getServer().getPlayerBanList();
        if (bans.isBanned(uuid)) {
            return failure(context, Component.translatable("commands.ban.failed", Component.text(name)));
        }

        String reason = hasArgument(context, "reason") ? argumentValue(context, "reason") : null;
        BanEntry<UUID> entry = bans.addBan(uuid, reason, null, sender.getName());
        player.getPlayer().ifPresent(online -> online.kick(Component.text(entry.reason()), KickReason.PLAYER_BANNED));

        return broadcastSuccess(context, Component.translatable("commands.ban.success",
                        Component.text(CommandUtils.displayName(player, name)))
                .append(Component.text(": " + entry.reason())));
    }
}
