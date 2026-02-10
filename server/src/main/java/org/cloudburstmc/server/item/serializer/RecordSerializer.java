package org.cloudburstmc.server.item.serializer;

import org.cloudburstmc.api.item.ItemIds;
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
            case FAR -> ItemIds.MUSIC_DISC_FAR;
            case MALL -> ItemIds.MUSIC_DISC_MALL;
            case STAL -> ItemIds.MUSIC_DISC_STAL;
            case WAIT -> ItemIds.MUSIC_DISC_WAIT;
            case WARD -> ItemIds.MUSIC_DISC_WARD;
            case CHIRP -> ItemIds.MUSIC_DISC_CHIRP;
            case STRAD -> ItemIds.MUSIC_DISC_STRAD;
            case BLOCKS -> ItemIds.MUSIC_DISC_BLOCKS;
            case ELEVEN -> ItemIds.MUSIC_DISC_11;
            case MELLOHI -> ItemIds.MUSIC_DISC_MELLOHI;
            case THIRTEEN -> ItemIds.MUSIC_DISC_13;
            case PIGSTEP -> ItemIds.MUSIC_DISC_PIGSTEP;
            case FIVE -> ItemIds.MUSIC_DISC_5;
            default -> ItemIds.MUSIC_DISC_CAT;
        };

        itemTag.putString(NAME_TAG, id.toString());
    }

    @Override
    public void deserialize(Identifier id, short meta, ItemStackBuilder builder, NbtMap tag) {
        super.deserialize(id, meta, builder, tag);
        if (id == ItemIds.MUSIC_DISC_13) {
            builder.data(ItemKeys.RECORD_TYPE, Record.THIRTEEN);
            return;
        }

        if (id == ItemIds.MUSIC_DISC_CAT) {
            builder.data(ItemKeys.RECORD_TYPE, Record.CAT);
            return;
        }

        if (id == ItemIds.MUSIC_DISC_BLOCKS) {
            builder.data(ItemKeys.RECORD_TYPE, Record.BLOCKS);
            return;
        }

        if (id == ItemIds.MUSIC_DISC_CHIRP) {
            builder.data(ItemKeys.RECORD_TYPE, Record.CHIRP);
            return;
        }

        if (id == ItemIds.MUSIC_DISC_FAR) {
            builder.data(ItemKeys.RECORD_TYPE, Record.FAR);
            return;
        }

        if (id == ItemIds.MUSIC_DISC_MALL) {
            builder.data(ItemKeys.RECORD_TYPE, Record.MALL);
            return;
        }

        if (id == ItemIds.MUSIC_DISC_MELLOHI) {
            builder.data(ItemKeys.RECORD_TYPE, Record.MELLOHI);
            return;
        }

        if (id == ItemIds.MUSIC_DISC_STAL) {
            builder.data(ItemKeys.RECORD_TYPE, Record.STAL);
            return;
        }

        if (id == ItemIds.MUSIC_DISC_STRAD) {
            builder.data(ItemKeys.RECORD_TYPE, Record.STRAD);
            return;
        }

        if (id == ItemIds.MUSIC_DISC_WARD) {
            builder.data(ItemKeys.RECORD_TYPE, Record.WARD);
            return;
        }

        if (id == ItemIds.MUSIC_DISC_11) {
            builder.data(ItemKeys.RECORD_TYPE, Record.ELEVEN);
            return;
        }

        if (id == ItemIds.MUSIC_DISC_WAIT) {
            builder.data(ItemKeys.RECORD_TYPE, Record.WAIT);
            return;
        }

        if (id == ItemIds.MUSIC_DISC_PIGSTEP) {
            builder.data(ItemKeys.RECORD_TYPE, Record.PIGSTEP);
            return;
        }

        if (id == ItemIds.MUSIC_DISC_5) {
            builder.data(ItemKeys.RECORD_TYPE, Record.FIVE);
        }

        builder.data(ItemKeys.RECORD_TYPE, Record.THIRTEEN);
    }

    @Override
    public Map<Class<?>, Object> getDefaultMetadataValues() {
        return DEFAULT_VALUES;
    }
}
