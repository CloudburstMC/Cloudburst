package org.cloudburstmc.server.nbt.codec;

import lombok.NoArgsConstructor;
import org.cloudburstmc.api.util.data.BannerPattern;
import org.cloudburstmc.api.util.data.DyeColor;
import org.cloudburstmc.nbt.NbtList;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.server.nbt.NbtKindMismatchException;
import org.cloudburstmc.server.nbt.NbtValueCodec;
import org.cloudburstmc.server.nbt.serializer.NbtTags;

import java.util.ArrayList;
import java.util.List;

/**
 * Encodes banner layers in order using Bedrock dye indices and pattern names.
 */
@NoArgsConstructor
public class BannerPatternsCodec implements NbtValueCodec<List<BannerPattern>> {

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
            throw new NbtKindMismatchException("Expected a list of compounds for banner patterns, got " + NbtTags.describe(nbt));
        }

        List<BannerPattern> patterns = new ArrayList<>(layers.size());
        for (Object layer : layers) {
            if (!(layer instanceof NbtMap compound)) {
                throw new NbtKindMismatchException("Expected a compound for a banner pattern layer, got " + NbtTags.describe(layer));
            }

            Object color = compound.get("Color");
            DyeColor dye;
            if (color == null) {
                dye = DyeColor.getByDyeData(0);
            } else if (color instanceof Number number) {
                dye = DyeColor.getByDyeData(number.intValue());
            } else {
                throw new NbtKindMismatchException("Expected a number for a banner pattern color, got " + NbtTags.describe(color));
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
}
