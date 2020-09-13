package org.cloudburstmc.server.item.serializer;

import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.item.ItemIds;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.data.Record;
import org.cloudburstmc.server.item.data.serializer.ItemDataSerializer;
import org.cloudburstmc.server.utils.Identifier;

public class RecordSerializer implements ItemDataSerializer<Record> {

    @Override
    public void serialize(ItemStack item, NbtMapBuilder itemTag, Record value) {
        Identifier id;

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
    public Record deserialize(Identifier id, Integer meta, NbtMap tag) {
        if (id == ItemIds.RECORD_13) {
            return Record.THIRTEEN;
        }

        if (id == ItemIds.RECORD_CAT) {
            return Record.CAT;
        }

        if (id == ItemIds.RECORD_BLOCKS) {
            return Record.BLOCKS;
        }

        if (id == ItemIds.RECORD_CHIRP) {
            return Record.CHIRP;
        }

        if (id == ItemIds.RECORD_FAR) {
            return Record.FAR;
        }

        if (id == ItemIds.RECORD_MALL) {
            return Record.MALL;
        }

        if (id == ItemIds.RECORD_MELLOHI) {
            return Record.MELLOHI;
        }

        if (id == ItemIds.RECORD_STAL) {
            return Record.STAL;
        }

        if (id == ItemIds.RECORD_STRAD) {
            return Record.STRAD;
        }

        if (id == ItemIds.RECORD_WARD) {
            return Record.WARD;
        }

        if (id == ItemIds.RECORD_11) {
            return Record.ELEVEN;
        }

        if (id == ItemIds.RECORD_WAIT) {
            return Record.WAIT;
        }

        if (id == ItemIds.RECORD_PIGSTEP) {
            return Record.PIGSTEP;
        }

        return Record.THIRTEEN;
    }
}
