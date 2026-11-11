package org.cloudburstmc.server.block.component;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.block.component.AttackBlockHandler;
import org.cloudburstmc.api.block.component.RedstoneSignalHandler;
import org.cloudburstmc.api.block.component.TickBlockHandler;
import org.cloudburstmc.api.block.component.UseBlockHandler;
import org.cloudburstmc.api.blockentity.BlockEntity;
import org.cloudburstmc.api.blockentity.BlockEntityTypes;
import org.cloudburstmc.api.event.player.PlayerInsertLecternBookEvent;
import org.cloudburstmc.api.event.player.PlayerTakeLecternBookEvent;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.blockentity.LecternBlockEntity;
import org.cloudburstmc.server.container.screen.CloudLecternContainerScreen;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.player.CloudPlayer;
import org.cloudburstmc.server.registry.CloudBlockEntityRegistry;

@UtilityClass
public class LecternBlockHandlers {

    public static final UseBlockHandler USE = (block, player, direction, item) -> {
        if (!(player instanceof CloudPlayer cloudPlayer)) {
            return false;
        }

        BlockEntity existing = block.getLevel().getBlockEntity(block.getPosition());
        if (existing == null && LecternBlockEntity.isBook(item)) {
            existing = CloudBlockEntityRegistry.get().newEntity(BlockEntityTypes.LECTERN, block);
        }

        if (!(existing instanceof LecternBlockEntity lectern)) {
            return false;
        }

        if (lectern.hasBook()) {
            cloudPlayer.getInventoryManager().openScreen(new CloudLecternContainerScreen(cloudPlayer, lectern));
            return true;
        }

        if (!LecternBlockEntity.isBook(item)) {
            return false;
        }

        PlayerInsertLecternBookEvent event = new PlayerInsertLecternBookEvent(cloudPlayer, lectern, item.withCount(1));
        cloudPlayer.getServer().getEventManager().fire(event);
        if (event.isCancelled() || lectern.isClosed() || lectern.hasBook() || !item.equals(cloudPlayer.getInventory().getSelectedItem()) || !LecternBlockEntity.isBook(event.getBook())) {
            return true;
        }

        if (!cloudPlayer.isCreative()) {
            cloudPlayer.getInventory().setSelectedItem(item.decreaseCount());
        }

        lectern.setBook(event.getBook());
        lectern.getLevel().addLevelSoundEvent(block.getPosition(), SoundEvent.LECTERN_BOOK_PLACE);

        return true;
    };

    public static final AttackBlockHandler ATTACK = (block, player, direction) -> {
        if (!(player instanceof CloudPlayer cloudPlayer)
                || !cloudPlayer.canInteractWithBlock(block.getPosition())
                || !(block.getLevel().getBlockEntity(block.getPosition()) instanceof LecternBlockEntity lectern)
                || !lectern.hasBook()) {
            return false;
        }

        ItemStack book = lectern.getBook();
        PlayerTakeLecternBookEvent event = new PlayerTakeLecternBookEvent(cloudPlayer, lectern, book);
        cloudPlayer.getServer().getEventManager().fire(event);
        if (!event.isCancelled() && !lectern.isClosed() && book.equals(lectern.getBook())) {
            lectern.setBook(ItemStack.EMPTY);
            if (!event.getBook().isEmpty()) {
                lectern.getLevel().dropItem(lectern.getPosition().toFloat().add(0.5f, 1, 0.5f),
                        event.getBook(), null, false, 10);
            }
        }

        return true;
    };

    public static final RedstoneSignalHandler GET_REDSTONE_SIGNAL = (block, face) -> block.getState().ensureTrait(BlockTraits.IS_POWERED) ? 15 : 0;

    public static final RedstoneSignalHandler GET_DIRECT_REDSTONE_SIGNAL = (block, face) -> face == Direction.UP ? GET_REDSTONE_SIGNAL.execute(block, face) : 0;

    public static final TickBlockHandler ON_TICK = (block, random) -> setPowered(block, false);

    public static void pulse(Block block) {
        if (block.getState().getType() != BlockTypes.LECTERN) {
            return;
        }

        setPowered(block, true);
        CloudLevel level = (CloudLevel) block.getLevel();
        level.scheduleUpdate(level.getBlock(block.getPosition()), 2);
    }

    private static void setPowered(Block block, boolean powered) {
        if (block.getState().ensureTrait(BlockTraits.IS_POWERED) == powered) {
            return;
        }

        CloudLevel level = (CloudLevel) block.getLevel();
        level.setBlockState(block.getPosition(), block.getState().withTrait(BlockTraits.IS_POWERED, powered), false, true);
        level.updateAround(block.getPosition().sub(0, 1, 0));
    }
}
