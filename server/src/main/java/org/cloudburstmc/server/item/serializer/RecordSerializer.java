package org.cloudburstmc.server.item.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.api.item.ItemIds;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.item.data.Record;
import org.cloudburstmc.api.util.Identifier;

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

        switch (value) {
            default:
            case CAT:
                id = ItemIds.RECORD_CAT;
                break;
            case FAR:
                id = ItemIds.RECORD_FAR;
                break;
            case MALL:
                id = ItemIds.RECORD_MALL;
                break;
            case STAL:
                id = ItemIds.RECORD_STAL;
                break;
            case WAIT:
                id = ItemIds.RECORD_WAIT;
                break;
            case WARD:
                id = ItemIds.RECORD_WARD;
                break;
            case CHIRP:
                id = ItemIds.RECORD_CHIRP;
                break;
            case STRAD:
                id = ItemIds.RECORD_STRAD;
                break;
            case BLOCKS:
                id = ItemIds.RECORD_BLOCKS;
                break;
            case ELEVEN:
                id = ItemIds.RECORD_11;
                break;
            case MELLOHI:
                id = ItemIds.RECORD_MELLOHI;
                break;
            case THIRTEEN:
                id = ItemIds.RECORD_13;
                break;
            case PIGSTEP:
                id = ItemIds.RECORD_PIGSTEP;
                break;
        }

        itemTag.putString(NAME_TAG, id.toString());
    }

    @Override
    public void deserialize(ItemStackBuilder builder, NbtMap tag) {
        super.deserialize(builder, tag);
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

        builder.data(ItemKeys.RECORD_TYPE, Record.THIRTEEN);
    }

    @Override
    public Map<Class<?>, Object> getDefaultMetadataValues() {
        return DEFAULT_VALUES;
    }
}
