package org.cloudburstmc.server.command.defaults;

import com.nukkitx.protocol.bedrock.data.command.CommandParamType;
import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandSender;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.CloudItemRegistry;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.TextFormat;

/**
 * Created on 2015/12/9 by xtypr.
 * Package cn.nukkit.command.defaults in project Nukkit .
 */
@Log4j2
public class GiveCommand extends Command {
    public GiveCommand() {
        super("give", CommandData.builder("give")
                .setDescription("commands.give.description")
                .setUsageMessage("commands.give.usage")
                .setPermissions("cloudburst.command.give")
                .setParameters(new CommandParameter[]{
                        new CommandParameter("player", CommandParamType.TARGET, false),
                        new CommandParameter("itemName", false, CommandParameter.ENUM_TYPE_ITEM_LIST),
                        new CommandParameter("amount", CommandParamType.INT, true),
                        new CommandParameter("meta", CommandParamType.INT, true),
                        new CommandParameter("tags...", CommandParamType.TEXT, true)
                }, new CommandParameter[]{
                        new CommandParameter("player", CommandParamType.TARGET, false),
                        new CommandParameter("item ID", CommandParamType.INT, false),
                        new CommandParameter("amount", CommandParamType.INT, true),
                        new CommandParameter("tags...", CommandParamType.TEXT, true)
                }, new CommandParameter[]{
                        new CommandParameter("player", CommandParamType.TARGET, false),
                        new CommandParameter("item ID:meta", CommandParamType.TEXT, false),
                        new CommandParameter("amount", CommandParamType.INT, true),
                        new CommandParameter("tags...", CommandParamType.TEXT, true)
                })
                .build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }

        if (args.length < 2) {
            return false;
        }

        Player player = sender.getServer().getPlayer(args[0]);
        ItemStack item;

        try {
            val registry = CloudItemRegistry.get();
            item = registry.getItem(registry.getType(Identifier.fromString(args[1])));
        } catch (Exception e) {
            log.throwing(e);
            return false;
        }

        try {
            item = item.withAmount(Integer.parseInt(args[2]));
        } catch (Exception e) {
            item = item.withAmount(item.getBehavior().getMaxStackSize(item));
        }

        if (player != null) {
            if (item.isNull()) {
                sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.give.item.invalid", args[1]));
                return true;
            }
            player.getInventory().addItem(item);
        } else {
            sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.player.notFound"));

            return true;
        }
        CommandUtils.broadcastCommandMessage(sender, new TranslationContainer(
                "%commands.give.success",
                item.getName() + " (" + item.getType() + ":" + ((CloudItemStack) item).getData() + ")",
                item.getAmount(),
                player.getName()));
        return true;
    }
}
