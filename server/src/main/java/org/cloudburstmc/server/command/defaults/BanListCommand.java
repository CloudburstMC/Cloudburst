package org.cloudburstmc.server.command.defaults;

import com.google.common.net.InetAddresses;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.ban.BanEntry;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.CommandUtils;

import java.util.Collection;
import java.util.function.Function;

public class BanListCommand extends AdvertisedCommand {

    public BanListCommand() {
        super("banlist", "Lists active player and IP bans", "cloudburst.command.banlist");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.executes(this::executeCommand)
                .then(Commands.literal("players").executes(context -> executeCommand(context, this::players)))
                .then(Commands.literal("ips").executes(context -> executeCommand(context, this::ips)));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        return players(context) + ips(context);
    }

    private int players(CommandContext<CommandSourceStack> context) {
        Server server = sender(context).getServer();
        return show(context, "commands.banlist.players", server.getPlayerBanList().getBanEntries(), uuid -> CommandUtils.displayName(server.getOfflinePlayer(uuid), uuid.toString()));
    }

    private int ips(CommandContext<CommandSourceStack> context) {
        return show(context, "commands.banlist.ips", sender(context).getServer().getIpBanList().getBanEntries(), InetAddresses::toAddrString);
    }

    private static <T> int show(CommandContext<CommandSourceStack> context, String title, Collection<BanEntry<T>> entries, Function<T, String> displayName) {
        context.getSource().sendSuccess(Component.translatable(title, Component.text(entries.size())));
        for (BanEntry<T> entry : entries) {
            context.getSource().sendSuccess(Component.text(displayName.apply(entry.target()) + " | " + entry.source() + " | " + entry.reason()));
        }
        return entries.size();
    }
}
