package org.cloudburstmc.server.inventory;

import org.cloudburstmc.api.block.Block;
import org.cloudburstmc.api.block.BlockTraits;
import org.cloudburstmc.api.block.BlockTypes;
import org.cloudburstmc.api.blockentity.Barrel;
import org.cloudburstmc.api.inventory.BarrelInventory;
import org.cloudburstmc.api.inventory.InventoryType;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.level.CloudLevel;

public class CloudBarrelInventory extends CloudContainer implements BarrelInventory {

    public CloudBarrelInventory(Barrel barrelEntity) {
        super(barrelEntity, InventoryType.BARREL);
    }

    @Override
    public Barrel getHolder() {
        return (Barrel) this.holder;
    }

    @Override
    public void onOpen(Player who) {
        super.onOpen(who);

        if (this.getListeners().size() == 1) {
            toggle(true);
        }
    }

    @Override
    public void onClose(Player who) {
        super.onClose(who);

        if (this.getListeners().isEmpty()) {
            toggle(false);
        }
    }

    protected void toggle(boolean open) {
        Barrel barrel = this.getHolder();
        CloudLevel level = (CloudLevel) barrel.getLevel();
        if (level != null) {
            Block block = barrel.getBlock();
            var state = block.getState();

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
