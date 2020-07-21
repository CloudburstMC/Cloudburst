package org.cloudburstmc.server.block.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.packet.LevelSoundEvent2Packet;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.item.Item;
import org.cloudburstmc.server.item.ItemTool;
import org.cloudburstmc.server.utils.BlockColor;
import org.cloudburstmc.server.utils.Identifier;

public class BlockBehaviorDoubleSlab extends BlockBehaviorSolid {

    private final Identifier slabId;
    private final BlockColor[] colors;

    protected BlockBehaviorDoubleSlab(Identifier slabId, BlockColor[] colors) {
        this.slabId = slabId;
        this.colors = colors;
    }

    public Identifier getSlabId() {
        return this.slabId;
    }

    @Override
    public float getResistance() {
        return 30;
    }

    @Override
    public float getHardness() {
        return 2;
    }

    @Override
    public int getToolType() {
        return ItemTool.TYPE_PICKAXE;
    }

    @Override
    public boolean canHarvestWithHand() {
        return false;
    }

    @Override
    public Item[] getDrops(BlockState blockState, Item hand) {
        if (hand.isPickaxe() && hand.getTier() >= ItemTool.TIER_WOODEN) {
            return new Item[]{
                    Item.get(this.slabId, this.getMeta() & 0x07, 2)
            };
        } else {
            return new Item[0];
        }
    }

    @Override
    public BlockColor getColor() {
        return colors[this.getMeta() & 0x7];
    }

    protected void playPlaceSound() {
        LevelSoundEvent2Packet pk = new LevelSoundEvent2Packet();
        pk.setSound(SoundEvent.ITEM_USE_ON);
        pk.setExtraData(725); // Who knows what this means? It's what is sent per ProxyPass
        pk.setPosition(Vector3f.from(this.getX() + 0.5f, this.getY() + 0.5f, this.getZ() + 0.5f));
        pk.setIdentifier("");
        pk.setBabySound(false);
        pk.setRelativeVolumeDisabled(false);


        this.getLevel().addChunkPacket(this.getChunk().getX(), this.getChunk().getZ(), pk);
    }
}
