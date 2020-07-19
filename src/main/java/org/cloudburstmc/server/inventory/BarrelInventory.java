package org.cloudburstmc.server.inventory;

import com.nukkitx.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.behavior.BlockBehaviorBarrel;
import org.cloudburstmc.server.blockentity.Barrel;
import org.cloudburstmc.server.level.Level;
import org.cloudburstmc.server.player.Player;

public class BarrelInventory extends ContainerInventory {

    public BarrelInventory(Barrel barrelEntity) {
        super(barrelEntity, InventoryType.BARREL);
    }

    @Override
    public Barrel getHolder() {
        return (Barrel) this.holder;
    }

    @Override
    public void onOpen(Player who) {
        super.onOpen(who);

        if (this.getViewers().size() == 1) {
            Barrel barrel = this.getHolder();
            Level level = barrel.getLevel();
            if (level != null) {
                BlockState blockState = barrel.getBlock();
                if (blockState instanceof BlockBehaviorBarrel) {
                    BlockBehaviorBarrel blockBarrel = (BlockBehaviorBarrel) blockState;
                    if (!blockBarrel.isOpen()) {
                        blockBarrel.setOpen(true);
                        level.addLevelSoundEvent(this.getHolder().getPosition(), SoundEvent.BARREL_OPEN);
                        sendBlockEventPacket(this.getHolder(), 1);
                    }
                }
            }
        }
    }

    @Override
    public void onClose(Player who) {
        super.onClose(who);

        if (this.getViewers().isEmpty()) {
            Barrel barrel = this.getHolder();
            Level level = barrel.getLevel();
            if (level != null) {
                BlockState blockState = barrel.getBlock();
                if (blockState instanceof BlockBehaviorBarrel) {
                    BlockBehaviorBarrel blockBarrel = (BlockBehaviorBarrel) blockState;
                    if (blockBarrel.isOpen()) {
                        blockBarrel.setOpen(false);
                        level.addLevelSoundEvent(this.getHolder().getPosition(), SoundEvent.BARREL_CLOSE);
                        sendBlockEventPacket(this.getHolder(), 0);
                    }
                }
            }
        }
    }
}