package org.cloudburstmc.server.player;

import org.cloudburstmc.api.enchantment.EnchantmentTypes;
import org.cloudburstmc.api.inventory.view.ArmorView;
import org.cloudburstmc.api.inventory.view.PlayerInventoryView;
import org.cloudburstmc.api.inventory.view.SlotGroup;
import org.cloudburstmc.api.item.ItemBehaviors;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.server.registry.CloudItemRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class PlayerExperience {

    private int points;
    private int level;
    private int enchantmentSeed = ThreadLocalRandom.current().nextInt();

    public int points() {
        return this.points;
    }

    public int level() {
        return this.level;
    }

    public float progress() {
        return (float) this.points / pointsToNextLevel(this.level);
    }

    public int enchantmentSeed() {
        return this.enchantmentSeed;
    }

    public void load(int points, int level, int enchantmentSeed) {
        this.setProgress(points, level);
        this.enchantmentSeed = enchantmentSeed;
    }

    public void setProgress(int points, int level) {
        this.level = Math.max(0, level);
        this.points = Math.clamp(points, 0, pointsToNextLevel(this.level) - 1);
    }

    public void setLevel(int level) {
        int nextLevel = Math.max(0, level);
        this.points = (int) ((long) this.points * pointsToNextLevel(nextLevel) / pointsToNextLevel(this.level));
        this.level = nextLevel;
    }

    public void givePoints(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Experience points to give must be non-negative");
        }

        long remaining = (long) this.points + amount;
        int cost = pointsToNextLevel(this.level);
        while (remaining >= cost && this.level < Integer.MAX_VALUE) {
            remaining -= cost;
            cost = pointsToNextLevel(++this.level);
        }

        this.points = (int) Math.min(remaining, cost - 1L);
    }

    public void completeEnchantment(int levelsSpent) {
        if (levelsSpent > 0) {
            this.setLevel(this.level - levelsSpent);
        }
        this.enchantmentSeed = ThreadLocalRandom.current().nextInt();
    }

    public static int pointsToNextLevel(int level) {
        if (level >= 30) {
            return (int) Math.min(Integer.MAX_VALUE, 112 + (level - 30L) * 9);
        }

        if (level >= 15) {
            return 37 + (level - 15) * 5;
        }

        return 7 + level * 2;
    }

    public static int repairEquipment(CloudPlayer player, int experience) {
        while (experience > 0) {
            List<MendingSlot> slots = collectMendingSlots(player);
            if (slots.isEmpty()) {
                break;
            }

            MendingSlot slot = slots.get(ThreadLocalRandom.current().nextInt(slots.size()));
            ItemStack item = slot.item();
            int durability = (int) Math.min(Integer.MAX_VALUE, (long) experience * 2);
            int repair = Math.min(durability, item.getDamage());
            slot.group().setItem(slot.index(), item.repair(repair));
            experience -= (int) ((long) repair * experience / durability);
        }

        return experience;
    }

    private static List<MendingSlot> collectMendingSlots(CloudPlayer player) {
        List<MendingSlot> slots = new ArrayList<>();
        PlayerInventoryView inventory = player.getInventory();
        addMendingSlot(slots, inventory, inventory.getSelectedSlot());
        addMendingSlot(slots, player.getOffhand(), 0);

        ArmorView armor = player.getArmor();
        for (int index = 0; index < armor.size(); index++) {
            addMendingSlot(slots, armor, index);
        }

        return slots;
    }

    private static void addMendingSlot(List<MendingSlot> slots, SlotGroup group, int index) {
        ItemStack item = group.getItem(index);
        if (item.hasDamage() && CloudItemRegistry.get().requireComponent(item.getType(), ItemBehaviors.DAMAGEABLE).get()
                && item.getOrDefault(ItemDataComponents.ENCHANTMENTS, Map.of()).containsKey(EnchantmentTypes.MENDING)) {
            slots.add(new MendingSlot(group, index, item));
        }
    }

    private record MendingSlot(SlotGroup group, int index, ItemStack item) {
    }
}
