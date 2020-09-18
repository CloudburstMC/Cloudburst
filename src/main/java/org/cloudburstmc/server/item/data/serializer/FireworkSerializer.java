package org.cloudburstmc.server.item.data.serializer;

import com.google.common.collect.ImmutableList;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import lombok.val;
import org.cloudburstmc.server.item.ItemStack;
import org.cloudburstmc.server.item.data.Firework;
import org.cloudburstmc.server.item.data.Firework.FireworkExplosion;
import org.cloudburstmc.server.item.data.Firework.FireworkExplosion.ExplosionType;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.data.DyeColor;

import java.util.ArrayList;
import java.util.List;

public class FireworkSerializer implements ItemDataSerializer<Firework> {

    private static final String TAG_FLIGHT = "Flight";
    private static final String TAG_EXPLOSIONS = "Explosions";
    private static final String TAG_FIREWORKS = "Fireworks";

    private static final String TAG_TYPE = "FireworkType";
    private static final String TAG_COLORS = "FireworkColor";
    private static final String TAG_FADES = "FireworkFade";
    private static final String TAG_TRAIL = "FireworkTrail";
    private static final String TAG_FLICKER = "FireworkFlicker";

    @Override
    public void serialize(ItemStack item, NbtMapBuilder itemTag, Firework value) {
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

        itemTag.putCompound(TAG_FIREWORKS, NbtMap.builder()
                .putList(TAG_EXPLOSIONS, NbtType.COMPOUND, explosionTags)
                .putBoolean(TAG_FLIGHT, value.isFlight())
                .build());
    }

    @Override
    public Firework deserialize(Identifier id, NbtMap tag) {
        val compound = tag.getCompound(TAG_FIREWORKS);

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

            return Firework.of(ImmutableList.copyOf(explosions), flight);
        }
        ;
        return null;
    }
}
