package org.cloudburstmc.server.command.defaults;

import com.nukkitx.protocol.bedrock.data.command.CommandParamType;
import com.nukkitx.protocol.bedrock.packet.SetDifficultyPacket;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandSender;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.level.Difficulty;
import org.cloudburstmc.server.locale.TranslationContainer;

import java.util.ArrayList;

/**
 * Created on 2015/11/12 by xtypr.
 * Package cn.nukkit.command.defaults in project Nukkit .
 */
public class DifficultyCommand extends Command {

    public DifficultyCommand() {
        super("difficulty", CommandData.builder("difficulty")
                .setDescription("commands.difficulty.description")
                .setUsageMessage("commands.difficulty.usage")
                .setPermissions("cloudburst.command.difficulty")
                .setParameters(new CommandParameter[]{
                        new CommandParameter("difficulty", CommandParamType.INT, false)
                }, new CommandParameter[]{
                        new CommandParameter("difficulty", new String[]{"peaceful", "p", "easy", "e",
                                "normal", "n", "hard", "h"})
                })
                .build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }

        if (args.length != 1) {
            return false;
        }

        Difficulty difficulty = Difficulty.fromString(args[0]);

        if (sender.getServer().isHardcore()) {
            difficulty = Difficulty.HARD;
        }

        if (difficulty != null) {
            sender.getServer().getConfig().setDifficulty(difficulty);

            SetDifficultyPacket packet = new SetDifficultyPacket();
            packet.setDifficulty(sender.getServer().getDifficulty().ordinal());
            CloudServer.broadcastPacket(new ArrayList<>(sender.getServer().getOnlinePlayers().values()), packet);

            CommandUtils.broadcastCommandMessage(sender, new TranslationContainer("%commands.difficulty.success", String.valueOf(difficulty)));
        } else {
            return false;
        }

        return true;
    }
}
