package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.Server;
import org.cloudburstmc.api.ban.BanList;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.player.OfflinePlayer;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.CommandUtils;

import java.util.Locale;
import java.util.UUID;

public class PardonCommand extends AdvertisedCommand {

    public PardonCommand() {
        super("pardon", "Removes a player ban", "cloudburst.command.pardon");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.then(Commands.argument("player", CommandArgumentTypes.string())
                .suggests((context, suggestions) -> {
                    Server server = sender(context).getServer();
                    server.getPlayerBanList().getBanEntries().stream()
                            .map(entry -> CommandUtils.displayName(server.getOfflinePlayer(entry.target()), entry.target().toString()))
                            .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(suggestions.getRemainingLowerCase()))
                            .forEach(suggestions::suggest);
                    return suggestions.buildFuture();
                }).executes(this::executeCommand));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        Server server = sender(context).getServer();
        String target = argumentValue(context, "player");
        UUID targetId;
        try {
            targetId = UUID.fromString(target);
        } catch (IllegalArgumentException e) {
            targetId = null;
        }

        OfflinePlayer player = targetId == null ? server.getOfflinePlayer(target) : server.getOfflinePlayer(targetId);
        UUID uuid = player.getUniqueId();
        BanList<UUID> bans = server.getPlayerBanList();
        if (uuid == null || !bans.isBanned(uuid)) {
            return failure(context, Component.translatable("commands.unban.failed", Component.text(target)));
        }

        bans.pardon(uuid);
        return broadcastSuccess(context, Component.translatable("commands.unban.success", Component.text(CommandUtils.displayName(player, target))));
    }
}
