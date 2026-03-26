package org.cloudburstmc.server.item.serializer;

import com.google.common.collect.ImmutableList;
import org.cloudburstmc.api.item.ItemKeys;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.api.util.data.FireworkData;
import org.cloudburstmc.api.util.data.FireworkData.FireworkExplosion;
import org.cloudburstmc.api.util.data.FireworkData.FireworkExplosion.ExplosionType;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

import java.util.ArrayList;
import java.util.List;

public class FireworkStarSerializer extends DefaultItemSerializer {

    private static final String TAG_FIREWORKS_ITEM = "FireworksItem";
    private static final String TAG_CUSTOM_COLOR = "customColor";
    private static final String TAG_COLORS = "FireworkColor";
    private static final String TAG_FADES = "FireworkFade";
    private static final String TAG_FLICKER = "FireworkFlicker";
    private static final String TAG_TRAIL = "FireworkTrail";
    private static final String TAG_TYPE = "FireworkType";

    @Override
    public void serialize(ItemStack item, NbtMapBuilder tag) {
        super.serialize(item, tag);

        FireworkData data = item.get(ItemKeys.FIREWORK_DATA);
        if (data == null || data.getExplosions().isEmpty()) {
            return;
        }

        FireworkExplosion explosion = data.getExplosions().getFirst();

        byte[] colors = new byte[explosion.getColors().size()];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = (byte) explosion.getColors().get(i).getDyeData();
        }

        byte[] fades = new byte[explosion.getFades().size()];
        for (int i = 0; i < fades.length; i++) {
            fades[i] = (byte) explosion.getFades().get(i).getDyeData();
        }

        tag.putCompound(TAG_FIREWORKS_ITEM, NbtMap.builder()
                .putByteArray(TAG_COLORS, colors)
                .putByteArray(TAG_FADES, fades)
                .putByte(TAG_FLICKER, (byte) (explosion.isFlicker() ? 1 : 0))
                .putByte(TAG_TRAIL, (byte) (explosion.isTrail() ? 1 : 0))
                .putByte(TAG_TYPE, (byte) explosion.getType().ordinal())
                .build());

        if (explosion.getCustomColor() != 0) {
            tag.putInt(TAG_CUSTOM_COLOR, explosion.getCustomColor());
        }
    }

    @Override
    public void deserialize(Identifier id, short meta, ItemStackBuilder builder, NbtMap tag) {
        super.deserialize(id, meta, builder, tag);

        NbtMap explosionTag = tag.getCompound(TAG_FIREWORKS_ITEM);
        if (explosionTag == null || explosionTag.isEmpty()) {
            return;
        }

        byte[] clrs = explosionTag.getByteArray(TAG_COLORS);
        byte[] fds = explosionTag.getByteArray(TAG_FADES);

        List<DyeColor> colors = new ArrayList<>(clrs.length);
        List<DyeColor> fades = new ArrayList<>(fds.length);

        for (byte c : clrs) {
            colors.add(DyeColor.getByDyeData(c));
        }

        for (byte f : fds) {
            fades.add(DyeColor.getByDyeData(f));
        }

        ExplosionType type = ExplosionType.values()[explosionTag.getByte(TAG_TYPE, (byte) 0)];
        boolean flicker = explosionTag.getByte(TAG_FLICKER, (byte) 0) != 0;
        boolean trail = explosionTag.getByte(TAG_TRAIL, (byte) 0) != 0;
        int customColor = tag.getInt(TAG_CUSTOM_COLOR, 0);

        FireworkExplosion explosion = FireworkExplosion.of(
                ImmutableList.copyOf(colors),
                ImmutableList.copyOf(fades),
                flicker,
                trail,
                type,
                customColor
        );

        builder.data(ItemKeys.FIREWORK_DATA,
                FireworkData.of(ImmutableList.of(explosion), (byte) 0));
    }
}
