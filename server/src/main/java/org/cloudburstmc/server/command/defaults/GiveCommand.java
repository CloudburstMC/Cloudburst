package org.cloudburstmc.server.command.defaults;

import lombok.extern.log4j.Log4j2;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.item.ItemComponents;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemType;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudItemRegistry;

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

        CloudPlayer player = (CloudPlayer) sender.getServer().getPlayer(args[0]);
        CloudItemRegistry registry = CloudItemRegistry.get();
        ItemType type;

        try {
            try {
                type = registry.getType(Integer.parseInt(args[1]));
            } catch (NumberFormatException e) {
                type = registry.getType(Identifier.parse(args[1]));
            }
        } catch (Exception e) {
            log.throwing(e);
            return false;
        }

        int maxStackSize = registry.requireComponent(type, ItemComponents.GET_MAX_STACK_SIZE).execute(ItemStack.from(type, 1));
        ItemStack stack;
        try {
            stack = ItemStack.from(type, GenericMath.clamp(Integer.parseInt(args[2]), 1, maxStackSize));
        } catch (NumberFormatException e) {
            stack = ItemStack.from(type);
        }

        if (player != null) {
            if (stack.isEmpty()) {
                sender.sendMessage(Component.translatable("commands.give.item.invalid",
                        Component.text(args[1])).color(NamedTextColor.RED));
                return true;
            }
            player.getContainer().addItem(stack);
        } else {
            sender.sendMessage(Component.translatable("commands.generic.player.notFound").color(NamedTextColor.RED));
            return true;
        }
        Object customName = stack.get(ItemKeys.CUSTOM_NAME);
        String itemDisplay = customName == null
                ? type.getId().toString()
                : customName + " (" + type.getId() + ")";
        CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.give.success",
                Component.text(itemDisplay),
                Component.text(stack.getCount()),
                Component.text(player.getName())));
        return true;
    }
}
