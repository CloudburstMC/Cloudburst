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
import org.cloudburstmc.nbt.NbtType;

import java.util.ArrayList;
import java.util.List;

public class FireworkRocketSerializer extends DefaultItemSerializer {

    private static final String TAG_FIREWORKS = "Fireworks";
    private static final String TAG_FLIGHT = "Flight";
    private static final String TAG_EXPLOSIONS = "Explosions";
    private static final String TAG_COLORS = "FireworkColor";
    private static final String TAG_FADES = "FireworkFade";
    private static final String TAG_FLICKER = "FireworkFlicker";
    private static final String TAG_TRAIL = "FireworkTrail";
    private static final String TAG_TYPE = "FireworkType";

    @Override
    public void serialize(ItemStack item, NbtMapBuilder tag) {
        super.serialize(item, tag);

        FireworkData data = item.get(ItemKeys.FIREWORK_DATA);
        if (data == null) {
            return;
        }

        List<NbtMap> explosionTags = new ArrayList<>();
        for (FireworkExplosion explosion : data.getExplosions()) {
            byte[] colors = new byte[explosion.getColors().size()];
            for (int i = 0; i < colors.length; i++) {
                colors[i] = (byte) explosion.getColors().get(i).getDyeData();
            }

            byte[] fades = new byte[explosion.getFades().size()];
            for (int i = 0; i < fades.length; i++) {
                fades[i] = (byte) explosion.getFades().get(i).getDyeData();
            }

            explosionTags.add(NbtMap.builder()
                    .putByteArray(TAG_COLORS, colors)
                    .putByteArray(TAG_FADES, fades)
                    .putByte(TAG_FLICKER, (byte) (explosion.isFlicker() ? 1 : 0))
                    .putByte(TAG_TRAIL, (byte) (explosion.isTrail() ? 1 : 0))
                    .putByte(TAG_TYPE, (byte) explosion.getType().ordinal())
                    .build());
        }

        tag.putCompound(TAG_FIREWORKS, NbtMap.builder()
                .putList(TAG_EXPLOSIONS, NbtType.COMPOUND, explosionTags)
                .putByte(TAG_FLIGHT, data.getFlightLevel())
                .build());
    }

    @Override
    public void deserialize(Identifier id, short meta, ItemStackBuilder builder, NbtMap tag) {
        super.deserialize(id, meta, builder, tag);

        NbtMap compound = tag.getCompound(TAG_FIREWORKS);
        if (compound == null || compound.isEmpty()) {
            return;
        }

        byte flightLevel = compound.getByte(TAG_FLIGHT, (byte) 0);
        List<NbtMap> explosionTags = compound.getList(TAG_EXPLOSIONS, NbtType.COMPOUND);
        List<FireworkExplosion> explosions = new ArrayList<>(explosionTags.size());

        for (NbtMap explosionTag : explosionTags) {
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

            explosions.add(FireworkExplosion.of(
                    ImmutableList.copyOf(colors),
                    ImmutableList.copyOf(fades),
                    flicker,
                    trail,
                    type,
                    0
            ));
        }

        builder.data(ItemKeys.FIREWORK_DATA, FireworkData.of(ImmutableList.copyOf(explosions), flightLevel));
    }
}
