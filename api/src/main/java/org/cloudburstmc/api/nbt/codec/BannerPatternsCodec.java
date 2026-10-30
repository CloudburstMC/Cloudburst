package org.cloudburstmc.api.nbt.codec;

import org.cloudburstmc.api.nbt.NbtKindMismatchException;
import org.cloudburstmc.api.nbt.NbtValueCodec;
import org.cloudburstmc.api.util.data.BannerPattern;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores banner pattern layers as the compounds today's save code writes: the dye
 * data masked to four bits under {@code Color}, then the pattern name under
 * {@code Pattern}. A layer with a missing or unknown pattern name fails the read
 * with the previous load path's error, exactly like the previous load path, which
 * converted the stored name after reading it.
 */
public final class BannerPatternsCodec implements NbtValueCodec<List<BannerPattern>> {

    public BannerPatternsCodec() {
    }

    @Override
    public Object toNbt(List<BannerPattern> value) {
        List<NbtMap> layers = new ArrayList<>(value.size());
        for (BannerPattern pattern : value) {
            layers.add(NbtMap.builder()
                    .putInt("Color", pattern.getColor().getDyeData() & 0x0f)
                    .putString("Pattern", pattern.getType().getName())
                    .build());
        }
        return new NbtList<>(NbtType.COMPOUND, layers);
    }

    @Override
    public List<BannerPattern> fromNbt(Object nbt) {
        if (!(nbt instanceof List<?> layers)) {
            throw new NbtKindMismatchException("Expected a list of compounds for banner patterns, got "
                    + describe(nbt));
        }
        List<BannerPattern> patterns = new ArrayList<>(layers.size());
        for (Object layer : layers) {
            if (!(layer instanceof NbtMap compound)) {
                throw new NbtKindMismatchException("Expected a compound for a banner pattern layer, got "
                        + describe(layer));
            }
            Object color = compound.get("Color");
            DyeColor dye;
            if (color == null) {
                dye = DyeColor.getByDyeData(0);
            } else if (color instanceof Number number) {
                dye = DyeColor.getByDyeData(number.intValue());
            } else {
                throw new NbtKindMismatchException("Expected a number for a banner pattern color, got "
                        + describe(color));
            }
            Object name = compound.get("Pattern");
            if (!(name instanceof String patternName)) {
                throw new IllegalArgumentException("Unknown banner pattern: " + name);
            }
            BannerPattern.Type type = BannerPattern.Type.getByName(patternName);
            if (type == null) {
                throw new IllegalArgumentException("Unknown banner pattern: " + patternName);
            }
            patterns.add(new BannerPattern(type, dye));
        }
        return patterns;
    }

    private static String describe(Object tag) {
        return switch (tag) {
            case List<?> list -> "a list of " + list.size() + " values";
            case NbtMap ignored -> "a compound";
            case Number ignored -> "a number";
            case String value -> "\"" + value + "\"";
            case null -> "nothing";
            default -> "an NBT " + tag.getClass().getSimpleName();
        };
    }
}
