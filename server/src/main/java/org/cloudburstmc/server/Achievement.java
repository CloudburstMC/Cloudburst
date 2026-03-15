package org.cloudburstmc.server;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.server.player.CloudPlayer;

import java.util.HashMap;

public record Achievement(String message, String... requires) {

    public static final HashMap<String, Achievement> achievements = new HashMap<String, Achievement>() {
        {
            put("mineWood", new Achievement("Getting Wood"));
            put("buildWorkBench", new Achievement("Benchmarking", "mineWood"));
            put("buildPickaxe", new Achievement("Time to Mine!", "buildWorkBench"));
            put("buildFurnace", new Achievement("Hot Topic", "buildPickaxe"));
            put("acquireIron", new Achievement("Acquire hardware", "buildFurnace"));
            put("buildHoe", new Achievement("Time to Farm!", "buildWorkBench"));
            put("makeBread", new Achievement("Bake Bread", "buildHoe"));
            put("bakeCake", new Achievement("The Lie", "buildHoe"));
            put("buildBetterPickaxe", new Achievement("Getting an Upgrade", "buildPickaxe"));
            put("buildSword", new Achievement("Time to Strike!", "buildWorkBench"));
            put("diamonds", new Achievement("DIAMONDS!", "acquireIron"));
        }
    };

    public static boolean broadcast(CloudPlayer player, String achievementId) {
        if (!achievements.containsKey(achievementId)) {
            return false;
        }
        Component achievementName = Component.text(achievements.get(achievementId).message()).color(NamedTextColor.GREEN);
        Component message = Component.translatable("chat.type.achievement", player.displayName(), achievementName);

        if (CloudServer.getInstance().getConfig().isAnnouncePlayerAchievements()) {
            CloudServer.getInstance().broadcastMessage(message);
        } else {
            player.sendMessage(message);
        }
        return true;
    }

    public static boolean add(String name, Achievement achievement) {
        if (achievements.containsKey(name)) {
            return false;
        }

        achievements.put(name, achievement);
        return true;
    }

    public void broadcast(CloudPlayer player) {
        Component achievementName = Component.text(this.message()).color(NamedTextColor.GREEN);
        Component message = Component.translatable("chat.type.achievement", player.displayName(), achievementName);

        if (CloudServer.getInstance().getConfig().isAnnouncePlayerAchievements()) {
            CloudServer.getInstance().broadcastMessage(message);
        } else {
            player.sendMessage(message);
        }
    }
}