package org.cloudburstmc.server.item.data;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.server.utils.BannerPattern;
import org.cloudburstmc.server.utils.data.DyeColor;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;
import java.util.List;

@Getter
@Immutable
@ParametersAreNonnullByDefault
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Banner {

    private final int type; //TODO: find values
    private final DyeColor base;
    private final ImmutableList<BannerPattern> patterns;

    public static Banner of(int type, DyeColor base, List<BannerPattern> patterns) {
        Preconditions.checkNotNull(base, "base");
        Preconditions.checkNotNull(patterns, "patterns");
        return new Banner(type, base, ImmutableList.copyOf(patterns));
    }
}
