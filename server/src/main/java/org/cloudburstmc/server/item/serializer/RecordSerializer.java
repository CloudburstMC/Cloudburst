package org.cloudburstmc.server.item.serializer;

import org.cloudburstmc.api.item.ItemTypes;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.item.data.Record;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

import java.util.Map;

public class RecordSerializer extends DefaultItemSerializer {

    private static final Map<Class<?>, Object> DEFAULT_VALUES;

    static {
        DEFAULT_VALUES = Map.of(Record.class, Record.CAT);
    }

    @Override
    public void serialize(ItemStack item, NbtMapBuilder itemTag) {
        super.serialize(item, itemTag);
        Identifier id;
        Record value = item.get(ItemKeys.RECORD_TYPE);

        id = switch (value) {
            case FAR -> ItemTypes.MUSIC_DISC_FAR.getId();
            case MALL -> ItemTypes.MUSIC_DISC_MALL.getId();
            case STAL -> ItemTypes.MUSIC_DISC_STAL.getId();
            case WAIT -> ItemTypes.MUSIC_DISC_WAIT.getId();
            case WARD -> ItemTypes.MUSIC_DISC_WARD.getId();
            case CHIRP -> ItemTypes.MUSIC_DISC_CHIRP.getId();
            case STRAD -> ItemTypes.MUSIC_DISC_STRAD.getId();
            case BLOCKS -> ItemTypes.MUSIC_DISC_BLOCKS.getId();
            case ELEVEN -> ItemTypes.MUSIC_DISC_11.getId();
            case MELLOHI -> ItemTypes.MUSIC_DISC_MELLOHI.getId();
            case THIRTEEN -> ItemTypes.MUSIC_DISC_13.getId();
            case PIGSTEP -> ItemTypes.MUSIC_DISC_PIGSTEP.getId();
            case FIVE -> ItemTypes.MUSIC_DISC_5.getId();
            default -> ItemTypes.MUSIC_DISC_CAT.getId();
        };

        itemTag.putString(NAME_TAG, id.toString());
    }

    @Override
    public void deserialize(Identifier id, short meta, ItemStackBuilder builder, NbtMap tag) {
        super.deserialize(id, meta, builder, tag);
        if (ItemTypes.MUSIC_DISC_13.getId().equals(id)) {
            builder.data(ItemKeys.RECORD_TYPE, Record.THIRTEEN);
            return;
        }

        if (ItemTypes.MUSIC_DISC_CAT.getId().equals(id)) {
            builder.data(ItemKeys.RECORD_TYPE, Record.CAT);
            return;
        }

        if (ItemTypes.MUSIC_DISC_BLOCKS.getId().equals(id)) {
            builder.data(ItemKeys.RECORD_TYPE, Record.BLOCKS);
            return;
        }

        if (ItemTypes.MUSIC_DISC_CHIRP.getId().equals(id)) {
            builder.data(ItemKeys.RECORD_TYPE, Record.CHIRP);
            return;
        }

        if (ItemTypes.MUSIC_DISC_FAR.getId().equals(id)) {
            builder.data(ItemKeys.RECORD_TYPE, Record.FAR);
            return;
        }

        if (ItemTypes.MUSIC_DISC_MALL.getId().equals(id)) {
            builder.data(ItemKeys.RECORD_TYPE, Record.MALL);
            return;
        }

        if (ItemTypes.MUSIC_DISC_MELLOHI.getId().equals(id)) {
            builder.data(ItemKeys.RECORD_TYPE, Record.MELLOHI);
            return;
        }

        if (ItemTypes.MUSIC_DISC_STAL.getId().equals(id)) {
            builder.data(ItemKeys.RECORD_TYPE, Record.STAL);
            return;
        }

        if (ItemTypes.MUSIC_DISC_STRAD.getId().equals(id)) {
            builder.data(ItemKeys.RECORD_TYPE, Record.STRAD);
            return;
        }

        if (ItemTypes.MUSIC_DISC_WARD.getId().equals(id)) {
            builder.data(ItemKeys.RECORD_TYPE, Record.WARD);
            return;
        }

        if (ItemTypes.MUSIC_DISC_11.getId().equals(id)) {
            builder.data(ItemKeys.RECORD_TYPE, Record.ELEVEN);
            return;
        }

        if (ItemTypes.MUSIC_DISC_WAIT.getId().equals(id)) {
            builder.data(ItemKeys.RECORD_TYPE, Record.WAIT);
            return;
        }

        if (ItemTypes.MUSIC_DISC_PIGSTEP.getId().equals(id)) {
            builder.data(ItemKeys.RECORD_TYPE, Record.PIGSTEP);
            return;
        }

        if (ItemTypes.MUSIC_DISC_5.getId().equals(id)) {
            builder.data(ItemKeys.RECORD_TYPE, Record.FIVE);
        }

        builder.data(ItemKeys.RECORD_TYPE, Record.THIRTEEN);
    }

    @Override
    public Map<Class<?>, Object> getDefaultMetadataValues() {
        return DEFAULT_VALUES;
    }
}
