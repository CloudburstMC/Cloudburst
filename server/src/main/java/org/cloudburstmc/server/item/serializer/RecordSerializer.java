package org.cloudburstmc.server.item.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.api.item.ItemIds;
import org.cloudburstmc.api.item.data.Record;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.item.CloudItemStackBuilder;

import java.util.Map;

public class RecordSerializer extends DefaultItemSerializer {

    private static final Map<Class<?>, Object> DEFAULT_VALUES;

    static {
        DEFAULT_VALUES = Map.of(Record.class, Record.CAT);
    }

    @Override
    public void serialize(CloudItemStack item, NbtMapBuilder itemTag) {
        super.serialize(item, itemTag);
        Identifier id;
        Record value = item.getMetadata(Record.class);

        id = switch (value) {
            case CAT -> ItemIds.RECORD_CAT;
            case FAR -> ItemIds.RECORD_FAR;
            case MALL -> ItemIds.RECORD_MALL;
            case STAL -> ItemIds.RECORD_STAL;
            case WAIT -> ItemIds.RECORD_WAIT;
            case WARD -> ItemIds.RECORD_WARD;
            case CHIRP -> ItemIds.RECORD_CHIRP;
            case STRAD -> ItemIds.RECORD_STRAD;
            case BLOCKS -> ItemIds.RECORD_BLOCKS;
            case ELEVEN -> ItemIds.RECORD_11;
            case MELLOHI -> ItemIds.RECORD_MELLOHI;
            case THIRTEEN -> ItemIds.RECORD_13;
            case PIGSTEP -> ItemIds.RECORD_PIGSTEP;
            case FIVE -> ItemIds.RECORD_5;
        };

        itemTag.putString(NAME_TAG, id.toString());
    }

    @Override
    public void deserialize(Identifier id, short meta, int amount, CloudItemStackBuilder builder, NbtMap tag) {
        super.deserialize(id, meta, amount, builder, tag);
        if (id == ItemIds.RECORD_13) {
            builder.itemData(Record.THIRTEEN);
            return;
        }

        if (id == ItemIds.RECORD_CAT) {
            builder.itemData(Record.CAT);
            return;
        }

        if (id == ItemIds.RECORD_BLOCKS) {
            builder.itemData(Record.BLOCKS);
            return;
        }

        if (id == ItemIds.RECORD_CHIRP) {
            builder.itemData(Record.CHIRP);
            return;
        }

        if (id == ItemIds.RECORD_FAR) {
            builder.itemData(Record.FAR);
            return;
        }

        if (id == ItemIds.RECORD_MALL) {
            builder.itemData(Record.MALL);
            return;
        }

        if (id == ItemIds.RECORD_MELLOHI) {
            builder.itemData(Record.MELLOHI);
            return;
        }

        if (id == ItemIds.RECORD_STAL) {
            builder.itemData(Record.STAL);
            return;
        }

        if (id == ItemIds.RECORD_STRAD) {
            builder.itemData(Record.STRAD);
            return;
        }

        if (id == ItemIds.RECORD_WARD) {
            builder.itemData(Record.WARD);
            return;
        }

        if (id == ItemIds.RECORD_11) {
            builder.itemData(Record.ELEVEN);
            return;
        }

        if (id == ItemIds.RECORD_WAIT) {
            builder.itemData(Record.WAIT);
            return;
        }

        if (id == ItemIds.RECORD_PIGSTEP) {
            builder.itemData(Record.PIGSTEP);
            return;
        }

        if (id == ItemIds.RECORD_5) {
            builder.itemData(Record.FIVE);
            return;
        }

        builder.itemData(Record.THIRTEEN);
    }

    @Override
    public Map<Class<?>, Object> getDefaultMetadataValues() {
        return DEFAULT_VALUES;
    }
}
