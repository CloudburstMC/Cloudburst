package org.cloudburstmc.server.blockentity.impl;

import com.nukkitx.math.vector.Vector3i;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.blockentity.BlockEntityType;
import org.cloudburstmc.server.blockentity.Jukebox;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.ItemTypes;
import org.cloudburstmc.server.item.ItemUtils;
import org.cloudburstmc.server.item.data.Record;
import org.cloudburstmc.server.level.chunk.Chunk;

import java.util.EnumMap;
import java.util.Map;

import static com.nukkitx.math.vector.Vector3i.UP;

/**
 * @author CreeperFace
 */
public class JukeboxBlockEntity extends BaseBlockEntity implements Jukebox {

    private static final Map<Record, SoundEvent> SOUND_MAP = new EnumMap<>(Record.class);

    static {
        SOUND_MAP.put(Record.THIRTEEN, SoundEvent.RECORD_13);
        SOUND_MAP.put(Record.CAT, SoundEvent.RECORD_CAT);
        SOUND_MAP.put(Record.BLOCKS, SoundEvent.RECORD_BLOCKS);
        SOUND_MAP.put(Record.CHIRP, SoundEvent.RECORD_CHIRP);
        SOUND_MAP.put(Record.FAR, SoundEvent.RECORD_FAR);
        SOUND_MAP.put(Record.MALL, SoundEvent.RECORD_MALL);
        SOUND_MAP.put(Record.MELLOHI, SoundEvent.RECORD_MELLOHI);
        SOUND_MAP.put(Record.STAL, SoundEvent.RECORD_STAL);
        SOUND_MAP.put(Record.STRAD, SoundEvent.RECORD_STRAD);
        SOUND_MAP.put(Record.WARD, SoundEvent.RECORD_WARD);
        SOUND_MAP.put(Record.ELEVEN, SoundEvent.RECORD_11);
        SOUND_MAP.put(Record.WAIT, SoundEvent.RECORD_WAIT);
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
            tag.putCompound("RecordItem", ((CloudItemStack) this.recordItem).getNbt());
        }
    }

    @Override
    public boolean isValid() {
        return this.getBlockState().getType() == BlockTypes.JUKEBOX;
    }

    public ItemStack getRecordItem() {
        return recordItem;
    }

    public void setRecordItem(ItemStack recordItem) {
        this.recordItem = recordItem;
    }

    public void play() {
        if (this.recordItem.getType() == ItemTypes.RECORD) {
            this.getLevel().addLevelSoundEvent(this.getPosition(), SOUND_MAP.get(this.recordItem.getMetadata(Record.class)));
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
