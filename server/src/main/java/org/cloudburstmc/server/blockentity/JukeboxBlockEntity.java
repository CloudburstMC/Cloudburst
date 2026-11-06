package org.cloudburstmc.server.blockentity;

import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.blockentity.Jukebox;
import org.cloudburstmc.api.blockentity.data.BlockEntityData;
import org.cloudburstmc.api.blockentity.data.JukeboxData;
import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.data.Record;
import org.cloudburstmc.api.level.chunk.Chunk;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.data.SoundEvent;

import java.util.EnumMap;
import java.util.Map;

import static org.cloudburstmc.math.vector.Vector3i.UP;

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
        SOUND_MAP.put(Record.PIGSTEP, SoundEvent.RECORD_PIGSTEP);
    }

    public JukeboxBlockEntity(BlockEntityType<?> type, Chunk chunk, Vector3i position) {
        super(type, chunk, position);
    }

    @Override
    protected Class<? extends BlockEntityData> minimumDataClass() {
        return JukeboxData.class;
    }

    @Override
    protected JukeboxData data() {
        return (JukeboxData) super.data();
    }

    @Override
    protected void loadCustom(NbtMap tag) {
        ItemStack loaded = data().getRecordItem();
        if (loaded != null && loaded.isEmpty()) {
            // An empty record compound carries no item; keep it absent so it stays omitted.
            data().setRecordItem(null);
        }
    }

    public ItemStack getRecordItem() {
        ItemStack recordItem = data().getRecordItem();
        return recordItem != null ? recordItem : ItemStack.EMPTY;
    }

    public void setRecordItem(ItemStack recordItem) {
        data().setRecordItem(recordItem == null || recordItem.isEmpty() ? null : recordItem);
    }

    public void play() {
        if (getRecordItem().getType() == ItemTypes.MUSIC_DISC_13) {

            this.getLevel().addLevelSoundEvent(this.getPosition(), SOUND_MAP.get(getRecordItem().get(ItemDataComponents.RECORD_TYPE)));
        }
    }

    public void stop() {
        this.getLevel().addLevelSoundEvent(this.getPosition(), SoundEvent.STOP_RECORD);
    }

    public void dropItem() {
        ItemStack recordItem = getRecordItem();
        if (!recordItem.isEmpty()) {
            this.stop();
            this.getLevel().dropItem(this.getPosition().add(UP), recordItem);
            data().setRecordItem(null);
        }
    }

    @Override
    public boolean isSpawnable() {
        return true;
    }

    @Override
    public void onBreak() {
        ItemStack recordItem = getRecordItem();
        if (!recordItem.isEmpty()) {
            this.stop();
            this.dropItemOnBreak(recordItem);
            data().setRecordItem(null);
        }
    }
}
