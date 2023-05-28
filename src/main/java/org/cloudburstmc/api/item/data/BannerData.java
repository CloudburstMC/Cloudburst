package org.cloudburstmc.api.item.data;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.api.util.data.BannerPattern;
import org.cloudburstmc.api.util.data.DyeColor;

import java.util.List;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class BannerData {

    private final int type; //TODO: find values
    private final DyeColor base;
    private final ImmutableList<BannerPattern> patterns;

    public static BannerData of(int type, DyeColor base, List<BannerPattern> patterns) {
        Preconditions.checkNotNull(base, "base");
        Preconditions.checkNotNull(patterns, "patterns");
        return new BannerData(type, base, ImmutableList.copyOf(patterns));
    }
}
