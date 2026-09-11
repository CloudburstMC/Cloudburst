package org.cloudburstmc.server.command.defaults;

import com.mojang.brigadier.context.CommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.cloudburstmc.api.command.CommandSender;
import org.cloudburstmc.api.command.CommandSourceStack;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.server.CloudServer;
import org.cloudburstmc.server.command.AdvertisedCommand;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.utils.ThreadCache;

public class GarbageCollectorCommand extends AdvertisedCommand {

    public GarbageCollectorCommand() {
        super("gc", "commands.gc.description", "cloudburst.command.gc");
    }

    @Override
    protected int execute(CommandContext<CommandSourceStack> context) {
        CommandSender sender = sender(context);
        int chunksCollected = 0;
        int entitiesCollected = 0;
        int tilesCollected = 0;
        long memory = Runtime.getRuntime().freeMemory();

        for (CloudLevel level : ((CloudServer) sender.getServer()).getLevels()) {
            int chunksCount = level.getChunkCount();
            int entitiesCount = level.getEntities().size();
            int tilesCount = level.getBlockEntities().size();
            level.doChunkGarbageCollection();
            chunksCollected += chunksCount - level.getChunkCount();
            entitiesCollected += entitiesCount - level.getEntities().size();
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
                .append(Component.text(GenericMath.round((freedMemory / 1024d / 1024d), 2) + " MB").color(NamedTextColor.RED)));
        return success();
    }
}
