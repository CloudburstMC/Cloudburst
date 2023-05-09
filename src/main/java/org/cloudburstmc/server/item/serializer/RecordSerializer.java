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
            default -> ItemIds.RECORD_CAT;
        };

        itemTag.putString(NAME_TAG, id.toString());
    }

    @Override
    public void deserialize(Identifier id, short meta, ItemStackBuilder builder, NbtMap tag) {
        super.deserialize(id, meta, builder, tag);
        if (id == ItemIds.RECORD_13) {
            builder.data(ItemKeys.RECORD_TYPE, Record.THIRTEEN);
            return;
        }

        if (id == ItemIds.RECORD_CAT) {
            builder.data(ItemKeys.RECORD_TYPE, Record.CAT);
            return;
        }

        if (id == ItemIds.RECORD_BLOCKS) {
            builder.data(ItemKeys.RECORD_TYPE, Record.BLOCKS);
            return;
        }

        if (id == ItemIds.RECORD_CHIRP) {
            builder.data(ItemKeys.RECORD_TYPE, Record.CHIRP);
            return;
        }

        if (id == ItemIds.RECORD_FAR) {
            builder.data(ItemKeys.RECORD_TYPE, Record.FAR);
            return;
        }

        if (id == ItemIds.RECORD_MALL) {
            builder.data(ItemKeys.RECORD_TYPE, Record.MALL);
            return;
        }

        if (id == ItemIds.RECORD_MELLOHI) {
            builder.data(ItemKeys.RECORD_TYPE, Record.MELLOHI);
            return;
        }

        if (id == ItemIds.RECORD_STAL) {
            builder.data(ItemKeys.RECORD_TYPE, Record.STAL);
            return;
        }

        if (id == ItemIds.RECORD_STRAD) {
            builder.data(ItemKeys.RECORD_TYPE, Record.STRAD);
            return;
        }

        if (id == ItemIds.RECORD_WARD) {
            builder.data(ItemKeys.RECORD_TYPE, Record.WARD);
            return;
        }

        if (id == ItemIds.RECORD_11) {
            builder.data(ItemKeys.RECORD_TYPE, Record.ELEVEN);
            return;
        }

        if (id == ItemIds.RECORD_WAIT) {
            builder.data(ItemKeys.RECORD_TYPE, Record.WAIT);
            return;
        }

        if (id == ItemIds.RECORD_PIGSTEP) {
            builder.data(ItemKeys.RECORD_TYPE, Record.PIGSTEP);
            return;
        }

        if (id == ItemIds.RECORD_5) {
            builder.data(ItemKeys.RECORD_TYPE, Record.FIVE);
        }

        builder.data(ItemKeys.RECORD_TYPE, Record.THIRTEEN);
    }

    @Override
    public Map<Class<?>, Object> getDefaultMetadataValues() {
        return DEFAULT_VALUES;
    }
}
