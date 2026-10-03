package org.cloudburstmc.server.item.serializer;

import org.cloudburstmc.api.item.ItemDataComponents;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.item.ItemStackBuilder;
import org.cloudburstmc.api.potion.EffectTypes;
import org.cloudburstmc.api.potion.PotionEffect;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;

import java.util.List;

public class SuspiciousStewItemSerializer extends DefaultItemSerializer {

    private static final String EFFECTS_TAG = "cloudburst:stew_effects";
    private static final List<List<PotionEffect>> VARIANTS = List.of(
            List.of(new PotionEffect(EffectTypes.NIGHT_VISION, 100, 0)),
            List.of(new PotionEffect(EffectTypes.JUMP_BOOST, 100, 0)),
            List.of(new PotionEffect(EffectTypes.WEAKNESS, 140, 0)),
            List.of(new PotionEffect(EffectTypes.BLINDNESS, 140, 0)),
            List.of(new PotionEffect(EffectTypes.POISON, 220, 0)),
            List.of(new PotionEffect(EffectTypes.SATURATION, 6, 0)),
            List.of(new PotionEffect(EffectTypes.SATURATION, 6, 0)),
            List.of(new PotionEffect(EffectTypes.FIRE_RESISTANCE, 60, 0)),
            List.of(new PotionEffect(EffectTypes.REGENERATION, 140, 0)),
            List.of(new PotionEffect(EffectTypes.WITHER, 140, 0)),
            List.of(new PotionEffect(EffectTypes.NIGHT_VISION, 100, 0)),
            List.of(new PotionEffect(EffectTypes.BLINDNESS, 140, 0)),
            List.of(new PotionEffect(EffectTypes.NAUSEA, 140, 0))
    );

    @Override
    public int getAuxValue(ItemStack item) {
        int variant = VARIANTS.indexOf(item.getOrDefault(ItemDataComponents.SUSPICIOUS_STEW_EFFECTS, List.of()));
        return Math.max(variant, 0);
    }

    @Override
    public void serialize(ItemStack item, NbtMapBuilder tag) {
        super.serialize(item, tag);
        List<PotionEffect> effects = item.getOrDefault(ItemDataComponents.SUSPICIOUS_STEW_EFFECTS, List.of());
        tag.putList(EFFECTS_TAG, NbtType.COMPOUND, effects.stream().map(effect -> NbtMap.builder()
                .putString("id", effect.getType().getId().toString())
                .putInt("duration", effect.getDuration())
                .putInt("amplifier", effect.getAmplifier())
                .putBoolean("ambient", effect.isAmbient())
                .putBoolean("particles", effect.hasParticles())
                .build()).toList());
    }

    @Override
    public void deserialize(Identifier id, short meta, ItemStackBuilder builder, NbtMap tag) {
        super.deserialize(id, meta, builder, tag);
        List<PotionEffect> effects;
        if (tag.containsKey(EFFECTS_TAG, NbtType.LIST)) {
            effects = tag.getList(EFFECTS_TAG, NbtType.COMPOUND).stream().map(effect -> new PotionEffect(
                    EffectTypes.get(Identifier.parse(effect.getString("id")))
                            .orElseThrow(() -> new IllegalArgumentException("Unknown stew effect: " + effect.getString("id"))),
                    effect.getInt("duration"), effect.getInt("amplifier"),
                    effect.getBoolean("ambient"), effect.getBoolean("particles", true))).toList();
        } else {
            if (meta < 0 || meta >= VARIANTS.size()) {
                throw new IllegalArgumentException("Invalid suspicious stew variant: " + meta);
            }

            effects = VARIANTS.get(meta);
        }

        builder.setData(ItemDataComponents.SUSPICIOUS_STEW_EFFECTS, effects);
    }
}
