package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.api.command.Commands;
import org.cloudburstmc.api.command.argument.CommandArguments;
import org.cloudburstmc.api.command.argument.CommandArgumentTypes;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentType;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.command.network.CommandNetworkData;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudEnchantmentRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnchantCommand extends AdvertisedCommand {

    public EnchantCommand() {
        super("enchant", "commands.enchant.description", CommandNetworkData.GAME_DIRECTORS,
                "cloudburst.command.enchant");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder, String label, CommandArguments arguments) {
        builder.then(Commands.argument("player", arguments.players())
                .then(Commands.argument("enchantmentName", arguments.enchantment())
                        .executes(this::executeCommand)
                        .then(Commands.argument("level", CommandArgumentTypes.integer(1))
                                .executes(this::executeCommand))));
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSender sender = sender(context);
        List<CloudPlayer> players = cloudPlayersArgument(context, "player");
        if (players.isEmpty()) {
            return success();
        }

        EnchantmentType enchantmentType = argumentValue(context, "enchantmentName", EnchantmentType.class);
        CloudEnchantmentRegistry registry = CloudEnchantmentRegistry.get();
        int enchantLevel = 1;
        if (hasArgument(context, "level")) {
            enchantLevel = argumentValue(context, "level", Integer.class);
        }

        if (enchantLevel > enchantmentType.maxLevel()) {
            sender.sendMessage(Component.translatable("commands.enchant.invalidLevel",
                    Component.text(enchantmentType.identifier().toString()), Component.text(enchantLevel)));
            return success();
        }

        Enchantment enchantment = registry.getEnchantment(enchantmentType, enchantLevel);
        for (CloudPlayer player : players) {
            ItemStack item = player.getInventory().getSelectedItem();
            if (item.isEmpty()) {
                sender.sendMessage(Component.translatable("commands.enchant.noItem",
                        Component.text(player.getName())));
                continue;
            }

            if (!registry.canEnchant(enchantment, item)) {
                sender.sendMessage(Component.translatable("commands.enchant.cantEnchant",
                        Component.text(enchantmentType.identifier().toString())));
                continue;
            }

            Map<EnchantmentType, Enchantment> enchantments = new HashMap<>(item.get(ItemKeys.ENCHANTMENTS));
            enchantments.put(enchantmentType, enchantment);
            player.getInventory().setSelectedItem(item.toBuilder()
                    .data(ItemKeys.ENCHANTMENTS, enchantments)
                    .build());
            CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.enchant.success",
                    Component.text(player.getName())));
        }

        return success();
    }
}
