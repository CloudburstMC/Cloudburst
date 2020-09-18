package org.cloudburstmc.server.item.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.item.CloudItemStack;
import org.cloudburstmc.server.item.CloudItemStackBuilder;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.item.data.Record;
import org.cloudburstmc.server.utils.Identifier;

public class RecordSerializer extends DefaultItemSerializer {

    @Override
    public void serialize(CloudItemStack item, NbtMapBuilder itemTag) {
        super.serialize(item, itemTag);
        Identifier id;
        Record value = item.getMetadata(Record.class);

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

        builder.itemData(Record.THIRTEEN);
    }
}
