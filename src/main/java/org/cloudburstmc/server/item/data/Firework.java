package org.cloudburstmc.server.item.data;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.server.utils.data.DyeColor;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;
import java.util.List;

@Getter
@Immutable
@ParametersAreNonnullByDefault
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Firework {

    private final ImmutableList<FireworkExplosion> explosions;
    private final boolean flight;

    public static Firework of(List<FireworkExplosion> explosions, boolean flight) {
        Preconditions.checkNotNull(explosions, "explosions");
        return new Firework(ImmutableList.copyOf(explosions), flight);
    }

    @Getter
    @ParametersAreNonnullByDefault
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class FireworkExplosion {

        private final ImmutableList<DyeColor> colors;
        private final ImmutableList<DyeColor> fades;
        private final boolean flicker;
        private final boolean trail;
        private final ExplosionType type;

        public static FireworkExplosion of(List<DyeColor> colors, List<DyeColor> fades, boolean flicker, boolean trail, ExplosionType type) {
            Preconditions.checkNotNull(colors, "colors");
            Preconditions.checkNotNull(fades, "fades");
            Preconditions.checkNotNull(type, "type");
            return new FireworkExplosion(ImmutableList.copyOf(colors), ImmutableList.copyOf(fades), flicker, trail, type);
        }

        public enum ExplosionType {
            SMALL_BALL,
            LARGE_BALL,
            STAR_SHAPED,
            CREEPER_SHAPED,
            BURST
        }
    }
}
