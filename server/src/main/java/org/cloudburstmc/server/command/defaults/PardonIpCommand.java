package org.cloudburstmc.server.command.defaults;

import com.google.common.net.InetAddresses;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.ban.BanList;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.server.command.AdvertisedCommand;

import java.net.InetAddress;

public class PardonIpCommand extends AdvertisedCommand {

    public PardonIpCommand() {
        super("pardon-ip", "Removes an IP address ban", "cloudburst.command.pardon-ip");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.then(Commands.argument("target", CommandArgumentTypes.string())
                .suggests((context, suggestions) -> {
                    sender(context).getServer().getIpBanList().getBanEntries().stream()
                            .map(entry -> InetAddresses.toAddrString(entry.target()))
                            .filter(ip -> ip.startsWith(suggestions.getRemainingLowerCase()))
                            .forEach(suggestions::suggest);
                    return suggestions.buildFuture();
                }).executes(this::executeCommand));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        String target = argumentValue(context, "target");
        if (!InetAddresses.isInetAddress(target)) {
            return failure(context, Component.translatable("commands.unbanip.invalid"));
        }

        InetAddress address = InetAddresses.forString(target);
        BanList<InetAddress> bans = sender(context).getServer().getIpBanList();
        if (!bans.isBanned(address)) {
            return failure(context, Component.text("IP address is not banned: " + InetAddresses.toAddrString(address)));
        }

        bans.pardon(address);
        return broadcastSuccess(context, Component.translatable("commands.unbanip.success", Component.text(InetAddresses.toAddrString(address))));
    }
}
