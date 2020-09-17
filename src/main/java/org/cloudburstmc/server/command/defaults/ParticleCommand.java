package org.cloudburstmc.server.command.defaults;

import com.nukkitx.math.vector.Vector3f;
import com.nukkitx.protocol.bedrock.data.command.CommandParamType;
import org.cloudburstmc.server.command.Command;
import org.cloudburstmc.server.command.CommandSender;
import org.cloudburstmc.server.command.data.CommandData;
import org.cloudburstmc.server.command.data.CommandParameter;
import org.cloudburstmc.server.item.behavior.Item;
import org.cloudburstmc.server.item.behavior.ItemIds;
import org.cloudburstmc.server.level.Location;
import org.cloudburstmc.server.level.particle.*;
import org.cloudburstmc.server.locale.TranslationContainer;
import org.cloudburstmc.server.player.Player;
import org.cloudburstmc.server.registry.BlockRegistry;
import org.cloudburstmc.server.utils.Identifier;

import java.util.Random;

/**
 * Created on 2015/11/12 by xtypr.
 * Package cn.nukkit.command.defaults in project Nukkit .
 */
public class ParticleCommand extends Command {
    private static final String[] ENUM_VALUES = new String[]{"explode", "hugeexplosion", "hugeexplosionseed", "bubble"
            , "splash", "wake", "water", "crit", "smoke", "spell", "instantspell", "dripwater", "driplava", "townaura"
            , "spore", "portal", "flame", "lava", "reddust", "snowballpoof", "slime", "itembreak", "terrain", "heart"
            , "ink", "droplet", "enchantmenttable", "happyvillager", "angryvillager", "forcefield"};

    public ParticleCommand() {
        super("particle", CommandData.builder("particle")
                .setDescription("commands.particle.description")
                .setUsageMessage("/particle <particle> <position> [count] [data]")
                .setPermissions("cloudburst.command.particle")
                .setParameters(new CommandParameter[]{
                        new CommandParameter("name", false, ENUM_VALUES),
                        new CommandParameter("position", CommandParamType.POSITION, false),
                        new CommandParameter("count", CommandParamType.INT, true),
                        new CommandParameter("data", true)
                })
                .build());
    }

    private static float getFloat(String arg, float defaultValue) {
        if (arg.startsWith("~")) {
            String relativePos = arg.substring(1);
            if (relativePos.isEmpty()) {
                return defaultValue;
            }
            return defaultValue + Float.parseFloat(relativePos);
        }
        return Float.parseFloat(arg);
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }

        if (args.length < 4) {
            return false;
        }

        Location defaultLocation;
        if (sender instanceof Player) {
            defaultLocation = ((Player) sender).getLocation();
        } else {
            defaultLocation = Location.from(Vector3f.ZERO, sender.getServer().getDefaultLevel());
        }

        String name = args[0].toLowerCase();

        float x;
        float y;
        float z;

        try {
            x = getFloat(args[1], defaultLocation.getX());
            y = getFloat(args[2], defaultLocation.getY());
            z = getFloat(args[3], defaultLocation.getZ());
        } catch (Exception e) {
            return false;
        }
        Location location = Location.from(Vector3f.from(x, y, z), defaultLocation.getLevel());

        int count = 1;
        if (args.length > 4) {
            try {
                double c = Double.parseDouble(args[4]);
                count = (int) c;
            } catch (Exception e) {
                //ignore
            }
        }
        count = Math.max(1, count);

        int data = -1;
        if (args.length > 5) {
            try {
                double d = Double.parseDouble(args[5]);
                data = (int) d;
            } catch (Exception e) {
                //ignore
            }
        }

        Particle particle = this.getParticle(name, location, data);

        if (particle == null) {
            location.getLevel().addParticleEffect(location.getPosition(), Identifier.fromString(args[0]), -1, location.getLevel().getDimension());
            return true;
        }

        sender.sendMessage(new TranslationContainer("commands.particle.success", name, count));

        Random random = new Random(System.currentTimeMillis());

        for (int i = 0; i < count; i++) {
            particle.setPosition(location.getPosition()
                    .add(random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1));
            location.getLevel().addParticle(particle);
        }

        return true;
    }

    private Particle getParticle(String name, Location loc, int data) {
        Vector3f pos = loc.getPosition();
        switch (name) {
            case "explode":
                return new ExplodeParticle(pos);
            case "hugeexplosion":
                return new HugeExplodeParticle(pos);
            case "hugeexplosionseed":
                return new HugeExplodeSeedParticle(pos);
            case "bubble":
                return new BubbleParticle(pos);
            case "splash":
                return new SplashParticle(pos);
            case "wake":
            case "water":
                return new WaterParticle(pos);
            case "crit":
                return new CriticalParticle(pos);
            case "smoke":
                return new SmokeParticle(pos, data != -1 ? data : 0);
            case "spell":
                return new EnchantParticle(pos);
            case "instantspell":
                return new InstantEnchantParticle(pos);
            case "dripwater":
                return new WaterDripParticle(pos);
            case "driplava":
                return new LavaDripParticle(pos);
            case "townaura":
            case "spore":
                return new SporeParticle(pos);
            case "portal":
                return new PortalParticle(pos);
            case "flame":
                return new FlameParticle(pos);
            case "lava":
                return new LavaParticle(pos);
            case "reddust":
                return new RedstoneParticle(pos, data != -1 ? data : 1);
            case "snowballpoof":
                return new ItemBreakParticle(pos, Item.get(ItemIds.SNOWBALL));
            case "slime":
                return new ItemBreakParticle(pos, Item.get(ItemIds.SLIME_BALL));
            case "itembreak":
                if (data != -1 && data != 0) {
                    return new ItemBreakParticle(pos, Item.get(data));
                }
                break;
            case "terrain":
                if (data != -1 && data != 0) {
//                    return new TerrainParticle(pos, BlockState.get(data)); //TODO: block name
                }
                break;
            case "heart":
                return new HeartParticle(pos, data != -1 ? data : 0);
            case "ink":
                return new InkParticle(pos, data != -1 ? data : 0);
            case "droplet":
                return new RainSplashParticle(pos);
            case "enchantmenttable":
                return new EnchantmentTableParticle(pos);
            case "happyvillager":
                return new HappyVillagerParticle(pos);
            case "angryvillager":
                return new AngryVillagerParticle(pos);
            case "forcefield":
                return new BlockForceFieldParticle(pos);
        }

        if (name.startsWith("iconcrack_")) {
            String[] d = name.split("_");
            if (d.length == 3) {
                return new ItemBreakParticle(pos, Item.get(Integer.parseInt(d[1]), Integer.parseInt(d[2])));
            }
        } else if (name.startsWith("blockcrack_")) {
            String[] d = name.split("_");
            if (d.length == 2) {
                return new TerrainParticle(pos, BlockRegistry.get().getBlock(Integer.parseInt(d[1]) & 0xff, Integer.parseInt(d[1]) >> 12));
            }
        } else if (name.startsWith("blockdust_")) {
            String[] d = name.split("_");
            if (d.length >= 4) {
                return new DustParticle(pos, Integer.parseInt(d[1]) & 0xff, Integer.parseInt(d[2]) & 0xff,
                        Integer.parseInt(d[3]) & 0xff, d.length >= 5 ? Integer.parseInt(d[4]) & 0xff : 255);
            }
        }

        return null;
    }
}
