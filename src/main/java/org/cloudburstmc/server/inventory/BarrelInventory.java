package org.cloudburstmc.server.inventory;

import com.nukkitx.protocol.bedrock.data.SoundEvent;
import lombok.val;
import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.Barrel;
import org.cloudburstmc.api.inventory.InventoryType;
import org.cloudburstmc.server.level.CloudLevel;
import org.cloudburstmc.server.player.CloudPlayer;

public class BarrelInventory extends CloudContainer {

    public BarrelInventory(Barrel barrelEntity) {
        super(barrelEntity, InventoryType.BARREL);
    }

    @Override
    public Barrel getHolder() {
        return (Barrel) this.holder;
    }

    @Override
    public void onOpen(CloudPlayer who) {
        super.onOpen(who);

        if (this.getViewers().size() == 1) {
            toggle(true);
        }
    }

    @Override
    public void onClose(CloudPlayer who) {
        super.onClose(who);

        if (this.getViewers().isEmpty()) {
            toggle(false);
        }
    }

    protected void toggle(boolean open) {
        Barrel barrel = this.getHolder();
        CloudLevel level = barrel.getLevel();
        if (level != null) {
            Block block = barrel.getBlock();
            val state = block.getState();

            if (state.getType() == BlockTypes.BARREL) {
                if (state.ensureTrait(BlockTraits.IS_OPEN) != open) {
                    block.set(state.withTrait(BlockTraits.IS_OPEN, open));
                    level.addLevelSoundEvent(this.getHolder().getPosition(), open ? SoundEvent.BARREL_OPEN : SoundEvent.BARREL_CLOSE);
                    sendBlockEventPacket(this.getHolder(), open ? 1 : 0);
                }
            }
        }
    }
}