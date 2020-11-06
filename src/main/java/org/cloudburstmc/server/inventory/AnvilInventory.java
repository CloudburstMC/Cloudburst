package org.cloudburstmc.server.inventory;

import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.player.Player.CraftingType;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class AnvilInventory extends FakeBlockUIComponent {

    public static final int TARGET = 0;
    public static final int SACRIFICE = 1;
    public static final int RESULT = 50;

    public AnvilInventory(PlayerUIInventory playerUI, Block block) {
        super(playerUI, InventoryType.ANVIL, 1, block);
    }

    public boolean onRename(Player player, ItemStack resultItem) {
//        ItemStack local = getItem(TARGET); //TODO: anvil
//        ItemStack second = getItem(SACRIFICE);
//
//        if (!resultItem.equals(local, true, false) || resultItem.getCount() != local.getCount()) {
//            //Item does not match target item. Everything must match except the tags.
//            return false;
//        }
//
//        if (local.equals(resultItem)) {
//            //just item transaction
//            return true;
//        }
//
//        if (!local.isNull() && second.isNull()) { //only rename
//            local.setCustomName(resultItem.getCustomName());
//            setItem(RESULT, local);
//            player.getInventory().addItem(local);
//            clearAll();
//            player.getInventory().sendContents(player);
//            sendContents(player);
//
//            player.getLevel().addLevelSoundEvent(player.getPosition(), SoundEvent.RANDOM_ANVIL_USE);
//            return true;
//        } else if (!local.isNull() && !second.isNull()) { //enchants combining
//            if (!local.equals(second, true, false)) {
//                return false;
//            }
//
//            if (!local.isNull() && !second.isNull()) {
//                ItemStack result = local.clone();
//                int enchants = 0;
//
//                ArrayList<EnchantmentInstance> enchantments = new ArrayList<>(Arrays.asList(second.getEnchantments()));
//
//                ArrayList<EnchantmentInstance> baseEnchants = new ArrayList<>();
//
//                for (CloudEnchantmentInstance ench : local.getEnchantments()) {
//                    if (ench.getType().isMajor()) {
//                        baseEnchants.add(ench);
//                    }
//                }
//
//                for (CloudEnchantmentInstance enchantment : enchantments) {
//                    if (enchantment.getLevel() < 0 || enchantment.getId() < 0) {
//                        continue;
//                    }
//
//                    if (enchantment.isMajor()) {
//                        boolean same = false;
//                        boolean another = false;
//
//                        for (EnchantmentInstance baseEnchant : baseEnchants) {
//                            if (baseEnchant.getId() == enchantment.getId())
//                                same = true;
//                            else {
//                                another = true;
//                            }
//                        }
//
//                        if (!same && another) {
//                            continue;
//                        }
//                    }
//
//                    EnchantmentInstance localEnchantment = local.getEnchantment(enchantment.getId());
//
//                    if (localEnchantment != null) {
//                        int level = Math.max(localEnchantment.getLevel(), enchantment.getLevel());
//
//                        if (localEnchantment.getLevel() == enchantment.getLevel())
//                            level++;
//
//                        enchantment.setLevel(level);
//                        result.addEnchantment(enchantment);
//                        continue;
//                    }
//
//                    result.addEnchantment(enchantment);
//                    enchants++;
//                }
//
//                result.setCustomName(resultItem.getCustomName());
//
//                player.getInventory().addItem(result);
//                player.getInventory().sendContents(player);
//                clearAll();
//                sendContents(player);
//
//                player.getLevel().addLevelSoundEvent(player.getPosition(), SoundEvent.RANDOM_ANVIL_USE);
//                return true;
//            }
//        }

        return false;
    }

    @Override
    public void onClose(Player who) {
        super.onClose(who);
        who.craftingType = CraftingType.SMALL;
        who.resetCraftingGridType();

        for (int i = 0; i < 2; ++i) {
            this.getHolder().getLevel().dropItem(this.getHolder().getPosition().toFloat().add(0.5, 0.5, 0.5), this.getItem(i));
            this.clear(i);
        }
    }

    @Override
    public void onOpen(Player who) {
        super.onOpen(who);
        who.craftingType = CraftingType.ANVIL;
    }
}
