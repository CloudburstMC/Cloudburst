package org.cloudburstmc.server.blockentity.impl;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.block.BlockIds;
import org.cloudburstmc.server.blockentity.BlockEntityType;
import org.cloudburstmc.server.blockentity.Jukebox;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.item.behavior.RecordItem;
import org.cloudburstmc.server.level.chunk.Chunk;
import org.cloudburstmc.server.utils.Identifier;

import java.util.IdentityHashMap;
import java.util.Map;

import static com.nukkitx.math.vector.Vector3i.UP;

/**
 * @author CreeperFace
 */
public class JukeboxBlockEntity extends BaseBlockEntity implements Jukebox {

    private static final Map<Identifier, SoundEvent> SOUND_MAP = new IdentityHashMap<>();

    static {
        SOUND_MAP.put(ItemIds.RECORD_13, SoundEvent.RECORD_13);
        SOUND_MAP.put(ItemIds.RECORD_CAT, SoundEvent.RECORD_CAT);
        SOUND_MAP.put(ItemIds.RECORD_BLOCKS, SoundEvent.RECORD_BLOCKS);
        SOUND_MAP.put(ItemIds.RECORD_CHIRP, SoundEvent.RECORD_CHIRP);
        SOUND_MAP.put(ItemIds.RECORD_FAR, SoundEvent.RECORD_FAR);
        SOUND_MAP.put(ItemIds.RECORD_MALL, SoundEvent.RECORD_MALL);
        SOUND_MAP.put(ItemIds.RECORD_MELLOHI, SoundEvent.RECORD_MELLOHI);
        SOUND_MAP.put(ItemIds.RECORD_STAL, SoundEvent.RECORD_STAL);
        SOUND_MAP.put(ItemIds.RECORD_STRAD, SoundEvent.RECORD_STRAD);
        SOUND_MAP.put(ItemIds.RECORD_WARD, SoundEvent.RECORD_WARD);
        SOUND_MAP.put(ItemIds.RECORD_11, SoundEvent.RECORD_11);
        SOUND_MAP.put(ItemIds.RECORD_WAIT, SoundEvent.RECORD_WAIT);
    }

    private ItemStack recordItem;

    public JukeboxBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForCompound("RecordItem", itemTag -> {
            this.recordItem = ItemUtils.deserializeItem(itemTag);
        });
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        if (this.recordItem != null && !this.recordItem.isNull()) {
            tag.putCompound("RecordItem", ItemUtils.serializeItem(this.recordItem).toBuilder().build());
        }
    }

    @Override
    public boolean isValid() {
        return this.getBlockState().getType() == BlockIds.JUKEBOX;
    }

    public ItemStack getRecordItem() {
        return recordItem;
    }

    public void setRecordItem(ItemStack recordItem) {
        this.recordItem = recordItem;
    }

    public void play() {
        if (this.recordItem instanceof RecordItem) {
            this.getLevel().addLevelSoundEvent(this.getPosition(), SOUND_MAP.get(this.recordItem.getId()));
        }
    }

    public void stop() {
        this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.STOP_RECORD);
    }

    public void dropItem() {
        if (this.recordItem != null && !this.recordItem.isNull()) {
            this.stop();
            this.getLevel().dropItem(this.getPosition().add(UP), this.recordItem);
            this.recordItem = null;
        }
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }
}
