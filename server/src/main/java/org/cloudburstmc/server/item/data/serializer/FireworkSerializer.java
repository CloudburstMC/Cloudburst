package org.cloudburstmc.server.item.data.serializer;

import com.google.common.collect.ImmutableList;
import org.cloudburstmc.api.item.ItemStack;
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

public class FireworkSerializer implements ItemDataSerializer<FireworkData> {

    private static final String TAG_FLIGHT = "Flight";
    private static final String TAG_EXPLOSIONS = "Explosions";
    private static final String TAG_FIREWORKS = "Fireworks";

    private static final String TAG_TYPE = "FireworkType";
    private static final String TAG_COLORS = "FireworkColor";
    private static final String TAG_FADES = "FireworkFade";
    private static final String TAG_TRAIL = "FireworkTrail";
    private static final String TAG_FLICKER = "FireworkFlicker";

    @Override
    public void serialize(ItemStack item, NbtMapBuilder tag, FireworkData value) {
        List<NbtMap> explosionTags = new ArrayList<>();
        for (FireworkExplosion explosion : value.getExplosions()) {
            byte[] clrs = new byte[explosion.getColors().size()];
            for (int i = 0; i < clrs.length; i++) {
                clrs[i] = (byte) explosion.getColors().get(i).getDyeData();
            }

            byte[] fds = new byte[explosion.getFades().size()];
            for (int i = 0; i < fds.length; i++) {
                fds[i] = (byte) explosion.getFades().get(i).getDyeData();
            }

            explosionTags.add(NbtMap.builder()
                    .putByteArray(TAG_COLORS, clrs)
                    .putByteArray(TAG_FADES, fds)
                    .putBoolean(TAG_FLICKER, explosion.isFlicker())
                    .putBoolean(TAG_TRAIL, explosion.isTrail())
                    .putByte(TAG_TYPE, (byte) explosion.getType().ordinal())
                    .build());
        }

        tag.putCompound(TAG_FIREWORKS, NbtMap.builder()
                .putList(TAG_EXPLOSIONS, NbtType.COMPOUND, explosionTags)
                .putBoolean(TAG_FLIGHT, value.isFlight())
                .build());
    }

    @Override
    public FireworkData deserialize(Identifier id, NbtMap tag) {
        var compound = tag.getCompound(TAG_FIREWORKS);

        if (compound != null) {
            boolean flight = compound.getBoolean(TAG_FLIGHT, true);

            List<NbtMap> explosionTags = compound.getList(TAG_EXPLOSIONS, NbtType.COMPOUND);
            List<FireworkExplosion> explosions = new ArrayList<>(explosionTags.size());

            for (NbtMap explosionTag : explosionTags) {
                byte[] clrs = explosionTag.getByteArray(TAG_COLORS);
                byte[] fds = explosionTag.getByteArray(TAG_FADES);

                List<DyeColor> colors = new ArrayList<>(clrs.length);
                List<DyeColor> fades = new ArrayList<>(fds.length);

                for (byte clr : clrs) {
                    colors.add(DyeColor.getByDyeData(clr));
                }

                for (byte fd : fds) {
                    fades.add(DyeColor.getByDyeData(fd));
                }

                explosions.add(FireworkExplosion.of(
                        ImmutableList.copyOf(colors),
                        ImmutableList.copyOf(fades),
                        explosionTag.getBoolean(TAG_FLICKER),
                        explosionTag.getBoolean(TAG_TRAIL),
                        ExplosionType.values()[explosionTag.getByte(TAG_TYPE)]
                ));
            }

            return FireworkData.of(ImmutableList.copyOf(explosions), flight);
        }
        ;
        return null;
    }
}
