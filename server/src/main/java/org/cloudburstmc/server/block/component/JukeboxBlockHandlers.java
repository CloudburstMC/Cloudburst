package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.component.ComparatorOutputHandler;
import org.cloudburstmc.api.block.component.ComplexBlockHandler;
import org.cloudburstmc.api.block.component.RedstoneSignalHandler;
import org.cloudburstmc.api.block.component.UseBlockHandler;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.blockentity.Jukebox;
import org.cloudburstmc.server.blockentity.JukeboxBlockEntity;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;

@UtilityClass
public class JukeboxBlockHandlers {

    public static final ComplexBlockHandler ON_ADDED = block -> {
        if (block.getLevel().getBlockEntity(block.getPosition()) == null) {
            CloudBlockEntityRegistry.get().newEntity(BlockEntityTypes.JUKEBOX, block);
        }
    };

    public static final ComplexBlockHandler ON_REMOVE = block -> {
        if (block.getLevel().getBlockEntity(block.getPosition()) instanceof JukeboxBlockEntity jukebox
                && !jukebox.isValid()) {
            jukebox.onBreak();
        }
    };

    public static final UseBlockHandler USE = (block, player, face, item) -> {
        if (player == null || player.isSpectator()) {
            return false;
        }

        BlockEntity existing = block.getLevel().getBlockEntity(block.getPosition());
        if (existing == null) {
            if (JukeboxBlockEntity.songFor(item) == null) {
                return false;
            }
            existing = CloudBlockEntityRegistry.get().newEntity(BlockEntityTypes.JUKEBOX, block);
        }

        if (!(existing instanceof JukeboxBlockEntity jukebox) || jukebox.isClosed()) {
            return false;
        }

        if (jukebox.eject()) {
            return true;
        }

        if (JukeboxBlockEntity.songFor(item) == null || !item.equals(player.getInventory().getSelectedItem())) {
            return false;
        }

        if (!player.isCreative()) {
            player.getInventory().setSelectedItem(item.decreaseCount());
        }

        jukebox.setRecord(item);

        return true;
    };

    public static final ComparatorOutputHandler GET_COMPARATOR_OUTPUT = block ->
            block.getLevel().getBlockEntity(block.getPosition()) instanceof Jukebox jukebox ? jukebox.getComparatorOutput() : 0;

    public static final RedstoneSignalHandler GET_REDSTONE_SIGNAL = (block, face) ->
            block.getLevel().getBlockEntity(block.getPosition()) instanceof JukeboxBlockEntity jukebox && jukebox.isPlaying() ? 15 : 0;
}
