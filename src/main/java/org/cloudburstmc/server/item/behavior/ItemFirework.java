package org.cloudburstmc.server.item.behavior;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.nbt.NbtMapBuilder;
import com.nukkitx.nbt.NbtType;
import org.cloudburstmc.server.block.Block;
import org.cloudburstmc.server.entity.EntityTypes;
import org.cloudburstmc.server.entity.misc.FireworksRocket;
import org.cloudburstmc.server.world.World;
import org.cloudburstmc.server.world.Location;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.EntityRegistry;
import org.cloudburstmc.server.utils.Identifier;
import org.cloudburstmc.server.utils.data.DyeColor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author CreeperFace
 */
public class ItemFirework extends Item {

    private static final String TAG_FLIGHT = "Flight";
    private static final String TAG_EXPLOSIONS = "Explosions";
    private static final String TAG_FIREWORKS = "Fireworks";

    private static final String TAG_TYPE = "FireworkType";
    private static final String TAG_COLORS = "FireworkColor";
    private static final String TAG_FADES = "FireworkFade";
    private static final String TAG_TRAIL = "FireworkTrail";
    private static final String TAG_FLICKER = "FireworkFlicker";

    private final List<FireworkExplosion> explosions = new ArrayList<>();
    private byte flight = 1; // ???

    public ItemFirework(Identifier id) {
        super(id);

        this.explosions.add(new FireworkExplosion()
                .addColor(DyeColor.BLACK)
                .type(FireworkExplosion.ExplosionType.CREEPER_SHAPED));
    }

    @Override
    public void loadAdditionalData(NbtMap tag) {
        super.loadAdditionalData(tag);

        tag.listenForCompound(TAG_FIREWORKS, compound -> {
            this.flight = compound.getByte(TAG_FLIGHT, (byte) 1);

            List<NbtMap> explosions = compound.getList(TAG_EXPLOSIONS, NbtType.COMPOUND);
            for (NbtMap explosionTag : explosions) {
                this.explosions.add(FireworkExplosion.from(explosionTag));
            }
        });
    }

    @Override
    public void saveAdditionalData(NbtMapBuilder tag) {
        super.saveAdditionalData(tag);

        List<NbtMap> explosionTags = new ArrayList<>();
        for (FireworkExplosion explosion : this.explosions) {
            explosionTags.add(explosion.createTag());
        }

        tag.putCompound(TAG_FIREWORKS, NbtMap.builder()
                .putList(TAG_EXPLOSIONS, NbtType.COMPOUND, explosionTags)
                .putByte(TAG_FLIGHT, this.flight)
                .build());
    }

    @Override
    public boolean canBeActivated() {
        return true;
    }

    @Override
    public boolean onActivate(World world, Player player, Block block, Block target, Direction face, Vector3f clickPos) {
        if (block.getState().getBehavior().canPassThrough()) {
            this.spawnFirework(world, block.getPosition().toFloat().add(0.5, 0.5, 0.5));

            if (!player.isCreative()) {
                player.getInventory().decreaseCount(player.getInventory().getHeldItemIndex());
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean onClickAir(Player player, Vector3f directionVector) {
        if (player.getInventory().getChestplate() instanceof ItemElytra && player.isGliding()) {
            this.spawnFirework(player.getWorld(), player.getPosition());

            player.setMotion(Vector3f.from(
                    -Math.sin(Math.toRadians(player.getYaw())) * Math.cos(Math.toRadians(player.getPitch())) * 2,
                    -Math.sin(Math.toRadians(player.getPitch())) * 2,
                    Math.cos(Math.toRadians(player.getYaw())) * Math.cos(Math.toRadians(player.getPitch())) * 2));

            if (!player.isCreative()) {
                this.decrementCount();
            }

            return true;
        }

        return false;
    }

    public void addExplosion(FireworkExplosion explosion) {
        this.explosions.add(explosion);
    }

    public void clearExplosions() {
        this.explosions.clear();
    }

    private void spawnFirework(World world, Vector3f pos) {
        FireworksRocket rocket = EntityRegistry.get().newEntity(EntityTypes.FIREWORKS_ROCKET, Location.from(pos, world));
        rocket.setFireworkData(this.createTag());

        rocket.spawnToAll();
    }

    public static class FireworkExplosion {

        private final List<DyeColor> colors = new ArrayList<>();
        private final List<DyeColor> fades = new ArrayList<>();
        private boolean flicker;
        private boolean trail;
        private ExplosionType type = ExplosionType.CREEPER_SHAPED;

        public List<DyeColor> getColors() {
            return this.colors;
        }

        public List<DyeColor> getFades() {
            return this.fades;
        }

        public boolean hasFlicker() {
            return this.flicker;
        }

        public boolean hasTrail() {
            return this.trail;
        }

        public ExplosionType getType() {
            return this.type;
        }

        public FireworkExplosion setFlicker(boolean flicker) {
            this.flicker = flicker;
            return this;
        }

        public FireworkExplosion setTrail(boolean trail) {
            this.trail = trail;
            return this;
        }

        public FireworkExplosion type(ExplosionType type) {
            this.type = type;
            return this;
        }

        public FireworkExplosion addColor(DyeColor color) {
            colors.add(color);
            return this;
        }

        public FireworkExplosion addFade(DyeColor fade) {
            fades.add(fade);
            return this;
        }

        public static FireworkExplosion from(NbtMap tag) {
            FireworkExplosion explosion = new FireworkExplosion();

            explosion.setFlicker(tag.getBoolean(TAG_FLICKER));
            explosion.setTrail(tag.getBoolean(TAG_TRAIL));
            explosion.type(ExplosionType.values()[tag.getByte(TAG_TYPE)]);

            byte[] clrs = tag.getByteArray(TAG_COLORS);
            byte[] fds = tag.getByteArray(TAG_FADES);

            for (int i = 0; i < clrs.length; i++) {
                explosion.addColor(DyeColor.getByDyeData(clrs[i]));
            }

            for (int i = 0; i < fds.length; i++) {
                explosion.addFade(DyeColor.getByDyeData(fds[i]));
            }
            return explosion;
        }

        public NbtMap createTag() {
            byte[] clrs = new byte[colors.size()];
            for (int i = 0; i < clrs.length; i++) {
                clrs[i] = (byte) colors.get(i).getDyeData();
            }

            byte[] fds = new byte[fades.size()];
            for (int i = 0; i < fds.length; i++) {
                fds[i] = (byte) fades.get(i).getDyeData();
            }

            return NbtMap.builder()
                    .putByteArray(TAG_COLORS, clrs)
                    .putByteArray(TAG_FADES, fds)
                    .putBoolean(TAG_FLICKER, this.flicker)
                    .putBoolean(TAG_TRAIL, this.trail)
                    .putByte(TAG_TYPE, (byte) this.type.ordinal())
                    .build();
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
