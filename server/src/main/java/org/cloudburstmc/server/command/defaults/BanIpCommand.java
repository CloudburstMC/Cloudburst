package org.cloudburstmc.server.command.defaults;

import com.google.common.net.InetAddresses;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.ban.BanEntry;
import org.cloudburstmc.api.ban.BanList;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.player.KickReason;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.command.AdvertisedCommand;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;

public class BanIpCommand extends AdvertisedCommand {

    public BanIpCommand() {
        super("ban-ip", "commands.banip.description", "cloudburst.command.ban-ip");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.then(Commands.argument("target", CommandArgumentTypes.string())
                .executes(this::executeCommand)
                .then(Commands.argument("reason", CommandArgumentTypes.message()).executes(this::executeCommand)));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        Server server = sender(context).getServer();
        String target = argumentValue(context, "target");
        InetAddress address = resolveAddress(server, target);
        if (address == null) {
            return failure(context, Component.translatable("commands.banip.invalid"));
        }

        BanList<InetAddress> bans = server.getIpBanList();
        if (bans.isBanned(address)) {
            return failure(context, Component.text("IP address is already banned: " + InetAddresses.toAddrString(address)));
        }

        List<? extends Player> affected = server.getOnlinePlayers().values().stream()
                .filter(player -> {
                    InetSocketAddress remote = player.getAddress();
                    return remote != null && address.equals(remote.getAddress());
                }).toList();
        String reason = hasArgument(context, "reason") ? argumentValue(context, "reason") : null;
        BanEntry<InetAddress> entry = bans.addBan(address, reason, null, sender(context).getName());

        context.getSource().broadcastSuccess(Component.translatable("commands.banip.success",
                Component.text(InetAddresses.toAddrString(address))).append(Component.text(": " + entry.reason())));
        for (Player player : affected) {
            player.kick(Component.text(entry.reason()), KickReason.IP_BANNED);
        }

        return affected.size();
    }

    private static InetAddress resolveAddress(Server server, String target) {
        if (InetAddresses.isInetAddress(target)) {
            return InetAddresses.forString(target);
        }

        return server.getOnlinePlayers().values().stream()
                .filter(player -> player.getName().equalsIgnoreCase(target))
                .map(Player::getAddress).filter(Objects::nonNull)
                .map(InetSocketAddress::getAddress).filter(Objects::nonNull)
                .findFirst().orElse(null);
    }
}
