package org.cloudburstmc.server.command.defaults;

import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandSender;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.math.NukkitMath;
import org.cloudburstmc.server.utils.TextFormat;
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

        for (World world : sender.getServer().getWorlds()) {
            int chunksCount = world.getChunkCount();
            int entitiesCount = world.getEntities().length;
            int tilesCount = world.getBlockEntities().size();
            world.doChunkGarbageCollection();
            //world.unloadChunks(true);
            chunksCollected += chunksCount - world.getChunkCount();
            entitiesCollected += entitiesCount - world.getEntities().length;
            tilesCollected += tilesCount - world.getBlockEntities().size();
        }

        ThreadCache.clean();
        System.gc();

        long freedMemory = Runtime.getRuntime().freeMemory() - memory;

        sender.sendMessage(TextFormat.GREEN + "---- " + TextFormat.WHITE + "Garbage collection result" + TextFormat.GREEN + " ----");
        sender.sendMessage(TextFormat.GOLD + "Chunks: " + TextFormat.RED + chunksCollected);
        sender.sendMessage(TextFormat.GOLD + "Entities: " + TextFormat.RED + entitiesCollected);
        sender.sendMessage(TextFormat.GOLD + "Block Entities: " + TextFormat.RED + tilesCollected);
        sender.sendMessage(TextFormat.GOLD + "Memory freed: " + TextFormat.RED + NukkitMath.round((freedMemory / 1024d / 1024d), 2) + " MB");
        return true;
    }
}
