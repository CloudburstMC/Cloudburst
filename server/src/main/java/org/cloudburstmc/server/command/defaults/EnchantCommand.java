package org.cloudburstmc.server.command.defaults;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.enchantment.Enchantment;
import org.cloudburstmc.api.enchantment.EnchantmentType;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandUtils;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.EnchantmentRegistry;

import java.util.HashMap;
import java.util.Map;

public class EnchantCommand extends Command {

    public EnchantCommand() {
        super("enchant", CommandData.builder("enchant")
                .setDescription("commands.enchant.description")
                .setUsageMessage("/enchant <player> <enchantment> [level]")
                .setPermissions("cloudburst.command.enchant")
                .setParameters(
                        new CommandParameter[]{
                                new CommandParameter("player", CommandParamType.TARGET, false),
                                new CommandParameter("enchantment", CommandParamType.INT, false),
                                new CommandParameter("level", CommandParamType.INT, true)
                        }, new CommandParameter[]{
                                new CommandParameter("player", CommandParamType.TARGET, false),
                                new CommandParameter("enchantment", false, CommandParameter.ENUM_TYPE_ENCHANTMENT_LIST),
                                new CommandParameter("level", CommandParamType.INT, true)
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
        if (player == null) {
            sender.sendMessage(Component.translatable("commands.generic.player.notFound").color(NamedTextColor.RED));
            return true;
        }

        EnchantmentType enchantmentType;
        EnchantmentRegistry registry = EnchantmentRegistry.get();
        int enchantLevel;

        try {
            enchantmentType = getTypeByName(registry, args[1]);
            enchantLevel = args.length == 3 ? Integer.parseInt(args[2]) : 1;
        } catch (NumberFormatException e) {
            return false;
        }

        if (enchantmentType == null) {
            sender.sendMessage(Component.translatable("commands.enchant.notFound", Component.text(args[1])));
            return true;
        }

        ItemStack item = player.getInventory().getSelectedItem();
        if (item.isEmpty()) {
            sender.sendMessage(Component.translatable("commands.enchant.noItem", Component.text(String.valueOf(item.get(ItemKeys.CUSTOM_NAME)))));
            return true;
        }

        if (enchantLevel < 1 || enchantLevel > enchantmentType.maxLevel()) {
            sender.sendMessage(Component.translatable("commands.enchant.invalidLevel",
                    Component.text(enchantmentType.identifier().toString()), Component.text(enchantLevel)));
            return true;
        }

        Enchantment enchantment = registry.getEnchantment(enchantmentType, enchantLevel);
        if (!registry.canEnchant(enchantment, item)) {
            sender.sendMessage(Component.translatable("commands.enchant.cantEnchant",
                    Component.text(enchantmentType.identifier().toString())));
            return true;
        }

        Map<EnchantmentType, Enchantment> enchantments = new HashMap<>(item.get(ItemKeys.ENCHANTMENTS));
        enchantments.put(enchantmentType, new Enchantment(enchantmentType, enchantLevel));
        item = item.toBuilder().data(ItemKeys.ENCHANTMENTS, enchantments).build();

        player.getInventory().setSelectedItem(item);
        CommandUtils.broadcastCommandMessage(sender, Component.translatable("commands.enchant.success", Component.text(sender.getName())));
        return true;
    }

    private static @Nullable EnchantmentType getTypeByName(EnchantmentRegistry registry, String value) {
        String normalizedValue = value.toLowerCase();
        if ("durability".equals(normalizedValue)) {
            normalizedValue = "unbreaking";
        }

        EnchantmentType type;
        try {
            type = registry.getType(Identifier.parse(normalizedValue));
        } catch (IllegalArgumentException e) {
            type = null;
        }
        if (type != null) {
            return type;
        }
        try {
            return registry.getType(Short.parseShort(normalizedValue));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
