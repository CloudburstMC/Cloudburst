package org.cloudburstmc.server.command.defaults;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.math.NukkitMath;
import org.cloudburstmc.server.utils.ThreadCache;

/**
 * Created on 2015/11/11 by xtypr.
 * Package cn.nukkit.command.defaults in project Nukkit .
 */
public class GarbageCollectorCommand extends Command {

    public GarbageCollectorCommand() {
        super("gc", CommandData.builder("gc")
                .setDescription("commands.gc.description")
                .setUsageMessage("/gc")
                .setPermissions("cloudburst.command.gc")
                .build());
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }

        int chunksCollected = 0;
        int entitiesCollected = 0;
        int tilesCollected = 0;
        long memory = Runtime.getRuntime().freeMemory();

        for (CloudLevel level : ((CloudServer) sender.getServer()).getLevels()) {
            int chunksCount = level.getChunkCount();
            int entitiesCount = level.getEntities().length;
            int tilesCount = level.getBlockEntities().size();
            level.doChunkGarbageCollection();
            chunksCollected += chunksCount - level.getChunkCount();
            entitiesCollected += entitiesCount - level.getEntities().length;
            tilesCollected += tilesCount - level.getBlockEntities().size();
        }

        ThreadCache.clean();
        System.gc();

        long freedMemory = Runtime.getRuntime().freeMemory() - memory;

        sender.sendMessage(Component.text("Garbage collection result").color(NamedTextColor.WHITE));
        sender.sendMessage(Component.text("Chunks: ").color(NamedTextColor.GOLD)
                .append(Component.text(chunksCollected).color(NamedTextColor.RED)));
        sender.sendMessage(Component.text("Entities: ").color(NamedTextColor.GOLD)
                .append(Component.text(entitiesCollected).color(NamedTextColor.RED)));
        sender.sendMessage(Component.text("Block Entities: ").color(NamedTextColor.GOLD)
                .append(Component.text(tilesCollected).color(NamedTextColor.RED)));
        sender.sendMessage(Component.text("Memory freed: ").color(NamedTextColor.GOLD)
                .append(Component.text(NukkitMath.round((freedMemory / 1024d / 1024d), 2) + " MB").color(NamedTextColor.RED)));
        return true;
    }
}
