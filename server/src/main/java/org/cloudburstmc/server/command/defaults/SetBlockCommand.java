package org.cloudburstmc.server.command.defaults;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.registry.BlockRegistry;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.block.util.BlockStateMetaMappings;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.utils.TextFormat;

public class SetBlockCommand extends Command {

    public SetBlockCommand() {
        super("setblock", CommandData.builder("setblock")
                .setDescription("commands.setblock.description")
                .setUsageMessage("/setblock <position> <tileName> [tileData] [replace|destroy|keep]")
                .setPermissions("cloudburst.command.setblock")
                .setParameters(
                        new CommandParameter[]{
                                new CommandParameter("position", CommandParamType.BLOCK_POSITION, false),
                                new CommandParameter("Block", new String[]{}),
                                new CommandParameter("tileData", CommandParamType.INT, true),
                                new CommandParameter("oldBlockHandling", true, new String[]{"replace", "destroy", "keep"})
                        })
                .build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(new TranslationContainer("commands.locate.fail.noplayer"));
            return true;
        }

        if (args.length < 4) {
            return false;
        }

        Player p = (Player) sender;
        Vector3i pos = CommandUtils.parseVector3f(args, p.getPosition()).map(v -> v.floor().toInt()).orElse(null);

        if (pos == null) {
            return false;
        }

        if (pos.getY() < 0 || pos.getY() > 255) {
            sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.setblock.outOfWorld"));
            return true;
        }

        Identifier id = Identifier.fromString(args[3]);
        int meta;
        if (args.length >= 5) {
            try {
                meta = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                return false;
            }
        } else {
            meta = 0;
        }

        BlockState state = BlockStateMetaMappings.getStateFromMeta(id, meta);

        if (state == null) {
            sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.setblock.notFound", args[3]));
            return true;
        }

        SetType setType;

        if (args.length >= 6) {
            try {
                setType = SetType.valueOf(args[5]);
            } catch (IllegalArgumentException e) {
                return false;
            }
        } else {
            setType = SetType.REPLACE;
        }

        if (setType != SetType.REPLACE) {
            BlockState existing = p.getLevel().getBlockState(pos);

            if (existing != BlockStates.AIR) {
                if (setType == SetType.DESTROY) {
                    p.getLevel().useBreakOn(pos);
                } else {
                    sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.setblock.noChange"));
                    return true;
                }
            }
        }

        p.getLevel().setBlockState(pos, state);
        sender.sendMessage(new TranslationContainer("%commands.setblock.success"));

        return true;
    }

    public enum SetType {
        REPLACE,
        DESTROY,
        KEEP
    }
}
