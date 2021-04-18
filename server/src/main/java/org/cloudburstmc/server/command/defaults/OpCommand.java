package org.cloudburstmc.server.command.defaults;

import com.nukkitx.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.utils.TextFormat;

import java.util.Optional;
import java.util.UUID;

/**
 * Created on 2015/11/12 by xtypr.
 * Package cn.nukkit.command.defaults in project Nukkit .
 */
public class OpCommand extends Command {

    public OpCommand() {
        super("op", CommandData.builder("op")
                .setDescription("commands.op.description")
                .setUsageMessage("/op <player>")
                .setPermissions("cloudburst.command.op.give")
                .setParameters(new CommandParameter[]{
                        new CommandParameter("player", CommandParamType.TARGET, false)
                })
                .build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }
        if (args.length == 0) {
            return false;
        }

        String name = args[0];
        Optional<UUID> uuid = ((CloudServer) sender.getServer()).lookupName(name);

        CommandUtils.broadcastCommandMessage(sender, new TranslationContainer("%commands.op.success", name));
        if (uuid.isPresent()) {
            Player player = ((CloudServer) sender.getServer()).getOfflinePlayer(uuid.get());
            if (player instanceof CloudPlayer) {
                ((CloudPlayer) player).sendMessage(new TranslationContainer(TextFormat.GRAY + "%commands.op.message"));
            }
            player.setOp(true);
        } else {
            sender.getServer().addOp(name);
        }

        return true;
    }
}
